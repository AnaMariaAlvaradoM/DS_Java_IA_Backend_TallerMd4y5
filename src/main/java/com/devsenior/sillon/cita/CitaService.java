package com.devsenior.sillon.cita;

import com.devsenior.sillon.cita.dto.CambiarEstadoCitaRequest;
import com.devsenior.sillon.cita.dto.CitaRespuesta;
import com.devsenior.sillon.cita.dto.CrearCitaRequest;
import com.devsenior.sillon.cita.dto.RecordatorioRespuesta;
import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.excepcion.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.PasarelaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.ia.MotorIaService;
import com.devsenior.sillon.ia.PromptService;
import com.devsenior.sillon.ia.dto.RespuestaIa;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CitaService {

    private final CitaRepository citaRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteService clienteService;
    private final ReglasAgenda reglasAgenda;
    private final MotorIaService motorIaService;
    private final PromptService promptService;
    private final Clock clock;

    public CitaService(
            CitaRepository citaRepository,
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository,
            ClienteService clienteService,
            ReglasAgenda reglasAgenda,
            MotorIaService motorIaService,
            PromptService promptService,
            Clock clock
    ) {
        this.citaRepository = citaRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteService = clienteService;
        this.reglasAgenda = reglasAgenda;
        this.motorIaService = motorIaService;
        this.promptService = promptService;
        this.clock = clock;
    }

    @Transactional
    public CitaRespuesta agendarCitaSalon(Long salonId, CrearCitaRequest request) {
        Cita cita = agendarInterno(salonId, request, OrigenCita.SALON, EstadoCita.CONFIRMADA);
        return mapearARespuesta(cita);
    }

    @Transactional
    public Cita agendarInterno(Long salonId, CrearCitaRequest request, OrigenCita origen, EstadoCita estadoInicial) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        // 1. El servicio existe en el salón y está activo
        Servicio servicio = servicioRepository.findByIdAndSalonId(request.servicioId(), salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado en este salón"));
        if (!servicio.isActivo()) {
            throw new ReglaNegocioException("El servicio no está activo para agendamiento");
        }

        // 2. El estilista existe en el salón, está activo y ofrece ese servicio
        Usuario estilista = usuarioRepository.findById(request.estilistaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estilista no encontrado"));
        if (!estilista.getSalon().getId().equals(salonId)) {
            throw new RecursoNoEncontradoException("Estilista no pertenece a este salón");
        }
        if (!estilista.isActivo()) {
            throw new ReglaNegocioException("El estilista no está activo");
        }
        if (!estilista.ofreceServicio(servicio.getId())) {
            throw new ReglaNegocioException("El estilista no ofrece el servicio seleccionado");
        }

        // 3. Se calcula el fin con la duración del servicio
        LocalDateTime fin = request.inicio().plusMinutes(servicio.getDuracionMinutos());

        // 4. La cita no está en el pasado (con el reloj del proyecto)
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!request.inicio().isAfter(ahora)) {
            throw new ReglaNegocioException("No se puede agendar una cita en el pasado");
        }

        // 5. Cumple el horario del salón
        reglasAgenda.validarHorario(request.inicio(), fin, salon.getConfiguracion());

        // 6. No se cruza con otra cita del estilista que ocupe agenda
        List<EstadoCita> estadosOcupan = List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);
        List<Cita> cruzadas = citaRepository.findCitasCruzadas(salonId, estilista.getId(), estadosOcupan, request.inicio(), fin);
        if (!cruzadas.isEmpty()) {
            throw new ReglaNegocioException("El estilista ya tiene una cita agendada en ese horario que genera cruce");
        }

        // 7. Se busca o crea el cliente, se copia el precio y se guarda
        Cliente cliente = clienteService.buscarOCrear(salonId, request.clienteNombre(), request.clienteTelefono());

        Cita cita = new Cita();
        cita.setInicio(request.inicio());
        cita.setFin(fin);
        cita.setPrecio(servicio.getPrecio());
        cita.setEstado(estadoInicial);
        cita.setOrigen(origen);
        cita.setNota(request.nota() != null ? request.nota().trim() : null);
        cita.setFechaCreacion(ahora);
        cita.setFechaActualizacion(ahora);
        cita.setSalon(salon);
        cita.setCliente(cliente);
        cita.setEstilista(estilista);
        cita.setServicio(servicio);

        return citaRepository.save(cita);
    }

    @Transactional(readOnly = true)
    public List<CitaRespuesta> listarCitasPorDia(UsuarioPrincipal principal, LocalDate fecha) {
        LocalDate dia = (fecha != null) ? fecha : LocalDate.now(clock);
        LocalDateTime desde = dia.atStartOfDay();
        LocalDateTime hasta = dia.plusDays(1).atStartOfDay();

        List<Cita> citas;
        if (principal.getRol() == Rol.ESTILISTA) {
            citas = citaRepository.findBySalonIdAndEstilistaIdAndDia(principal.getSalonId(), principal.getId(), desde, hasta);
        } else {
            citas = citaRepository.findBySalonIdAndDia(principal.getSalonId(), desde, hasta);
        }

        return citas.stream()
                .map(this::mapearARespuesta)
                .toList();
    }

    @Transactional
    public CitaRespuesta cambiarEstadoCita(UsuarioPrincipal principal, Long citaId, CambiarEstadoCitaRequest request) {
        Cita cita = citaRepository.findByIdAndSalonId(citaId, principal.getSalonId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));

        // Si es estilista, debe ser suya y solo puede cambiar a COMPLETADA o NO_ASISTIO
        if (principal.getRol() == Rol.ESTILISTA) {
            if (!cita.getEstilista().getId().equals(principal.getId())) {
                throw new RecursoNoEncontradoException("Cita no encontrada");
            }
            if (request.estado() != EstadoCita.COMPLETADA && request.estado() != EstadoCita.NO_ASISTIO) {
                throw new AccionNoPermitidaException("Un estilista solo puede cambiar sus citas a COMPLETADA o NO_ASISTIO");
            }
        }

        // Validar transición válida de la máquina de estados
        if (!cita.getEstado().puedeTransicionarA(request.estado())) {
            throw new ReglaNegocioException("Transición no permitida de " + cita.getEstado() + " a " + request.estado());
        }

        cita.setEstado(request.estado());
        cita.setFechaActualizacion(LocalDateTime.now(clock));

        cita = citaRepository.saveAndFlush(cita);
        return mapearARespuesta(cita);
    }

    @Transactional
    public RecordatorioRespuesta generarRecordatorio(Long salonId, Long citaId) {
        Cita cita = citaRepository.findByIdAndSalonId(citaId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));

        if (cita.getEstado() != EstadoCita.PENDIENTE && cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new ReglaNegocioException("Solo se pueden generar recordatorios para citas en estado PENDIENTE o CONFIRMADA");
        }

        Salon salon = cita.getSalon();
        if (salon.getCreditosIa() <= 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA disponibles");
        }

        Map<String, String> variables = Map.of(
                "salonNombre", salon.getNombre(),
                "clienteNombre", cita.getCliente().getNombre(),
                "servicio", cita.getServicio().getNombre(),
                "estilista", cita.getEstilista().getNombre(),
                "fecha", cita.getInicio().toLocalDate().toString(),
                "hora", cita.getInicio().toLocalTime().toString(),
                "valor", String.valueOf(cita.getPrecio()),
                "direccion", salon.getDireccion(),
                "ciudad", salon.getCiudad()
        );

        String prompt = promptService.cargarYRenderizar("recordatorio.txt", variables);
        RespuestaIa respuestaIa = motorIaService.ejecutarConFallback(
                "Eres un asistente que responde solo en JSON con el campo mensaje.",
                prompt
        );

        JsonNode json = motorIaService.extraerYValidarJson(respuestaIa.contenido());
        if (!json.hasNonNull("mensaje") || json.get("mensaje").asText().isBlank()) {
            throw new PasarelaException("La respuesta de la IA no contiene el campo mensaje");
        }

        String mensaje = json.get("mensaje").asText().trim();
        String nombreEstilista = cita.getEstilista().getNombre();
        String primerNombreEstilista = nombreEstilista.split(" ")[0];
        String horaTexto = cita.getInicio().toLocalTime().toString();
        if (!mensaje.contains(nombreEstilista) && !mensaje.contains(primerNombreEstilista)) {
            mensaje = mensaje + " (Cita de " + cita.getServicio().getNombre() + " con " + nombreEstilista + " el " + cita.getInicio().toLocalDate() + " a las " + horaTexto + ")";
        } else if (!mensaje.contains(horaTexto)) {
            mensaje = mensaje + " (Hora: " + horaTexto + ")";
        }

        salon.setCreditosIa(salon.getCreditosIa() - 1);
        salonRepository.save(salon);

        return new RecordatorioRespuesta(
                cita.getId(),
                cita.getCliente().getTelefono(),
                mensaje,
                salon.getCreditosIa(),
                respuestaIa.factura()
        );
    }

    public CitaRespuesta mapearARespuesta(Cita cita) {
        return new CitaRespuesta(
                cita.getId(),
                cita.getInicio(),
                cita.getFin(),
                cita.getEstado(),
                cita.getOrigen(),
                cita.getPrecio(),
                cita.getServicio().getId(),
                cita.getServicio().getNombre(),
                cita.getEstilista().getId(),
                cita.getEstilista().getNombre(),
                cita.getCliente().getNombre(),
                cita.getCliente().getTelefono(),
                cita.getNota(),
                cita.getFechaCreacion(),
                cita.getFechaActualizacion()
        );
    }
}
