package com.devsenior.sillon.publico;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.cita.CitaRepository;
import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@Service
public class PublicoService {

    private final SalonRepository salones;
    private final ServicioRepository servicios;
    private final UsuarioRepository usuarios;
    private final CitaRepository citas;
    private final ReglasAgenda reglas;

    public PublicoService(SalonRepository salones, ServicioRepository servicios,
            UsuarioRepository usuarios, CitaRepository citas, ReglasAgenda reglas) {
        this.salones = salones;
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.citas = citas;
        this.reglas = reglas;
    }

    @Transactional(readOnly = true)
    public CatalogoPublicoResponse catalogo(String slug) {
        Salon salon = buscarSalon(slug);
        Long salonId = salon.getId();

        List<CatalogoPublicoResponse.ServicioPublico> catalogo = servicios.findAllBySalonId(salonId)
                .stream()
                .filter(Servicio::isActivo)
                .sorted(Comparator.comparing(Servicio::getId))
                .map(s -> new CatalogoPublicoResponse.ServicioPublico(
                        s.getId(), s.getNombre(), s.getDescripcion(),
                        s.getDuracionMinutos(), s.getPrecio()))
                .toList();

        List<CatalogoPublicoResponse.EstilistaPublico> estilistas =
                usuarios.findAllBySalonIdAndRol(salonId, Rol.ESTILISTA)
                .stream()
                .filter(Usuario::isActivo)
                .sorted(Comparator.comparing(Usuario::getId))
                .map(u -> new CatalogoPublicoResponse.EstilistaPublico(
                        u.getId(), u.getNombre(),
                        u.getServicios().stream()
                                .filter(Servicio::isActivo)
                                .filter(s -> salonId.equals(s.getSalon().getId()))
                                .map(Servicio::getId)
                                .sorted()
                                .toList()))
                .toList();

        return new CatalogoPublicoResponse(
                salon.getNombre(), salon.getSlug(), salon.getCiudad(),
                salon.getDireccion(), salon.getTelefono(), horario(salon), catalogo, estilistas);
    }

    @Transactional(readOnly = true)
    public ConocimientoChat conocimientoChat(String slug) {
        Salon salon = buscarSalon(slug);
        var formato = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO"));
        String catalogo = servicios.findAllBySalonId(salon.getId()).stream()
                .filter(Servicio::isActivo)
                .sorted(Comparator.comparing(Servicio::getId))
                .map(servicio -> "%s (%d minutos): $%s".formatted(servicio.getNombre(),
                        servicio.getDuracionMinutos(), formato.format(servicio.getPrecio())))
                .collect(Collectors.joining("\n"));
        return new ConocimientoChat(salon.getId(), salon.getNombre(), salon.getDireccion() + ", " + salon.getCiudad(),
                horario(salon), catalogo);
    }

    private static String horario(Salon salon) {
        var conf = salon.getConfiguracion();
        return conf.getHoraApertura() + " a " + conf.getHoraCierre()
                + (conf.isAbreDomingos() ? " (domingos abierto)" : " (domingos cerrado)");
    }

    @Transactional(readOnly = true)
    public DisponibilidadPublicaResponse disponibilidad(
            String slug, Long servicioId, Long estilistaId, LocalDate fecha) {
        Salon salon = buscarSalon(slug);
        Long salonId = salon.getId();

        Servicio servicio = servicios.findByIdAndSalonId(servicioId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
        if (!servicio.isActivo()) {
            throw new ReglaDeNegocioException("El servicio está inactivo");
        }

        // Consulta de pertenencia. No se usa el método con bloqueo de agendamiento
        // porque esta operación solamente calcula disponibilidad.
        Usuario estilista = usuarios.findAllBySalonIdAndRol(salonId, Rol.ESTILISTA)
                .stream()
                .filter(u -> estilistaId.equals(u.getId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Estilista no encontrado"));
        if (!estilista.isActivo()) {
            throw new ReglaDeNegocioException("El estilista está inactivo");
        }
        if (!estilista.ofreceServicio(servicioId)) {
            throw new ReglaDeNegocioException("El estilista no ofrece el servicio solicitado");
        }

        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();
        List<Cita> agenda = citas
                .findAllBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                        salonId, estilistaId, desde, hasta);

        return new DisponibilidadPublicaResponse(
                fecha, servicio.getNombre(), estilista.getNombre(),
                servicio.getDuracionMinutos(),
                reglas.calcularHorariosLibres(
                        salon, estilistaId, fecha, servicio.getDuracionMinutos(), agenda));
    }

    private Salon buscarSalon(String slug) {
        return salones.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
    }
}
