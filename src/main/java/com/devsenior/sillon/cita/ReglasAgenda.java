package com.devsenior.sillon.cita;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReglasAgenda {
    public void validarHorario(LocalDateTime fechaHoraCita, int duracionMinutos,
                               LocalTime apertura, LocalTime cierre, boolean abreDomingo) {
        LocalDate fecha = fechaHoraCita.toLocalDate();
        LocalTime inicio = fechaHoraCita.toLocalTime();
        LocalDateTime fechaHoraFin = fechaHoraCita.plusMinutes(duracionMinutos);

        String horarioMensaje = "Horario del salón: " + apertura + " a " + cierre;
        // 1. Validar que empiece y termine el mismo día
        if (!fecha.equals(fechaHoraFin.toLocalDate())) {
            throw new ReglaNegocioException("La cita debe empezar y terminar el mismo día. " + horarioMensaje);
        }

        // 2. Validar día de atención (Domingo solo si abre el salón)
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !abreDomingo) {
            throw new ReglaNegocioException("El salón no abre los domingos. " + horarioMensaje);
        }

        // 3. Validar apertura y cierre
        if (inicio.isBefore(apertura) || fechaHoraFin.toLocalTime().isAfter(cierre)) {
            throw new ReglaNegocioException("La cita está fuera del horario de atención. " + horarioMensaje);
        }
    }

    /**
     * Regla de negocio para determinar si dos intervalos de tiempo se cruzan.
     * Se cruzan si el inicio de uno es anterior al fin del otro y el fin del primero es posterior al inicio del segundo.
     */
    public boolean seCruzan(LocalDateTime inicio1, LocalDateTime fin1, LocalDateTime inicio2, LocalDateTime fin2) {
        return inicio1.isBefore(fin2) && fin1.isAfter(inicio2);
    }

    /**
     * Calcula los horarios libres recorriendo desde la apertura de intervalo en intervalo,
     * descartando las horas pasadas y las que se cruzan con citas ocupadas.
     */
    public List<LocalTime> calcularHorariosLibres(LocalDate fecha, int duracionMinutos,
                                                  LocalTime apertura, LocalTime cierre,
                                                  boolean abreDomingo,
                                                  List<IntervaloOcupado> citasOcupadas,
                                                  LocalDateTime referenciaActual) {

        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !abreDomingo) {
            return new ArrayList<>();
        }

        List<LocalTime> horariosLibres = new ArrayList<>();
        int intervaloMinutos = 30; // Intervalo de salto para los slots disponibles
        LocalTime cursor = apertura;

        // Mientras el servicio quepa completo antes del cierre
        while (!cursor.plusMinutes(duracionMinutos).isAfter(cierre)) {
            LocalDateTime inicioCandidato = LocalDateTime.of(fecha, cursor);
            LocalDateTime finCandidato = inicioCandidato.plusMinutes(duracionMinutos);

            // 1. Descarta las horas que ya pasaron
            boolean yaPaso = inicioCandidato.isBefore(referenciaActual);

            // 2. Descarta las horas que se cruzan con citas ocupadas
            boolean seCruzaConOcupada = false;
            if (citasOcupadas != null) {
                for (IntervaloOcupado ocupada : citasOcupadas) {
                    if (seCruzan(inicioCandidato, finCandidato, ocupada.inicio(), ocupada.fin())) {
                        seCruzaConOcupada = true;
                        break;
                    }
                }
            }

            if (!yaPaso && !seCruzaConOcupada) {
                horariosLibres.add(cursor);
            }

            cursor = cursor.plusMinutes(intervaloMinutos);
        }

        return horariosLibres;
    }

    /**
     * Record auxiliar para representar los intervalos ocupados al evaluar cruces.
     */
    public record IntervaloOcupado(LocalDateTime inicio, LocalDateTime fin) {}
}

