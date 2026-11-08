package com.devsenior.sillon.cita;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteServicio;
import com.devsenior.sillon.compartido.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepositorio;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepositorio;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CitaServicio {
    private static final List<EstadoCita> ESTADOS_OCUPADOS = List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);
    private final CitaRepositorio citas;
    private final ServicioRepositorio servicios;
    private final UsuarioRepositorio usuarios;
    private final ClienteServicio clientes;
    private final ReglasAgenda reglas;
    private final Clock clock;

    public CitaServicio(CitaRepositorio citas, ServicioRepositorio servicios, UsuarioRepositorio usuarios,
            ClienteServicio clientes, ReglasAgenda reglas, Clock clock) {
        this.citas = citas;
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.reglas = reglas;
        this.clock = clock;
    }

    @Transactional
    public CitaRespuesta agendarSalon(Long salonId, CitaSolicitud solicitud) {
        return agendar(salonId, solicitud, EstadoCita.CONFIRMADA, OrigenCita.SALON);
    }

    // La Fase 5 podrá reutilizar este flujo con PENDIENTE y EN_LINEA.
    private CitaRespuesta agendar(Long salonId, CitaSolicitud solicitud, EstadoCita estado, OrigenCita origen) {
        Servicio servicio = servicios.findByIdAndSalon_Id(solicitud.servicioId(), salonId)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado"));
        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw new ReglaNegocioException("El servicio no está activo");
        }
        Usuario estilista = usuarios.buscarEstilistaParaAgendar(solicitud.estilistaId(), salonId, Rol.ESTILISTA)
                .orElseThrow(() -> new NoEncontradoException("Estilista no encontrado"));
        if (!Boolean.TRUE.equals(estilista.getActivo()) || !estilista.ofreceServicio(servicio.getId())) {
            throw new ReglaNegocioException("El estilista no está activo o no ofrece el servicio");
        }
        LocalDateTime fin = solicitud.inicio().plusMinutes(servicio.getDuracionMinutos());
        reglas.validarNoPasado(solicitud.inicio());
        reglas.validarHorario(servicio.getSalon().getConfiguracion(), solicitud.inicio(), fin);
        List<Cita> cruces = citas.buscarCrucesParaActualizar(salonId, estilista.getId(), ESTADOS_OCUPADOS,
                solicitud.inicio(), fin);
        if (reglas.hayCruce(solicitud.inicio(), fin, cruces)) {
            throw new ReglaNegocioException("El estilista ya tiene una cita en ese horario");
        }
        Cliente cliente = clientes.buscarOCrear(salonId, solicitud.clienteNombre(), solicitud.clienteTelefono());
        LocalDateTime ahora = LocalDateTime.now(clock);
        Cita cita = new Cita();
        cita.setSalon(servicio.getSalon());
        cita.setCliente(cliente);
        cita.setServicio(servicio);
        cita.setEstilista(estilista);
        cita.setInicio(solicitud.inicio());
        cita.setFin(fin);
        cita.setPrecio(servicio.getPrecio());
        cita.setEstado(estado);
        cita.setOrigen(origen);
        cita.setNota(solicitud.nota());
        cita.setFechaCreacion(ahora);
        cita.setFechaActualizacion(ahora);
        return CitaRespuesta.desde(citas.saveAndFlush(cita));
    }

    @Transactional(readOnly = true)
    public List<CitaRespuesta> listar(UsuarioPrincipal usuario, LocalDate fecha) {
        LocalDate dia = fecha == null ? LocalDate.now(clock) : fecha;
        LocalDateTime desde = dia.atStartOfDay();
        LocalDateTime hasta = dia.plusDays(1).atStartOfDay();
        List<Cita> resultado = usuario.getRol() == Rol.ESTILISTA
                ? citas.findAllBySalon_IdAndEstilista_IdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                        usuario.getSalonId(), usuario.getId(), desde, hasta)
                : citas.findAllBySalon_IdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                        usuario.getSalonId(), desde, hasta);
        return resultado.stream().map(CitaRespuesta::desde).toList();
    }

    @Transactional
    public CitaRespuesta cambiarEstado(UsuarioPrincipal usuario, Long id, EstadoCita destino) {
        Cita cita = citas.buscarParaActualizar(id, usuario.getSalonId())
                .orElseThrow(() -> new NoEncontradoException("Cita no encontrada"));
        if (usuario.getRol() == Rol.ESTILISTA) {
            if (!cita.getEstilista().getId().equals(usuario.getId())) {
                throw new NoEncontradoException("Cita no encontrada");
            }
            if (destino != EstadoCita.COMPLETADA && destino != EstadoCita.NO_ASISTIO) {
                throw new AccionNoPermitidaException("El estilista no puede cambiar la cita a ese estado");
            }
        }
        if (!cita.getEstado().puedeCambiarA(destino)) {
            throw new ReglaNegocioException("Transición de estado no permitida");
        }
        cita.setEstado(destino);
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!ahora.isAfter(cita.getFechaActualizacion())) {
            ahora = cita.getFechaActualizacion().plusNanos(1_000);
        }
        cita.setFechaActualizacion(ahora);
        return CitaRespuesta.desde(citas.saveAndFlush(cita));
    }
}
