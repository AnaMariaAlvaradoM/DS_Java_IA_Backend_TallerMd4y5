package com.devsenior.sillon.cita;

import com.devsenior.sillon.compartido.ConflictoExcepcion;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Component
public class ReglasAgenda {

    public void validarHorario(ConfiguracionSalon config, LocalDateTime inicio, LocalDateTime fin) {

        if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY && !config.isAbreDomingos()) {
            throw new ConflictoExcepcion("El salon no atiende los domingos");
        }

        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new ConflictoExcepcion("La cita debe iniciar y terminar el mismo dia");
        }

        if (inicio.toLocalTime().isBefore(config.getHoraApertura()) || fin.toLocalTime().isAfter(config.getHoraCierre())) {
            throw new ConflictoExcepcion(construirMensajeHorario(config));
        }
    }

    private String construirMensajeHorario(ConfiguracionSalon config) {
        return String.format("El salon atiende de %s a %s", config.getHoraApertura(), config.getHoraCierre());
    }
}
