package com.devSenior.sillon.cita.reglas;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.devSenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devSenior.sillon.salon.entidad.ConfiguracionSalon;

@Component
public class ReglasAgenda {

	public static final int ANTICIPACION_MAXIMA_DIAS = 90;

	private final Clock reloj;

	public ReglasAgenda(Clock reloj) {
		this.reloj = reloj;
	}

	public void validar(ConfiguracionSalon configuracion, LocalDateTime inicio, LocalDateTime fin) {
		LocalDateTime ahora = LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
		if (!inicio.isAfter(ahora)) {
			throw new FirmaInvalidaException("No se puede agendar en el pasado");
		}
		if (inicio.isAfter(ahora.plusDays(ANTICIPACION_MAXIMA_DIAS))) {
			throw new FirmaInvalidaException("La anticipación máxima es de 90 días");
		}
		if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()) {
			throw new FirmaInvalidaException("abreDomingos: el salón no abre los domingos");
		}
		if (inicio.toLocalTime().isBefore(configuracion.getHoraApertura())) {
			throw new FirmaInvalidaException("horaApertura: la cita empieza antes de abrir");
		}
		if (fin.toLocalTime().isAfter(configuracion.getHoraCierre()) || fin.toLocalDate().isAfter(inicio.toLocalDate())) {
			throw new FirmaInvalidaException("horaCierre: la cita termina después de cerrar");
		}
		long minutosDesdeApertura = Duration.between(configuracion.getHoraApertura(), inicio.toLocalTime()).toMinutes();
		if (minutosDesdeApertura % configuracion.getIntervaloMinutos() != 0) {
			throw new FirmaInvalidaException("intervaloMinutos: el inicio no está alineado al intervalo");
		}
	}

	public boolean seCruzan(LocalDateTime inicioA, LocalDateTime finA, LocalDateTime inicioB, LocalDateTime finB) {
		return inicioA.isBefore(finB) && finA.isAfter(inicioB);
	}

	public List<LocalTime> horariosLibres(ConfiguracionSalon configuracion, LocalDate fecha, int duracionMinutos,
			List<LocalDateTime[]> ocupados) {
		if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()) {
			return List.of();
		}
		LocalDateTime ahora = LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
		LocalDateTime limite = ahora.plusDays(ANTICIPACION_MAXIMA_DIAS);
		List<LocalTime> libres = new ArrayList<>();
		LocalTime cursor = configuracion.getHoraApertura();
		while (!cursor.plusMinutes(duracionMinutos).isAfter(configuracion.getHoraCierre())
				&& cursor.plusMinutes(duracionMinutos).isAfter(cursor)) {
			LocalDateTime inicio = fecha.atTime(cursor);
			LocalDateTime fin = inicio.plusMinutes(duracionMinutos);
			boolean libre = inicio.isAfter(ahora) && !inicio.isAfter(limite) && sinCruce(inicio, fin, ocupados);
			if (libre) {
				libres.add(cursor);
			}
			LocalTime siguiente = cursor.plusMinutes(configuracion.getIntervaloMinutos());
			if (!siguiente.isAfter(cursor)) {
				break;
			}
			cursor = siguiente;
		}
		return libres;
	}

	private boolean sinCruce(LocalDateTime inicio, LocalDateTime fin, List<LocalDateTime[]> ocupados) {
		for (LocalDateTime[] ocupado : ocupados) {
			if (seCruzan(inicio, fin, ocupado[0], ocupado[1])) {
				return false;
			}
		}
		return true;
	}

}
