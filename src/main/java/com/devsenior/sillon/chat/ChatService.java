package com.devsenior.sillon.chat;

import com.devsenior.sillon.chat.dto.ChatRequest;
import com.devsenior.sillon.chat.dto.ChatRespuesta;
import com.devsenior.sillon.chat.dto.SolicitudContactoRespuesta;
import com.devsenior.sillon.chat.dto.TurnoChatDTO;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.PasarelaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.ia.MotorIaService;
import com.devsenior.sillon.ia.PromptService;
import com.devsenior.sillon.ia.dto.RespuestaIa;
import com.devsenior.sillon.publico.PublicoService;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    public static final String MENSAJE_SEGURO_ANTIPROMESAS =
            "Por políticas del salón no puedo ofrecer ese beneficio por este medio. " +
                    "He registrado tu solicitud para que una persona del salón se comunique contigo.";

    private static final List<String> PALABRAS_PROHIBIDAS = List.of(
            "cupon", "descuento", "gratis", "gratuito", "gratuita",
            "promocion", "promo", "reembolso", "cortesia", "regalo",
            "2x1", "rebaja", "mitad de precio"
    );

    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudContactoRepository solicitudContactoRepository;
    private final MotorIaService motorIaService;
    private final PromptService promptService;
    private final Clock clock;

    public ChatService(
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository,
            SolicitudContactoRepository solicitudContactoRepository,
            MotorIaService motorIaService,
            PromptService promptService,
            Clock clock
    ) {
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.solicitudContactoRepository = solicitudContactoRepository;
        this.motorIaService = motorIaService;
        this.promptService = promptService;
        this.clock = clock;
    }

    @Transactional
    public ChatRespuesta conversar(String slug, ChatRequest request) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        if (salon.getCreditosIa() <= 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA disponibles");
        }

        List<Servicio> serviciosActivos = servicioRepository.findBySalonIdAndActivoTrue(salon.getId());
        String serviciosTexto = serviciosActivos.isEmpty()
                ? "Sin servicios activos registrados"
                : serviciosActivos.stream()
                        .map(s -> String.format("- %s: $%d COP (%d minutos)%s",
                                s.getNombre(),
                                s.getPrecio(),
                                s.getDuracionMinutos(),
                                s.getDescripcion() != null && !s.getDescripcion().isBlank()
                                        ? " - " + s.getDescripcion() : ""))
                        .collect(Collectors.joining("\n"));

        List<Usuario> estilistasActivos = usuarioRepository.findBySalonIdAndRol(salon.getId(), Rol.ESTILISTA)
                .stream()
                .filter(Usuario::isActivo)
                .toList();
        String estilistasTexto = estilistasActivos.isEmpty()
                ? "Sin estilistas activos registrados"
                : estilistasActivos.stream()
                        .map(u -> "- " + u.getNombre() + " (servicios: " +
                                u.getServicios().stream().map(Servicio::getNombre).collect(Collectors.joining(", ")) + ")")
                        .collect(Collectors.joining("\n"));

        String historialTexto = request.historial().stream()
                .map(t -> t.rol().name() + ": " + t.texto().trim())
                .collect(Collectors.joining("\n"));

        Map<String, String> variables = Map.of(
                "salonNombre", salon.getNombre(),
                "direccion", salon.getDireccion(),
                "ciudad", salon.getCiudad(),
                "telefono", salon.getTelefono(),
                "horario", PublicoService.formatearHorario(salon.getConfiguracion()),
                "servicios", serviciosTexto,
                "estilistas", estilistasTexto,
                "clienteNombre", request.clienteNombre().trim(),
                "clienteContacto", request.clienteContacto().trim(),
                "historial", historialTexto
        );

        String prompt = promptService.cargarYRenderizar("chatbot.txt", variables);
        RespuestaIa respuestaIa = motorIaService.ejecutarConFallback(
                "Eres el chatbot del salón. Responde siempre en JSON con respuesta y escalar.",
                prompt
        );

        JsonNode json = motorIaService.extraerYValidarJson(respuestaIa.contenido());
        if (!json.hasNonNull("respuesta") || json.get("respuesta").asText().isBlank()) {
            throw new PasarelaException("La respuesta JSON del chatbot no contiene el campo respuesta");
        }

        Boolean escalarPorIa = null;
        if (json.hasNonNull("escalar") && json.get("escalar").isBoolean()) {
            escalarPorIa = json.get("escalar").asBoolean();
        } else if (json.hasNonNull("escalado") && json.get("escalado").isBoolean()) {
            escalarPorIa = json.get("escalado").asBoolean();
        }

        if (escalarPorIa == null) {
            throw new PasarelaException("La respuesta JSON del chatbot no contiene el campo escalar booleano");
        }

        String textoRespuesta = json.get("respuesta").asText().trim();

        salon.setCreditosIa(salon.getCreditosIa() - 1);
        salonRepository.save(salon);

        String mensajesCliente = extraerMensajesCliente(request.historial());

        if (contienePromesaProhibida(textoRespuesta)) {
            SolicitudContacto solicitud = crearSolicitud(
                    salon,
                    request.clienteNombre().trim(),
                    request.clienteContacto().trim(),
                    mensajesCliente,
                    MotivoSolicitud.REGLA_DE_SEGURIDAD
            );
            return new ChatRespuesta(MENSAJE_SEGURO_ANTIPROMESAS, true, solicitud.getId(), respuestaIa.factura());
        }

        if (escalarPorIa) {
            SolicitudContacto solicitud = crearSolicitud(
                    salon,
                    request.clienteNombre().trim(),
                    request.clienteContacto().trim(),
                    mensajesCliente,
                    MotivoSolicitud.PEDIDO_POR_IA
            );
            return new ChatRespuesta(textoRespuesta, true, solicitud.getId(), respuestaIa.factura());
        }

        return new ChatRespuesta(textoRespuesta, false, null, respuestaIa.factura());
    }

    @Transactional(readOnly = true)
    public List<SolicitudContactoRespuesta> listarSolicitudes(Long salonId) {
        return solicitudContactoRepository.findBySalonIdOrderByFechaCreacionDesc(salonId).stream()
                .map(this::mapearARespuesta)
                .toList();
    }

    @Transactional
    public SolicitudContactoRespuesta marcarComoAtendida(Long salonId, Long solicitudId) {
        SolicitudContacto solicitud = solicitudContactoRepository.findByIdAndSalonId(solicitudId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de contacto no encontrada"));

        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ReglaNegocioException("La solicitud ya fue marcada como atendida");
        }

        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        solicitud = solicitudContactoRepository.save(solicitud);
        return mapearARespuesta(solicitud);
    }

    public boolean contienePromesaProhibida(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();

        for (String palabra : PALABRAS_PROHIBIDAS) {
            if (normalizado.contains(palabra)) {
                return true;
            }
        }
        return false;
    }

    private String extraerMensajesCliente(List<TurnoChatDTO> historial) {
        return historial.stream()
                .map(t -> t.rol().name() + ": " + t.texto().trim())
                .collect(Collectors.joining(" | "));
    }

    private SolicitudContacto crearSolicitud(
            Salon salon,
            String clienteNombre,
            String clienteContacto,
            String mensajes,
            MotivoSolicitud motivo
    ) {
        SolicitudContacto solicitud = new SolicitudContacto();
        solicitud.setSalon(salon);
        solicitud.setClienteNombre(clienteNombre);
        solicitud.setClienteContacto(clienteContacto);
        solicitud.setMensajes(mensajes);
        solicitud.setMotivo(motivo);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaCreacion(LocalDateTime.now(clock));
        return solicitudContactoRepository.save(solicitud);
    }

    private SolicitudContactoRespuesta mapearARespuesta(SolicitudContacto solicitud) {
        return new SolicitudContactoRespuesta(
                solicitud.getId(),
                solicitud.getClienteNombre(),
                solicitud.getClienteContacto(),
                solicitud.getMensajes(),
                solicitud.getMotivo(),
                solicitud.getEstado(),
                solicitud.getFechaCreacion()
        );
    }
}
