package com.devsenior.sillon.chat;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosService;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.dto.FacturaIA;
import com.devsenior.sillon.publico.CatalogoService;
import com.devsenior.sillon.publico.SalonPublicoRespuesta;

@Service
public class ChatbotService {

    private static final String MENSAJE_SEGURO = "Gracias por tu mensaje. Una persona de nuestro equipo te va a "
            + "contactar pronto para ayudarte con esto.";
    private static final List<String> PALABRAS_PROHIBIDAS = List.of("descuento", "cupón", "cupon", "gratis",
            "promoción", "promocion", "reembols");

    private final CatalogoService catalogoService;
    private final CreditosService creditosService;
    private final AsistenteIA asistenteIA;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final LectorJsonIA lectorJsonIA;
    private final SolicitudContactoService solicitudContactoService;

    public ChatbotService(CatalogoService catalogoService, CreditosService creditosService,
            AsistenteIA asistenteIA, BibliotecaPrompts bibliotecaPrompts, LectorJsonIA lectorJsonIA,
            SolicitudContactoService solicitudContactoService) {
        this.catalogoService = catalogoService;
        this.creditosService = creditosService;
        this.asistenteIA = asistenteIA;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.lectorJsonIA = lectorJsonIA;
        this.solicitudContactoService = solicitudContactoService;
    }

    public ChatRespuesta chatear(String slug, ChatRequest request) {
        SalonPublicoRespuesta salon = catalogoService.obtenerCatalogo(slug);

        creditosService.verificar(salon.id());

        String catalogo = ChatMapper.formatearCatalogo(salon.servicios());
        String instrucciones = bibliotecaPrompts.cargar("chatbot", Map.of(
                "salon", salon.nombre(),
                "direccion", salon.direccion(),
                "horario", salon.horario(),
                "catalogo", catalogo,
                "cliente", request.clienteNombre()));
        String mensaje = ChatMapper.formatearHistorial(request.historial());
        RespuestaIA respuestaIA = asistenteIA.completar(instrucciones, mensaje);

        creditosService.consumir(salon.id());

        ChatIaRespuesta json = lectorJsonIA.leer(respuestaIA, ChatIaRespuesta.class);
        String respuestaTexto = json.respuesta();
        if (respuestaTexto == null || respuestaTexto.isBlank()) {
            throw new RespuestaIAInvalidaException(respuestaIA.proveedor(), respuestaIA.texto());
        }

        String respuestaFinal = respuestaTexto;
        boolean escalado = false;
        Long solicitudId = null;

        if (contienePalabraProhibida(respuestaTexto)) {
            respuestaFinal = MENSAJE_SEGURO;
            escalado = true;
            solicitudId = solicitudContactoService.crear(salon.id(), request.clienteNombre(),
                    request.clienteContacto(), ChatMapper.formatearMensajesCliente(request.historial()),
                    MotivoSolicitud.REGLA_DE_SEGURIDAD);
        } else if (Boolean.TRUE.equals(json.escalar())) {
            escalado = true;
            solicitudId = solicitudContactoService.crear(salon.id(), request.clienteNombre(),
                    request.clienteContacto(), ChatMapper.formatearMensajesCliente(request.historial()),
                    MotivoSolicitud.PEDIDO_POR_IA);
        }

        return new ChatRespuesta(respuestaFinal, escalado, solicitudId, FacturaIA.de(respuestaIA));
    }

    private boolean contienePalabraProhibida(String texto) {
        String normalizado = texto.toLowerCase();
        return PALABRAS_PROHIBIDAS.stream().anyMatch(normalizado::contains);
    }
}
