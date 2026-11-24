package com.devsenior.sillon.cita.servicio;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;

@Component
public class ReglasAgenda {

	private final Clock reloj;

	public ReglasAgenda(Clock reloj) {
		this.reloj = reloj;
	}

	/**
	 * Calcula la hora exacta de finalización sumando la duración del servicio.
	 */
	public LocalDateTime calcularFin(LocalDateTime inicio, int duracionMinutos) {
		return inicio.plusMinutes(duracionMinutos);
	}

	/**
	 * Rechaza citas cuya fecha y hora de inicio ya hayan pasado según la zona del negocio.
	 */
	public void validarNoEsEnElPasado(LocalDateTime inicio) {
		if (inicio.isBefore(LocalDateTime.now(reloj))) {
			throw new ExcepcionConflicto("No se puede agendar una cita en una fecha u hora pasada.");
		}
	}

	/**
	 * Valida día domingo, hora de apertura y finalización completa dentro del cierre del salón.
	 */
	public void validarHorarioSalon(
			LocalDateTime inicio,
			LocalDateTime fin,
			ConfiguracionSalon configuracion) {

		if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()) {
			throw new ExcepcionConflicto("El salón no abre los domingos.");
		}

		if (!fin.toLocalDate().equals(inicio.toLocalDate())) {
			throw new ExcepcionConflicto("La cita excede el horario de cierre del salón.");
		}

		LocalTime horaInicio = inicio.toLocalTime();
		LocalTime horaFin = fin.toLocalTime();

		if (horaInicio.isBefore(configuracion.getHoraApertura())) {
			throw new ExcepcionConflicto("El horario solicitado es anterior a la apertura del salón.");
		}

		if (horaFin.isAfter(configuracion.getHoraCierre())) {
			throw new ExcepcionConflicto("La cita excede el horario de cierre del salón.");
		}
	}

	/**
	 * Determina si dos intervalos [inicioA, finA) y [inicioB, finB) se cruzan.
	 * Los intervalos contiguos no se consideran cruzados.
	 */
	public boolean seCruzan(
			LocalDateTime inicioA,
			LocalDateTime finA,
			LocalDateTime inicioB,
			LocalDateTime finB) {

		return inicioA.isBefore(finB) && finA.isAfter(inicioB);
	}

	/**
	 * Verifica que el intervalo solicitado no se cruce con citas existentes que ocupan agenda.
	 */
	public void validarSinCruces(
			LocalDateTime inicio,
			LocalDateTime fin,
			List<Cita> citasExistentes) {

		if (hayCruce(inicio, fin, citasExistentes)) {
			throw new ExcepcionConflicto("El horario solicitado no está disponible.");
		}
	}

	/**
	 * Calcula los horarios libres disponibles en una fecha según el intervalo del salón,
	 * la duración completa del servicio, la hora actual y las citas que ocupan agenda.
	 */
	public List<LocalTime> calcularHorariosLibres(
			LocalDate fecha,
			int duracionMinutos,
			ConfiguracionSalon configuracion,
			List<Cita> citasExistentes) {

		if (duracionMinutos <= 0 || configuracion.getIntervaloMinutos() <= 0) {
			return List.of();
		}

		if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()) {
			return List.of();
		}

		LocalDateTime ahora = LocalDateTime.now(reloj);
		if (fecha.isBefore(ahora.toLocalDate())) {
			return List.of();
		}

		LocalDateTime limiteCierre = fecha.atTime(configuracion.getHoraCierre());
		LocalDateTime inicioSlot = fecha.atTime(configuracion.getHoraApertura());
		int intervalo = configuracion.getIntervaloMinutos();

		List<LocalTime> horariosLibres = new ArrayList<>();
		while (true) {
			LocalDateTime finSlot = calcularFin(inicioSlot, duracionMinutos);
			if (finSlot.isAfter(limiteCierre)) {
				break;
			}
			if (!inicioSlot.isBefore(ahora) && !hayCruce(inicioSlot, finSlot, citasExistentes)) {
				horariosLibres.add(inicioSlot.toLocalTime());
			}
			inicioSlot = inicioSlot.plusMinutes(intervalo);
		}

		return List.copyOf(horariosLibres);
	}

	private boolean hayCruce(
			LocalDateTime inicio,
			LocalDateTime fin,
			List<Cita> citasExistentes) {

		if (citasExistentes == null || citasExistentes.isEmpty()) {
			return false;
		}

		for (Cita cita : citasExistentes) {
			if (cita.getEstado() != null
					&& cita.getEstado().ocupaAgenda()
					&& seCruzan(inicio, fin, cita.getInicio(), cita.getFin())) {
				return true;
			}
		}
		return false;
	}

}
