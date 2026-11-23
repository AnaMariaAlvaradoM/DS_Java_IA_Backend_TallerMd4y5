package com.devsenior.sillon.chat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.dto.FacturaIA;
import com.devsenior.sillon.publico.PublicoServicio;
import com.devsenior.sillon.salon.CreditoIaServicio;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class ChatServicio {

    private static final String PROMPT = "chatbot";
    private static final String MENSAJE_SALON_NO_ENCONTRADO = "Salón no encontrado";
    private static final int MAXIMO_MENSAJES = 2000;
    private static final List<String> PROMESAS_PROHIBIDAS = List.of(
            "descuento", "cupón", "cupon", "gratis", "promoción", "promocion", "reembols");
    private static final String RESPUESTA_SEGURA =
            "Una persona de nuestro equipo revisará su solicitud y lo contactará pronto.";

    private final SalonRepository salonRepository;
    private final PublicoServicio publicoServicio;
    private final SolicitudServicio solicitudServicio;
    private final CreditoIaServicio creditoIaServicio;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final AsistenteIA asistenteIA;
    private final LectorJsonIA lectorJsonIA;

    public ChatServicio(SalonRepository salonRepository, PublicoServicio publicoServicio,
            SolicitudServicio solicitudServicio, CreditoIaServicio creditoIaServicio,
            BibliotecaPrompts bibliotecaPrompts, AsistenteIA asistenteIA, LectorJsonIA lectorJsonIA) {
        this.salonRepository = salonRepository;
        this.publicoServicio = publicoServicio;
        this.solicitudServicio = solicitudServicio;
        this.creditoIaServicio = creditoIaServicio;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.asistenteIA = asistenteIA;
        this.lectorJsonIA = lectorJsonIA;
    }

    public ChatRespuesta responder(String slug, ChatSolicitud solicitud) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_SALON_NO_ENCONTRADO));
        creditoIaServicio.verificar(salon.getId());
        String instrucciones = bibliotecaPrompts.cargar(PROMPT, Map.of(
                "salon", salon.getNombre(),
                "direccion", salon.getDireccion() + ", " + salon.getCiudad(),
                "horario", publicoServicio.textoHorario(slug),
                "catalogo", publicoServicio.textoCatalogo(slug),
                "cliente", solicitud.clienteNombre()));
        RespuestaIA respuesta = asistenteIA.completar(instrucciones,
                comoConversacion(solicitud.historial()));
        creditoIaServicio.consumir(salon.getId());
        RespuestaChatbotIA chatbot = lectorJsonIA.leer(respuesta, RespuestaChatbotIA.class);
        validar(chatbot);
        if (prometeAlgoNoPermitido(chatbot.respuesta())) {
            Long solicitudId = solicitudServicio.crear(salon.getId(), solicitud.clienteNombre(),
                    solicitud.clienteContacto(), mensajesDelCliente(solicitud),
                    MotivoSolicitud.REGLA_DE_SEGURIDAD);
            return new ChatRespuesta(RESPUESTA_SEGURA, true, solicitudId, FacturaIA.de(respuesta));
        }
        Long solicitudId = chatbot.escalar()
                ? solicitudServicio.crear(salon.getId(), solicitud.clienteNombre(),
                        solicitud.clienteContacto(), mensajesDelCliente(solicitud),
                        MotivoSolicitud.PEDIDO_POR_IA)
                : null;
        return new ChatRespuesta(chatbot.respuesta(), chatbot.escalar(), solicitudId,
                FacturaIA.de(respuesta));
    }

    private void validar(RespuestaChatbotIA chatbot) {
        if (chatbot.respuesta() == null || chatbot.respuesta().isBlank()) {
            throw new RespuestaIAInvalidaException("La respuesta del chatbot está vacía");
        }
    }

    private boolean prometeAlgoNoPermitido(String respuesta) {
        String texto = respuesta.toLowerCase(Locale.ROOT);
        return PROMESAS_PROHIBIDAS.stream().anyMatch(texto::contains);
    }

    private static String comoConversacion(List<ChatSolicitud.Turno> historial) {
        return historial.stream()
                .map(turno -> (turno.rol() == ChatSolicitud.Rol.CLIENTE ? "Cliente: "
                        : "Asistente: ") + turno.texto())
                .collect(Collectors.joining("\n"));
    }

    private static String mensajesDelCliente(ChatSolicitud solicitud) {
        String mensajes = solicitud.historial().stream()
                .filter(turno -> turno.rol() == ChatSolicitud.Rol.CLIENTE)
                .map(ChatSolicitud.Turno::texto)
                .collect(Collectors.joining(" | "));
        if (mensajes.length() > MAXIMO_MENSAJES) {
            mensajes = mensajes.substring(0, MAXIMO_MENSAJES);
        }
        return mensajes;
    }
}
