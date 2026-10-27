package com.devsenior.sillon.chat;

import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosService;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.dto.FacturaIA;
import com.devsenior.sillon.publico.CatalogoService;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    record RespuestaChatIa(Object respuesta, Object escalar) { }
    private final CatalogoService catalogo;
    private final CreditosService creditos;
    private final BibliotecaPrompts prompts;
    private final AsistenteIA asistente;
    private final LectorJsonIA lector;
    private final ReglaAntiPromesas regla;
    private final SolicitudService solicitudes;

    public ChatService(CatalogoService catalogo, CreditosService creditos, BibliotecaPrompts prompts,
            AsistenteIA asistente, LectorJsonIA lector, ReglaAntiPromesas regla, SolicitudService solicitudes) {
        this.catalogo = catalogo; this.creditos = creditos; this.prompts = prompts; this.asistente = asistente;
        this.lector = lector; this.regla = regla; this.solicitudes = solicitudes;
    }

    public ChatResponse responder(String slug, ChatRequest request) {
        var conocimiento = catalogo.conocimiento(slug);
        creditos.verificar(conocimiento.salonId());
        String instrucciones = prompts.cargar("chatbot", Map.of("salon", conocimiento.nombre(),
                "direccion", conocimiento.direccion(), "horario", conocimiento.horario(),
                "catalogo", conocimiento.catalogo(), "cliente", request.clienteNombre()));
        String historial = request.historial().stream().map(turno ->
                (turno.rol() == RolTurno.CLIENTE ? "Cliente: " : "Asistente: ")
                        + turno.texto().replace('\r', ' ').replace('\n', ' ')).collect(Collectors.joining("\n"));
        var respuestaIa = asistente.completar(instrucciones, historial);
        creditos.consumir(conocimiento.salonId());
        var contenido = lector.leer(respuestaIa, RespuestaChatIa.class);
        if (!(contenido.respuesta() instanceof String respuesta) || respuesta.isBlank()
                || !(contenido.escalar() instanceof Boolean escalar)) {
            throw new RespuestaIAInvalidaException(respuestaIa.proveedor(), respuestaIa.texto());
        }
        boolean bloqueada = regla.bloquear(respuesta);
        Long solicitudId = null;
        if (bloqueada || escalar) {
            String mensajes = request.historial().stream().filter(turno -> turno.rol() == RolTurno.CLIENTE)
                    .map(TurnoChat::texto).collect(Collectors.joining("\n"));
            solicitudId = solicitudes.crear(conocimiento.salonId(), request.clienteNombre(), request.clienteContacto(), mensajes,
                    bloqueada ? MotivoSolicitud.REGLA_DE_SEGURIDAD : MotivoSolicitud.PEDIDO_POR_IA).id();
        }
        return new ChatResponse(bloqueada ? ReglaAntiPromesas.MENSAJE_SEGURO : respuesta, bloqueada || escalar,
                solicitudId, FacturaIA.de(respuestaIa));
    }
}
