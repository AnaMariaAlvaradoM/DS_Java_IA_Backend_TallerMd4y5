package co.sillon;

import jakarta.validation.Valid;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import static co.sillon.Fallos.fallo;

@RestController
@RequestMapping("/api")
@Transactional
class Agenda {
    private final Datos datos;
    private final Clock clock;
    Agenda(Datos datos, Clock clock) { this.datos=datos; this.clock=clock; }

    private List<ReglasAgenda.Tramo> ocupados(Usuario u, LocalDate fecha) {
        return datos.em().createQuery("""
                from Cita where estilista=:u and inicio<:fin and fin>:desde and estado<>'CANCELADA'
                """,Cita.class).setParameter("u",u).setParameter("desde",fecha.atStartOfDay())
                .setParameter("fin",fecha.plusDays(1).atStartOfDay()).getResultList().stream()
                .map(c -> new ReglasAgenda.Tramo(c.inicio,c.fin)).toList();
    }
    private Usuario profesional(Salon s, long id, Servicio servicio) {
        var u=datos.estilista(s,id);
        if (!u.servicios.contains(servicio)) throw fallo(409,"El estilista no ofrece el servicio solicitado");
        return u;
    }
    record Disponibilidad(LocalDate fecha, List<String> horariosLibres) {}
    @GetMapping("/publico/salones/{slug}/disponibilidad")
    Disponibilidad disponibilidad(@PathVariable String slug, @RequestParam long servicioId, @RequestParam long estilistaId,
                                 @RequestParam LocalDate fecha) {
        var s=datos.publico(slug,false);
        var servicio=datos.servicio(s,servicioId);
        var u=profesional(s,estilistaId,servicio);
        var ocupados=ocupados(u,fecha);
        var ahora=LocalDateTime.now(clock);
        List<String> libres=new ArrayList<>();
        for (var t=fecha.atTime(s.horaApertura); t.isBefore(fecha.atTime(s.horaCierre)); t=t.plusMinutes(s.intervaloMinutos)) {
            if (ReglasAgenda.disponible(s,t,servicio.duracionMinutos,ahora,ocupados))
                libres.add(t.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        return new Disponibilidad(fecha,libres);
    }
    @PostMapping("/publico/salones/{slug}/reservas") @ResponseStatus(HttpStatus.CREATED)
    Vistas.CitaDto reservar(@PathVariable String slug, @RequestBody @Valid Entradas.Reserva r) {
        return crear(datos.publico(slug,true),r,true);
    }
    @PostMapping("/citas") @ResponseStatus(HttpStatus.CREATED) @SecurityRequirement(name="sesion")
    Vistas.CitaDto agendar(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid Entradas.Reserva r) {
        return crear(datos.salon(Actor.de(jwt).salon(),true),r,false);
    }
    private Vistas.CitaDto crear(Salon s, Entradas.Reserva r, boolean publica) {
        var servicio=datos.servicio(s,r.servicioId());
        var u=profesional(s,r.estilistaId(),servicio);
        var ahora=LocalDateTime.now(clock);
        var tramos=ocupados(u,r.inicio().toLocalDate());
        if (!ReglasAgenda.disponible(s,r.inicio(),servicio.duracionMinutos,ahora,tramos))
            throw fallo(409,"El horario solicitado no está disponible");
        var telefono=r.clienteTelefono().replaceAll("[^0-9]","");
        if (telefono.length()<7 || telefono.length()>15) throw fallo(400,"Revisa clienteTelefono");
        var cliente=datos.em().createQuery("from Cliente where salon=:s and telefono=:t",Cliente.class)
            .setParameter("s",s).setParameter("t",telefono).getResultList().stream().findFirst().orElseGet(() -> {
                var nuevo=new Cliente(); nuevo.salon=s; nuevo.nombre=r.clienteNombre().strip(); nuevo.telefono=telefono;
                datos.em().persist(nuevo); return nuevo;
            });
        var c=new Cita();
        c.salon=s; c.cliente=cliente; c.estilista=u; c.servicio=servicio; c.inicio=r.inicio();
        c.fin=c.inicio.plusMinutes(servicio.duracionMinutos); c.precio=servicio.precio; c.nota=r.nota();
        c.estado=publica ? "PENDIENTE" : "CONFIRMADA"; c.origen=publica ? "EN_LINEA" : "SALON";
        c.fechaCreacion=ahora; c.fechaActualizacion=ahora; datos.em().persist(c);
        return Vistas.cita(c);
    }
    @GetMapping("/citas") @SecurityRequirement(name="sesion")
    List<Vistas.CitaDto> listar(@AuthenticationPrincipal Jwt jwt, @RequestParam LocalDate fecha) {
        Actor a=Actor.de(jwt);
        String filtro=a.dueno() ? "" : " and estilista.id=:estilista";
        var q=datos.em().createQuery("from Cita where salon.id=:salon and inicio>=:desde and inicio<:hasta" +
                filtro+" order by inicio,id",Cita.class).setParameter("salon",a.salon())
                .setParameter("desde",fecha.atStartOfDay()).setParameter("hasta",fecha.plusDays(1).atStartOfDay());
        if (!a.dueno()) q.setParameter("estilista",a.id());
        return q.getResultList().stream().map(Vistas::cita).toList();
    }
    @PatchMapping("/citas/{id}/estado") @SecurityRequirement(name="sesion")
    Vistas.CitaDto estado(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody Entradas.CambioEstado r) {
        var a=Actor.de(jwt);
        datos.salon(a.salon(),true);
        var c=datos.cita(a,id);
        if (!a.dueno() && !Set.of("COMPLETADA","NO_ASISTIO").contains(r.estado())) throw fallo(403,"Acción reservada al dueño");
        if (!ReglasAgenda.transicion(c.estado,r.estado())) throw fallo(409,"Transición de estado no permitida");
        c.estado=r.estado(); c.fechaActualizacion=LocalDateTime.now(clock);
        return Vistas.cita(c);
    }
    @GetMapping("/clientes") @SecurityRequirement(name="sesion")
    List<Vistas.ClienteDto> clientes(@AuthenticationPrincipal Jwt jwt) {
        return datos.em().createQuery("from Cliente where salon.id=:s order by nombre,id",Cliente.class)
                .setParameter("s",Actor.de(jwt).salon()).getResultList().stream()
                .map(c -> new Vistas.ClienteDto(c.id,c.nombre,c.telefono)).toList();
    }
}
