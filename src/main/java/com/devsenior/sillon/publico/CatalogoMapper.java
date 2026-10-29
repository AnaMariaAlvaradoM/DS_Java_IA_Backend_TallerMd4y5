package com.devsenior.sillon.publico;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Usuario;

public class CatalogoMapper {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("h:mm a",
            new Locale("es", "CO"));

    private CatalogoMapper() {
    }

    public static ServicioPublicoRespuesta aServicioPublico(Servicio servicio) {
        return new ServicioPublicoRespuesta(servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.getDuracionMinutos(), servicio.getPrecio());
    }

    public static EstilistaPublicoRespuesta aEstilistaPublico(Usuario estilista) {
        List<Long> servicioIds = estilista.getServicios().stream().map(Servicio::getId).toList();
        return new EstilistaPublicoRespuesta(estilista.getId(), estilista.getNombre(), servicioIds);
    }

    public static SalonPublicoRespuesta aSalonPublico(Salon salon, List<Servicio> servicios,
            List<Usuario> estilistas) {
        List<ServicioPublicoRespuesta> serviciosPublicos = servicios.stream().map(CatalogoMapper::aServicioPublico)
                .toList();
        List<EstilistaPublicoRespuesta> estilistasPublicos = estilistas.stream()
                .map(CatalogoMapper::aEstilistaPublico).toList();
        String horario = formatearHorario(salon.getConfiguracion());

        return new SalonPublicoRespuesta(salon.getId(), salon.getNombre(), salon.getSlug(), salon.getCiudad(),
                salon.getDireccion(), salon.getTelefono(), horario, serviciosPublicos, estilistasPublicos);
    }

    public static String formatearHorario(ConfiguracionSalon configuracion) {
        String dias = configuracion.isAbreDomingos() ? "Lunes a domingo" : "Lunes a sábado (domingos cerrado)";
        String apertura = formatearHora(configuracion.getHoraApertura());
        String cierre = formatearHora(configuracion.getHoraCierre());
        return dias + ", de " + apertura + " a " + cierre;
    }

    private static String formatearHora(LocalTime hora) {
        return hora.format(FORMATO_HORA).replace(' ', ' ').replace(' ', ' ');
    }
}
