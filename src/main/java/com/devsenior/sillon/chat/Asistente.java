package com.devsenior.sillon.chat;

import static com.devsenior.sillon.compartido.Fallos.fallo;

import com.devsenior.sillon.compartido.Datos;
import com.devsenior.sillon.compartido.Entradas;
import com.devsenior.sillon.compartido.Vistas;
import com.devsenior.sillon.ia.IaDatos;
import com.devsenior.sillon.ia.ProveedorIa;
import com.devsenior.sillon.seguridad.Actor;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class Asistente {
    private final Datos datos;
    private final IaDatos operaciones;
    private final ProveedorIa ia;
    static final String MENSAJE_SEGURO =
            "Una persona del salón puede ayudarle con esa consulta. Nuestro equipo se pondrá en"
                + " contacto con usted.";
    private static final Pattern OFERTA =
            Pattern.compile(
                    "cup[oó]n|descuento|gratis|gratuit|promoci[oó]n|reembols|[0-9]\\s*%|rebaja",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    Asistente(Datos datos, IaDatos operaciones, ProveedorIa ia) {
        this.datos = datos;
        this.operaciones = operaciones;
        this.ia = ia;
    }

    public record Recordatorio(
            long citaId,
            String mensaje,
            String telefonoCliente,
            ProveedorIa.Factura factura,
            int creditosRestantes) {}

    public record RespuestaChat(
            String respuesta, boolean escalado, Long solicitudId, ProveedorIa.Factura factura) {}

    public static boolean ofrecePromocion(String texto) {
        return OFERTA.matcher(texto).find();
    }

    private String texto(tools.jackson.databind.JsonNode obj, String campo) {
        var n = obj.path(campo);
        if (!n.isTextual() || n.asText().isBlank()) throw fallo(502, "Falta el texto de " + campo);
        return n.asText().strip();
    }

    @io.swagger.v3.oas.annotations.tags.Tag(name = "6 Agenda")
    @PostMapping("/citas/{id}/recordatorio")
    @SecurityRequirement(name = "sesion")
    Recordatorio recordar(@AuthenticationPrincipal Actor a, @PathVariable long id) {
        var c = operaciones.paraCita(a, id);
        var instrucciones =
                ia.promptRecordatorio()
                        .replace("{salon}", c.salon())
                        .replace("{cliente}", c.cliente())
                        .replace("{datos}", c.hechos());
        var r = ia.consultar(instrucciones, "Prepare el borrador de la cita.");
        int saldo = operaciones.consumir(c.salonId());
        var mensaje = texto(ia.leer(r.texto()), "mensaje");
        if (mensaje.length() > 700) throw fallo(502, "El recordatorio supera los 700 caracteres");
        return new Recordatorio(id, mensaje, c.telefono(), r.factura(), saldo);
    }

    @io.swagger.v3.oas.annotations.tags.Tag(name = "0 Público")
    @PostMapping("/publico/salones/{slug}/chat")
    RespuestaChat chat(@PathVariable String slug, @RequestBody @Valid Entradas.Chat p) {
        var c = operaciones.paraChat(slug);
        var prompt =
                ia.promptChat()
                        .replace("{salon}", c.salon())
                        .replace("{direccion}", c.direccion())
                        .replace("{horario}", c.horario())
                        .replace("{catalogo}", c.catalogo())
                        .replace("{cliente}", "la persona del último turno");
        String historial =
                p.historial().stream()
                        .map(t ->
                                (t.rol().equals("CLIENTE") ? "Cliente: " : "Asistente: ")
                                        + t.texto())
                        .collect(Collectors.joining("\n"));
        var r = ia.consultar(prompt, "Nombre del cliente: " + p.clienteNombre() + "\n" + historial);
        operaciones.consumir(c.salonId());
        var objeto = ia.leer(r.texto());
        String respuesta = texto(objeto, "respuesta");
        if (!objeto.path("escalar").isBoolean())
            throw fallo(502, "Falta la decisión de derivación de IA");
        boolean bloqueo = ofrecePromocion(respuesta);
        boolean escalar = bloqueo || objeto.get("escalar").asBoolean();
        Long solicitud =
                escalar
                        ? operaciones.solicitar(
                                c.salonId(), p, bloqueo ? "REGLA_DE_SEGURIDAD" : "PEDIDO_POR_IA")
                        : null;
        return new RespuestaChat(
                bloqueo ? MENSAJE_SEGURO : respuesta, escalar, solicitud, r.factura());
    }

    private Vistas.SolicitudDto vista(Solicitud s) {
        return new Vistas.SolicitudDto(
                s.getId(),
                s.getClienteNombre(),
                s.getClienteContacto(),
                s.getMensajes(),
                s.getMotivo(),
                s.getEstado(),
                s.getFechaCreacion());
    }

    @io.swagger.v3.oas.annotations.tags.Tag(name = "8 Solicitudes")
    @GetMapping("/solicitudes")
    @Transactional(readOnly = true)
    @SecurityRequirement(name = "sesion")
    List<Vistas.SolicitudDto> solicitudes(@AuthenticationPrincipal Actor a) {
        return datos
                .em()
                .createQuery(
                        "from Solicitud where salon.id=:id order by fechaCreacion desc,id desc",
                        Solicitud.class)
                .setParameter("id", a.salon())
                .getResultList()
                .stream()
                .map(this::vista)
                .toList();
    }

    @io.swagger.v3.oas.annotations.tags.Tag(name = "8 Solicitudes")
    @PatchMapping("/solicitudes/{id}/atendida")
    @Transactional
    @SecurityRequirement(name = "sesion")
    Vistas.SolicitudDto atender(@AuthenticationPrincipal Actor a, @PathVariable long id) {
        var s =
                datos
                        .em()
                        .createQuery(
                                "from Solicitud where salon.id=:salon and id=:id", Solicitud.class)
                        .setParameter("salon", a.salon())
                        .setParameter("id", id)
                        .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
                        .getResultList()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> fallo(404, "Solicitud desconocida"));
        if (s.getEstado().equals("ATENDIDA")) throw fallo(409, "La solicitud ya fue atendida");
        s.setEstado("ATENDIDA");
        return vista(s);
    }
}
