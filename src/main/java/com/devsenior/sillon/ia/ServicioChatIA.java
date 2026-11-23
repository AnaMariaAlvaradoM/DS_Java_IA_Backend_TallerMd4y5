
package com.devsenior.sillon.ia;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.devsenior.sillon.compartido.ErrorPasarelaException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.ServicioService;
import com.devsenior.sillon.solicitud.ServicioSolicitudesContacto;
import com.devsenior.sillon.solicitud.SolicitudContacto;

@Service
public class ServicioChatIA {

    private static final String MENSAJE_SEGURO =
            "Su consulta requiere revisión. Una persona del salón se comunicará con usted.";

    private static final List<String> TERMINOS_BLOQUEADOS = List.of(
            "descuento",
            "cupón",
            "cupon",
            "gratis",
            "promoción",
            "promocion",
            "reembols",
            "silla50"
    );

    private final SalonRepository salonRepository;
    private final ServicioService servicioService;
    private final ServicioCreditosIA servicioCreditos;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final AsistenteIA asistenteIA;
    private final LectorJsonIA lectorJson;
    private final ServicioSolicitudesContacto servicioSolicitudes;

    public ServicioChatIA(
            SalonRepository salonRepository,
            ServicioService servicioService,
            ServicioCreditosIA servicioCreditos,
            BibliotecaPrompts bibliotecaPrompts,
            AsistenteIA asistenteIA,
            LectorJsonIA lectorJson,
            ServicioSolicitudesContacto servicioSolicitudes
    ) {
        this.salonRepository = salonRepository;
        this.servicioService = servicioService;
        this.servicioCreditos = servicioCreditos;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.asistenteIA = asistenteIA;
        this.lectorJson = lectorJson;
        this.servicioSolicitudes = servicioSolicitudes;
    }

    public RespuestaChatIA responder(
            String slug,
            SolicitudChatIA solicitud
    ) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Salón no encontrado"
                        )
                );

        Long salonId = salon.getId();

        servicioCreditos.verificar(salonId);

        String instrucciones = bibliotecaPrompts.cargar(
                "chatbot",
                Map.of(
                        "salon", salon.getNombre(),
                        "direccion", salon.getDireccion(),
                        "horario", servicioService.obtenerHorarioParaIA(salonId),
                        "catalogo", servicioService.obtenerCatalogoParaIA(salonId),
                        "cliente", solicitud.clienteNombre()
                )
        );

        String historial = solicitud.historial()
                .stream()
                .map(turno -> {
                    String emisor =
                            turno.rol() == SolicitudChatIA.RolTurno.CLIENTE
                                    ? "Cliente: "
                                    : "Asistente: ";

                    return emisor + turno.texto();
                })
                .collect(Collectors.joining("\n"));

        ProveedorIA.Resultado resultado =
                asistenteIA.generar(instrucciones, historial);

        servicioCreditos.consumir(salonId);

        String respuesta;
        boolean escalar;

        try {
            var objeto = lectorJson.leerObjeto(
                    resultado.contenido()
            );

            respuesta = lectorJson.leerTextoObligatorio(
                    objeto, "respuesta"
            );

            escalar = lectorJson.leerBooleanoObligatorio(
                    objeto, "escalar"
            );

        } catch (IllegalStateException ex) {
            throw new ErrorPasarelaException(
                    "La IA devolvió una respuesta de chatbot inválida"
            );
        }

        SolicitudContacto.Motivo motivo =
                SolicitudContacto.Motivo.PEDIDO_POR_IA;

        String respuestaNormalizada =
                respuesta.toLowerCase(Locale.ROOT);

        boolean respuestaInsegura = TERMINOS_BLOQUEADOS
                .stream()
                .anyMatch(respuestaNormalizada::contains);

        if (respuestaInsegura) {
            respuesta = MENSAJE_SEGURO;
            escalar = true;
            motivo = SolicitudContacto.Motivo.REGLA_DE_SEGURIDAD;
        }

        Long solicitudId = null;

        if (escalar) {
            String mensajesCliente = solicitud.historial()
                    .stream()
                    .filter(turno ->
                            turno.rol() == SolicitudChatIA.RolTurno.CLIENTE
                    )
                    .map(SolicitudChatIA.Turno::texto)
                    .collect(Collectors.joining("\n"));

            SolicitudContacto contacto = servicioSolicitudes.crear(
                    salonId,
                    solicitud.clienteNombre(),
                    solicitud.clienteContacto(),
                    mensajesCliente,
                    motivo
            );

            solicitudId = contacto.getId();
        }

        return new RespuestaChatIA(
                respuesta,
                escalar,
                solicitudId,
                resultado.factura()
        );
    }
}
