package com.devsenior.sillon.cita;

import static com.devsenior.sillon.compartido.Fallos.fallo;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.compartido.Datos;
import com.devsenior.sillon.compartido.Entradas;
import com.devsenior.sillon.compartido.Vistas;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.seguridad.Actor;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Usuario;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api")
@Transactional
public class Agenda {
    private final Datos datos;
    private final Clock clock;

    Agenda(Datos datos, Clock clock) {
        this.datos = datos;
        this.clock = clock;
    }

    private List<ReglasAgenda.Tramo> ocupados(Usuario u, LocalDate fecha) {
        return datos
                .em()
                .createQuery(
                        """
                        from Cita where estilista=:u and inicio<:fin and fin>:desde and estado in (PENDIENTE,CONFIRMADA)
                        """,
                        Cita.class)
                .setParameter("u", u)
                .setParameter("desde", fecha.atStartOfDay())
                .setParameter("fin", fecha.plusDays(1).atStartOfDay())
                .getResultList()
                .stream()
                .map(c -> new ReglasAgenda.Tramo(c.getInicio(), c.getFin()))
                .toList();
    }

    private Usuario profesional(Salon s, long id, Servicio servicio) {
        var u = datos.estilista(s, id);
        if (!servicio.getActivo() || !u.getActivo())
            throw fallo(409, "El servicio o el estilista está inactivo");
        if (!u.getServicios().contains(servicio))
            throw fallo(409, "El estilista no ofrece el servicio solicitado");
        return u;
    }

    public record Disponibilidad(
            LocalDate fecha,
            String servicio,
            String estilista,
            int duracionMinutos,
            List<String> horariosLibres) {}

    @io.swagger.v3.oas.annotations.tags.Tag(name = "0 Público")
    @GetMapping("/publico/salones/{slug}/disponibilidad")
    Disponibilidad disponibilidad(
            @PathVariable String slug,
            @RequestParam long servicioId,
            @RequestParam long estilistaId,
            @RequestParam LocalDate fecha) {
        var s = datos.publico(slug, false);
        var servicio = datos.servicio(s, servicioId);
        var u = profesional(s, estilistaId, servicio);
        var ocupados = ocupados(u, fecha);
        var ahora = LocalDateTime.now(clock);
        List<String> libres = new ArrayList<>();
        for (var t = fecha.atTime(s.getHoraApertura());
                t.isBefore(fecha.atTime(s.getHoraCierre()));
                t = t.plusMinutes(s.getIntervaloMinutos())) {
            if (ReglasAgenda.disponible(s, t, servicio.getDuracionMinutos(), ahora, ocupados))
                libres.add(t.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        return new Disponibilidad(
                fecha, servicio.getNombre(), u.getNombre(), servicio.getDuracionMinutos(), libres);
    }

    @io.swagger.v3.oas.annotations.tags.Tag(name = "0 Público")
    @PostMapping("/publico/salones/{slug}/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    Vistas.CitaDto reservar(@PathVariable String slug, @RequestBody @Valid Entradas.Reserva r) {
        return crear(datos.publico(slug, true), r, true);
    }

    @io.swagger.v3.oas.annotations.tags.Tag(name = "6 Agenda")
    @PostMapping("/citas")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "sesion")
    Vistas.CitaDto agendar(
            @AuthenticationPrincipal Actor jwt, @RequestBody @Valid Entradas.Reserva r) {
        return crear(datos.salon(jwt.salon(), true), r, false);
    }

    private Vistas.CitaDto crear(Salon s, Entradas.Reserva r, boolean publica) {
        var servicio = datos.servicio(s, r.servicioId());
        var u = profesional(s, r.estilistaId(), servicio);
        var ahora = LocalDateTime.now(clock);
        var tramos = ocupados(u, r.inicio().toLocalDate());
        if (!ReglasAgenda.admiteCita(s, r.inicio(), servicio.getDuracionMinutos(), ahora, tramos))
            throw fallo(
                    409,
                    "Horario no disponible. El salón atiende de "
                            + s.getHoraApertura()
                            + " a "
                            + s.getHoraCierre());
        var telefono = r.clienteTelefono().replaceAll("[^+0-9]", "");
        if (telefono.length() < 7 || !telefono.matches("\\+?[0-9]{7,30}"))
            throw fallo(400, "Revisa clienteTelefono");
        var cliente =
                datos
                        .em()
                        .createQuery("from Cliente where salon=:s and telefono=:t", Cliente.class)
                        .setParameter("s", s)
                        .setParameter("t", telefono)
                        .getResultList()
                        .stream()
                        .findFirst()
                        .orElseGet(() -> {
                            var nuevo = new Cliente();
                            nuevo.setSalon(s);
                            nuevo.setNombre(r.clienteNombre().strip());
                            nuevo.setTelefono(telefono);
                            datos.em().persist(nuevo);
                            return nuevo;
                        });
        var c = new Cita();
        c.setSalon(s);
        c.setCliente(cliente);
        c.setEstilista(u);
        c.setServicio(servicio);
        c.setInicio(r.inicio());
        c.setFin(c.getInicio().plusMinutes(servicio.getDuracionMinutos()));
        c.setPrecio(servicio.getPrecio());
        c.setNota(r.nota());
        c.setEstado(publica ? "PENDIENTE" : "CONFIRMADA");
        c.setOrigen(publica ? "EN_LINEA" : "SALON");
        c.setFechaCreacion(ahora);
        c.setFechaActualizacion(ahora);
        datos.em().persist(c);
        return Vistas.cita(c);
    }

    @io.swagger.v3.oas.annotations.tags.Tag(name = "6 Agenda")
    @GetMapping("/citas")
    @SecurityRequirement(name = "sesion")
    List<Vistas.CitaDto> listar(
            @AuthenticationPrincipal Actor jwt, @RequestParam(required = false) LocalDate fecha) {
        if (fecha == null) fecha = LocalDate.now(clock);
        Actor a = jwt;
        String filtro = a.dueno() ? "" : " and estilista.id=:estilista";
        var q =
                datos.em()
                        .createQuery(
                                "from Cita where salon.id=:salon and inicio>=:desde and"
                                    + " inicio<:hasta"
                                        + filtro
                                        + " order by inicio,id",
                                Cita.class)
                        .setParameter("salon", a.salon())
                        .setParameter("desde", fecha.atStartOfDay())
                        .setParameter("hasta", fecha.plusDays(1).atStartOfDay());
        if (!a.dueno()) q.setParameter("estilista", a.id());
        return q.getResultList().stream().map(Vistas::cita).toList();
    }

    @io.swagger.v3.oas.annotations.tags.Tag(name = "6 Agenda")
    @PatchMapping("/citas/{id}/estado")
    @SecurityRequirement(name = "sesion")
    Vistas.CitaDto estado(
            @AuthenticationPrincipal Actor jwt,
            @PathVariable long id,
            @Valid @RequestBody Entradas.CambioEstado r) {
        var a = jwt;
        datos.salon(a.salon(), true);
        var c = datos.cita(a, id);
        if (!a.dueno() && !Set.of("COMPLETADA", "NO_ASISTIO").contains(r.estado()))
            throw fallo(403, "Acción reservada al dueño");
        if (!ReglasAgenda.transicion(c.getEstado(), r.estado()))
            throw fallo(409, "Transición de estado no permitida");
        c.setEstado(r.estado());
        c.setFechaActualizacion(LocalDateTime.now(clock));
        return Vistas.cita(c);
    }

    @io.swagger.v3.oas.annotations.tags.Tag(name = "5 Clientes")
    @GetMapping("/clientes")
    @SecurityRequirement(name = "sesion")
    List<Vistas.ClienteDto> clientes(@AuthenticationPrincipal Actor jwt) {
        return datos
                .em()
                .createQuery("from Cliente where salon.id=:s order by nombre,id", Cliente.class)
                .setParameter("s", jwt.salon())
                .getResultList()
                .stream()
                .map(c -> new Vistas.ClienteDto(c.getId(), c.getNombre(), c.getTelefono()))
                .toList();
    }
}
