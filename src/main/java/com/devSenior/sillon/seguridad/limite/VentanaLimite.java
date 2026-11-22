package com.devSenior.sillon.seguridad.limite;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class VentanaLimite {

	private final int maximo;
	private final Duration ventana;
	private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

	public VentanaLimite(int maximo, Duration ventana) {
		this.maximo = maximo;
		this.ventana = ventana;
	}

	public boolean permitir(String clave, Instant ahora) {
		AtomicBoolean permitido = new AtomicBoolean(false);
		estados.compute(clave, (ignorada, actual) -> {
			if (actual == null || !ahora.isBefore(actual.vence())) {
				permitido.set(true);
				return new Estado(1, ahora.plus(ventana));
			}
			if (actual.cuenta() >= maximo) {
				permitido.set(false);
				return actual;
			}
			permitido.set(true);
			return new Estado(actual.cuenta() + 1, actual.vence());
		});
		return permitido.get();
	}

	private record Estado(int cuenta, Instant vence) {
	}

}
