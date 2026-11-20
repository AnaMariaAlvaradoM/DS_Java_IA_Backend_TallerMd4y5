package com.devsenior.sillon.cita.servicio;

import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReglasAgenda {

    public void validarHorario(LocalDateTime inicio, LocalDateTime fin, ConfiguracionSalon config) {
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new ReglaNegocioException(
                    "La cita debe iniciar y terminar el mismo día dentro del horario "
                            + config.getHoraApertura() + " a " + config.getHoraCierre());
        }

        if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY && !config.isAbreDomingos()) {
            throw new ReglaNegocioException(
                    "El salón no atiende los domingos. Horario de atención: "
                            + config.getHoraApertura() + " a " + config.getHoraCierre());
        }

        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFin = fin.toLocalTime();

        if (horaInicio.isBefore(config.getHoraApertura()) || horaFin.isAfter(config.getHoraCierre())) {
            throw new ReglaNegocioException(
                    "La cita está fuera del horario de atención del salón ("
                            + config.getHoraApertura() + " a " + config.getHoraCierre() + ")");
        }
    }

    public List<LocalTime> calcularHorariosLibres(
            LocalDate fecha,
            int duracionMinutos,
            ConfiguracionSalon config,
            List<Cita> citasOcupadas,
            LocalDateTime ahora) {
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !config.isAbreDomingos()) {
            return List.of();
        }

        List<LocalTime> libres = new ArrayList<>();
        LocalTime actual = config.getHoraApertura();
        int intervalo = config.getIntervaloMinutos() > 0 ? config.getIntervaloMinutos() : 30;

        while (!actual.plusMinutes(duracionMinutos).isAfter(config.getHoraCierre())) {
            LocalDateTime inicioSlot = LocalDateTime.of(fecha, actual);
            LocalDateTime finSlot = inicioSlot.plusMinutes(duracionMinutos);

            boolean esFuturo = inicioSlot.isAfter(ahora);
            boolean seCruza = citasOcupadas.stream()
                    .anyMatch(cita -> seCruzan(inicioSlot, finSlot, cita.getInicio(), cita.getFin()));

            if (esFuturo && !seCruza) {
                libres.add(actual);
            }

            actual = actual.plusMinutes(intervalo);
        }

        return libres;
    }

    public boolean seCruzan(LocalDateTime inicioA, LocalDateTime finA, LocalDateTime inicioB, LocalDateTime finB) {
        return inicioA.isBefore(finB) && finA.isAfter(inicioB);
    }
}