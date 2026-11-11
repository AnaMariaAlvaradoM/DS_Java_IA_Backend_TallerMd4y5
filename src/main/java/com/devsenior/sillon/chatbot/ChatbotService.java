package com.devsenior.sillon.chatbot;

import com.devsenior.sillon.chatbot.dto.ChatPeticionDto;
import com.devsenior.sillon.chatbot.dto.ChatRespuestaDto;
import com.devsenior.sillon.chatbot.dto.RespuestaIaChatDto;
import com.devsenior.sillon.chatbot.dto.RolChat;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosService;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.dto.FacturaIA;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.solicitud.MotivoSolicitud;
import com.devsenior.sillon.solicitud.SolicitudContacto;
import com.devsenior.sillon.solicitud.SolicitudContactoRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final CreditosService creditosService;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final AsistenteIA asistenteIA;
    private final LectorJsonIA lectorJsonIA;
    private final SolicitudContactoRepository solicitudRepository;
    private final Clock clock;

    public ChatbotService(
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            CreditosService creditosService,
            BibliotecaPrompts bibliotecaPrompts,
            AsistenteIA asistenteIA,
            LectorJsonIA lectorJsonIA,
            SolicitudContactoRepository solicitudRepository,
            Clock clock) {
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.creditosService = creditosService;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.asistenteIA = asistenteIA;
        this.lectorJsonIA = lectorJsonIA;
        this.solicitudRepository = solicitudRepository;
        this.clock = clock;
    }

    public ChatRespuestaDto procesarChat(String slug, ChatPeticionDto peticion) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));

        Long salonId = salon.getId();

        creditosService.verificar(salonId);

        String catalogo = servicioRepository.findBySalonIdAndActivoTrue(salonId)
                .stream()
                .map(s -> String.format("- %s: %d min, $%s", s.getNombre(), s.getDuracionMinutos(), s.getPrecio()))
                .collect(Collectors.joining("\n"));

        if (catalogo.isBlank()) {
            catalogo = "No hay servicios registrados en este momento.";
        }

        String nombreLimpio = sanitizar(peticion.clienteNombre());
        String contactoLimpio = sanitizar(peticion.clienteContacto());
        String clienteTexto = String.format("%s (%s)", nombreLimpio, contactoLimpio);

        String instrucciones = bibliotecaPrompts.cargar("chatbot", Map.of(
                "salon", salon.getNombre(),
                "direccion", String.format("%s, %s", salon.getDireccion(), salon.getCiudad()),
                "horario", obtenerHorarioTexto(salon),
                "catalogo", catalogo,
                "cliente", clienteTexto
        ));

        String historialConversacion = peticion.historial().stream()
                .map(m -> String.format("%s: %s", m.rol() == RolChat.CLIENTE ? "Cliente" : "Asistente", m.texto()))
                .collect(Collectors.joining("\n"));

        RespuestaIA respuestaIA = asistenteIA.completar(instrucciones, historialConversacion);

        creditosService.consumir(salonId);

        RespuestaIaChatDto respuestaDto = lectorJsonIA.leer(respuestaIA, RespuestaIaChatDto.class);

        if (respuestaDto == null || respuestaDto.respuesta() == null || respuestaDto.respuesta().isBlank() || respuestaDto.escalar() == null) {
            throw new RespuestaIAInvalidaException(respuestaIA.proveedor(), respuestaIA.texto());
        }

        String textoRespuesta = respuestaDto.respuesta();
        boolean escalar = respuestaDto.escalar();
        MotivoSolicitud motivoEscalamiento = MotivoSolicitud.PEDIDO_POR_IA;

        if (ReglaAntiPromesas.contienePromesa(textoRespuesta)) {
            textoRespuesta = ReglaAntiPromesas.MENSAJE_SEGURO;
            escalar = true;
            motivoEscalamiento = MotivoSolicitud.REGLA_DE_SEGURIDAD;
        }

        Long solicitudId = null;
        if (escalar) {
            String mensajesFormateados = recortar(historialConversacion, 2000);
            SolicitudContacto solicitud = new SolicitudContacto(
                    salon,
                    peticion.clienteNombre(),
                    peticion.clienteContacto(),
                    mensajesFormateados,
                    motivoEscalamiento,
                    LocalDateTime.now(clock)
            );
            SolicitudContacto guardada = solicitudRepository.save(solicitud);
            solicitudId = guardada.getId();
        }

        return new ChatRespuestaDto(
                textoRespuesta,
                escalar,
                solicitudId,
                FacturaIA.de(respuestaIA)
        );
    }

    private String sanitizar(String entrada) {
        if (entrada == null) return "";
        return entrada.replace("{", "").replace("}", "").replace("\n", " ").replace("\r", " ").trim();
    }

    private String recortar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max);
    }

    private String obtenerHorarioTexto(Salon salon) {
        try {
            if (salon.getConfiguracion() != null
                    && salon.getConfiguracion().getHoraApertura() != null
                    && salon.getConfiguracion().getHoraCierre() != null) {
                return String.format("%s a %s",
                        salon.getConfiguracion().getHoraApertura(),
                        salon.getConfiguracion().getHoraCierre());
            }
        } catch (Exception ignored) {
        }
        return "Consultar directamente con el salón";
    }
}