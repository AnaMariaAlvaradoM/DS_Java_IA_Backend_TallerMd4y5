package com.devsenior.sillon.cita;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class RecordatorioMapper {

    private static final Locale LOCALE_CO = new Locale("es", "CO");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE_CO);
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("h:mm a", LOCALE_CO);

    private RecordatorioMapper() {
    }

    public static DatosRecordatorio aDatosRecordatorio(Cita cita) {
        return new DatosRecordatorio(cita.getId(), cita.getSalon().getNombre(), cita.getCliente().getNombre(),
                cita.getCliente().getTelefono(), formatearDatos(cita));
    }

    public static String formatearDatos(Cita cita) {
        StringBuilder datos = new StringBuilder();
        datos.append("Servicio: ").append(cita.getServicio().getNombre())
                .append(" (").append(cita.getServicio().getDuracionMinutos()).append(" minutos)\n");
        datos.append("Estilista: ").append(cita.getEstilista().getNombre()).append('\n');
        datos.append("Fecha: ").append(formatearFecha(cita.getInicio().toLocalDate())).append('\n');
        datos.append("Hora: ").append(formatearHora(cita.getInicio().toLocalTime())).append('\n');
        datos.append("Valor: $").append(formatearValor(cita.getPrecio())).append('\n');
        datos.append("Dirección: ").append(cita.getSalon().getDireccion()).append(", ")
                .append(cita.getSalon().getCiudad());
        return datos.toString();
    }

    private static String formatearFecha(LocalDate fecha) {
        return fecha.format(FORMATO_FECHA);
    }

    private static String formatearHora(LocalTime hora) {
        return hora.format(FORMATO_HORA).replace(' ', ' ').replace(' ', ' ');
    }

    private static String formatearValor(Long precio) {
        return NumberFormat.getIntegerInstance(LOCALE_CO).format(precio);
    }
}
