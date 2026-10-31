package com.devsenior.sillon.cita;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;

@Component
public class ReglasAgenda {

    private final Clock clock;

    public ReglasAgenda(Clock clock) {
        this.clock = clock;
    }

    public void validarHorario(ConfiguracionSalon configuracion, LocalDateTime inicio, LocalDateTime fin) {
        String rango = "El horario del salon es de " + configuracion.getHoraApertura() + " a "
                + configuracion.getHoraCierre();

        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new ReglaNegocioException("La cita debe iniciar y terminar el mismo dia. " + rango);
        }
        if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()) {
            throw new ReglaNegocioException("El salon no atiende los domingos. " + rango);
        }
        if (inicio.toLocalTime().isBefore(configuracion.getHoraApertura())
                || fin.toLocalTime().isAfter(configuracion.getHoraCierre())) {
            throw new ReglaNegocioException("La cita debe estar dentro del horario del salon. " + rango);
        }
    }

    public List<LocalTime> calcularHorariosLibres(ConfiguracionSalon configuracion, LocalDate fecha,
            int duracionMinutos, List<Cita> citasOcupadas) {
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()) {
            return List.of();
        }

        int minutoApertura = configuracion.getHoraApertura().toSecondOfDay() / 60;
        int minutoCierre = configuracion.getHoraCierre().toSecondOfDay() / 60;
        int intervalo = configuracion.getIntervaloMinutos();
        LocalDateTime ahora = LocalDateTime.now(clock);

        List<LocalTime> disponibles = new ArrayList<>();
        for (int minuto = minutoApertura; minuto + duracionMinutos <= minutoCierre; minuto += intervalo) {
            LocalTime horaSlot = LocalTime.ofSecondOfDay(minuto * 60L);
            LocalDateTime inicioSlot = LocalDateTime.of(fecha, horaSlot);
            LocalDateTime finSlot = inicioSlot.plusMinutes(duracionMinutos);

            boolean yaPaso = inicioSlot.isBefore(ahora);
            boolean seCruza = citasOcupadas.stream()
                    .anyMatch(cita -> cita.getInicio().isBefore(finSlot) && cita.getFin().isAfter(inicioSlot));

            if (!yaPaso && !seCruza) {
                disponibles.add(horaSlot);
            }
        }
        return disponibles;
    }
}
