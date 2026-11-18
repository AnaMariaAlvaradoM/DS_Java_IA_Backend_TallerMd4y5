package com.devsenior.sillon.publico;

import com.devsenior.sillon.cita.service.CitaService;
import com.devsenior.sillon.cita.ReglasAgenda;

import com.devsenior.sillon.cita.model.Cita;
import com.devsenior.sillon.cita.model.EstadoCita;

import com.devsenior.sillon.cita.repository.CitaRepository;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.publico.dto.CatalogoResponse;
import com.devsenior.sillon.publico.dto.ConocimientoChatbot;
import com.devsenior.sillon.publico.dto.DisponibilidadResponse;
import com.devsenior.sillon.publico.dto.EstilistaResponse1;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.servicio.dto.ServicioResponse;
import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import com.devsenior.sillon.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class CatalogoService {
    private final SalonRepository salonRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;
    private final CitaRepository citaRepository;
    private final ReglasAgenda reglasAgenda;
    private final CitaService citaService;
    private final Clock clock;

    public CatalogoService(SalonRepository salonRepository, UsuarioRepository usuarioRepository, ServicioRepository servicioRepository, CitaRepository citaRepository, ReglasAgenda reglasAgenda, CitaService citaService) {
        this(salonRepository, usuarioRepository, servicioRepository, citaRepository, reglasAgenda, citaService, Clock.systemDefaultZone());
    }

    @Autowired
    public CatalogoService(SalonRepository salonRepository, UsuarioRepository usuarioRepository, ServicioRepository servicioRepository, CitaRepository citaRepository, ReglasAgenda reglasAgenda, CitaService citaService, Clock clock) {
        this.salonRepository = salonRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioRepository = servicioRepository;
        this.citaRepository = citaRepository;
        this.reglasAgenda = reglasAgenda;
        this.citaService = citaService;
        this.clock = clock;
    }
    @Transactional(readOnly = true)
    public CatalogoResponse obtenerCatalogo(String slug) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("Salon no encontrado"));

        List<ServicioResponse> servicios = servicioRepository.findAllBySalonIdAndActivoTrue(salon.getId())
                .stream()
                .map(servicio -> new ServicioResponse(
                        servicio.getId(),
                        servicio.getNombre(),
                        servicio.getDescripcion(),
                        servicio.getDuracionMinutos(),
                        servicio.getPrecio(),
                        servicio.getActivo()
                ))
                .toList();

        List<EstilistaResponse1> estilistas = usuarioRepository.findBySalonIdAndRolAndActivoTrue(salon.getId(), Rol.ESTILISTA)
                .stream()
                .map(estilista -> new EstilistaResponse1(
                        estilista.getId(),
                        estilista.getNombre(),
                        estilista.getServicios().stream().map(servicio -> servicio.getId()).toList()
                ))
                .toList();
        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("h:mm a", new Locale("es", "ES"));
        String horaApertura = salon.getConfiguracion().getHoraApertura().format(formatoHora).replace("AM", "a. m.").replace("PM", "p. m.");
        String horaCierre = salon.getConfiguracion().getHoraCierre().format(formatoHora).replace("AM", "a. m.").replace("PM", "p. m.");

        String domingo = salon.getConfiguracion().isAbreDomingos() ? "(domingos abierto)" : "(domingos cerrado)";
        String textoHorario = "Lunes a sábado " + domingo + ", de " + horaApertura + " a " + horaCierre;
        return new CatalogoResponse(
                salon.getNombresalon(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
               textoHorario,
                servicios,
                estilistas
        );
    }
    @Transactional(readOnly = true)
    public DisponibilidadResponse obtenerDisponibilidad(String slug, Long servicioId, Long estilistaId, LocalDate fecha) {

        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));


        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado"));
        int duracionMinutos = servicio.getDuracionMinutos();


        Usuario estilista = usuarioRepository.findByIdAndSalonIdAndActivoTrueAndRol(
                estilistaId,
                salon.getId(),
                Rol.ESTILISTA
        )
                .orElseThrow(() -> new NoEncontradoException("Estilista no encontrado o no activo en el salón"));

        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();
        List<EstadoCita> estadosQueOcupan = List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);

        List<Cita> citas = citaRepository.findCitasQueOcupanAgenda(estilistaId, estadosQueOcupan, inicioDia, finDia);

        List<ReglasAgenda.IntervaloOcupado> citasOcupadas = citas.stream()
                .map(cita -> {
                    LocalDateTime inicio = cita.getFechaHora();
                    // Si fin es null, se calcula sumando la duración del servicio de esa cita
                    LocalDateTime fin = (cita.getFinalizacion() != null)
                            ? cita.getFinalizacion()
                            : inicio.plusMinutes(cita.getServicio().getDuracionMinutos());

                    return new ReglasAgenda.IntervaloOcupado(inicio, fin);
                })
                .toList();

        List<LocalTime> horariosLibres = reglasAgenda.calcularHorariosLibres(
                fecha,
                duracionMinutos,
                salon.getConfiguracion().getHoraApertura(),
                salon.getConfiguracion().getHoraCierre(),
                salon.getConfiguracion().isAbreDomingos(),
                citasOcupadas,
                LocalDateTime.now(clock)
        );


        return new DisponibilidadResponse(
                fecha,
                servicioId,
                estilistaId,
                duracionMinutos,
                horariosLibres
        );
    }
    @Transactional(readOnly = true)
    public ConocimientoChatbot obtenerConocimientoParaChatbot(Long salonId) {

        // 1. Obtener el salón (para sacar su horario)
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));

        // 2. Obtener SOLO los servicios activos de este salón
        // (Asegúrate de tener este método en tu ServicioRepository)
        List<Servicio> serviciosActivos = servicioRepository.findAllBySalonIdAndActivoTrue(salon.getId());

        Locale localeCo = Locale.forLanguageTag("es-CO");

        // 3. Armar el texto de servicios (una línea por servicio)
        String textoServicios = serviciosActivos.stream()
                .map(s -> String.format(localeCo, "- %s: %d minutos, $%,d",
                        s.getNombre(),
                        s.getDuracionMinutos(),
                        s.getPrecio().longValue()))
                .collect(Collectors.joining("\n"));

        String textoHorario = salon.getConfiguracion().toString();

        return new ConocimientoChatbot(textoServicios, textoHorario);
    }
}
