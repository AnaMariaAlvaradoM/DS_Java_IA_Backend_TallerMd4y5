package com.devsenior.sillon.cita.servicio;

import com.devsenior.sillon.cita.dto.CitaRequest;
import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.cita.modelo.EstadoCita;
import com.devsenior.sillon.cita.modelo.OrigenCita;
import com.devsenior.sillon.cita.repositorio.CitaRepository;
import com.devsenior.sillon.cliente.modelo.Cliente;
import com.devsenior.sillon.cliente.servicio.ClienteService;
import com.devsenior.sillon.compartido.excepcion.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.servicio.repositorio.ServicioRepository;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.devsenior.sillon.usuario.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class CitaService {

    private static final List<EstadoCita> ESTADOS_OCUPADOS = List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);

    private final CitaRepository citaRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteService clienteService;
    private final ReglasAgenda reglasAgenda;
    private final Clock clock;

    public CitaService(
            CitaRepository citaRepository,
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository,
            ClienteService clienteService,
            ReglasAgenda reglasAgenda,
            Clock clock) {
        this.citaRepository = citaRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteService = clienteService;
        this.reglasAgenda = reglasAgenda;
        this.clock = clock;
    }

    @Transactional
    public CitaResponse crearDesdeSalon(Long salonId, CitaRequest request) {
        return agendar(salonId, request, EstadoCita.CONFIRMADA, OrigenCita.SALON);
    }

    @Transactional
    public CitaResponse crearReservaEnLinea(Long salonId, CitaRequest request) {
        return agendar(salonId, request, EstadoCita.PENDIENTE, OrigenCita.EN_LINEA);
    }

    private CitaResponse agendar(Long salonId, CitaRequest request, EstadoCita estadoInicial, OrigenCita origen) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        // 1. Servicio existe en el salón y está activo
        Servicio servicio = servicioRepository.findByIdAndSalonId(request.servicioId(), salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado en este salón"));
        if (!servicio.isActivo()) {
            throw new ReglaNegocioException("El servicio no está activo");
        }

        // 2. Estilista existe en el salón, está activo y ofrece el servicio
        Usuario estilista = usuarioRepository.findByIdAndSalonIdAndRol(request.estilistaId(), salonId, Rol.ESTILISTA)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estilista no encontrado en este salón"));
        if (!estilista.isActivo()) {
            throw new ReglaNegocioException("El estilista no está activo");
        }
        if (!estilista.ofreceServicio(servicio.getId())) {
            throw new ReglaNegocioException("El estilista no ofrece el servicio seleccionado");
        }

        // 3. Calcular fin según duración del servicio
        LocalDateTime inicio = request.inicio();
        LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos());

        // 4. La cita no puede estar en el pasado
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!inicio.isAfter(ahora)) {
            throw new ReglaNegocioException("No se pueden agendar citas en el pasado");
        }

        // 5. Cumplir horario del salón
        reglasAgenda.validarHorario(inicio, fin, salon.getConfiguracion());

        // 6. Sin cruces con otras citas que ocupen agenda
        boolean seCruza = citaRepository.existsByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                estilista.getId(), ESTADOS_OCUPADOS, fin, inicio);
        if (seCruza) {
            throw new ReglaNegocioException("El estilista ya tiene otra cita programada en ese horario");
        }

        // 7. Buscar o crear cliente, copiar precio y guardar
        Cliente cliente = clienteService.buscarOCrear(salon, request.clienteNombre(), request.clienteTelefono());

        Cita cita = new Cita();
        cita.setSalon(salon);
        cita.setCliente(cliente);
        cita.setEstilista(estilista);
        cita.setServicio(servicio);
        cita.setInicio(inicio);
        cita.setFin(fin);
        cita.setPrecio(servicio.getPrecio());
        cita.setEstado(estadoInicial);
        cita.setOrigen(origen);
        cita.setNota(request.nota() != null ? request.nota().trim() : null);
        cita.setFechaCreacion(ahora);
        cita.setFechaActualizacion(ahora);

        return CitaResponse.desde(citaRepository.save(cita));
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> listarPorDia(UsuarioPrincipal usuario, LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
        LocalDateTime desde = dia.atStartOfDay();
        LocalDateTime hasta = dia.atTime(LocalTime.MAX);

        List<Cita> citas;
        if (usuario.getRol() == Rol.ESTILISTA) {
            citas = citaRepository.findBySalonIdAndEstilistaIdAndInicioBetweenOrderByInicioAsc(
                    usuario.getSalonId(), usuario.getId(), desde, hasta);
        } else {
            citas = citaRepository.findBySalonIdAndInicioBetweenOrderByInicioAsc(
                    usuario.getSalonId(), desde, hasta);
        }

        return citas.stream().map(CitaResponse::desde).toList();
    }

    @Transactional
    public CitaResponse cambiarEstado(UsuarioPrincipal usuario, Long citaId, EstadoCita destino) {
        Cita cita = citaRepository.findByIdAndSalonId(citaId, usuario.getSalonId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));

        if (usuario.getRol() == Rol.ESTILISTA) {
            if (!cita.getEstilista().getId().equals(usuario.getId())) {
                throw new RecursoNoEncontradoException("Cita no encontrada");
            }
            if (destino != EstadoCita.COMPLETADA && destino != EstadoCita.NO_ASISTIO) {
                throw new AccionNoPermitidaException(
                        "Un estilista solo puede marcar citas como COMPLETADA o NO_ASISTIO");
            }
        }

        if (!cita.getEstado().puedeCambiarA(destino)) {
            throw new ReglaNegocioException("No se puede cambiar el estado de " + cita.getEstado() + " a " + destino);
        }

        cita.setEstado(destino);
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!ahora.isAfter(cita.getFechaCreacion())) {
            ahora = cita.getFechaCreacion().plusNanos(1_000_000);
        }
        cita.setFechaActualizacion(ahora);

        return CitaResponse.desde(citaRepository.saveAndFlush(cita));
    }

    public record DatosRecordatorio(
            Long citaId,
            String nombreSalon,
            String nombreCliente,
            String telefonoCliente,
            String datosTexto
    ) {}

    @Transactional(readOnly = true)
    public DatosRecordatorio obtenerDatosRecordatorio(Long salonId, Long citaId) {
        Cita cita = citaRepository.findByIdAndSalonId(citaId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));

        if (!cita.getEstado().ocupaAgenda()) {
            throw new ReglaNegocioException("Solo se pueden generar recordatorios para citas PENDIENTE o CONFIRMADA");
        }

        java.util.Locale locale = java.util.Locale.forLanguageTag("es-CO");
        String fecha = cita.getInicio().format(java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale));
        String hora = cita.getInicio().format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", locale))
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ');
        String valor = "$" + java.text.NumberFormat.getIntegerInstance(locale).format(cita.getPrecio());

        String datos = String.join("\n",
                "Servicio: " + cita.getServicio().getNombre() + " (" + cita.getServicio().getDuracionMinutos() + " minutos)",
                "Estilista: " + cita.getEstilista().getNombre(),
                "Fecha: " + fecha,
                "Hora: " + hora,
                "Valor: " + valor,
                "Dirección: " + cita.getSalon().getDireccion() + ", " + cita.getSalon().getCiudad()
        );

        return new DatosRecordatorio(
                cita.getId(),
                cita.getSalon().getNombre(),
                cita.getCliente().getNombre(),
                cita.getCliente().getTelefono(),
                datos
        );
    }
}