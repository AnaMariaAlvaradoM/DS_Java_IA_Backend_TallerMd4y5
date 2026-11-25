package com.devsenior.sillon.chat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosIaServicio;
import com.devsenior.sillon.ia.LectorJson;
import com.devsenior.sillon.ia.RespuestaIa;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;

import tools.jackson.databind.JsonNode;

@Service
public class ChatServicio {

    private static final String MENSAJE_SEGURO =
            "Con gusto te pondremos en contacto con nuestro equipo para brindarte atención personalizada y resolver todas tus dudas.";

    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final SolicitudContactoRepository solicitudRepository;
    private final AsistenteIa asistenteIa;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final LectorJson lectorJson;
    private final CreditosIaServicio creditosIaServicio;
    private final Clock reloj;

    public ChatServicio(SalonRepository salonRepository,
                        ServicioRepository servicioRepository,
                        SolicitudContactoRepository solicitudRepository,
                        AsistenteIa asistenteIa,
                        BibliotecaPrompts bibliotecaPrompts,
                        LectorJson lectorJson,
                        CreditosIaServicio creditosIaServicio,
                        Clock reloj) {
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.solicitudRepository = solicitudRepository;
        this.asistenteIa = asistenteIa;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.lectorJson = lectorJson;
        this.creditosIaServicio = creditosIaServicio;
        this.reloj = reloj;
    }

    public ChatRespuesta procesarChat(String slug, ChatPeticion peticion) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado con slug: " + slug));

        creditosIaServicio.verificarCreditos(salon);

        String horarioTexto = formatearHorario(salon.getConfiguracion());
        String catalogoTexto = armarCatalogo(salon.getId());
        String direccionTexto = (salon.getDireccion() != null ? salon.getDireccion() : "")
                + (salon.getCiudad() != null ? ", " + salon.getCiudad() : "");

        String instrucciones = bibliotecaPrompts.obtenerPrompt("chatbot", Map.of(
                "salon", salon.getNombre(),
                "direccion", direccionTexto,
                "horario", horarioTexto,
                "catalogo", catalogoTexto,
                "cliente", peticion.clienteNombre()
        ));

        String historialTexto = armarHistorial(peticion.historial());

        RespuestaIa respuestaIa = asistenteIa.completar(instrucciones, historialTexto);

        creditosIaServicio.consumirCredito(salon.getId());

        JsonNode nodo = lectorJson.leerJson(respuestaIa.texto(), "respuesta", "escalar");
        String respuesta = nodo.get("respuesta").asString();
        boolean escalar = nodo.get("escalar").asBoolean();

        if (respuesta == null || respuesta.isBlank()) {
            throw new PasarelaException("La respuesta del chatbot no puede estar vacía.");
        }

        MotivoSolicitud motivo = null;
        if (contienePromesaProhibida(respuesta)) {
            respuesta = MENSAJE_SEGURO;
            escalar = true;
            motivo = MotivoSolicitud.REGLA_DE_SEGURIDAD;
        } else if (escalar) {
            motivo = MotivoSolicitud.PEDIDO_POR_IA;
        }

        Long solicitudId = null;
        if (escalar) {
            String mensajesCliente = extraerMensajesCliente(peticion.historial());

            SolicitudContacto solicitud = new SolicitudContacto();
            solicitud.setSalon(salon);
            solicitud.setClienteNombre(peticion.clienteNombre());
            solicitud.setClienteContacto(peticion.clienteContacto());
            solicitud.setMensajes(mensajesCliente);
            solicitud.setMotivo(motivo);
            solicitud.setEstado(EstadoSolicitud.PENDIENTE);
            solicitud.setFechaCreacion(LocalDateTime.now(reloj));

            SolicitudContacto guardada = solicitudRepository.save(solicitud);
            solicitudId = guardada.getId();
        }

        return new ChatRespuesta(
                respuesta,
                escalar,
                solicitudId,
                respuestaIa.factura()
        );
    }

    public static boolean contienePromesaProhibida(String texto) {
        if (texto == null) {
            return false;
        }
        String t = texto.toLowerCase();
        return t.contains("descuento")
                || t.contains("cupón")
                || t.contains("cupon")
                || t.contains("gratis")
                || t.contains("promoción")
                || t.contains("promocion")
                || t.contains("reembols");
    }

    private String armarCatalogo(Long salonId) {
        List<Servicio> servicios = servicioRepository.findBySalonIdAndActivoTrue(salonId);
        if (servicios.isEmpty()) {
            return "No hay servicios disponibles en este momento.";
        }
        return servicios.stream()
                .map(s -> "- " + s.getNombre() + ": " + s.getDuracionMinutos() + " minutos, $"
                        + String.format(Locale.of("es", "CO"), "%,d", s.getPrecio()).replace(',', '.'))
                .collect(Collectors.joining("\n"));
    }

    private String armarHistorial(List<TurnoPeticion> historial) {
        return historial.stream()
                .map(t -> (t.rol() == RolTurno.CLIENTE ? "Cliente: " : "Asistente: ") + t.texto())
                .collect(Collectors.joining("\n"));
    }

    private String extraerMensajesCliente(List<TurnoPeticion> historial) {
        String mensajes = historial.stream()
                .filter(t -> t.rol() == RolTurno.CLIENTE)
                .map(TurnoPeticion::texto)
                .collect(Collectors.joining("\n"));

        if (mensajes.length() > 2000) {
            return mensajes.substring(0, 2000);
        }
        return mensajes;
    }

    private String formatearHorario(ConfiguracionSalon config) {
        if (config == null) {
            return "Lunes a sábado, horario estándar";
        }
        String dias = config.isAbreDomingos()
                ? "Lunes a domingo"
                : "Lunes a sábado (domingos cerrado)";

        String apertura = formatearHora(config.getHoraApertura());
        String cierre = formatearHora(config.getHoraCierre());

        return String.format("%s, de %s a %s", dias, apertura, cierre);
    }

    private String formatearHora(LocalTime hora) {
        if (hora == null) {
            return "";
        }
        return DateTimeFormatter.ofPattern("h:mm a", Locale.of("es", "CO"))
                .format(hora)
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ');
    }
}
