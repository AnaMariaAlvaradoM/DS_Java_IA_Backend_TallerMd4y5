package com.devsenior.sillon.cita.service;

import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.model.ConfiguracionSalon;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Reglas de agenda sin base de datos: horario del salón, pasado y horarios libres.
 * "Ahora" sale del Clock del proyecto (zona de Colombia), así Render en UTC no cambia el resultado
 * y los tests son repetibles con un reloj fijo.
 */
@Component
public class ReglasAgenda {

    /** Intervalo ocupado [inicio, fin) de una cita existente. */
    public record Intervalo(LocalDateTime inicio, LocalDateTime fin) {

        /** Dos intervalos se cruzan si cada uno empieza antes de que el otro termine. 10:45 no choca con 10:45. */
        public boolean seCruzaCon(LocalDateTime otroInicio, LocalDateTime otroFin) {
            return inicio.isBefore(otroFin) && fin.isAfter(otroInicio);
        }
    }

    private final Clock clock;

    public ReglasAgenda(Clock clock) {
        this.clock = clock;
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }

    public LocalDate hoy() {
        return LocalDate.now(clock);
    }

    /** 409 si la cita empieza en el pasado. */
    public void validarNoPasado(LocalDateTime inicio) {
        if (inicio.isBefore(ahora())) {
            throw new ReglaNegocioException("No se puede agendar en el pasado");
        }
    }

    /** 409 si el día no es de atención o la cita no cabe completa dentro del horario del mismo día. */
    public void validarHorario(ConfiguracionSalon configuracion, LocalDateTime inicio, LocalDateTime fin) {
        if (!esDiaDeAtencion(configuracion, inicio.toLocalDate())) {
            throw new ReglaNegocioException("El salón no atiende ese día. Horario: " + configuracion.descripcion());
        }
        LocalDateTime apertura = inicio.toLocalDate().atTime(configuracion.getHoraApertura());
        LocalDateTime cierre = inicio.toLocalDate().atTime(configuracion.getHoraCierre());
        if (inicio.isBefore(apertura) || fin.isAfter(cierre)) {
            throw new ReglaNegocioException("La cita debe empezar y terminar dentro del horario del salón: "
                    + configuracion.descripcion());
        }
    }

    /**
     * Horas de inicio libres: de intervalo en intervalo desde la apertura, mientras el servicio quepa completo
     * antes del cierre, sin cruzarse con citas que ocupan agenda y nunca en el pasado.
     */
    public List<LocalTime> horariosLibres(ConfiguracionSalon configuracion, LocalDate fecha,
                                          int duracionMinutos, List<Intervalo> ocupados) {
        List<LocalTime> libres = new ArrayList<>();
        if (!esDiaDeAtencion(configuracion, fecha)) {
            return libres;
        }
        LocalDateTime ahora = ahora();
        LocalDateTime cierre = fecha.atTime(configuracion.getHoraCierre());
        LocalDateTime candidato = fecha.atTime(configuracion.getHoraApertura());
        while (!candidato.plusMinutes(duracionMinutos).isAfter(cierre)) {
            LocalDateTime finCandidato = candidato.plusMinutes(duracionMinutos);
            LocalDateTime inicio = candidato;
            boolean pasado = inicio.isBefore(ahora);
            boolean ocupado = ocupados.stream().anyMatch(o -> o.seCruzaCon(inicio, finCandidato));
            if (!pasado && !ocupado) {
                libres.add(candidato.toLocalTime());
            }
            candidato = candidato.plusMinutes(configuracion.getIntervaloMinutos());
        }
        return libres;
    }

    public boolean esDiaDeAtencion(ConfiguracionSalon configuracion, LocalDate fecha) {
        return fecha.getDayOfWeek() != DayOfWeek.SUNDAY || configuracion.isAbreDomingos();
    }
}
