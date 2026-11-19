package com.devsenior.sillon.salon.servicio;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.devsenior.sillon.salon.repositorio.SalonRepositorio;

@Service
public class GeneradorSlug {

	private final SalonRepositorio salonRepositorio;

	public GeneradorSlug(SalonRepositorio salonRepositorio) {
		this.salonRepositorio = salonRepositorio;
	}

	public String generar(String nombre) {
		String base = normalizar(nombre);
		if (!salonRepositorio.existsBySlug(base)) {
			return base;
		}
		int sufijo = 2;
		String candidato;
		do {
			candidato = base + "-" + sufijo;
			sufijo++;
		}
		while (salonRepositorio.existsBySlug(candidato));
		return candidato;
	}

	private String normalizar(String nombre) {
		String minusculas = nombre.toLowerCase(Locale.ROOT);
		String sinTildes = Normalizer.normalize(minusculas, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
		String conGuiones = sinTildes.replaceAll("[^a-z0-9]", "-");
		return conGuiones.replaceAll("^-+", "").replaceAll("-+$", "");
	}

}
