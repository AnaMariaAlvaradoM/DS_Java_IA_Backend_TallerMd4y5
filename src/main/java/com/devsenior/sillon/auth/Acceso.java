package com.devsenior.sillon.auth;

import static com.devsenior.sillon.compartido.Fallos.fallo;

import com.devsenior.sillon.compartido.Datos;
import com.devsenior.sillon.compartido.Entradas;
import com.devsenior.sillon.compartido.Vistas;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.seguridad.Tokens;
import com.devsenior.sillon.usuario.Usuario;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@Transactional
public class Acceso {
    private final Datos datos;
    private final BCryptPasswordEncoder passwords;
    private final Tokens encoder;
    private final Clock clock;

    Acceso(Datos datos, BCryptPasswordEncoder passwords, Tokens encoder, Clock clock) {
        this.datos = datos;
        this.passwords = passwords;
        this.encoder = encoder;
        this.clock = clock;
    }

    @io.swagger.v3.oas.annotations.tags.Tag(name = "1 Autenticación")
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    Vistas.Sesion registrar(@RequestBody @Valid Entradas.Registro r) {
        String email = email(r.email());
        verificarDisponible(email);
        var s = new Salon();
        s.setNombre(r.salonNombre().strip());
        s.setCiudad(r.ciudad().strip());
        s.setDireccion(r.direccion().strip());
        s.setTelefono(r.telefono().strip());
        String base =
                Normalizer.normalize(s.getNombre(), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "")
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-|-$", "");
        String raiz = base.isEmpty() ? "salon" : base;
        s.setSlug(raiz);
        for (int n = 2;
                !datos.em()
                        .createQuery("select id from Salon where slug=:slug", Long.class)
                        .setParameter("slug", s.getSlug())
                        .getResultList()
                        .isEmpty();
                n++) s.setSlug(raiz + "-" + n);
        s.setFechaCreacion(LocalDateTime.now(clock));
        datos.em().persist(s);
        var u = usuario(s, r.nombre(), email, r.password(), "DUENO");
        datos.em().persist(u);
        return sesion(u);
    }

    public static String email(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public void verificarDisponible(String email) {
        if (!datos.em()
                .createQuery("select id from Usuario where email=:email", Long.class)
                .setParameter("email", email)
                .getResultList()
                .isEmpty()) throw fallo(409, "El email ya está registrado");
    }

    public Usuario usuario(Salon s, String nombre, String email, String password, String rol) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw fallo(400, "password supera el máximo de bytes");
        var u = new Usuario();
        u.setSalon(s);
        u.setNombre(nombre.strip());
        u.setEmail(email);
        u.setPassword(passwords.encode(password));
        u.setRol(rol);
        return u;
    }

    @io.swagger.v3.oas.annotations.tags.Tag(name = "1 Autenticación")
    @PostMapping("/login")
    Vistas.Sesion login(@RequestBody @Valid Entradas.Login r) {
        var u =
                datos
                        .em()
                        .createQuery("from Usuario where email=:email", Usuario.class)
                        .setParameter("email", email(r.email()))
                        .getResultList()
                        .stream()
                        .findFirst();
        if (u.isEmpty()
                || !u.get().getActivo()
                || r.password().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwords.matches(r.password(), u.get().getPassword()))
            throw fallo(401, "Credenciales incorrectas");
        return sesion(u.get());
    }

    private Vistas.Sesion sesion(Usuario u) {
        return new Vistas.Sesion(
                encoder.emitir(u),
                "Bearer",
                480,
                u.getId(),
                u.getNombre(),
                u.getRol(),
                u.getSalon().getId(),
                u.getSalon().getSlug());
    }
}
