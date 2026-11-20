package com.devsenior.sillon.cita;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.AccesoDenegadoException;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CitaService {
    private static final List<EstadoCita> ESTADOS_OCUPADOS = List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);

    private final CitaRepository citas;
    private final SalonRepository salones;
    private final ServicioRepository servicios;
    private final UsuarioRepository usuarios;
    private final ClienteService clientes;
    private final ReglasAgenda reglas;
    private final Clock reloj;

    public CitaService(CitaRepository citas, SalonRepository salones, ServicioRepository servicios,
                       UsuarioRepository usuarios, ClienteService clientes, ReglasAgenda reglas, Clock reloj) {
        this.citas = citas;
        this.salones = salones;
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.reglas = reglas;
        this.reloj = reloj;
    }

    @Transactional
    public CitaRespuesta agendarDesdeSalon(Long salonId, CitaSolicitud solicitud) {
        return agendar(salonId, solicitud, EstadoCita.CONFIRMADA, OrigenCita.SALON);
    }

    @Transactional
    public CitaRespuesta agendarEnLinea(Long salonId, CitaSolicitud solicitud) {
        return agendar(salonId, solicitud, EstadoCita.PENDIENTE, OrigenCita.EN_LINEA);
    }

    private CitaRespuesta agendar(Long salonId, CitaSolicitud solicitud, EstadoCita estado, OrigenCita origen) {
        Salon salon = salones.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("El salón no existe"));
        Servicio servicio = servicios.findByIdAndSalonId(solicitud.servicioId(), salonId)
                .orElseThrow(() -> new NoEncontradoException("El servicio no existe en este salón"));
        if (!servicio.isActivo()) {
            throw new ReglaNegocioException("El servicio no está disponible");
        }
        Usuario estilista = usuarios.findByIdAndSalonIdAndRol(solicitud.estilistaId(), salonId, Rol.ESTILISTA)
                .orElseThrow(() -> new NoEncontradoException("El estilista no existe en este salón"));
        if (!estilista.isActivo() || !estilista.ofreceServicio(servicio.getId())) {
            throw new ReglaNegocioException("El estilista no ofrece este servicio");
        }
        LocalDateTime fin = solicitud.inicio().plusMinutes(servicio.getDuracionMinutos());
        reglas.validarNoPasado(solicitud.inicio());
        reglas.validarHorario(salon.getConfiguracion(), solicitud.inicio(), fin);
        if (citas.existsBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                salonId, estilista.getId(), ESTADOS_OCUPADOS, fin, solicitud.inicio())) {
            throw new ReglaNegocioException("El estilista ya tiene una cita en ese horario");
        }
        Cliente cliente = clientes.buscarOCrear(salon, solicitud.clienteNombre(), solicitud.clienteTelefono());
        Cita cita = new Cita();
        cita.setSalon(salon);
        cita.setCliente(cliente);
        cita.setEstilista(estilista);
        cita.setServicio(servicio);
        cita.setInicio(solicitud.inicio());
        cita.setFin(fin);
        cita.setPrecio(servicio.getPrecio());
        cita.setEstado(estado);
        cita.setOrigen(origen);
        cita.setNota(solicitud.nota());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        cita.setFechaCreacion(ahora);
        cita.setFechaActualizacion(ahora);
        return mapear(citas.saveAndFlush(cita));
    }

    @Transactional(readOnly = true)
    public List<CitaRespuesta> listar(Long salonId, Long usuarioId, Rol rol, LocalDate fecha) {
        LocalDate dia = fecha == null ? LocalDate.now(reloj) : fecha;
        LocalDateTime desde = dia.atStartOfDay();
        LocalDateTime hasta = dia.plusDays(1).atStartOfDay().minusNanos(1);
        List<Cita> resultado = rol == Rol.ESTILISTA
                ? citas.findBySalonIdAndEstilistaIdAndInicioBetweenOrderByInicioAsc(salonId, usuarioId, desde, hasta)
                : citas.findBySalonIdAndInicioBetweenOrderByInicioAsc(salonId, desde, hasta);
        return resultado.stream().map(this::mapear).toList();
    }

    @Transactional
    public CitaRespuesta cambiarEstado(Long salonId, Long usuarioId, Rol rol, Long citaId, EstadoCita destino) {
        Cita cita = citas.findByIdAndSalonId(citaId, salonId)
                .orElseThrow(() -> new NoEncontradoException("La cita no existe en este salón"));
        if (rol == Rol.ESTILISTA) {
            if (!cita.getEstilista().getId().equals(usuarioId)) {
                throw new NoEncontradoException("La cita no existe para este estilista");
            }
            if (destino != EstadoCita.COMPLETADA && destino != EstadoCita.NO_ASISTIO) {
                throw new AccesoDenegadoException("El estilista solo puede completar o marcar no asistencia");
            }
        }
        if (!cita.getEstado().puedeTransicionarA(destino)) {
            throw new ReglaNegocioException("No se permite cambiar la cita a ese estado");
        }
        cita.setEstado(destino);
        LocalDateTime actualizacion = LocalDateTime.now(reloj);
        if (!actualizacion.isAfter(cita.getFechaActualizacion())) {
            actualizacion = cita.getFechaActualizacion().plusNanos(1000);
        }
        cita.setFechaActualizacion(actualizacion);
        return mapear(citas.saveAndFlush(cita));
    }

    private CitaRespuesta mapear(Cita cita) {
        return new CitaRespuesta(cita.getId(), cita.getInicio(), cita.getFin(), cita.getEstado(),
                cita.getOrigen(), cita.getPrecio(), cita.getServicio().getId(), cita.getServicio().getNombre(),
                cita.getEstilista().getId(), cita.getEstilista().getNombre(), cita.getCliente().getNombre(),
                cita.getCliente().getTelefono(), cita.getNota(), cita.getFechaCreacion(), cita.getFechaActualizacion());
    }
}
