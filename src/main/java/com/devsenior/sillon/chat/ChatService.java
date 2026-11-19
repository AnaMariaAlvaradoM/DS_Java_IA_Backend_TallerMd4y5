package com.devsenior.sillon.chat;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.chat.dto.ChatRequest;
import com.devsenior.sillon.chat.dto.ChatResponse;
import com.devsenior.sillon.compartido.excepcion.ErrorDePasarelaException;
import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.LectorJsonIa;
import com.devsenior.sillon.ia.RespuestaIa;
import com.devsenior.sillon.ia.RespuestaIaInvalidaException;
import com.devsenior.sillon.publico.PublicoService;
import com.devsenior.sillon.salon.CreditosIaService;

@Service
public class ChatService {

    private static final List<String> BLOQUEADAS = List.of(
            "descuento", "cupón", "cupon", "gratis", "promoción", "promocion", "reembols");
    private static final String MENSAJE_SEGURO =
            "Una persona del salón se pondrá en contacto con usted para ayudarle.";

    private final PublicoService catalogo;
    private final CreditosIaService creditos;
    private final BibliotecaPrompts prompts;
    private final AsistenteIa asistente;
    private final LectorJsonIa json;
    private final SolicitudContactoService solicitudes;

    public ChatService(PublicoService catalogo, CreditosIaService creditos, BibliotecaPrompts prompts,
            AsistenteIa asistente, LectorJsonIa json, SolicitudContactoService solicitudes) {
        this.catalogo = catalogo;
        this.creditos = creditos;
        this.prompts = prompts;
        this.asistente = asistente;
        this.json = json;
        this.solicitudes = solicitudes;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChatResponse responder(String slug, ChatRequest peticion) {
        var conocimiento = catalogo.conocimientoChat(slug);
        Long salonId = conocimiento.salonId();
        creditos.verificar(salonId);
        String prompt = prompts.chatbot(conocimiento.salon(), conocimiento.direccion(), conocimiento.horario(),
                conocimiento.catalogo(), peticion.clienteNombre());
        String historial = peticion.historial().stream()
                .map(turno -> (turno.rol().equals("CLIENTE") ? "Cliente: " : "Asistente: ") + turno.texto())
                .collect(Collectors.joining("\n"));
        RespuestaIa resultado;
        try {
            resultado = asistente.responder(prompt, historial);
        } catch (RespuestaIaInvalidaException ex) {
            creditos.consumir(salonId);
            throw respuestaInvalida();
        }
        creditos.consumir(salonId);
        tools.jackson.databind.JsonNode objeto;
        try {
            objeto = json.objeto(resultado.texto());
        } catch (RespuestaIaInvalidaException ex) {
            throw respuestaInvalida();
        }
        if (!objeto.path("respuesta").isString() || objeto.path("respuesta").asString().isBlank()
                || !objeto.path("escalar").isBoolean()) {
            throw respuestaInvalida();
        }
        String respuesta = objeto.path("respuesta").asString();
        String minusculas = respuesta.toLowerCase(Locale.ROOT);
        boolean bloqueada = BLOQUEADAS.stream().anyMatch(minusculas::contains);
        boolean escalado = bloqueada || objeto.path("escalar").asBoolean();
        Long solicitudId = null;
        if (escalado) {
            String mensajesCliente = peticion.historial().stream().filter(turno -> turno.rol().equals("CLIENTE"))
                    .map(turno -> turno.texto()).collect(Collectors.joining("\n"));
            solicitudId = solicitudes.crear(salonId, peticion.clienteNombre(), peticion.clienteContacto(),
                    mensajesCliente, bloqueada ? MotivoSolicitud.REGLA_DE_SEGURIDAD : MotivoSolicitud.PEDIDO_POR_IA);
        }
        return new ChatResponse(bloqueada ? MENSAJE_SEGURO : respuesta, escalado, solicitudId, resultado.factura());
    }

    private static ErrorDePasarelaException respuestaInvalida() {
        return new ErrorDePasarelaException("La IA no devolvió respuesta y escalar con los tipos requeridos");
    }
}
