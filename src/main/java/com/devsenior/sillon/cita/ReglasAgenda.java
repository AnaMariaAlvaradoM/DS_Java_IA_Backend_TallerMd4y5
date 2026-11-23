
package com.devsenior.sillon.cita;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;

@Component
public class ReglasAgenda {

    public void validarHorario(
            LocalDateTime inicio,
            LocalDateTime fin,
            ConfiguracionSalon configuracion
    ) {

        if (inicio == null || fin == null || configuracion == null) {
            throw new ReglaNegocioException(
                    "Los datos del horario son obligatorios"
            );
        }

        if (!inicio.isBefore(fin)) {
            throw new ReglaNegocioException(
                    "La hora de inicio debe ser anterior al fin"
            );
        }

        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new ReglaNegocioException(
                    "La cita debe iniciar y terminar el mismo día"
            );
        }

        if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY
                && !configuracion.isAbreDomingos()) {
            throw new ReglaNegocioException(
                    "El salón no atiende los domingos"
            );
        }

        if (inicio.toLocalTime().isBefore(
                    configuracion.getHoraApertura())
                || fin.toLocalTime().isAfter(
                    configuracion.getHoraCierre())) {

            throw new ReglaNegocioException(
                    "La cita debe estar dentro del horario del salón: "
                    + configuracion.getHoraApertura()
                    + " a "
                    + configuracion.getHoraCierre()
            );
        }
    }


    public boolean seCruzan(
            LocalDateTime inicioPrimera,
            LocalDateTime finPrimera,
            LocalDateTime inicioSegunda,
            LocalDateTime finSegunda
    ) {
        return inicioPrimera.isBefore(finSegunda)
                && finPrimera.isAfter(inicioSegunda);
    }


    public List<LocalTime> calcularHorariosLibres(
            LocalDate fecha,
            int duracionMinutos,
            ConfiguracionSalon configuracion,
            List<Cita> citas,
            Clock reloj
    ) {
        if (fecha == null || configuracion == null
                || citas == null || reloj == null) {
            throw new IllegalArgumentException(
                    "Los datos de disponibilidad son obligatorios"
            );
        }

        if (duracionMinutos <= 0
                || configuracion.getIntervaloMinutos() <= 0) {
            throw new IllegalArgumentException(
                    "La duración y el intervalo deben ser positivos"
            );
        }

        List<LocalTime> horariosLibres = new ArrayList<>();

        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY
                && !configuracion.isAbreDomingos()) {
            return horariosLibres;
        }

        LocalDateTime apertura = LocalDateTime.of(
                fecha, configuracion.getHoraApertura()
        );

        LocalDateTime cierre = LocalDateTime.of(
                fecha, configuracion.getHoraCierre()
        );

        LocalDateTime ahora = LocalDateTime.now(reloj);

        LocalDateTime inicio = apertura;

        while (!inicio.plusMinutes(duracionMinutos).isAfter(cierre)) {

            LocalDateTime fin = inicio.plusMinutes(duracionMinutos);

            boolean horarioFuturo = !inicio.isBefore(ahora);

            boolean ocupado = false;

            for (Cita cita : citas) {
                if (cita.getEstado() != null
                        && cita.getEstado().ocupaAgenda()
                        && seCruzan(
                                inicio,
                                fin,
                                cita.getInicio(),
                                cita.getFin()
                        )) {
                    ocupado = true;
                    break;
                }
            }

            if (horarioFuturo && !ocupado) {
                horariosLibres.add(inicio.toLocalTime());
            }

            inicio = inicio.plusMinutes(
                    configuracion.getIntervaloMinutos()
            );
        }

        return horariosLibres;
    }


}
