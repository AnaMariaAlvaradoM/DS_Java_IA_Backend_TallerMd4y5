package com.devsenior.sillon.cita;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;

/**
 * Reglas de negocio de la agenda sin dependencias de base de datos.
 * - Valida horarios de apertura, cierre y dias de atencion.
 * - Determina si dos intervalos de cita se cruzan.
 * - Calcula horarios libres para disponibilidad.
 */
@Component
public class ReglasAgenda {

    /**
     * Valida que la cita cumpla el horario del salon:
     * - Domingo solo si el salon abre los domingos.
     * - La cita debe empezar y terminar en la misma fecha.
     * - No puede empezar antes de la apertura.
     * - No puede terminar despues del cierre.
     */
    public void validarHorario(LocalDateTime inicio, LocalDateTime fin, ConfiguracionSalon configuracion) {
        if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()) {
            throw new ReglaNegocioException("El salón no abre los domingos.");
        }

        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new ReglaNegocioException("La cita debe iniciar y terminar el mismo día.");
        }

        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFin = fin.toLocalTime();

        if (horaInicio.isBefore(configuracion.getHoraApertura())) {
            throw new ReglaNegocioException("La hora de inicio no puede ser anterior a la apertura del salón ("
                    + configuracion.getHoraApertura() + ").");
        }

        if (horaFin.isAfter(configuracion.getHoraCierre())) {
            throw new ReglaNegocioException("La cita no puede terminar después del horario de cierre del salón ("
                    + configuracion.getHoraCierre() + ").");
        }
    }

    /**
     * Regla de negocio de cruce:
     * Dos intervalos se cruzan si el inicio de uno es anterior al fin del otro
     * Y el fin del primero es posterior al inicio del segundo.
     * Una cita que termina a las 10:45 NO se cruza con otra que empieza a las 10:45.
     */
    public boolean seCruzan(LocalDateTime inicio1, LocalDateTime fin1, LocalDateTime inicio2, LocalDateTime fin2) {
        return inicio1.isBefore(fin2) && fin1.isAfter(inicio2);
    }

    /**
     * Calcula los horarios libres recorriendo desde la apertura hasta el cierre de intervalo en intervalo.
     * Descarta las horas que se cruzan con citas ocupadas y las que ya pasaron en el momento actual.
     */
    public List<LocalTime> calcularHorariosLibres(LocalDate fecha, int duracionServicioMinutos,
                                                 ConfiguracionSalon configuracion, List<Cita> citasOcupadas,
                                                 LocalDateTime ahora) {
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()) {
            return List.of();
        }

        List<LocalTime> horariosLibres = new ArrayList<>();
        LocalTime horaActual = configuracion.getHoraApertura();
        LocalTime horaCierre = configuracion.getHoraCierre();
        int intervalo = configuracion.getIntervaloMinutos();

        while (!horaActual.isAfter(horaCierre)) {
            LocalTime finCita = horaActual.plusMinutes(duracionServicioMinutos);

            // Si el servicio supera el cierre o pasa a la medianoche del dia siguiente, no cabe
            if (finCita.isAfter(horaCierre) || finCita.isBefore(horaActual)) {
                break;
            }

            LocalDateTime inicioSlot = LocalDateTime.of(fecha, horaActual);
            LocalDateTime finSlot = LocalDateTime.of(fecha, finCita);

            // Descarta si ya paso
            boolean esPasada = inicioSlot.isBefore(ahora);

            // Descarta si se cruza con citas ocupadas
            boolean cruce = citasOcupadas.stream()
                    .anyMatch(c -> seCruzan(inicioSlot, finSlot, c.getInicio(), c.getFin()));

            if (!esPasada && !cruce) {
                horariosLibres.add(horaActual);
            }

            horaActual = horaActual.plusMinutes(intervalo);
        }

        return horariosLibres;
    }
}
