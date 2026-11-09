package com.devsenior.sillon.publico;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.cita.CitaRepositorio;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepositorio;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepositorio;
import com.devsenior.sillon.servicio.ServicioRespuesta;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepositorio;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoServicio {
    private static final List<EstadoCita> OCUPAN_AGENDA = List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("h:mm a",
            Locale.forLanguageTag("es-CO"));
    private final SalonRepositorio salones;
    private final ServicioRepositorio servicios;
    private final UsuarioRepositorio usuarios;
    private final CitaRepositorio citas;
    private final ReglasAgenda reglas;

    public CatalogoServicio(SalonRepositorio salones, ServicioRepositorio servicios, UsuarioRepositorio usuarios,
            CitaRepositorio citas, ReglasAgenda reglas) {
        this.salones = salones;
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.citas = citas;
        this.reglas = reglas;
    }

    @Transactional(readOnly = true)
    public CatalogoRespuesta catalogo(String slug) {
        Salon salon = buscarSalon(slug);
        List<ServicioRespuesta> serviciosActivos = servicios.findAllBySalon_IdAndActivoTrueOrderByIdAsc(salon.getId())
                .stream().map(ServicioRespuesta::desde).toList();
        List<EstilistaPublicoRespuesta> estilistasActivos = usuarios
                .findAllBySalon_IdAndRolAndActivoTrueOrderByIdAsc(salon.getId(), Rol.ESTILISTA)
                .stream().map(EstilistaPublicoRespuesta::desde).toList();
        return new CatalogoRespuesta(salon.getNombre(), salon.getSlug(), salon.getCiudad(),
                salon.getDireccion(), salon.getTelefono(), horarioTexto(salon.getConfiguracion()),
                serviciosActivos, estilistasActivos);
    }

    @Transactional(readOnly = true)
    public DisponibilidadRespuesta disponibilidad(String slug, Long servicioId, Long estilistaId, LocalDate fecha) {
        Salon salon = buscarSalon(slug);
        Servicio servicio = servicios.findByIdAndSalon_Id(servicioId, salon.getId())
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado"));
        Usuario estilista = usuarios.findByIdAndSalon_IdAndRol(estilistaId, salon.getId(), Rol.ESTILISTA)
                .orElseThrow(() -> new NoEncontradoException("Estilista no encontrado"));
        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw new ReglaNegocioException("El servicio no está activo");
        }
        if (!Boolean.TRUE.equals(estilista.getActivo()) || !estilista.ofreceServicio(servicioId)) {
            throw new ReglaNegocioException("El estilista no está activo o no ofrece el servicio");
        }
        List<Cita> ocupadas = citas.buscarOcupadas(salon.getId(), estilistaId, OCUPAN_AGENDA,
                fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        return new DisponibilidadRespuesta(fecha, servicio.getNombre(), estilista.getNombre(),
                servicio.getDuracionMinutos(),
                reglas.horariosLibres(salon.getConfiguracion(), fecha, servicio.getDuracionMinutos(), ocupadas));
    }

    @Transactional(readOnly = true)
    public Long salonId(String slug) { return buscarSalon(slug).getId(); }

    @Transactional(readOnly = true)
    public ContextoChatSalon contextoChat(String slug) {
        Salon salon = buscarSalon(slug);
        StringBuilder catalogo = new StringBuilder();
        for (Servicio servicio : servicios.findAllBySalon_IdAndActivoTrueOrderByIdAsc(salon.getId())) {
            if (!catalogo.isEmpty()) catalogo.append('\n');
            catalogo.append(servicio.getNombre()).append(": ")
                    .append(servicio.getDuracionMinutos()).append(" minutos, $")
                    .append(java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO"))
                            .format(servicio.getPrecio()));
        }
        return new ContextoChatSalon(salon.getId(), salon.getNombre(),
                salon.getDireccion() + ", " + salon.getCiudad(),
                horarioTexto(salon.getConfiguracion()), catalogo.toString());
    }

    private Salon buscarSalon(String slug) {
        return salones.findBySlug(slug).orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
    }

    private String horarioTexto(ConfiguracionSalon configuracion) {
        String dias = Boolean.TRUE.equals(configuracion.getAbreDomingos())
                ? "Lunes a domingo" : "Lunes a sábado (domingos cerrado)";
        return dias + ", de " + horaTexto(configuracion.getHoraApertura())
                + " a " + horaTexto(configuracion.getHoraCierre());
    }

    private String horaTexto(java.time.LocalTime hora) {
        return hora.format(FORMATO_HORA).replace('\u00a0', ' ').replace('\u202f', ' ').toLowerCase(Locale.ROOT);
    }
}
