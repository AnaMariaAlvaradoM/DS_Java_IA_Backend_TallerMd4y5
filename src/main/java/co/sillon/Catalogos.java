package co.sillon;

import java.util.List;
import jakarta.validation.Valid;
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
class Catalogos {
    final Datos datos;
    final Acceso acceso;
    Catalogos(Datos datos, Acceso acceso) { this.datos=datos; this.acceso=acceso; }

    private Salon salon(Jwt jwt, boolean escritura) { return datos.salon(Actor.de(jwt).salon(),escritura); }
    @GetMapping("/salon") @SecurityRequirement(name="sesion")
    Vistas.MiSalon salon(@AuthenticationPrincipal Jwt jwt) { return Vistas.salon(salon(jwt,false)); }

    @PutMapping("/salon/configuracion") @SecurityRequirement(name="sesion")
    Vistas.MiSalon configurar(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid Entradas.Configuracion r) {
        if (!r.horaApertura().isBefore(r.horaCierre())) throw fallo(409,"La apertura debe preceder al cierre");
        if (r.horaApertura().getSecond()!=0 || r.horaCierre().getSecond()!=0 ||
            r.horaApertura().getNano()!=0 || r.horaCierre().getNano()!=0) throw fallo(400,"Indica horas con precisión de minutos");
        var s=salon(jwt,true);
        s.horaApertura=r.horaApertura(); s.horaCierre=r.horaCierre();
        s.abreDomingos=r.abreDomingos(); s.intervaloMinutos=r.intervaloMinutos();
        return Vistas.salon(s);
    }
    @GetMapping("/servicios") @SecurityRequirement(name="sesion")
    List<Vistas.ServicioDto> servicios(@AuthenticationPrincipal Jwt jwt) {
        return datos.servicios(salon(jwt,false)).stream().map(Vistas::servicio).toList();
    }
    @PostMapping("/servicios") @ResponseStatus(HttpStatus.CREATED) @SecurityRequirement(name="sesion")
    Vistas.ServicioDto crearServicio(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid Entradas.NuevoServicio r) {
        var s=new Servicio();
        s.salon=salon(jwt,true); s.nombre=r.nombre().strip(); s.descripcion=r.descripcion();
        s.duracionMinutos=r.duracionMinutos(); s.precio=r.precio(); datos.em().persist(s);
        return Vistas.servicio(s);
    }
    @GetMapping("/estilistas") @SecurityRequirement(name="sesion")
    List<Vistas.Profesional> estilistas(@AuthenticationPrincipal Jwt jwt) {
        return datos.estilistas(salon(jwt,false)).stream().map(Vistas::profesional).toList();
    }
    @PostMapping("/estilistas") @ResponseStatus(HttpStatus.CREATED) @SecurityRequirement(name="sesion")
    Vistas.Profesional estilista(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid Entradas.Estilista r) {
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
        return new Vistas.Catalogo(s.nombre,s.slug,s.ciudad,s.direccion,s.telefono,Vistas.configuracion(s),
                datos.servicios(s).stream().map(Vistas::servicio).toList(),
                datos.estilistas(s).stream().map(Vistas::profesional).toList());
    }
}
