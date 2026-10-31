package com.devsenior.sillon.publico;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.cita.CitaRepository;
import com.devsenior.sillon.cita.CitaRequest;
import com.devsenior.sillon.cita.CitaRespuesta;
import com.devsenior.sillon.cita.CitaService;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.OrigenCita;
import com.devsenior.sillon.cita.ReglasAgenda;
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
public class CatalogoService {

    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final ReglasAgenda reglasAgenda;
    private final CitaService citaService;

    public CatalogoService(SalonRepository salonRepository, ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository, CitaRepository citaRepository, ReglasAgenda reglasAgenda,
            CitaService citaService) {
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.reglasAgenda = reglasAgenda;
        this.citaService = citaService;
    }

    @Transactional(readOnly = true)
    public SalonPublicoRespuesta obtenerCatalogo(String slug) {
        Salon salon = buscarSalon(slug);

        List<Servicio> servicios = servicioRepository.findBySalonId(salon.getId()).stream()
                .filter(Servicio::isActivo).toList();
        List<Usuario> estilistas = usuarioRepository.findBySalonIdAndRol(salon.getId(), Rol.ESTILISTA).stream()
                .filter(Usuario::isActivo).toList();

        return CatalogoMapper.aSalonPublico(salon, servicios, estilistas);
    }

    @Transactional(readOnly = true)
    public DisponibilidadRespuesta obtenerDisponibilidad(String slug, Long servicioId, Long estilistaId,
            LocalDate fecha) {
        Salon salon = buscarSalon(slug);

        Servicio servicio = servicioRepository.findByIdAndSalonId(servicioId, salon.getId())
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));

        Usuario estilista = usuarioRepository.findByIdAndSalonIdAndRol(estilistaId, salon.getId(), Rol.ESTILISTA)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estilista no encontrado"));

        if (!estilista.ofreceServicio(servicio.getId())) {
            throw new ReglaNegocioException("El estilista no ofrece este servicio");
        }

        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();
        List<Cita> citasOcupadas = citaRepository
                .findBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(salon.getId(),
                        estilista.getId(), desde, hasta)
                .stream()
                .filter(cita -> cita.getEstado().ocupaAgenda())
                .toList();

        List<LocalTime> horariosLibres = reglasAgenda.calcularHorariosLibres(salon.getConfiguracion(),
                fecha, servicio.getDuracionMinutos(), citasOcupadas);

        return new DisponibilidadRespuesta(fecha, servicio.getNombre(), estilista.getNombre(),
                servicio.getDuracionMinutos(), horariosLibres);
    }

    @Transactional
    public CitaRespuesta reservar(String slug, CitaRequest request) {
        Salon salon = buscarSalon(slug);
        return citaService.agendar(salon.getId(), request, EstadoCita.PENDIENTE, OrigenCita.EN_LINEA);
    }

    private Salon buscarSalon(String slug) {
        return salonRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salon no encontrado"));
    }
}
