package com.devsenior.sillon.cita;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cita.dto.CitaRequest;
import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.excepcion.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
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

    private static final List<EstadoCita> ESTADOS_OCUPADOS = Arrays.stream(EstadoCita.values())
            .filter(EstadoCita::ocupaAgenda)
            .toList();

    private final CitaRepository citas;
    private final SalonRepository salones;
    private final ServicioRepository servicios;
    private final UsuarioRepository usuarios;
    private final ClienteService clientes;
    private final ReglasAgenda reglas;
    private final Clock clock;

    public CitaService(CitaRepository citas, ServicioRepository servicios, UsuarioRepository usuarios,
            ClienteService clientes, ReglasAgenda reglas, Clock clock, SalonRepository salones) {
        this.citas = citas;
        this.salones = salones;
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.reglas = reglas;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CitaResponse agendarEnSalon(Long salonId, CitaRequest peticion) {
        return aRespuesta(agendar(salonId, peticion, EstadoCita.CONFIRMADA, OrigenCita.SALON));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CitaResponse reservarEnLinea(String slug, CitaRequest peticion) {
        Long salonId = salones.findBySlug(slug).map(Salon::getId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        return aRespuesta(agendar(salonId, peticion, EstadoCita.PENDIENTE, OrigenCita.EN_LINEA));
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> listar(UsuarioPrincipal principal, LocalDate fecha) {
        LocalDate dia = fecha == null ? LocalDate.now(clock) : fecha;
        LocalDateTime desde = dia.atStartOfDay();
        LocalDateTime hasta = dia.plusDays(1).atStartOfDay();
        List<Cita> agenda = switch (principal.getRol()) {
            case DUENO -> citas.findAllBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                    principal.getSalonId(), desde, hasta);
            case ESTILISTA -> citas.findAllBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                    principal.getSalonId(), principal.getId(), desde, hasta);
            default -> throw new AccionNoPermitidaException("El rol no puede consultar la agenda");
        };
        return agenda.stream().map(CitaService::aRespuesta).toList();
    }

    @Transactional
    public CitaResponse cambiarEstado(UsuarioPrincipal principal, Long id, EstadoCita destino) {
        Cita cita = citas.findOneByIdAndSalonId(id, principal.getSalonId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
        if (principal.getRol() == Rol.ESTILISTA) {
            if (!principal.getId().equals(cita.getEstilista().getId())) {
                throw new RecursoNoEncontradoException("Cita no encontrada");
            }
            if (destino != EstadoCita.COMPLETADA && destino != EstadoCita.NO_ASISTIO) {
                throw new AccionNoPermitidaException("El estilista solo puede marcar COMPLETADA o NO_ASISTIO");
            }
        } else if (principal.getRol() != Rol.DUENO) {
            throw new AccionNoPermitidaException("El rol no puede cambiar el estado de una cita");
        }
        if (!cita.getEstado().puedeCambiarA(destino)) {
            throw new ReglaDeNegocioException("No se puede cambiar de " + cita.getEstado() + " a " + destino);
        }
        cita.setEstado(destino);
        return aRespuesta(citas.saveAndFlush(cita));
    }

    private static CitaResponse aRespuesta(Cita cita) {
        return new CitaResponse(cita.getId(), cita.getInicio(), cita.getFin(), cita.getEstado(), cita.getOrigen(),
                cita.getPrecio(), cita.getServicio().getId(), cita.getServicio().getNombre(), cita.getEstilista().getId(),
                cita.getEstilista().getNombre(), cita.getCliente().getNombre(), cita.getCliente().getTelefono(),
                cita.getNota(), cita.getFechaCreacion(), cita.getFechaActualizacion());
    }

    private Cita agendar(Long salonId, CitaRequest peticion, EstadoCita estado, OrigenCita origen) {
        Servicio servicio = servicios.findByIdAndSalonId(peticion.servicioId(), salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
        if (!servicio.isActivo()) {
            throw new ReglaDeNegocioException("El servicio está inactivo");
        }

        // El bloqueo por estilista dura hasta el commit; READ_COMMITTED evita leer un snapshot previo al bloqueo.
        Usuario estilista = usuarios.findByIdAndSalonIdAndRol(peticion.estilistaId(), salonId, Rol.ESTILISTA)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estilista no encontrado"));
        if (!estilista.isActivo()) {
            throw new ReglaDeNegocioException("El estilista está inactivo");
        }
        if (!estilista.ofreceServicio(servicio.getId())) {
            throw new ReglaDeNegocioException("El estilista no ofrece el servicio solicitado");
        }

        LocalDateTime inicio = peticion.inicio();
        LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos());
        if (inicio.isBefore(LocalDateTime.now(clock))) {
            throw new ReglaDeNegocioException("No se puede agendar una cita en el pasado");
        }
        Salon salon = estilista.getSalon();
        reglas.validarHorario(salon, inicio, fin);
        if (citas.existsBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                salonId, estilista.getId(), ESTADOS_OCUPADOS, fin, inicio)) {
            throw new ReglaDeNegocioException("El estilista ya tiene una cita que se cruza con ese horario");
        }

        Cita cita = new Cita();
        cita.setSalon(salon);
        cita.setCliente(clientes.buscarOCrear(salonId, peticion.clienteNombre(), peticion.clienteTelefono()));
        cita.setEstilista(estilista);
        cita.setServicio(servicio);
        cita.setInicio(inicio);
        cita.setFin(fin);
        cita.setEstado(estado);
        cita.setOrigen(origen);
        cita.setNota(peticion.nota());
        return citas.saveAndFlush(cita);
    }
}
