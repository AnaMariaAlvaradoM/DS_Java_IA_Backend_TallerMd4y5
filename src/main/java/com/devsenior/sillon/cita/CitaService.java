package com.devsenior.sillon.cita;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@Service
public class CitaService {

    private final CitaRepository citaRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final ClienteService clienteService;
    private final ReglasAgenda reglasAgenda;
    private final Clock clock;

    public CitaService(CitaRepository citaRepository, ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository, SalonRepository salonRepository, ClienteService clienteService,
            ReglasAgenda reglasAgenda, Clock clock) {
        this.citaRepository = citaRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.clienteService = clienteService;
        this.reglasAgenda = reglasAgenda;
        this.clock = clock;
    }

    @Transactional
    public CitaRespuesta agendar(Long salonId, CitaRequest request, EstadoCita estado, OrigenCita origen) {
        Servicio servicio = servicioRepository.findByIdAndSalonId(request.servicioId(), salonId)
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));

        Usuario estilista = usuarioRepository.findByIdAndSalonIdAndRol(request.estilistaId(), salonId, Rol.ESTILISTA)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estilista no encontrado"));

        if (!estilista.ofreceServicio(servicio.getId())) {
            throw new ReglaNegocioException("El estilista no ofrece este servicio");
        }

        LocalDateTime inicio = request.inicio();
        LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos());

        if (inicio.isBefore(LocalDateTime.now(clock))) {
            throw new ReglaNegocioException("No se pueden agendar citas en el pasado");
        }

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salon no encontrado"));
        reglasAgenda.validarHorario(salon.getConfiguracion(), inicio, fin);

        List<EstadoCita> estadosQueOcupan = Arrays.stream(EstadoCita.values())
                .filter(EstadoCita::ocupaAgenda)
                .toList();
        List<Cita> cruces = citaRepository.findBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                salonId, estilista.getId(), estadosQueOcupan, fin, inicio);
        if (!cruces.isEmpty()) {
            throw new ReglaNegocioException("El estilista ya tiene una cita en ese horario");
        }

        Cliente cliente = clienteService.buscarOCrear(salonId, request.clienteNombre(), request.clienteTelefono());

        Cita cita = new Cita();
        cita.setSalon(salon);
        cita.setCliente(cliente);
        cita.setEstilista(estilista);
        cita.setServicio(servicio);
        cita.setInicio(inicio);
        cita.setFin(fin);
        cita.setPrecio(servicio.getPrecio());
        cita.setEstado(estado);
        cita.setOrigen(origen);
        cita.setNota(request.nota());
        LocalDateTime ahora = LocalDateTime.now(clock);
        cita.setFechaCreacion(ahora);
        cita.setFechaActualizacion(ahora);

        return CitaMapper.aRespuesta(citaRepository.save(cita));
    }

    @Transactional
    public List<CitaRespuesta> listarPorDia(Long salonId, Long usuarioId, Rol rol, LocalDate fecha) {
        LocalDate diaConsultado = fecha != null ? fecha : LocalDate.now(clock);
        LocalDateTime desde = diaConsultado.atStartOfDay();
        LocalDateTime hasta = diaConsultado.plusDays(1).atStartOfDay();

        List<Cita> citas = rol == Rol.ESTILISTA
                ? citaRepository.findBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(
                        salonId, usuarioId, desde, hasta)
                : citaRepository.findBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(salonId, desde,
                        hasta);

        return citas.stream().map(CitaMapper::aRespuesta).toList();
    }

    @Transactional
    public CitaRespuesta cambiarEstado(Long salonId, Long citaId, Long usuarioId, Rol rol, EstadoCita destino) {
        Cita cita = citaRepository.findByIdAndSalonId(citaId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));

        if (rol == Rol.ESTILISTA) {
            if (!cita.getEstilista().getId().equals(usuarioId)) {
                throw new RecursoNoEncontradoException("Cita no encontrada");
            }
            if (destino != EstadoCita.COMPLETADA && destino != EstadoCita.NO_ASISTIO) {
                throw new AccionNoPermitidaException("El estilista solo puede marcar la cita como completada o no asistio");
            }
        }

        if (!cita.getEstado().puedeCambiarA(destino)) {
            throw new ReglaNegocioException("No se puede cambiar de " + cita.getEstado() + " a " + destino);
        }

        cita.setEstado(destino);
        cita.setFechaActualizacion(LocalDateTime.now(clock));

        return CitaMapper.aRespuesta(citaRepository.saveAndFlush(cita));
    }
}
