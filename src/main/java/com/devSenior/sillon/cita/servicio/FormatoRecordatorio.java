package com.devSenior.sillon.cita.servicio;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class FormatoRecordatorio {

	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy",
			new Locale("es", "CO"));

	private FormatoRecordatorio() {
	}

	public static String fecha(LocalDate fecha) {
		return FECHA.format(fecha);
	}

	public static String hora(LocalTime hora) {
		int hora12 = hora.getHour() % 12;
		if (hora12 == 0) {
			hora12 = 12;
		}
		String periodo = hora.getHour() < 12 ? "a. m." : "p. m.";
		return (hora12 + ":" + String.format("%02d", hora.getMinute()) + " " + periodo).replace('\u00A0', ' ')
				.replace('\u202F', ' ');
	}

	public static String dinero(int precio) {
		DecimalFormatSymbols simbolos = new DecimalFormatSymbols(new Locale("es", "CO"));
		simbolos.setGroupingSeparator('.');
		return "$" + new DecimalFormat("#,##0", simbolos).format(precio);
	}

	public static String datos(String servicio, int duracionMinutos, String estilista, LocalDate fecha, LocalTime hora,
			int precio, String direccion) {
		return "Servicio: " + servicio + ". Duración: " + duracionMinutos + " minutos. Estilista: " + estilista
				+ ". Fecha: " + fecha(fecha) + ". Hora: " + hora(hora) + ". Valor: " + dinero(precio) + ". Dirección: "
				+ (direccion == null ? "" : direccion) + ".";
	}

}
