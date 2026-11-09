package com.devsenior.sillon.chat;

import com.devsenior.sillon.ia.FlujoIa;
import com.devsenior.sillon.publico.CatalogoServicio;
import com.devsenior.sillon.publico.ContextoChatSalon;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChatServicio {
    private static final String RESPUESTA_SEGURA =
            "Una persona del salón revisará su solicitud y se pondrá en contacto con usted.";
    private final CatalogoServicio catalogo;
    private final FlujoIa flujo;
    private final SolicitudContactoServicio solicitudes;

    public ChatServicio(CatalogoServicio catalogo, FlujoIa flujo, SolicitudContactoServicio solicitudes) {
        this.catalogo = catalogo;
        this.flujo = flujo;
        this.solicitudes = solicitudes;
    }

    public ChatRespuesta responder(String slug, ChatSolicitud solicitud) {
        ContextoChatSalon contexto = catalogo.contextoChat(slug);
        StringBuilder historial = new StringBuilder();
        StringBuilder mensajesCliente = new StringBuilder();
        for (TurnoChatSolicitud turno : solicitud.historial()) {
            if (!historial.isEmpty()) historial.append('\n');
            historial.append(turno.rol() == RolTurno.CLIENTE ? "Cliente: " : "Asistente: ")
                    .append(turno.texto().replace('\r', ' ').replace('\n', ' '));
            if (turno.rol() == RolTurno.CLIENTE) {
                if (!mensajesCliente.isEmpty()) mensajesCliente.append('\n');
                mensajesCliente.append(turno.texto());
            }
        }
        FlujoIa.Resultado resultado = flujo.ejecutar(() -> { }, contexto.salonId(), "chatbot.txt",
                Map.of("salon", contexto.salon(), "direccion", contexto.direccion(),
                        "horario", contexto.horario(), "catalogo", contexto.catalogo(),
                        "cliente", "Nombre: " + solicitud.clienteNombre() + "\nHistorial:\n" + historial),
                Map.of("respuesta", String.class, "escalar", Boolean.class));
        String respuesta = resultado.contenido().path("respuesta").asText();
        boolean escalar = resultado.contenido().path("escalar").asBoolean();
        MotivoSolicitud motivo = null;
        if (promesaProhibida(respuesta)) {
            respuesta = RESPUESTA_SEGURA;
            motivo = MotivoSolicitud.REGLA_DE_SEGURIDAD;
        } else if (escalar) {
            motivo = MotivoSolicitud.PEDIDO_POR_IA;
        }
        Long solicitudId = motivo == null ? null : solicitudes.crear(contexto.salonId(),
                solicitud.clienteNombre(), solicitud.clienteContacto(),
                mensajesCliente.substring(0, Math.min(2000, mensajesCliente.length())), motivo);
        return new ChatRespuesta(respuesta, motivo != null, solicitudId, resultado.factura());
    }

    private boolean promesaProhibida(String respuesta) {
        String normalizada = Normalizer.normalize(respuesta, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return normalizada.contains("descuento") || normalizada.contains("cupon")
                || normalizada.contains("gratis") || normalizada.contains("promocion")
                || normalizada.contains("reembols") || normalizada.contains("silla50");
    }
}
