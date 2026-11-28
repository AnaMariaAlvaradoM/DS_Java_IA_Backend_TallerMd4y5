package com.devSenior.sillon.ia.servicio;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devSenior.sillon.compartido.excepcion.IntegracionInvalidaException;
import com.devSenior.sillon.compartido.excepcion.ServicioNoDisponibleException;
import com.devSenior.sillon.ia.dto.MensajeIa;
import com.devSenior.sillon.ia.dto.ResultadoIa;
import com.devSenior.sillon.ia.proveedor.ProveedorCaidoException;
import com.devSenior.sillon.ia.proveedor.ProveedorClaude;
import com.devSenior.sillon.ia.proveedor.ProveedorIa;
import com.devSenior.sillon.ia.proveedor.ProveedorGemini;

@Service
public class AsistenteIa {

	private static final Logger BITACORA = LoggerFactory.getLogger(AsistenteIa.class);

	private final ProveedorGemini principal;
	private final ProveedorClaude respaldo;

	public AsistenteIa(ProveedorGemini principal, ProveedorClaude respaldo) {
		this.principal = principal;
		this.respaldo = respaldo;
	}

	public ResultadoIa redactar(String instrucciones, List<MensajeIa> mensajes) {
		IntegracionInvalidaException jsonInvalido = null;
		for (ProveedorIa proveedor : List.of(principal, respaldo)) {
			try {
				return proveedor.completar(instrucciones, mensajes);
			} catch (ProveedorCaidoException excepcion) {
				BITACORA.error("Proveedor de IA no disponible: {}", proveedor.nombre());
			} catch (IntegracionInvalidaException excepcion) {
				BITACORA.error("Proveedor de IA devolvió una respuesta inutilizable: {}", proveedor.nombre());
				jsonInvalido = excepcion;
			}
		}
		if (jsonInvalido != null) {
			throw jsonInvalido;
		}
		throw new ServicioNoDisponibleException("Ningún proveedor de IA está disponible");
	}

}
