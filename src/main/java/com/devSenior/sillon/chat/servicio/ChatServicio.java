package com.devSenior.sillon.chat.servicio;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.devSenior.sillon.chat.dto.ChatRespuesta;
import com.devSenior.sillon.chat.dto.RolConversacion;
import com.devSenior.sillon.chat.dto.SolicitudChat;
import com.devSenior.sillon.chat.dto.TurnoChat;
import com.devSenior.sillon.chat.entidad.MotivoSolicitud;
import com.devSenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devSenior.sillon.ia.dto.MensajeIa;
import com.devSenior.sillon.ia.dto.ResultadoIa;
import com.devSenior.sillon.ia.json.LectorJson;
import com.devSenior.sillon.ia.prompt.BibliotecaPrompts;
import com.devSenior.sillon.ia.servicio.AsistenteIa;
import com.devSenior.sillon.ia.servicio.CreditoIaServicio;

import tools.jackson.databind.JsonNode;

@Service
public class ChatServicio {

	static final String MENSAJE_SEGURO = "No puedo confirmar esa condicion comercial. Un asesor del salon revisara tu solicitud.";

	private static final Pattern PROHIBIDO = Pattern.compile("descuento|cup[oó]n|gratis|promoci[oó]n|reembols",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private final ContextoChatServicio contextoChatServicio;
	private final CreditoIaServicio creditoIaServicio;
	private final BibliotecaPrompts bibliotecaPrompts;
	private final AsistenteIa asistenteIa;
	private final LectorJson lectorJson;
	private final SolicitudContactoServicio solicitudContactoServicio;

	public ChatServicio(ContextoChatServicio contextoChatServicio, CreditoIaServicio creditoIaServicio,
			BibliotecaPrompts bibliotecaPrompts, AsistenteIa asistenteIa, LectorJson lectorJson,
			SolicitudContactoServicio solicitudContactoServicio) {
		this.contextoChatServicio = contextoChatServicio;
		this.creditoIaServicio = creditoIaServicio;
		this.bibliotecaPrompts = bibliotecaPrompts;
		this.asistenteIa = asistenteIa;
		this.lectorJson = lectorJson;
		this.solicitudContactoServicio = solicitudContactoServicio;
	}

	public ChatRespuesta responder(String slug, SolicitudChat solicitud) {
		if (solicitud.historial() == null || solicitud.historial().isEmpty()) {
			throw new FirmaInvalidaException("historial: debe tener al menos un turno");
		}
		ContextoChat contexto = contextoChatServicio.cargar(slug);
		creditoIaServicio.verificar(contexto.salonId());
		String instrucciones = bibliotecaPrompts.cargar("chatbot.txt", Map.of(
				"salón", contexto.salon(),
				"dirección", contexto.direccion() == null ? "" : contexto.direccion(),
				"horario", contexto.horario(),
				"catálogo", contexto.catalogo(),
				"cliente", solicitud.clienteNombre()));
		ResultadoIa resultado = asistenteIa.redactar(instrucciones, mensajes(solicitud.historial()));
		JsonNode nodo = lectorJson.leerObjeto(resultado.texto());
		String respuesta = lectorJson.texto(nodo, "respuesta");
		boolean escalar = lectorJson.booleano(nodo, "escalar");
		Long solicitudId = null;
		boolean escalado = escalar;
		if (PROHIBIDO.matcher(respuesta).find()) {
			respuesta = MENSAJE_SEGURO;
			escalado = true;
			solicitudId = solicitudContactoServicio.crear(contexto.salonId(), solicitud.clienteNombre(),
					solicitud.clienteContacto(), contextoDe(solicitud), MotivoSolicitud.REGLA_DE_SEGURIDAD);
		} else if (escalar) {
			solicitudId = solicitudContactoServicio.crear(contexto.salonId(), solicitud.clienteNombre(),
					solicitud.clienteContacto(), contextoDe(solicitud), MotivoSolicitud.PEDIDO_POR_IA);
		}
		creditoIaServicio.consumir(contexto.salonId());
		return new ChatRespuesta(respuesta, escalado, solicitudId, resultado.factura());
	}

	private List<MensajeIa> mensajes(List<TurnoChat> historial) {
		StringBuilder texto = new StringBuilder();
		for (TurnoChat turno : historial) {
			String quien = turno.rol() == RolConversacion.ASISTENTE ? "Asistente: " : "Cliente: ";
			texto.append(quien).append(turno.texto()).append('\n');
		}
		return List.of(new MensajeIa("user", texto.toString().trim()));
	}

	private String contextoDe(SolicitudChat solicitud) {
		StringBuilder texto = new StringBuilder();
		for (TurnoChat turno : solicitud.historial()) {
			String quien = turno.rol() == RolConversacion.ASISTENTE ? "Asistente: " : "Cliente: ";
			texto.append(quien).append(turno.texto()).append('\n');
		}
		return texto.toString();
	}

}
