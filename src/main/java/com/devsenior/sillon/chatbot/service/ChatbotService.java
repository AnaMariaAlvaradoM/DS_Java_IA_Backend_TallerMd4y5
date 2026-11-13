package com.devsenior.sillon.chatbot.service;



import com.devsenior.sillon.chatbot.contacto.model.MotivoSolicitud;
import com.devsenior.sillon.chatbot.contacto.service.SolicitudContactoService;
import com.devsenior.sillon.chatbot.dto.*;

import com.devsenior.sillon.compartido.IaNoDisponibleException;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ServicioExternoException;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.service.CreditoIaService;
import com.devsenior.sillon.ia.util.BibliotecaPrompts;
import com.devsenior.sillon.ia.util.LectorJsonIA;
import com.devsenior.sillon.publico.CatalogoService;
import com.devsenior.sillon.publico.dto.ConocimientoChatbot;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final String MENSAJE_SEGURIDAD_FIJO =
            "Para atender su solicitud sobre promociones, descuentos, reembolsos o condiciones especiales, un asesor de nuestro equipo se pondrá en contacto con usted a la brevedad.";

    private final CatalogoService catalogoService;
    private final CreditoIaService creditoIaService;
    private final AsistenteIA asistenteIA;
    private final LectorJsonIA lectorJsonIA;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final SolicitudContactoService solicitudContactoService;
    private final SalonRepository salonRepository;


    public ChatbotService(CatalogoService catalogoService,
                          CreditoIaService creditoIaService,
                          AsistenteIA asistenteIA,
                          LectorJsonIA lectorJsonIA,
                          BibliotecaPrompts bibliotecaPrompts,
                          SolicitudContactoService solicitudContactoService,
                          SalonRepository salonRepository
                         ) {
        this.catalogoService = catalogoService;
        this.creditoIaService = creditoIaService;
        this.asistenteIA = asistenteIA;
        this.lectorJsonIA = lectorJsonIA;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.solicitudContactoService = solicitudContactoService;
        this.salonRepository = salonRepository;

    }

    // SIN @Transactional en el orquestador (Fase 7)
    public ChatbotResponse procesarMensaje(String slug,
                                           String clienteNombre,
                                           String clienteContacto,
                                           String direccionSalon,
                                           List<MensajeChat> historial) {

        // 1. Validar lo que no depende de la IA y formatear el historial
        if (historial == null || historial.isEmpty()) {
            throw new IllegalArgumentException("El historial de chat no puede estar vacío.");
        }

        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        ConocimientoChatbot conocimiento = catalogoService.obtenerConocimientoParaChatbot(salon.getId());

        String historialFormateado = formatearHistorial(historial);

        // 2. Verificar créditos
        creditoIaService.verificar(salon.getId());

        // 3. Cargar el prompt y llamar a la IA
        RespuestaIA respuestaIa;
        try {
            String systemPrompt = bibliotecaPrompts.cargar("chatbot", Map.of(
                    "salon", slug,
                    "direccion", direccionSalon != null ? direccionSalon : "No especificada",
                    "horario", conocimiento.textoHorario(),
                    "catalogo", conocimiento.textoServicios(),
                    "cliente", clienteNombre
            ));

            // El texto del cliente/historial NUNCA va dentro de las instrucciones del sistema
            String userPrompt = "Historial de la conversación:\n" + historialFormateado;

            respuestaIa = asistenteIA.completar(systemPrompt, userPrompt);

        } catch (IaNoDisponibleException e) {
            throw e; // Pasa directo al ControllerAdvice (HTTP 503)
        } catch (Exception e) {
            throw new ServicioExternoException("Error al procesar la solicitud con la IA: " + e.getMessage());
        }

        // 4. Consumir el crédito atómicamente
        int creditosRestantes = creditoIaService.consumirCredito(salon.getId());

        // 5. Leer y validar el JSON devuelto por la IA
        RespuestaChatbotIA jsonParseado;
        try {
            jsonParseado = lectorJsonIA.leer(respuestaIa, RespuestaChatbotIA.class);
        } catch (Exception e) {
            throw new ServicioExternoException("La IA devolvió un JSON con formato inválido: " + e.getMessage());
        }

        String respuestaTexto = jsonParseado.respuesta();
        if (respuestaTexto == null || respuestaTexto.isBlank()) {
            throw new ServicioExternoException("La IA generó una respuesta vacía.");
        }

        boolean escalado = Boolean.TRUE.equals(jsonParseado.escalar());

        // 6. Aplicar Regla Anti-Promesas (Seguridad en Java) y Escalado
        boolean contienePalabraSensible = evaluarReglaAntiPromesas(respuestaTexto);
        Long solicitudId = null; // <-- Variable para guardar el ID

        if (contienePalabraSensible) {
            escalado = true;
            respuestaTexto = MENSAJE_SEGURIDAD_FIJO;
            String mensajesCliente = extraerMensajesCliente(historial);
            var solicitud = solicitudContactoService.crearSolicitud(
                    slug, clienteNombre, clienteContacto, mensajesCliente, MotivoSolicitud.REGLA_DE_SEGURIDAD
            );
            solicitudId = solicitud.getId(); // Capturamos el ID

        } else if (escalado) {
            String mensajesCliente = extraerMensajesCliente(historial);
            var solicitud = solicitudContactoService.crearSolicitud(
                    slug, clienteNombre, clienteContacto, mensajesCliente, MotivoSolicitud.PEDIDO_POR_IA
            );
            solicitudId = solicitud.getId(); // Capturamos el ID
        }

        return new ChatbotResponse(
                respuestaTexto,
                escalado,       // Variable boolean
                solicitudId,    // ID o null
                respuestaIa
        );
    }

    private String formatearHistorial(List<MensajeChat> historial) {
        return historial.stream()
                .map(m -> {
                    String rol = "CLIENTE".equalsIgnoreCase(m.rol()) ? "Cliente" : "Asistente";
                    return rol + ": " + m.texto();
                })
                .collect(Collectors.joining("\n"));
    }

    private String extraerMensajesCliente(List<MensajeChat> historial) {
        String textoConcatenado = historial.stream()
                .filter(m -> "CLIENTE".equalsIgnoreCase(m.rol()))
                .map(MensajeChat::texto)
                .collect(Collectors.joining(" | "));

        if (textoConcatenado.length() > 2000) {
            return textoConcatenado.substring(0, 2000);
        }
        return textoConcatenado;
    }

    private boolean evaluarReglaAntiPromesas(String texto) {
        if (texto == null) return false;
        String lower = texto.toLowerCase();
        return lower.contains("descuento") ||
                lower.contains("cupón") ||
                lower.contains("cupon") ||
                lower.contains("gratis") ||
                lower.contains("promoción") ||
                lower.contains("promocion") ||
                lower.contains("reembols");
    }

}