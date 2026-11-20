package com.devsenior.sillon.cita;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;

@Component
public class ReglasAgenda {

    private final Clock clock;

    public ReglasAgenda(Clock clock) {
        this.clock = clock;
    }

    public void validarHorario(Salon salon, LocalDateTime inicio, LocalDateTime fin) {
        ConfiguracionSalon horario = salon.getConfiguracion();
        LocalDate dia = inicio.toLocalDate();
        if (!esDiaDeAtencion(dia, horario)
                || !dia.equals(fin.toLocalDate())
                || !fin.isAfter(inicio)
                || inicio.toLocalTime().isBefore(horario.getHoraApertura())
                || fin.toLocalTime().isAfter(horario.getHoraCierre())) {
            throw new ReglaDeNegocioException("La cita debe quedar dentro del horario del salón: "
                    + horario.getHoraApertura() + " a " + horario.getHoraCierre()
                    + (horario.isAbreDomingos() ? " (domingos abierto)" : " (domingos cerrado)"));
        }
    }

    public List<LocalTime> calcularHorariosLibres(Salon salon, Long estilistaId, LocalDate dia,
            int duracionMinutos, List<Cita> citas) {
        Long salonId = Objects.requireNonNull(salon.getId(), "El salón debe tener id");
        Objects.requireNonNull(estilistaId, "El estilista debe tener id");
        ConfiguracionSalon horario = salon.getConfiguracion();
        int intervaloMinutos = horario.getIntervaloMinutos();
        if (intervaloMinutos <= 0 || duracionMinutos <= 0) {
            throw new IllegalArgumentException("La duración y el intervalo deben ser positivos");
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!esDiaDeAtencion(dia, horario) || dia.isBefore(ahora.toLocalDate())) {
            return List.of();
        }

        List<Cita> ocupadas = citas.stream()
                .filter(cita -> cita.getEstado().ocupaAgenda())
                .filter(cita -> salonId.equals(cita.getSalon().getId())
                        && estilistaId.equals(cita.getEstilista().getId()))
                .toList();
        LocalDateTime ultimoInicio = dia.atTime(horario.getHoraCierre()).minusMinutes(duracionMinutos);
        List<LocalTime> libres = new ArrayList<>();
        for (LocalDateTime inicio = dia.atTime(horario.getHoraApertura()); !inicio.isAfter(ultimoInicio);
                inicio = inicio.plusMinutes(intervaloMinutos)) {
            if (!inicio.isBefore(ahora) && !hayCruce(ocupadas, inicio, inicio.plusMinutes(duracionMinutos))) {
                libres.add(inicio.toLocalTime());
            }
        }
        return List.copyOf(libres);
    }

    private static boolean esDiaDeAtencion(LocalDate dia, ConfiguracionSalon horario) {
        return dia.getDayOfWeek() != DayOfWeek.SUNDAY || horario.isAbreDomingos();
    }

    private static boolean hayCruce(List<Cita> citas, LocalDateTime inicio, LocalDateTime fin) {
        return citas.stream().anyMatch(cita -> cita.getInicio().isBefore(fin) && cita.getFin().isAfter(inicio));
    }
}
