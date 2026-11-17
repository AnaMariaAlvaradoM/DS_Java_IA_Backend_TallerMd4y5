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

@Component
public class ReglasAgenda {

    public void validarHorario(ConfiguracionSalon configuracion, LocalDateTime inicio,
            LocalDateTime fin) {
        String horario = "de " + configuracion.getHoraApertura() + " a "
                + configuracion.getHoraCierre();
        if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()) {
            throw new ReglaNegocioException(
                    "El salón no abre los domingos (horario: " + horario + ")");
        }
        boolean mismoDia = inicio.toLocalDate().equals(fin.toLocalDate());
        boolean desdeApertura = !inicio.toLocalTime().isBefore(configuracion.getHoraApertura());
        boolean hastaCierre = !fin.toLocalTime().isAfter(configuracion.getHoraCierre());
        if (!mismoDia || !desdeApertura || !hastaCierre) {
            throw new ReglaNegocioException(
                    "La cita debe estar dentro del horario del salón: " + horario);
        }
    }

    public List<LocalDateTime> calcularHorariosLibres(ConfiguracionSalon configuracion,
            LocalDate fecha, int duracionMinutos, List<Cita> ocupadas,
            LocalDateTime ahora) {
        List<LocalDateTime> libres = new ArrayList<>();
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()) {
            return libres;
        }
        LocalTime apertura = configuracion.getHoraApertura();
        LocalTime cierre = configuracion.getHoraCierre();
        int intervalo = configuracion.getIntervaloMinutos();
        LocalDateTime candidato = fecha.atTime(apertura);
        LocalDateTime finDelDia = fecha.atTime(cierre);
        while (!candidato.plusMinutes(duracionMinutos).isAfter(finDelDia)) {
            final LocalDateTime inicioCandidato = candidato;
            LocalDateTime finCandidato = inicioCandidato.plusMinutes(duracionMinutos);
            boolean yaPaso = !inicioCandidato.isAfter(ahora);
            boolean choca = ocupadas.stream().anyMatch(
                    cita -> seCruzan(inicioCandidato, finCandidato, cita.getInicio(), cita.getFin()));
            if (!yaPaso && !choca) {
                libres.add(inicioCandidato);
            }
            candidato = candidato.plusMinutes(intervalo);
        }
        return libres;
    }

    public boolean seCruzan(LocalDateTime inicioA, LocalDateTime finA, LocalDateTime inicioB,
            LocalDateTime finB) {
        return inicioA.isBefore(finB) && finA.isAfter(inicioB);
    }
}
