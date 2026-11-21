package com.devsenior.sillon.cita;

import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
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
            throw new ReglaNegocioException("La cita debe empezar y terminar el mismo día");
        }

        if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY && !config.isAbreDomingos()) {
            throw new ReglaNegocioException("El salón no abre los domingos");
        }

        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFin = fin.toLocalTime();

        if (horaInicio.isBefore(config.getHoraApertura()) || horaFin.isAfter(config.getHoraCierre())) {
            throw new ReglaNegocioException("La cita está fuera del horario de atención del salón (" +
                    config.getHoraApertura() + " a " + config.getHoraCierre() + ")");
        }
    }

    public boolean seCruzan(LocalDateTime inicio1, LocalDateTime fin1, LocalDateTime inicio2, LocalDateTime fin2) {
        return inicio1.isBefore(fin2) && fin1.isAfter(inicio2);
    }

    public List<LocalTime> calcularHorariosLibres(
            LocalDate fecha,
            int duracionMinutos,
            ConfiguracionSalon config,
            List<Cita> citasOcupadas,
            LocalDateTime ahora
    ) {
        List<LocalTime> libres = new ArrayList<>();

        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !config.isAbreDomingos()) {
            return libres;
        }

        LocalTime actual = config.getHoraApertura();
        LocalTime horaCierre = config.getHoraCierre();
        int intervalo = config.getIntervaloMinutos();

        while (true) {
            LocalTime finEstimado = actual.plusMinutes(duracionMinutos);
            if (finEstimado.isAfter(horaCierre)) {
                break;
            }

            LocalDateTime inicioSlot = LocalDateTime.of(fecha, actual);
            LocalDateTime finSlot = LocalDateTime.of(fecha, finEstimado);

            // Descarta si ya pasó
            if (!inicioSlot.isAfter(ahora)) {
                actual = actual.plusMinutes(intervalo);
                continue;
            }

            // Descarta si se cruza con alguna cita ocupada
            boolean cruzado = false;
            for (Cita ocupada : citasOcupadas) {
                if (seCruzan(inicioSlot, finSlot, ocupada.getInicio(), ocupada.getFin())) {
                    cruzado = true;
                    break;
                }
            }

            if (!cruzado) {
                libres.add(actual);
            }

            actual = actual.plusMinutes(intervalo);
        }

        return libres;
    }
}
