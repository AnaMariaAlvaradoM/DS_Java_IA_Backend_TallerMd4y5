package com.devsenior.sillon.cita;

import com.devsenior.sillon.salon.Salon;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class ReglasAgenda {
    public record Tramo(LocalDateTime inicio, LocalDateTime fin) {
        boolean cruza(LocalDateTime desde, LocalDateTime hasta) {
            return inicio.isBefore(hasta) && fin.isAfter(desde);
        }
    }

    public static boolean disponible(
            Salon salon,
            LocalDateTime inicio,
            int duracion,
            LocalDateTime ahora,
            List<Tramo> ocupados) {
        var apertura = inicio.toLocalDate().atTime(salon.getHoraApertura());
        return inicio.getSecond() == 0
                && inicio.getNano() == 0
                && ChronoUnit.MINUTES.between(apertura, inicio) % salon.getIntervaloMinutos() == 0
                && admiteCita(salon, inicio, duracion, ahora, ocupados);
    }

    public static boolean admiteCita(
            Salon salon,
            LocalDateTime inicio,
            int duracion,
            LocalDateTime ahora,
            List<Tramo> ocupados) {
        var fecha = inicio.toLocalDate();
        var apertura = fecha.atTime(salon.getHoraApertura());
        var fin = inicio.plusMinutes(duracion);
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !salon.getAbreDomingos()) return false;
        if (!inicio.isAfter(ahora)
                || inicio.isBefore(apertura)
                || fin.isAfter(fecha.atTime(salon.getHoraCierre()))) return false;
        return ocupados.stream().noneMatch(tramo -> tramo.cruza(inicio, fin));
    }

    public static boolean transicion(String anterior, String nuevo) {
        return switch (anterior) {
            case "PENDIENTE" -> nuevo.equals("CONFIRMADA") || nuevo.equals("CANCELADA");
            case "CONFIRMADA" -> List.of("COMPLETADA", "NO_ASISTIO", "CANCELADA").contains(nuevo);
            default -> false;
        };
    }
}
