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
    private final Clock reloj;

    public ReglasAgenda(Clock reloj) {
        this.reloj = reloj;
    }

    public void validarNoPasado(LocalDateTime inicio) {
        if (!inicio.isAfter(LocalDateTime.now(reloj))) {
            throw new ReglaNegocioException("No se puede agendar una cita en el pasado");
        }
    }

    public void validarHorario(ConfiguracionSalon configuracion, LocalDateTime inicio, LocalDateTime fin) {
        LocalTime apertura = configuracion.getHoraApertura();
        LocalTime cierre = configuracion.getHoraCierre();
        boolean invalido = inicio.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()
                || !inicio.toLocalDate().equals(fin.toLocalDate())
                || !fin.isAfter(inicio)
                || inicio.toLocalTime().isBefore(apertura)
                || fin.toLocalTime().isAfter(cierre);
        if (invalido) {
            throw new ReglaNegocioException("La cita debe estar dentro del horario del salón ("
                    + apertura + " a " + cierre + ")");
        }
    }

    public List<LocalTime> horariosLibres(ConfiguracionSalon configuracion, LocalDate fecha,
                                          int duracionMinutos, List<Cita> citasOcupadas) {
        if (fecha.isBefore(LocalDate.now(reloj))
                || fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()) {
            return List.of();
        }
        int intervalo = configuracion.getIntervaloMinutos();
        if (intervalo <= 0 || duracionMinutos <= 0) {
            throw new IllegalArgumentException("La duración y el intervalo deben ser positivos");
        }

        List<LocalTime> libres = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime cierre = LocalDateTime.of(fecha, configuracion.getHoraCierre());
        for (LocalDateTime inicio = LocalDateTime.of(fecha, configuracion.getHoraApertura());
             !inicio.plusMinutes(duracionMinutos).isAfter(cierre);
             inicio = inicio.plusMinutes(intervalo)) {
            LocalDateTime candidato = inicio;
            LocalDateTime fin = inicio.plusMinutes(duracionMinutos);
            boolean ocupado = citasOcupadas.stream().anyMatch(cita -> cita.getEstado().ocupaAgenda()
                    && cruzan(candidato, fin, cita.getInicio(), cita.getFin()));
            if (inicio.isAfter(ahora) && !ocupado) {
                libres.add(inicio.toLocalTime());
            }
        }
        return libres;
    }

    public boolean cruzan(LocalDateTime primerInicio, LocalDateTime primerFin,
                          LocalDateTime segundoInicio, LocalDateTime segundoFin) {
        return primerInicio.isBefore(segundoFin) && primerFin.isAfter(segundoInicio);
    }
}
