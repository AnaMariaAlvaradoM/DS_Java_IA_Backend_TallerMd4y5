package com.devsenior.sillon.cita;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@Service
public class CitaService {

    private final CitaRepository citaRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteService clienteService;
    private final ReglasAgenda reglasAgenda;
    private final Clock clock;

    public CitaService(CitaRepository citaRepository, SalonRepository salonRepository,
            ServicioRepository servicioRepository, UsuarioRepository usuarioRepository, ClienteService clienteService,
            ReglasAgenda reglasAgenda, Clock clock) {
        this.citaRepository = citaRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteService = clienteService;
        this.reglasAgenda = reglasAgenda;
        this.clock = clock;
    }

    /** Cita creada por el dueño: nace CONFIRMADA con origen SALON. */
    @Transactional
    public CitaRespuesta crearDesdeSalon(Long salonId, CitaPeticion peticion) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("El salón no existe"));
        return CitaRespuesta.de(agendar(salon, peticion, EstadoCita.CONFIRMADA, OrigenCita.SALON));
    }

    /** Reserva del cliente final, sin cuenta: nace PENDIENTE con origen EN_LINEA. */
    @Transactional
    public CitaRespuesta reservarEnLinea(String slug, CitaPeticion peticion) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("El salón no existe"));
        return CitaRespuesta.de(agendar(salon, peticion, EstadoCita.PENDIENTE, OrigenCita.EN_LINEA));
    }

    /** Un solo método para agendar, con las validaciones en este orden (ver SPEC). */
    private Cita agendar(Salon salon, CitaPeticion p, EstadoCita estado, OrigenCita origen) {
        // 1. El servicio existe en el salón y está activo
        Servicio servicio = servicioRepository.findByIdAndSalonId(p.servicioId(), salon.getId())
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new NoEncontradoException("El servicio no existe"));

        // 2. El estilista existe en el salón, está activo y ofrece ese servicio
        Usuario estilista = usuarioRepository.findByIdAndSalonIdAndRol(p.estilistaId(), salon.getId(), Rol.ESTILISTA)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new NoEncontradoException("El estilista no existe"));
        if (!estilista.ofrece(servicio.getId())) {
            throw new ReglaNegocioException(estilista.getNombre() + " no ofrece el servicio " + servicio.getNombre());
        }

        // 3. El fin lo calcula el sistema con la duración del servicio
        LocalDateTime inicio = p.inicio().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos());

        // 4. No en el pasado (con el reloj de Colombia)
        if (inicio.isBefore(LocalDateTime.now(clock))) {
            throw new ReglaNegocioException("No se puede agendar en el pasado");
        }

        // 5. Dentro del horario del salón
        reglasAgenda.validarHorario(salon.getConfiguracion(), inicio, fin);

        // 6. Sin cruces con citas del estilista que ocupen agenda
        if (citaRepository.existsByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(estilista.getId(),
                EstadoCita.queOcupanAgenda(), fin, inicio)) {
            throw new ReglaNegocioException(estilista.getNombre() + " ya tiene una cita que se cruza con ese horario");
        }

        // 7. Cliente (buscar o crear), precio copiado, estado y origen
        Cliente cliente = clienteService.buscarOCrear(salon, p.clienteNombre(), p.clienteTelefono());
        LocalDateTime ahora = ahora();
        Cita cita = new Cita();
        cita.setSalon(salon);
        cita.setServicio(servicio);
        cita.setEstilista(estilista);
        cita.setCliente(cliente);
        cita.setInicio(inicio);
        cita.setFin(fin);
        cita.setPrecio(servicio.getPrecio());
        cita.setEstado(estado);
        cita.setOrigen(origen);
        cita.setNota(p.nota() == null || p.nota().isBlank() ? null : p.nota().trim());
        cita.setFechaCreacion(ahora);
        cita.setFechaActualizacion(ahora);
        return citaRepository.save(cita);
    }

    /** Agenda de un día: el dueño ve todas las del salón; el estilista solo las suyas. */
    @Transactional(readOnly = true)
    public List<CitaRespuesta> listar(UsuarioPrincipal usuario, LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
        LocalDateTime desde = dia.atStartOfDay();
        LocalDateTime hasta = dia.plusDays(1).atStartOfDay();
        List<Cita> citas = usuario.esEstilista()
                ? citaRepository.findAllBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                        usuario.getSalonId(), usuario.getId(), desde, hasta)
                : citaRepository.findAllBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                        usuario.getSalonId(), desde, hasta);
        return citas.stream().map(CitaRespuesta::de).toList();
    }

    /**
     * Orden: la cita es del salón (404) → si es estilista, debe ser suya (404) y el destino
     * COMPLETADA o NO_ASISTIO (403) → la transición debe ser válida (409).
     */
    @Transactional
    public CitaRespuesta cambiarEstado(UsuarioPrincipal usuario, Long citaId, EstadoCita destino) {
        Cita cita = citaRepository.findByIdAndSalonId(citaId, usuario.getSalonId())
                .orElseThrow(() -> new NoEncontradoException("La cita no existe"));
        if (usuario.esEstilista()) {
            if (!cita.getEstilista().getId().equals(usuario.getId())) {
                throw new NoEncontradoException("La cita no existe");
            }
            if (destino != EstadoCita.COMPLETADA && destino != EstadoCita.NO_ASISTIO) {
                throw new AccionNoPermitidaException("Un estilista solo puede marcar sus citas como COMPLETADA o NO_ASISTIO");
            }
        }
        if (!cita.getEstado().puedeCambiarA(destino)) {
            throw new ReglaNegocioException("No se puede pasar una cita de " + cita.getEstado() + " a " + destino);
        }
        cita.setEstado(destino);
        cita.setFechaActualizacion(ahora());
        // saveAndFlush: la fecha de actualización sale correcta en la respuesta (Clase 17)
        return CitaRespuesta.de(citaRepository.saveAndFlush(cita));
    }

    /** Microsegundos: es la precisión que guarda la base de datos. */
    private LocalDateTime ahora() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
