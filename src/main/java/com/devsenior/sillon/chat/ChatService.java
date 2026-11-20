package com.devsenior.sillon.chat;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosIaService;
import com.devsenior.sillon.ia.LectorJsonIa;
import com.devsenior.sillon.ia.RespuestaIa;
import com.devsenior.sillon.publico.CatalogoResponse;
import com.devsenior.sillon.publico.CatalogoService;
import com.devsenior.sillon.salon.SalonRepository;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
public class ChatService {
    public static final String RESPUESTA_SEGURA =
            "No puedo confirmar descuentos, promociones ni reembolsos. Un miembro del equipo revisará su consulta y se comunicará con usted.";

    private final SalonRepository salones;
    private final CatalogoService catalogo;
    private final CreditosIaService creditos;
    private final BibliotecaPrompts prompts;
    private final AsistenteIa ia;
    private final LectorJsonIa lector;
    private final SolicitudService solicitudes;

    public ChatService(SalonRepository salones, CatalogoService catalogo, CreditosIaService creditos,
                       BibliotecaPrompts prompts, AsistenteIa ia, LectorJsonIa lector,
                       SolicitudService solicitudes) {
        this.salones = salones;
        this.catalogo = catalogo;
        this.creditos = creditos;
        this.prompts = prompts;
        this.ia = ia;
        this.lector = lector;
        this.solicitudes = solicitudes;
    }

    public ChatRespuesta responder(String slug, ChatSolicitud solicitud) {
        Long salonId = salones.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado")).getId();
        CatalogoResponse datos = catalogo.obtener(slug);
        creditos.verificar(salonId);
        String instrucciones = prompts.cargar("chatbot", Map.of(
                "salon", datos.nombre(), "direccion", datos.direccion(), "horario", datos.horario(),
                "catalogo", describirServicios(datos)));
        String historial = solicitud.historial().stream()
                .map(turno -> (turno.rol() == RolTurno.CLIENTE ? "Cliente: " : "Asistente: ") + turno.texto())
                .reduce((a, b) -> a + "\n" + b).orElse("");
        RespuestaIa generada = ia.generar(instrucciones, "Cliente: " + solicitud.clienteNombre() + "\n" + historial);
        creditos.consumir(salonId);
        JsonNode objeto = lector.leerObjeto(generada.texto());
        String respuesta = lector.textoObligatorio(objeto, "respuesta", 700);
        boolean escalar = lector.booleanoObligatorio(objeto, "escalar");
        MotivoSolicitud motivo = null;
        if (violaSeguridad(respuesta)) {
            respuesta = RESPUESTA_SEGURA;
            escalar = true;
            motivo = MotivoSolicitud.REGLA_DE_SEGURIDAD;
        } else if (escalar) {
            motivo = MotivoSolicitud.PEDIDO_POR_IA;
        }
        Long solicitudId = null;
        if (escalar) {
            String mensajes = solicitud.historial().stream()
                    .filter(turno -> turno.rol() == RolTurno.CLIENTE)
                    .map(TurnoChat::texto).reduce((a, b) -> a + "\n" + b).orElse("");
            solicitudId = solicitudes.crear(salonId, solicitud.clienteNombre(),
                    solicitud.clienteContacto(), mensajes, motivo);
        }
        return new ChatRespuesta(respuesta, escalar, solicitudId, generada.factura());
    }

    private String describirServicios(CatalogoResponse datos) {
        if (datos.servicios().isEmpty()) {
            return "Sin servicios activos.";
        }
        return datos.servicios().stream()
                .map(servicio -> servicio.nombre() + ": " + servicio.duracionMinutos() +
                        " minutos, $" + servicio.precio())
                .reduce((a, b) -> a + "\n" + b).orElse("");
    }

    private boolean violaSeguridad(String respuesta) {
        String texto = respuesta.toLowerCase(Locale.ROOT);
        return texto.contains("descuento") || texto.contains("cupón") || texto.contains("cupon")
                || texto.contains("gratis") || texto.contains("promoción") || texto.contains("promocion")
                || texto.contains("reembols");
    }
}
