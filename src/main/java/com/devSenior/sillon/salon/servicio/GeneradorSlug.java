package com.devSenior.sillon.salon.servicio;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@Component
public class GeneradorSlug {

	private final SalonRepositorio salonRepositorio;

	public GeneradorSlug(SalonRepositorio salonRepositorio) {
		this.salonRepositorio = salonRepositorio;
	}

	public String generar(String nombre) {
		String base = normalizar(nombre);
		if (base.isEmpty()) {
			base = "salon";
		}
		if (!salonRepositorio.existsBySlug(base)) {
			return base;
		}
		int sufijo = 2;
		String candidato = base + "-" + sufijo;
		while (salonRepositorio.existsBySlug(candidato)) {
			sufijo++;
			candidato = base + "-" + sufijo;
		}
		return candidato;
	}

	static String normalizar(String nombre) {
		String texto = nombre == null ? "" : nombre;
		String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		String minusculas = sinTildes.toLowerCase(Locale.ROOT);
		String conGuiones = minusculas.replaceAll("[^a-z0-9]+", "-");
		return conGuiones.replaceAll("^-+", "").replaceAll("-+$", "");
	}

}
