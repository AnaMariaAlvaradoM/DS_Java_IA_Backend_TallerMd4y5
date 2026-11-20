package co.sillon;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

final class ReglasAgenda {
    record Tramo(LocalDateTime inicio, LocalDateTime fin) {
        boolean cruza(LocalDateTime desde, LocalDateTime hasta) {
            return inicio.isBefore(hasta) && fin.isAfter(desde);
        }
    }
    static boolean disponible(Salon salon, LocalDateTime inicio, int duracion, LocalDateTime ahora, List<Tramo> ocupados) {
        var fecha=inicio.toLocalDate();
        var apertura=fecha.atTime(salon.horaApertura);
        var fin=inicio.plusMinutes(duracion);
        if (fecha.getDayOfWeek()==DayOfWeek.SUNDAY && !salon.abreDomingos) return false;
        if (!inicio.isAfter(ahora) || inicio.isBefore(apertura) || fin.isAfter(fecha.atTime(salon.horaCierre))) return false;
        if (inicio.getSecond()!=0 || inicio.getNano()!=0 || ChronoUnit.MINUTES.between(apertura,inicio)%salon.intervaloMinutos!=0)
            return false;
        return ocupados.stream().noneMatch(tramo -> tramo.cruza(inicio,fin));
    }
    static boolean transicion(String anterior, String nuevo) {
        return switch (anterior) {
            case "PENDIENTE" -> nuevo.equals("CONFIRMADA") || nuevo.equals("CANCELADA");
            case "CONFIRMADA" -> List.of("COMPLETADA","NO_ASISTIO","CANCELADA").contains(nuevo);
            default -> false;
        };
    }
}
