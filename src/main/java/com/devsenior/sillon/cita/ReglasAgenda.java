package com.devsenior.sillon.cita;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;

/** Reglas de horario y disponibilidad. Sin base de datos: fácil de probar con tests. */
@Component
public class ReglasAgenda {

    private static final Locale ES_CO = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("h:mm a", ES_CO);

    /** Dos intervalos se cruzan si cada uno empieza antes de que el otro termine. */
    public static boolean seCruzan(LocalDateTime inicioA, LocalDateTime finA, LocalDateTime inicioB,
            LocalDateTime finB) {
        return inicioA.isBefore(finB) && finA.isAfter(inicioB);
    }

    public boolean esDiaDeAtencion(ConfiguracionSalon config, LocalDate fecha) {
        return fecha.getDayOfWeek() != DayOfWeek.SUNDAY || config.isAbreDomingos();
    }

    /** 409 si la cita cae en un día cerrado o fuera del horario del salón. */
    public void validarHorario(ConfiguracionSalon config, LocalDateTime inicio, LocalDateTime fin) {
        if (!esDiaDeAtencion(config, inicio.toLocalDate())) {
            throw new ReglaNegocioException("El salón no atiende los domingos. Horario: " + horarioTexto(config));
        }
        boolean mismoDia = inicio.toLocalDate().equals(fin.toLocalDate());
        if (!mismoDia
                || inicio.toLocalTime().isBefore(config.getHoraApertura())
                || fin.toLocalTime().isAfter(config.getHoraCierre())) {
            throw new ReglaNegocioException("La cita está fuera del horario del salón: " + horarioTexto(config));
        }
    }

    /**
     * Horas de inicio libres: desde la apertura, de intervalo en intervalo, mientras el servicio
     * quepa completo antes del cierre, sin cruzarse con citas ocupadas y nunca en el pasado.
     */
    public List<LocalTime> horariosLibres(ConfiguracionSalon config, LocalDate fecha, int duracionMinutos,
            List<Cita> ocupadas, LocalDateTime ahora) {
        List<LocalTime> libres = new ArrayList<>();
        if (!esDiaDeAtencion(config, fecha)) {
            return libres;
        }
        LocalDateTime cursor = fecha.atTime(config.getHoraApertura());
        LocalDateTime cierre = fecha.atTime(config.getHoraCierre());
        while (!cursor.plusMinutes(duracionMinutos).isAfter(cierre)) {
            LocalDateTime fin = cursor.plusMinutes(duracionMinutos);
            final LocalDateTime inicio = cursor;
            boolean pasado = inicio.isBefore(ahora);
            boolean cruza = ocupadas.stream().anyMatch(c -> seCruzan(inicio, fin, c.getInicio(), c.getFin()));
            if (!pasado && !cruza) {
                libres.add(inicio.toLocalTime());
            }
            cursor = cursor.plusMinutes(config.getIntervaloMinutos());
        }
        return libres;
    }

    /** "Lunes a sábado (domingos cerrado), de 8:00 a. m. a 7:00 p. m." */
    public static String horarioTexto(ConfiguracionSalon config) {
        String dias = config.isAbreDomingos() ? "Todos los días" : "Lunes a sábado (domingos cerrado)";
        return dias + ", de " + hora(config.getHoraApertura()) + " a " + hora(config.getHoraCierre());
    }

    /** Java separa "a. m." con un espacio invisible: se reemplaza por un espacio normal. */
    public static String hora(LocalTime hora) {
        return HORA.format(hora).replace(' ', ' ').replace(' ', ' ');
    }
}
