package com.devsenior.sillon.ia.servicio;

import com.devsenior.sillon.compartido.excepcion.ErrorPasarelaException;
import com.devsenior.sillon.ia.dto.ChatRequest;
import com.devsenior.sillon.ia.dto.ChatResponse;
import com.devsenior.sillon.ia.dto.RespuestaIa;
import com.devsenior.sillon.ia.util.BibliotecaPrompts;
import com.devsenior.sillon.ia.util.LectorJsonIa;
import com.devsenior.sillon.salon.dto.CatalogoPublicoResponse;
import com.devsenior.sillon.salon.servicio.CatalogoService;
import com.devsenior.sillon.solicitud.modelo.MotivoSolicitud;
import com.devsenior.sillon.solicitud.servicio.SolicitudService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatbotIaService {

    private static final List<String> PALABRAS_PROHIBIDAS = List.of(
            "descuento", "cupón", "cupon", "gratis", "promoción", "promocion", "reembols", "silla50");

    private static final String MENSAJE_SEGURO = "Con gusto una persona de nuestro equipo le atenderá personalmente para brindarle información.";

    private final CatalogoService catalogoService;
    private final CreditosIaService creditosIaService;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final AsistenteIaService asistenteIaService;
    private final LectorJsonIa lectorJsonIa;
    private final SolicitudService solicitudService;

    public ChatbotIaService(
            CatalogoService catalogoService,
            CreditosIaService creditosIaService,
            BibliotecaPrompts bibliotecaPrompts,
            AsistenteIaService asistenteIaService,
            LectorJsonIa lectorJsonIa,
            SolicitudService solicitudService) {
        this.catalogoService = catalogoService;
        this.creditosIaService = creditosIaService;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.asistenteIaService = asistenteIaService;
        this.lectorJsonIa = lectorJsonIa;
        this.solicitudService = solicitudService;
    }

    public ChatResponse responder(String slug, ChatRequest request) {
        // 1. Validar salón y armar catálogo real
        Long salonId = catalogoService.obtenerSalonIdPorSlug(slug);
        CatalogoPublicoResponse catalogo = catalogoService.obtenerCatalogo(slug);

        // 2. Verificar créditos
        creditosIaService.verificar(salonId);

        // 3. Cargar prompt con datos reales y convertir historial
        String textoCatalogo = catalogo.servicios().stream()
                .map(s -> "- " + s.nombre() + " (" + s.duracionMinutos() + " min): $" + s.precio())
                .collect(Collectors.joining("\n"));

        String instrucciones = bibliotecaPrompts.cargar("chatbot.txt", Map.of(
                "salon", catalogo.nombre(),
                "direccion", catalogo.direccion() + ", " + catalogo.ciudad(),
                "horario", catalogo.horario(),
                "catalogo", textoCatalogo.isBlank() ? "Sin servicios activos" : textoCatalogo,
                "cliente", request.clienteNombre()));

        String conversacion = request.historial().stream()
                .map(t -> (t.rol() == ChatRequest.RolChat.CLIENTE ? "Cliente: " : "Asistente: ") + t.texto())
                .collect(Collectors.joining("\n"));

        RespuestaIa respuestaIa = asistenteIaService.generar(instrucciones, conversacion);

        // 4. Consumir crédito
        creditosIaService.consumir(salonId);

        // 5. Leer y validar JSON
        JsonNode json = lectorJsonIa.leerJson(respuestaIa.contenido());
        String textoRespuesta = json.path("respuesta").asText("").trim();
        if (textoRespuesta.isEmpty()) {
            throw new ErrorPasarelaException("La IA generó una respuesta vacía");
        }
        boolean escalarIa = json.path("escalar").asBoolean(false);

        // 6. Regla anti-promesas en Java (garantía en código)
        String lower = textoRespuesta.toLowerCase(Locale.ROOT);
        boolean violaSeguridad = PALABRAS_PROHIBIDAS.stream().anyMatch(lower::contains);

        Long solicitudId = null;
        boolean escalado = escalarIa;

        if (violaSeguridad) {
            textoRespuesta = MENSAJE_SEGURO;
            escalado = true;
            solicitudId = solicitudService.crear(
                    salonId,
                    request.clienteNombre(),
                    request.clienteContacto(),
                    extraerMensajesCliente(request),
                    MotivoSolicitud.REGLA_DE_SEGURIDAD);
        } else if (escalarIa) {
            solicitudId = solicitudService.crear(
                    salonId,
                    request.clienteNombre(),
                    request.clienteContacto(),
                    extraerMensajesCliente(request),
                    MotivoSolicitud.PEDIDO_POR_IA);
        }

        return new ChatResponse(textoRespuesta, escalado, solicitudId, respuestaIa.factura());
    }

    private String extraerMensajesCliente(ChatRequest request) {
        return request.historial().stream()
                .filter(t -> t.rol() == ChatRequest.RolChat.CLIENTE)
                .map(ChatRequest.TurnoChat::texto)
                .collect(Collectors.joining(" | "));
    }
}