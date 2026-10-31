package com.devsenior.sillon.chat;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.devsenior.sillon.publico.ServicioPublicoRespuesta;

public final class ChatMapper {

    private static final Locale LOCALE_CO = new Locale("es", "CO");
    private static final int LONGITUD_MAXIMA_MENSAJES = 2000;

    private ChatMapper() {
    }

    public static String formatearCatalogo(List<ServicioPublicoRespuesta> servicios) {
        return servicios.stream()
                .map(servicio -> "- " + servicio.nombre() + " (" + servicio.duracionMinutos() + " minutos): $"
                        + formatearValor(servicio.precio()))
                .collect(Collectors.joining("\n"));
    }

    public static String formatearHistorial(List<TurnoChat> historial) {
        return historial.stream()
                .map(turno -> (turno.rol() == RolTurno.CLIENTE ? "Cliente: " : "Asistente: ") + turno.texto())
                .collect(Collectors.joining("\n"));
    }

    public static String formatearMensajesCliente(List<TurnoChat> historial) {
        String mensajes = historial.stream()
                .filter(turno -> turno.rol() == RolTurno.CLIENTE)
                .map(TurnoChat::texto)
                .collect(Collectors.joining("\n"));
        return mensajes.length() > LONGITUD_MAXIMA_MENSAJES ? mensajes.substring(0, LONGITUD_MAXIMA_MENSAJES)
                : mensajes;
    }

    private static String formatearValor(Long precio) {
        return NumberFormat.getIntegerInstance(LOCALE_CO).format(precio);
    }
}
