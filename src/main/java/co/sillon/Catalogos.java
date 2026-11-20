package co.sillon;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import static co.sillon.Fallos.fallo;

@RestController
@RequestMapping("/api")
@Transactional
class Catalogos {
    final Datos datos;
    final Acceso acceso;
    Catalogos(Datos datos, Acceso acceso) { this.datos=datos; this.acceso=acceso; }

    private Salon salon(Actor jwt, boolean escritura) { return datos.salon(jwt.salon(),escritura); }
    @GetMapping("/salon") @SecurityRequirement(name="sesion")
    Vistas.MiSalon salon(@AuthenticationPrincipal Actor jwt) { return Vistas.salon(salon(jwt,false)); }

    @PutMapping("/salon/configuracion") @SecurityRequirement(name="sesion")
    Vistas.MiSalon configurar(@AuthenticationPrincipal Actor jwt, @RequestBody @Valid Entradas.Configuracion r) {
        if (!r.horaApertura().isBefore(r.horaCierre())) throw fallo(409,"La apertura debe preceder al cierre");
        if (r.horaApertura().getSecond()!=0 || r.horaCierre().getSecond()!=0 ||
            r.horaApertura().getNano()!=0 || r.horaCierre().getNano()!=0) throw fallo(400,"Indica horas con precisión de minutos");
        var s=salon(jwt,true);
        s.horaApertura=r.horaApertura(); s.horaCierre=r.horaCierre();
        s.abreDomingos=r.abreDomingos(); s.intervaloMinutos=r.intervaloMinutos();
        return Vistas.salon(s);
    }
    @GetMapping("/servicios") @SecurityRequirement(name="sesion")
    List<Vistas.ServicioDto> servicios(@AuthenticationPrincipal Actor jwt) {
        return datos.servicios(salon(jwt,false)).stream().map(Vistas::servicio).toList();
    }
    @PostMapping("/servicios") @ResponseStatus(HttpStatus.CREATED) @SecurityRequirement(name="sesion")
    Vistas.ServicioDto crearServicio(@AuthenticationPrincipal Actor jwt, @RequestBody @Valid Entradas.NuevoServicio r) {
        var s=new Servicio();
        s.salon=salon(jwt,true);
        asignar(s,r); datos.em().persist(s);
        return Vistas.servicio(s);
    }
    @PutMapping("/servicios/{id}") @SecurityRequirement(name="sesion")
    Vistas.ServicioDto editar(@AuthenticationPrincipal Actor jwt,@PathVariable long id,@Valid @RequestBody Entradas.NuevoServicio r) {
        var s=datos.servicio(salon(jwt,true),id); asignar(s,r); return Vistas.servicio(s);
    }
    private void asignar(Servicio s, Entradas.NuevoServicio r) {
        s.activo=r.activo()==null || r.activo(); s.nombre=r.nombre().strip(); s.descripcion=r.descripcion();
        s.duracionMinutos=r.duracionMinutos(); s.precio=r.precio();
    }
    @GetMapping("/estilistas") @SecurityRequirement(name="sesion")
    List<Vistas.Profesional> estilistas(@AuthenticationPrincipal Actor jwt) {
        return datos.estilistas(salon(jwt,false)).stream().map(Vistas::profesional).toList();
    }
    @PostMapping("/estilistas") @ResponseStatus(HttpStatus.CREATED) @SecurityRequirement(name="sesion")
    Vistas.Profesional estilista(@AuthenticationPrincipal Actor jwt, @RequestBody @Valid Entradas.Estilista r) {
        Salon s=salon(jwt,true);
        if (s.plan.equals("GRATIS") && datos.estilistas(s).size()>=2) throw fallo(402,"El plan GRATIS admite dos estilistas");
        String email=Acceso.email(r.email());
        acceso.verificarDisponible(email);
        var u=acceso.usuario(s,r.nombre(),email,r.password(),"ESTILISTA");
        for (Long id : r.servicioIds()) u.servicios.add(datos.servicio(s,id));
        datos.em().persist(u);
        return Vistas.profesional(u);
    }
    @GetMapping("/publico/salones/{slug}")
    Vistas.Catalogo catalogo(@PathVariable String slug) {
        var s=datos.publico(slug,false);
        return new Vistas.Catalogo(s.nombre,s.slug,s.ciudad,s.direccion,s.telefono,horario(s),
                datos.servicios(s).stream().filter(x -> x.activo).map(Vistas::servicio).toList(),
                datos.estilistas(s).stream().filter(u -> u.activo).map(u -> new Vistas.PublicoProfesional(u.id,u.nombre,
                        u.servicios.stream().filter(x -> x.activo).map(x -> x.id).sorted().toList())).toList());
    }
    static String horario(Salon s) {
        return (s.abreDomingos ? "Lunes a domingo" : "Lunes a sábado (domingo cerrado)")+" de "+s.horaApertura+" a "+s.horaCierre;
    }
    @GetMapping("/admin/salones") @SecurityRequirement(name="sesion")
    List<Vistas.MiSalon> plataforma() {
        return datos.em().createQuery("from Salon order by id",Salon.class).getResultList().stream().map(Vistas::salon).toList();
    }
}