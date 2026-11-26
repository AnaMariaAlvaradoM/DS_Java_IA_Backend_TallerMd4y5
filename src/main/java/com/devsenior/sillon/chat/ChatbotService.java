package com.devsenior.sillon.chat;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditoService;
import com.devsenior.sillon.ia.LectorJson;
import com.devsenior.sillon.ia.RespuestaIa;
import com.devsenior.sillon.publico.CatalogoService;
import com.devsenior.sillon.salon.Salon;

/**
 * Chatbot del salón. Responde con el catálogo real de la base de datos, no agenda ni cancela
 * (escala a una persona) y aplica en Java la regla anti-promesas. Sin @Transactional: la IA puede tardar.
 */
@Service
public class ChatbotService {

    private static final int MAXIMO_RESPUESTA = 1000;

    /** Un prompt pide; solo el código garantiza (Clase 19). Falsos positivos aceptados a propósito. */
    private static final Pattern PROMESA = Pattern.compile(
            "descuento|cupón|cupon|gratis|promoción|promocion|reembols", Pattern.CASE_INSENSITIVE);

    static final String MENSAJE_SEGURO = "Gracias por escribirnos. Una persona del salón revisará su consulta "
            + "y lo contactará pronto.";

    private final CatalogoService catalogoService;
    private final CreditoService creditoService;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final AsistenteIa asistenteIa;
    private final LectorJson lectorJson;
    private final SolicitudService solicitudService;

    public ChatbotService(CatalogoService catalogoService, CreditoService creditoService,
            BibliotecaPrompts bibliotecaPrompts, AsistenteIa asistenteIa, LectorJson lectorJson,
            SolicitudService solicitudService) {
        this.catalogoService = catalogoService;
        this.creditoService = creditoService;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.asistenteIa = asistenteIa;
        this.lectorJson = lectorJson;
        this.solicitudService = solicitudService;
    }

    public ChatRespuesta responder(String slug, ChatPeticion peticion) {
        // 1. Lo que no depende de la IA: el salón existe (404)
        Salon salon = catalogoService.salonPorSlug(slug);
        // 2. Créditos (402 sin llamar a la IA)
        creditoService.verificar(salon.getId());
        // 3. Prompt con el catálogo real y llamada a la IA.
        //    El texto del cliente va como mensaje de usuario, nunca dentro de las instrucciones.
        String instrucciones = bibliotecaPrompts.cargar("chatbot", Map.of(
                "salon", salon.getNombre(),
                "direccion", salon.getDireccion() + ", " + salon.getCiudad(),
                "horario", ReglasAgenda.horarioTexto(salon.getConfiguracion()),
                "catalogo", catalogoService.catalogoTexto(salon.getId()),
                "cliente", peticion.clienteNombre()));
        String conversacion = peticion.historial().stream()
                .map(t -> (t.rol() == RolTurno.CLIENTE ? "Cliente: " : "Asistente: ") + t.texto())
                .collect(Collectors.joining("\n"));
        RespuestaIa respuestaIa = asistenteIa.generar(instrucciones, conversacion);
        // 4. La IA ya respondió: se consume el crédito
        creditoService.consumir(salon.getId());
        // 5. Leer y validar el JSON (502 si no sirve)
        Map<String, Object> json = lectorJson.leer(respuestaIa.texto());
        String respuesta = lectorJson.textoObligatorio(json, "respuesta", MAXIMO_RESPUESTA);
        boolean escalar = lectorJson.booleanoObligatorio(json, "escalar");

        // 6. Reglas de negocio: primero la regla anti-promesas, luego el escalamiento pedido por la IA
        String mensajesCliente = peticion.historial().stream()
                .filter(t -> t.rol() == RolTurno.CLIENTE)
                .map(Turno::texto)
                .collect(Collectors.joining("\n"));
        if (contienePromesa(respuesta)) {
            Long solicitudId = solicitudService.crear(salon.getId(), peticion.clienteNombre(),
                    peticion.clienteContacto(), mensajesCliente, MotivoSolicitud.REGLA_DE_SEGURIDAD);
            return new ChatRespuesta(MENSAJE_SEGURO, true, solicitudId, respuestaIa.factura());
        }
        if (escalar) {
            Long solicitudId = solicitudService.crear(salon.getId(), peticion.clienteNombre(),
                    peticion.clienteContacto(), mensajesCliente, MotivoSolicitud.PEDIDO_POR_IA);
            return new ChatRespuesta(respuesta, true, solicitudId, respuestaIa.factura());
        }
        return new ChatRespuesta(respuesta, false, null, respuestaIa.factura());
    }

    static boolean contienePromesa(String texto) {
        return PROMESA.matcher(texto).find();
    }
}
