package com.devsenior.sillon.salon.servicio;

import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.cita.modelo.EstadoCita;
import com.devsenior.sillon.cita.repositorio.CitaRepository;
import com.devsenior.sillon.cita.servicio.ReglasAgenda;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.dto.CatalogoPublicoResponse;
import com.devsenior.sillon.salon.dto.DisponibilidadResponse;
import com.devsenior.sillon.salon.dto.EstilistaPublicoDto;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepository;
import com.devsenior.sillon.servicio.dto.ServicioResponse;
import com.devsenior.sillon.servicio.mapper.ServicioMapper;
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
public class CatalogoService {

    private static final List<EstadoCita> ESTADOS_OCUPADOS = List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);

    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final ServicioMapper servicioMapper;
    private final ReglasAgenda reglasAgenda;
    private final Clock clock;

    public CatalogoService(
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository,
            CitaRepository citaRepository,
            ServicioMapper servicioMapper,
            ReglasAgenda reglasAgenda,
            Clock clock
    ) {
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.servicioMapper = servicioMapper;
        this.reglasAgenda = reglasAgenda;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CatalogoPublicoResponse obtenerCatalogo(String slug) {
        Salon salon = buscarPorSlug(slug);

        List<ServicioResponse> servicios = servicioRepository.findBySalonIdAndActivoTrue(salon.getId()).stream()
                .map(servicioMapper::aResponse)
                .toList();

        List<EstilistaPublicoDto> estilistas = usuarioRepository.findBySalonIdAndRolAndActivoTrue(salon.getId(), Rol.ESTILISTA)
                .stream()
                .map(EstilistaPublicoDto::desde)
                .toList();

        return new CatalogoPublicoResponse(
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                formatearHorario(salon.getConfiguracion()),
                servicios,
                estilistas
        );
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse consultarDisponibilidad(String slug, Long servicioId, Long estilistaId, LocalDate fecha) {
        Salon salon = buscarPorSlug(slug);

        Servicio servicio = servicioRepository.findByIdAndSalonId(servicioId, salon.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado en este salón"));
        if (!servicio.isActivo()) {
            throw new ReglaNegocioException("El servicio no está activo");
        }

        Usuario estilista = usuarioRepository.findByIdAndSalonIdAndRol(estilistaId, salon.getId(), Rol.ESTILISTA)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estilista no encontrado en este salón"));
        if (!estilista.isActivo()) {
            throw new ReglaNegocioException("El estilista no está activo");
        }
        if (!estilista.ofreceServicio(servicio.getId())) {
            throw new ReglaNegocioException("El estilista no ofrece el servicio seleccionado");
        }

        List<Cita> citasOcupadas = citaRepository.findByEstilistaIdAndEstadoInAndInicioBetweenOrderByInicioAsc(
                estilista.getId(),
                ESTADOS_OCUPADOS,
                fecha.atStartOfDay(),
                fecha.atTime(LocalTime.MAX)
        );

        List<LocalTime> libres = reglasAgenda.calcularHorariosLibres(
                fecha,
                servicio.getDuracionMinutos(),
                salon.getConfiguracion(),
                citasOcupadas,
                LocalDateTime.now(clock)
        );

        return new DisponibilidadResponse(
                fecha,
                servicio.getNombre(),
                estilista.getNombre(),
                servicio.getDuracionMinutos(),
                libres
        );
    }

    @Transactional(readOnly = true)
    public Long obtenerSalonIdPorSlug(String slug) {
        return buscarPorSlug(slug).getId();
    }

    private Salon buscarPorSlug(String slug) {
        return salonRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
    }

    private String formatearHorario(ConfiguracionSalon config) {
        String dias = config.isAbreDomingos()
                ? "Todos los días"
                : "Lunes a sábado (domingos cerrado)";
        return dias + ", de " + config.getHoraApertura() + " a " + config.getHoraCierre();
    }
}

