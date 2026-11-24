package com.devsenior.sillon.cita.modelo;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoCita {

	PENDIENTE,
	CONFIRMADA,
	COMPLETADA,
	CANCELADA,
	NO_ASISTIO;

	/**
	 * Indica si una cita en este estado bloquea disponibilidad en la agenda del estilista.
	 */
	public boolean ocupaAgenda() {
		return this == PENDIENTE || this == CONFIRMADA;
	}

	/**
	 * Verifica las transiciones permitidas según SPEC §9.6.
	 */
	public boolean puedeTransicionarA(EstadoCita destino) {
		if (destino == null || destino == this) {
			return false;
		}
		return switch (this) {
			case PENDIENTE -> transicionesDesdePendiente().contains(destino);
			case CONFIRMADA -> transicionesDesdeConfirmada().contains(destino);
			case COMPLETADA, CANCELADA, NO_ASISTIO -> false;
		};
	}

	private static Set<EstadoCita> transicionesDesdePendiente() {
		return EnumSet.of(CONFIRMADA, CANCELADA);
	}

	private static Set<EstadoCita> transicionesDesdeConfirmada() {
		return EnumSet.of(COMPLETADA, CANCELADA, NO_ASISTIO);
	}

}
