package com.devsenior.sillon.cita;

import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReglasAgenda {
    private final Clock reloj;
    public ReglasAgenda(Clock reloj) { this.reloj = reloj; }

    public void validarHorario(ConfiguracionSalon config, LocalDateTime inicio, LocalDateTime fin) {
        if (cerrado(config, inicio.toLocalDate()) || !inicio.toLocalDate().equals(fin.toLocalDate())
                || inicio.toLocalTime().isBefore(config.getHoraApertura())
                || fin.toLocalTime().isAfter(config.getHoraCierre())) {
            throw new ReglaNegocioException("Horario del salón: " + config.getHoraApertura() + " a "
                    + config.getHoraCierre() + (config.isAbreDomingos() ? ", todos los días" : ", domingos cerrado"));
        }
    }

    public List<LocalTime> horariosLibres(ConfiguracionSalon config, LocalDate fecha,
            int duracionMinutos, List<Cita> citas) {
        if (cerrado(config, fecha)) { return List.of(); }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime cierre = fecha.atTime(config.getHoraCierre());
        List<LocalTime> libres = new ArrayList<>();
        for (LocalDateTime inicio = fecha.atTime(config.getHoraApertura());
                !inicio.plusMinutes(duracionMinutos).isAfter(cierre);
                inicio = inicio.plusMinutes(config.getIntervaloMinutos())) {
            LocalDateTime fin = inicio.plusMinutes(duracionMinutos);
            boolean ocupado = false;
            for (Cita cita : citas) {
                if (cita.getEstado().ocupaAgenda() && seCruzan(inicio, fin, cita.getInicio(), cita.getFin())) {
                    ocupado = true;
                    break;
                }
            }
            if (!inicio.isBefore(ahora) && !ocupado) { libres.add(inicio.toLocalTime()); }
        }
        return libres;
    }

    public boolean seCruzan(LocalDateTime inicio, LocalDateTime fin, LocalDateTime otroInicio, LocalDateTime otroFin) {
        return inicio.isBefore(otroFin) && fin.isAfter(otroInicio);
    }

    private boolean cerrado(ConfiguracionSalon config, LocalDate fecha) {
        return fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !config.isAbreDomingos();
    }
}
