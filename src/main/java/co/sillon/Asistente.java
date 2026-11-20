package co.sillon;

import jakarta.validation.Valid;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import static co.sillon.Fallos.fallo;

@RestController
@RequestMapping("/api")
@Transactional
class Asistente {
    private final Datos datos;
    private final ProveedorIa ia;
    private final Clock clock;
    private static final Pattern OFERTA=Pattern.compile("cup[oó]n|descuento|gratis|gratuit|promoci[oó]n|[0-9]\\s*%|rebaja",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    private static final Pattern HUMANO=Pattern.compile("reservar|agendar|cancelar mi cita|con (una persona|alguien)|hablar con",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);

    Asistente(Datos datos, ProveedorIa ia, Clock clock) { this.datos=datos; this.ia=ia; this.clock=clock; }
    record Recordatorio(String mensaje, String telefonoCliente, ProveedorIa.Factura factura, int creditosRestantes) {}
    record RespuestaChat(String respuesta, boolean escalado, Long solicitudId, ProveedorIa.Factura factura, int creditosRestantes) {}
    static boolean ofrecePromocion(String texto) { return OFERTA.matcher(texto).find(); }
    private void requiereCredito(Salon s) {
        if (s.creditosIa < 1) throw fallo(402,"El salón agotó sus créditos de IA");
    }
    private String texto(ProveedorIa.Resultado r, String campo) {
        var n=r.contenido().path(campo);
        if (!n.isTextual() || n.asText().isBlank()) throw fallo(502,"La IA devolvió una respuesta incompleta");
        return n.asText().strip();
    }
    @PostMapping("/citas/{id}/recordatorio") @SecurityRequirement(name="sesion")
    Recordatorio recordar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        var a=Actor.de(jwt);
        var s=datos.salon(a.salon(),true);
        var c=datos.cita(a,id);
        if (!Set.of("PENDIENTE","CONFIRMADA").contains(c.estado)) throw fallo(409,"La cita ya terminó o se canceló");
        requiereCredito(s);
        String hechos=String.join("\n","Servicio: "+c.servicio.nombre,"Estilista: "+c.estilista.nombre,
                "Fecha: "+c.inicio.toLocalDate(),"Hora: "+c.inicio.format(DateTimeFormatter.ofPattern("HH:mm")),
                "Valor: "+c.precio.toPlainString()+" COP","Dirección: "+s.direccion);
        var r=ia.consultar(ia.promptRecordatorio+"\n"+hechos,List.of(Map.of("role","user","content","Redacta el recordatorio de esta cita.")));
        var mensaje=texto(r,"mensaje");
        s.creditosIa--;
        return new Recordatorio(mensaje,c.cliente.telefono,r.factura(),s.creditosIa);
    }
    @PostMapping("/publico/salones/{slug}/chat")
    RespuestaChat chat(@PathVariable String slug, @RequestBody @Valid Entradas.Chat peticion) {
        var s=datos.publico(slug,true);
        var ultimo=peticion.historial().getLast();
        if (!ultimo.rol().equals("CLIENTE")) throw fallo(400,"El historial debe terminar con el mensaje del cliente");
        requiereCredito(s);
        var hechos=new StringBuilder(ia.promptChat).append("\nSalón: ").append(s.nombre)
            .append("\nDirección: ").append(s.direccion).append("\nHorario: ").append(s.horaApertura)
            .append(" a ").append(s.horaCierre).append(". Abre domingos: ").append(s.abreDomingos);
        for (var servicio : datos.servicios(s)) hechos.append("\nServicio: ").append(servicio.nombre)
            .append(", ").append(servicio.duracionMinutos).append(" min, ").append(servicio.precio.toPlainString()).append(" COP");
        var mensajes=peticion.historial().stream()
                .map(t -> Map.of("role", t.rol().equals("CLIENTE") ? "user" : "assistant","content",t.texto())).toList();
        var r=ia.consultar(hechos.toString(),mensajes);
        var respuesta=texto(r,"respuesta");
        if (!r.contenido().path("escalar").isBoolean()) throw fallo(502,"Falta la decisión de derivación de IA");
        boolean escalar=r.contenido().get("escalar").asBoolean() || HUMANO.matcher(ultimo.texto()).find();
        if (ofrecePromocion(respuesta)) {
            respuesta="Una persona del salón puede ayudarte con esa consulta."; escalar=true;
        }
        Long solicitudId=null;
        if (escalar) {
            var solicitud=new Solicitud();
            solicitud.salon=s; solicitud.clienteNombre=peticion.clienteNombre(); solicitud.clienteContacto=peticion.clienteContacto();
            solicitud.mensaje=ultimo.texto(); solicitud.fechaCreacion=LocalDateTime.now(clock);
            datos.em().persist(solicitud); solicitudId=solicitud.id;
        }
        s.creditosIa--;
        return new RespuestaChat(respuesta,escalar,solicitudId,r.factura(),s.creditosIa);
    }
    @GetMapping("/solicitudes") @SecurityRequirement(name="sesion")
    List<Vistas.SolicitudDto> solicitudes(@AuthenticationPrincipal Jwt jwt) {
        return datos.em().createQuery("from Solicitud where salon.id=:id order by fechaCreacion desc",Solicitud.class)
                .setParameter("id",Actor.de(jwt).salon()).getResultList().stream()
                .map(s -> new Vistas.SolicitudDto(s.id,s.clienteNombre,s.clienteContacto,s.mensaje,s.fechaCreacion)).toList();
    }
}
