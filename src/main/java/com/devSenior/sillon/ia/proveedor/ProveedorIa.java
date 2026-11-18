package com.devSenior.sillon.ia.proveedor;

import java.util.List;

import com.devSenior.sillon.ia.dto.FacturaIa;
import com.devSenior.sillon.ia.dto.MensajeIa;
import com.devSenior.sillon.ia.dto.ResultadoIa;

public interface ProveedorIa {

	String nombre();

	ResultadoIa completar(String instrucciones, List<MensajeIa> mensajes);

	default FacturaIa factura(String modelo, int entrada, int salida, long milisegundos) {
		return new FacturaIa(nombre(), modelo, entrada, salida, java.math.BigDecimal.ZERO, milisegundos);
	}

}
