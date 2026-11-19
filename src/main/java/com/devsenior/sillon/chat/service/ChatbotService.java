package com.devsenior.sillon.chat.service;

import com.devsenior.sillon.chat.dto.ChatRequest;
import com.devsenior.sillon.chat.dto.ChatResponse;
import com.devsenior.sillon.chat.dto.ChatbotIA;
import com.devsenior.sillon.chat.dto.TurnoChat;
import com.devsenior.sillon.chat.model.MotivoSolicitud;
import com.devsenior.sillon.chat.model.RolTurno;
import com.devsenior.sillon.ia.dto.FacturaIA;
import com.devsenior.sillon.ia.excepcion.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.model.RespuestaIA;
import com.devsenior.sillon.ia.service.AsistenteIA;
import com.devsenior.sillon.ia.service.BibliotecaPrompts;
import com.devsenior.sillon.ia.service.LectorJsonIA;
import com.devsenior.sillon.publico.service.CatalogoService;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.service.CreditosService;
import com.devsenior.sillon.salon.service.SalonService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Chatbot del salón. Sin @Transactional: la IA no debe ocupar una conexión mientras responde.
 * Orden: salón (404) → créditos (402) → prompt e IA → consumir crédito → validar JSON (502)
 * → regla anti-promesas en Java → escalar si corresponde.
 */
@Service
public class ChatbotService {

    /** Lo que recibe el cliente cuando Java bloquea una promesa indebida. No contiene ninguna palabra prohibida. */
    static final String MENSAJE_SEGURO =
            "Gracias por escribirnos. Una persona de nuestro equipo revisará su solicitud y lo contactará pronto.";

    /** Si la respuesta de la IA contiene alguna de estas palabras, no se le muestra al cliente. */
    private static final List<String> PALABRAS_DE_PROMESA =
            List.of("descuento", "cupón", "cupon", "gratis", "promoción", "promocion", "reembols");

    private final SalonService salonService;
    private final CatalogoService catalogoService;
    private final CreditosService creditosService;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final AsistenteIA asistenteIA;
    private final LectorJsonIA lectorJsonIA;
    private final SolicitudService solicitudService;

    public ChatbotService(SalonService salonService, CatalogoService catalogoService, CreditosService creditosService,
                          BibliotecaPrompts bibliotecaPrompts, AsistenteIA asistenteIA, LectorJsonIA lectorJsonIA,
                          SolicitudService solicitudService) {
        this.salonService = salonService;
        this.catalogoService = catalogoService;
        this.creditosService = creditosService;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.asistenteIA = asistenteIA;
        this.lectorJsonIA = lectorJsonIA;
        this.solicitudService = solicitudService;
    }

    public ChatResponse conversar(String slug, ChatRequest request) {
        Salon salon = salonService.obtenerPorSlug(slug);

        creditosService.verificar(salon.getId());

        String instrucciones = bibliotecaPrompts.cargar("chatbot", Map.of(
                "salon", salon.getNombre(),
                "direccion", salon.getDireccion() + ", " + salon.getCiudad(),
                "horario", salon.getConfiguracion().descripcion(),
                "catalogo", catalogoService.textoCatalogo(salon.getId()),
                "cliente", request.clienteNombre().trim()));
        RespuestaIA respuesta = asistenteIA.completar(instrucciones, conversacion(request.historial()));

        creditosService.consumir(salon.getId());

        ChatbotIA chatbot = lectorJsonIA.leer(respuesta, ChatbotIA.class);
        if (chatbot.respuesta() == null || chatbot.respuesta().isBlank() || chatbot.escalar() == null) {
            throw new RespuestaIAInvalidaException("el chatbot no trae las claves respuesta y escalar");
        }

        if (contienePromesa(chatbot.respuesta())) {
            Long solicitudId = escalar(salon.getId(), request, MotivoSolicitud.REGLA_DE_SEGURIDAD);
            return new ChatResponse(MENSAJE_SEGURO, true, solicitudId, FacturaIA.de(respuesta));
        }
        if (chatbot.escalar()) {
            Long solicitudId = escalar(salon.getId(), request, MotivoSolicitud.PEDIDO_POR_IA);
            return new ChatResponse(chatbot.respuesta().trim(), true, solicitudId, FacturaIA.de(respuesta));
        }
        return new ChatResponse(chatbot.respuesta().trim(), false, null, FacturaIA.de(respuesta));
    }

    /** Un solo mensaje con una línea por turno. El texto del cliente nunca va dentro de las instrucciones. */
    static String conversacion(List<TurnoChat> historial) {
        return historial.stream()
                .map(turno -> (turno.rol() == RolTurno.CLIENTE ? "Cliente: " : "Asistente: ") + turno.texto().trim())
                .collect(Collectors.joining("\n"));
    }

    /** Regla anti-promesas: un prompt pide, solo el código garantiza. Se acepta el falso positivo a propósito. */
    static boolean contienePromesa(String texto) {
        String minusculas = texto.toLowerCase(Locale.ROOT);
        return PALABRAS_DE_PROMESA.stream().anyMatch(minusculas::contains);
    }

    /** La solicitud guarda solo lo que escribió el cliente (máximo 2000 caracteres). */
    private Long escalar(Long salonId, ChatRequest request, MotivoSolicitud motivo) {
        String mensajesDelCliente = request.historial().stream()
                .filter(turno -> turno.rol() == RolTurno.CLIENTE)
                .map(turno -> turno.texto().trim())
                .collect(Collectors.joining("\n"));
        return solicitudService.crear(salonId, request.clienteNombre(), request.clienteContacto(),
                mensajesDelCliente, motivo);
    }
}
