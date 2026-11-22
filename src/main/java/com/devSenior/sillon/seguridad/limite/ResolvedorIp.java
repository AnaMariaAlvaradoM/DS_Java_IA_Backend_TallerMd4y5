package com.devSenior.sillon.seguridad.limite;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

public class ResolvedorIp {

	private final Set<String> proxies;

	public ResolvedorIp(String direccionesProxy) {
		if (direccionesProxy == null || direccionesProxy.isBlank()) {
			this.proxies = Set.of();
		} else {
			this.proxies = Arrays.stream(direccionesProxy.split(","))
					.map(String::trim)
					.filter(direccion -> !direccion.isEmpty())
					.collect(Collectors.toUnmodifiableSet());
		}
	}

	public String ip(HttpServletRequest peticion) {
		String remota = peticion.getRemoteAddr() == null ? "" : peticion.getRemoteAddr();
		if (!proxies.contains(remota)) {
			return remota;
		}
		String encabezado = peticion.getHeader("X-Forwarded-For");
		if (encabezado == null || encabezado.isBlank()) {
			return remota;
		}
		String[] partes = encabezado.split(",");
		return partes[partes.length - 1].trim();
	}

}
