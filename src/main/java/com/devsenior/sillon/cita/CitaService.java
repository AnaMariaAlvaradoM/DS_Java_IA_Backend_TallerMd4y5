package com.devsenior.sillon.cita;

import com.devsenior.sillon.cita.dto.CitaRequestDto;
import com.devsenior.sillon.cita.dto.CitaResponseDto;
import com.devsenior.sillon.cita.dto.DatosRecordatorioCita;
import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.ConflictoExcepcion;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class CitaService {

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
    public CitaResponseDto crearCita(Long salonId, CitaRequestDto request) {
        return agendar(salonId, request, EstadoCita.CONFIRMADA, OrigenCita.SALON);
    }

    private CitaResponseDto agendar(Long salonId, CitaRequestDto request, EstadoCita estado, OrigenCita origen) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salon no encontrado"));

        Servicio servicio = servicioRepository.findByIdAndSalonId(request.servicioId(), salonId)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado en este salon"));

        if (!servicio.isActivo()) {
            throw new ConflictoExcepcion("El servicio seleccionado no esta activo");
        }

        Usuario estilista = usuarioRepository.findByIdAndSalonIdAndRol(request.estilistaId(), salonId, Rol.ESTILISTA)
                .orElseThrow(() -> new NoEncontradoException("Estilista no encontrado en este salon"));

        if (!estilista.isActivo()) {
            throw new ConflictoExcepcion("El estilista seleccionado no esta activo");
        }

        if (!estilista.ofreceServicio(servicio.getId())) {
            throw new ConflictoExcepcion("El estilista no ofrece el servicio seleccionado");
        }

        LocalDateTime inicio = request.inicio();
        LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos());

        LocalDateTime ahora = LocalDateTime.now(clock);
        if (inicio.isBefore(ahora)) {
            throw new ConflictoExcepcion("No se pueden agendar citas en una fecha y hora pasada");
        }

        reglasAgenda.validarHorario(salon.getConfiguracion(), inicio, fin);

        List<EstadoCita> estadosOcupanAgenda = List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);
        boolean hayCruce = citaRepository.existsByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                estilista.getId(),
                estadosOcupanAgenda,
                fin,
                inicio
        );

        if (hayCruce) {
            throw new ConflictoExcepcion("El estilista ya tiene una cita agendada en ese horario");
        }

        Cliente cliente = clienteService.buscarOCrear(request.clienteNombre(), request.clienteTelefono(), salon);

        String notaLimpia = request.nota() != null ? request.nota().trim() : null;

        Cita cita = new Cita(
                inicio,
                fin,
                servicio.getPrecio(),
                notaLimpia,
                estado,
                origen,
                salon,
                estilista,
                servicio,
                cliente
        );

        cita.setFechaCreacion(ahora);
        cita.setFechaActualizacion(ahora);

        cita = citaRepository.save(cita);
        return mapearADto(cita);
    }

    private CitaResponseDto mapearADto(Cita cita) {
        return new CitaResponseDto(
                cita.getId(),
                cita.getInicio(),
                cita.getFin(),
                cita.getPrecio(),
                cita.getNota(),
                cita.getEstado(),
                cita.getOrigen(),
                cita.getServicio().getId(),
                cita.getServicio().getNombre(),
                cita.getEstilista().getId(),
                cita.getEstilista().getNombre(),
                cita.getCliente().getNombre(),
                cita.getCliente().getTelefono(),
                cita.getFechaCreacion(),
                cita.getFechaActualizacion()
        );
    }

    @Transactional(readOnly = true)
    public List<CitaResponseDto> listarPorDia(Long salonId, Long usuarioId, Rol rol, LocalDate fecha) {
        LocalDate fechaConsulta = (fecha != null) ? fecha : LocalDate.now(clock);

        LocalDateTime inicioDia = fechaConsulta.atStartOfDay();
        LocalDateTime finDia = fechaConsulta.atTime(23, 59, 59);

        List<Cita> citas;
        if (rol == Rol.DUENO) {
            citas = citaRepository.findBySalonIdAndInicioBetweenOrderByInicioAsc(salonId, inicioDia, finDia);
        } else {
            citas = citaRepository.findBySalonIdAndEstilistaIdAndInicioBetweenOrderByInicioAsc(salonId, usuarioId, inicioDia, finDia);
        }

        return citas.stream()
                .map(this::mapearADto)
                .toList();
    }

    @Transactional
    public CitaResponseDto cambiarEstado(Long salonId, Long usuarioId, Rol rol, Long citaId, EstadoCita nuevoEstado) {
        Cita cita = citaRepository.findByIdAndSalonId(citaId, salonId)
                .orElseThrow(() -> new NoEncontradoException("Cita no encontrada"));

        if (rol == Rol.ESTILISTA) {
            if (!cita.getEstilista().getId().equals(usuarioId)) {
                throw new NoEncontradoException("Cita no encontrada");
            }

            if (nuevoEstado != EstadoCita.COMPLETADA && nuevoEstado != EstadoCita.NO_ASISTIO) {
                throw new AccionNoPermitidaException("Un estilista solo puede cambiar el estado a COMPLETADA o NO_ASISTIO");
            }
        }

        if (!cita.getEstado().puedePasarA(nuevoEstado)) {
            throw new ConflictoExcepcion(
                    String.format("No se puede cambiar el estado de %s a %s", cita.getEstado(), nuevoEstado)
            );
        }

        cita.setEstado(nuevoEstado);
        cita.setFechaActualizacion(LocalDateTime.now(clock));

        cita = citaRepository.saveAndFlush(cita);
        return mapearADto(cita);
    }

    @Transactional
    public CitaResponseDto reservarEnLinea(String slug, CitaRequestDto request) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));

        return agendar(
                salon.getId(),
                request,
                EstadoCita.PENDIENTE,
                OrigenCita.EN_LINEA
        );
    }

    @Transactional(readOnly = true)
    public DatosRecordatorioCita obtenerDatosVerificadosRecordatorio(Long citaId, Long salonId) {
        Cita cita = citaRepository.findByIdAndSalonId(citaId, salonId)
                .orElseThrow(() -> new NoEncontradoException("Cita no encontrada"));

        if (!cita.getEstado().ocupaAgenda()) {
            throw new ConflictoExcepcion("Solo se pueden generar recordatorios para citas en estado PENDIENTE o CONFIRMADA");
        }

        Locale localeEs = Locale.forLanguageTag("es-CO");
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", localeEs);
        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("h:mm a", localeEs);

        String fechaFormateada = cita.getInicio().format(formatoFecha);
        String horaFormateada = cita.getInicio().format(formatoHora)
                .replace("\u202f", " ")
                .replace("\u00a0", " ");

        long duracionMinutos = java.time.Duration.between(cita.getInicio(), cita.getFin()).toMinutes();
        String servicioTexto = String.format("%s (%d minutos)", cita.getServicio().getNombre(), duracionMinutos);
        String valorTexto = String.format("$%,d", cita.getPrecio().longValue()).replace(',', '.');
        String direccionTexto = String.format("%s, %s", cita.getSalon().getDireccion(), cita.getSalon().getCiudad());

        String datosTexto = String.format(
                "Servicio: %s\nEstilista: %s\nFecha: %s\nHora: %s\nValor: %s\nDirección: %s",
                servicioTexto,
                cita.getEstilista().getNombre(),
                fechaFormateada,
                horaFormateada,
                valorTexto,
                direccionTexto
        );

        return new DatosRecordatorioCita(
                cita.getSalon().getNombre(),
                cita.getCliente().getNombre(),
                cita.getCliente().getTelefono(),
                datosTexto
        );
    }

}