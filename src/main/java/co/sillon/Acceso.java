package co.sillon;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static co.sillon.Fallos.fallo;

@RestController
@RequestMapping("/api/auth")
@Transactional
class Acceso {
    private final Datos datos;
    private final BCryptPasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final Clock clock;
    Acceso(Datos datos, BCryptPasswordEncoder passwords, JwtEncoder encoder, Clock clock) {
        this.datos=datos; this.passwords=passwords; this.encoder=encoder; this.clock=clock;
    }
    @PostMapping("/registro") @ResponseStatus(HttpStatus.CREATED)
    Vistas.Sesion registrar(@RequestBody @Valid Entradas.Registro r) {
        String email = email(r.email());
        verificarDisponible(email);
        var s = new Salon();
        s.nombre=r.salonNombre().strip(); s.ciudad=r.ciudad().strip(); s.direccion=r.direccion().strip();
        s.telefono=r.telefono().strip();
        String base=Normalizer.normalize(s.nombre,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");
        s.slug=(base.isEmpty() ? "salon" : base) + "-" + UUID.randomUUID().toString().substring(0,8);
        datos.em().persist(s);
        var u = usuario(s,r.nombre(),email,r.password(),"DUENO");
        datos.em().persist(u);
        return sesion(u);
    }
    static String email(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    void verificarDisponible(String email) {
        if (!datos.em().createQuery("select id from Usuario where email=:email",Long.class)
                .setParameter("email",email).getResultList().isEmpty()) throw fallo(409,"El email ya está registrado");
    }
    Usuario usuario(Salon s, String nombre, String email, String password, String rol) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) throw fallo(400,"password supera el máximo de bytes");
        var u=new Usuario();
        u.salon=s; u.nombre=nombre.strip(); u.email=email; u.passwordHash=passwords.encode(password); u.rol=rol;
        return u;
    }
    @PostMapping("/login")
    Vistas.Sesion login(@RequestBody @Valid Entradas.Login r) {
        var u=datos.em().createQuery("from Usuario where email=:email",Usuario.class)
                .setParameter("email",email(r.email())).getResultList().stream().findFirst();
        if (u.isEmpty() || r.password().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwords.matches(r.password(),u.get().passwordHash)) throw fallo(401,"Credenciales incorrectas");
        return sesion(u.get());
    }
    private Vistas.Sesion sesion(Usuario u) {
        Instant ahora=clock.instant();
        var claims=JwtClaimsSet.builder().issuer("sillon").issuedAt(ahora).expiresAt(ahora.plus(Duration.ofHours(8)))
                .subject(u.id.toString()).claim("scope",u.rol).claim("salon",u.salon.id).build();
        var token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims));
        return new Vistas.Sesion(token.getTokenValue(),u.rol,u.salon.slug);
    }
}
