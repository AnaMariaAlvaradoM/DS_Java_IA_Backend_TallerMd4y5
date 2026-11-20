package com.devSenior.sillon.cita.servicio;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.devSenior.sillon.cita.dto.RecordatorioRespuesta;
import com.devSenior.sillon.compartido.excepcion.IntegracionInvalidaException;
import com.devSenior.sillon.ia.dto.MensajeIa;
import com.devSenior.sillon.ia.dto.ResultadoIa;
import com.devSenior.sillon.ia.json.LectorJson;
import com.devSenior.sillon.ia.prompt.BibliotecaPrompts;
import com.devSenior.sillon.ia.servicio.AsistenteIa;
import com.devSenior.sillon.ia.servicio.CreditoIaServicio;

import tools.jackson.databind.JsonNode;

@Service
public class RecordatorioServicio {

	private static final int MENSAJE_MAXIMO = 700;

	private final LecturaCitaServicio lecturaCitaServicio;
	private final CreditoIaServicio creditoIaServicio;
	private final BibliotecaPrompts bibliotecaPrompts;
	private final AsistenteIa asistenteIa;
	private final LectorJson lectorJson;

	public RecordatorioServicio(LecturaCitaServicio lecturaCitaServicio, CreditoIaServicio creditoIaServicio,
			BibliotecaPrompts bibliotecaPrompts, AsistenteIa asistenteIa, LectorJson lectorJson) {
		this.lecturaCitaServicio = lecturaCitaServicio;
		this.creditoIaServicio = creditoIaServicio;
		this.bibliotecaPrompts = bibliotecaPrompts;
		this.asistenteIa = asistenteIa;
		this.lectorJson = lectorJson;
	}

	public RecordatorioRespuesta generar(Long salonId, Long citaId) {
		DatosRecordatorio datos = lecturaCitaServicio.paraRecordatorio(salonId, citaId);
		creditoIaServicio.verificar(salonId);
		String instrucciones = bibliotecaPrompts.cargar("recordatorio.txt", Map.of(
				"salón", datos.salon(),
				"cliente", datos.cliente(),
				"datos", datos.datos()));
		ResultadoIa resultado = asistenteIa.redactar(instrucciones,
				List.of(new MensajeIa("user", "Redacta el recordatorio con los datos verificados.")));
		String mensaje = mensaje(resultado.texto());
		int restantes = creditoIaServicio.consumir(salonId);
		return new RecordatorioRespuesta(datos.citaId(), datos.telefonoCliente(), mensaje, restantes,
				resultado.factura());
	}

	private String mensaje(String texto) {
		JsonNode nodo = lectorJson.leerObjeto(texto);
		String mensaje = lectorJson.texto(nodo, "mensaje");
		if (mensaje.length() > MENSAJE_MAXIMO) {
			throw new IntegracionInvalidaException("El mensaje supera 700 caracteres");
		}
		return mensaje.replace('\u00A0', ' ').replace('\u202F', ' ');
	}

}
