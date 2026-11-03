package com.devsenior.sillon.publico;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.cita.CitaRepository;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.compartido.ConflictoExcepcion;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.publico.dto.DisponibilidadDto;
import com.devsenior.sillon.publico.dto.EstilistaPublicoDto;
import com.devsenior.sillon.publico.dto.SalonCatalogoResponseDto;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.servicio.dto.ServicioResponseDto;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CatalogoService {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final ReglasAgenda reglasAgenda;
    private final Clock clock;

    public CatalogoService(
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository,
            CitaRepository citaRepository,
            ReglasAgenda reglasAgenda,
            Clock clock) {
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.reglasAgenda = reglasAgenda;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SalonCatalogoResponseDto obtenerCatalogo(String slug) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));

        List<ServicioResponseDto> servicios = servicioRepository.findBySalonIdAndActivoTrue(salon.getId())
                .stream()
                .map(s -> new ServicioResponseDto(
                        s.getId(),
                        s.getNombre(),
                        s.getDescripcion(),
                        s.getPrecio(),
                        s.getDuracionMinutos(),
                        s.isActivo()
                ))
                .toList();

        List<EstilistaPublicoDto> estilistas = usuarioRepository.findBySalonIdAndRolAndActivoTrue(salon.getId(), Rol.ESTILISTA)
                .stream()
                .map(e -> new EstilistaPublicoDto(
                        e.getId(),
                        e.getNombre(),
                        e.getServicios().stream().map(Servicio::getId).toList()
                ))
                .toList();

        ConfiguracionSalon config = salon.getConfiguracion();
        String diasAtencion = config.isAbreDomingos() ? "Lunes a domingo" : "Lunes a sábado";
        String horarioTexto = String.format("%s, de %s a %s",
                diasAtencion,
                config.getHoraApertura().format(FORMATO_HORA),
                config.getHoraCierre().format(FORMATO_HORA)
        );

        return new SalonCatalogoResponseDto(
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                horarioTexto,
                servicios,
                estilistas
        );
    }

    @Transactional(readOnly = true)
    public DisponibilidadDto calcularDisponibilidad(String slug, Long servicioId, Long estilistaId, LocalDate fecha) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));

        Servicio servicio = servicioRepository.findByIdAndSalonId(servicioId, salon.getId())
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado en este salón"));

        Usuario estilista = usuarioRepository.findByIdAndSalonIdAndRol(estilistaId, salon.getId(), Rol.ESTILISTA)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new NoEncontradoException("Estilista no encontrado en este salón"));

        if (!estilista.ofreceServicio(servicioId)) {
            throw new ConflictoExcepcion("El estilista no ofrece el servicio especificado");
        }

        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.atTime(23, 59, 59);
        List<EstadoCita> estadosOcupados = List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);

        List<Cita> citasOcupadas = citaRepository.findByEstilistaIdAndEstadoInAndInicioBetween(
                estilistaId,
                estadosOcupados,
                inicioDia,
                finDia
        );

        LocalDateTime ahora = LocalDateTime.now(clock);
        List<String> horariosLibres = reglasAgenda.calcularHorariosLibres(
                salon.getConfiguracion(),
                fecha,
                servicio.getDuracionMinutos(),
                citasOcupadas,
                ahora
        );

        return new DisponibilidadDto(
                fecha,
                servicio.getNombre(),
                estilista.getNombre(),
                servicio.getDuracionMinutos(),
                horariosLibres
        );
    }
}