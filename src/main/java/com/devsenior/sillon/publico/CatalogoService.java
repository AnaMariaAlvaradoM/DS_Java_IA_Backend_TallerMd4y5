package com.devsenior.sillon.publico;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.cita.CitaRepository;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioMapper;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

/** Datos públicos del salón por slug: catálogo, disponibilidad y textos para el chatbot. */
@Service
public class CatalogoService {

    private static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final ReglasAgenda reglasAgenda;
    private final Clock clock;

    public CatalogoService(SalonRepository salonRepository, ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository, CitaRepository citaRepository, ReglasAgenda reglasAgenda,
            Clock clock) {
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.reglasAgenda = reglasAgenda;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CatalogoPublico catalogo(String slug) {
        Salon salon = salonPorSlug(slug);
        List<Servicio> servicios = servicioRepository.findAllBySalonIdAndActivoTrueOrderByIdAsc(salon.getId());
        List<EstilistaPublico> estilistas = usuarioRepository
                .findAllBySalonIdAndRolOrderByIdAsc(salon.getId(), Rol.ESTILISTA).stream()
                .filter(Usuario::isActivo)
                .map(u -> new EstilistaPublico(u.getId(), u.getNombre(),
                        u.getServicios().stream().map(Servicio::getId).sorted().toList()))
                .toList();
        return new CatalogoPublico(salon.getNombre(), salon.getSlug(), salon.getCiudad(), salon.getDireccion(),
                salon.getTelefono(), ReglasAgenda.horarioTexto(salon.getConfiguracion()),
                servicios.stream().map(ServicioMapper::aRespuesta).toList(), estilistas);
    }

    @Transactional(readOnly = true)
    public DisponibilidadRespuesta disponibilidad(String slug, Long servicioId, Long estilistaId, LocalDate fecha) {
        Salon salon = salonPorSlug(slug);
        Servicio servicio = servicioRepository.findByIdAndSalonId(servicioId, salon.getId())
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new NoEncontradoException("El servicio no existe"));
        Usuario estilista = usuarioRepository.findByIdAndSalonIdAndRol(estilistaId, salon.getId(), Rol.ESTILISTA)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new NoEncontradoException("El estilista no existe"));
        if (!estilista.ofrece(servicio.getId())) {
            throw new ReglaNegocioException(estilista.getNombre() + " no ofrece el servicio " + servicio.getNombre());
        }
        List<Cita> ocupadas = citaRepository.findAllByEstilistaIdAndEstadoInAndInicioGreaterThanEqualAndInicioLessThan(
                estilista.getId(), EstadoCita.queOcupanAgenda(), fecha.atStartOfDay(),
                fecha.plusDays(1).atStartOfDay());
        List<String> libres = reglasAgenda
                .horariosLibres(salon.getConfiguracion(), fecha, servicio.getDuracionMinutos(), ocupadas,
                        LocalDateTime.now(clock))
                .stream().map(HH_MM_SS::format).toList();
        return new DisponibilidadRespuesta(fecha, servicio.getNombre(), estilista.getNombre(),
                servicio.getDuracionMinutos(), libres);
    }

    /** Texto del catálogo para el prompt del chatbot: una línea por servicio activo. */
    @Transactional(readOnly = true)
    public String catalogoTexto(Long salonId) {
        return servicioRepository.findAllBySalonIdAndActivoTrueOrderByIdAsc(salonId).stream()
                .map(s -> "- " + s.getNombre() + ": " + s.getDuracionMinutos() + " minutos, $"
                        + String.format(java.util.Locale.forLanguageTag("es-CO"), "%,d", s.getPrecio()))
                .collect(Collectors.joining("\n"));
    }

    public Salon salonPorSlug(String slug) {
        return salonRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("El salón no existe"));
    }
}
