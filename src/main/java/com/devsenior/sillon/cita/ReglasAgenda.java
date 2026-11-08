package com.devsenior.sillon.cita;

import com.devsenior.sillon.compartido.ReglaNegocioException;
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
    private final Clock clock;

    public ReglasAgenda(Clock clock) { this.clock = clock; }

    public void validarNoPasado(LocalDateTime inicio) {
        if (inicio.isBefore(LocalDateTime.now(clock))) {
            throw new ReglaNegocioException("No se puede agendar una cita en el pasado");
        }
    }

    public void validarHorario(ConfiguracionSalon configuracion, LocalDateTime inicio, LocalDateTime fin) {
        boolean domingoCerrado = inicio.getDayOfWeek() == DayOfWeek.SUNDAY
                && !Boolean.TRUE.equals(configuracion.getAbreDomingos());
        if (domingoCerrado || !inicio.toLocalDate().equals(fin.toLocalDate())
                || inicio.toLocalTime().isBefore(configuracion.getHoraApertura())
                || fin.toLocalTime().isAfter(configuracion.getHoraCierre())) {
            throw new ReglaNegocioException("La cita debe estar dentro del horario del salón: "
                    + configuracion.getHoraApertura() + " a " + configuracion.getHoraCierre()
                    + (domingoCerrado ? "; domingos cerrado" : ""));
        }
    }

    public boolean seCruzan(LocalDateTime inicioA, LocalDateTime finA,
            LocalDateTime inicioB, LocalDateTime finB) {
        return inicioA.isBefore(finB) && finA.isAfter(inicioB);
    }

    public boolean hayCruce(LocalDateTime inicio, LocalDateTime fin, List<Cita> citas) {
        return citas.stream().anyMatch(cita -> cita.getEstado().ocupaAgenda()
                && seCruzan(inicio, fin, cita.getInicio(), cita.getFin()));
    }

    public List<LocalTime> horariosLibres(ConfiguracionSalon configuracion, LocalDate fecha,
            int duracionMinutos, List<Cita> citas) {
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !Boolean.TRUE.equals(configuracion.getAbreDomingos())) {
            return List.of();
        }
        List<LocalTime> libres = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime cierre = fecha.atTime(configuracion.getHoraCierre());
        for (LocalDateTime inicio = fecha.atTime(configuracion.getHoraApertura());
                !inicio.plusMinutes(duracionMinutos).isAfter(cierre);
                inicio = inicio.plusMinutes(configuracion.getIntervaloMinutos())) {
            LocalDateTime fin = inicio.plusMinutes(duracionMinutos);
            if (!inicio.isBefore(ahora) && !hayCruce(inicio, fin, citas)) {
                libres.add(inicio.toLocalTime());
            }
        }
        return libres;
    }
}
