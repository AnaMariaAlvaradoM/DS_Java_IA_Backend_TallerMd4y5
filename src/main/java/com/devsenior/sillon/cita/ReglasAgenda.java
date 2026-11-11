package com.devsenior.sillon.cita;

import com.devsenior.sillon.compartido.ConflictoExcepcion;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReglasAgenda {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    public void validarHorario(ConfiguracionSalon config, LocalDateTime inicio, LocalDateTime fin) {
        if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY && !config.isAbreDomingos()) {
            throw new ConflictoExcepcion("El salon no abre los domingos");
        }

        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new ConflictoExcepcion("La cita debe iniciar y terminar el mismo dia");
        }

        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFin = fin.toLocalTime();

        if (horaInicio.isBefore(config.getHoraApertura()) || horaFin.isAfter(config.getHoraCierre())) {
            throw new ConflictoExcepcion(
                    String.format("El salon atiende de %s a %s",
                            config.getHoraApertura().format(FORMATO_HORA),
                            config.getHoraCierre().format(FORMATO_HORA))
            );
        }
    }

    public List<String> calcularHorariosLibres(
            ConfiguracionSalon config,
            LocalDate fecha,
            int duracionMinutos,
            List<Cita> citasOcupadas,
            LocalDateTime ahora) {

        List<String> horariosLibres = new ArrayList<>();

        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !config.isAbreDomingos()) {
            return horariosLibres;
        }

        LocalDateTime limiteCierre = fecha.atTime(config.getHoraCierre());
        LocalDateTime inicioTentativo = fecha.atTime(config.getHoraApertura());
        int intervalo = config.getIntervaloMinutos();

        while (!inicioTentativo.plusMinutes(duracionMinutos).isAfter(limiteCierre)) {
            final LocalDateTime candidato = inicioTentativo;
            final LocalDateTime finCandidato = candidato.plusMinutes(duracionMinutos);

            if (candidato.isBefore(ahora)) {
                inicioTentativo = inicioTentativo.plusMinutes(intervalo);
                continue;
            }

            boolean hayCruce = citasOcupadas.stream().anyMatch(cita ->
                    candidato.isBefore(cita.getFin()) && finCandidato.isAfter(cita.getInicio())
            );

            if (!hayCruce) {
                horariosLibres.add(candidato.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
            }

            inicioTentativo = inicioTentativo.plusMinutes(intervalo);
        }
        return horariosLibres;
    }
}