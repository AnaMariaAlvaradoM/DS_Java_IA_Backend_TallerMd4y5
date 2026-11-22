package com.devsenior.sillon.seguridad;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;

/**
 * El usuario autenticado. Es la llave del multi-inquilino: lleva el salonId, y los controladores lo reciben con
 * {@code @AuthenticationPrincipal} para pasarle ese salonId al servicio. Nunca se confia en un id que llegue
 * en la URL o en el cuerpo.
 */
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final Long salonId;
    private final String email;
    private final String password;
    private final Rol rol;
    private final boolean activo;

    public UsuarioPrincipal(Long id, Long salonId, String email, String password, Rol rol, boolean activo) {
        this.id = id;
        this.salonId = salonId;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.activo = activo;
    }

    /** Requiere el salon cargado: UsuarioRepository.findByEmail lo trae con un join. */
    public static UsuarioPrincipal desde(Usuario usuario) {
        return new UsuarioPrincipal(usuario.getId(), usuario.getSalon().getId(), usuario.getEmail(),
                usuario.getPassword(), usuario.getRol(), usuario.isActivo());
    }

    public Long getId() {
        return id;
    }

    public Long getSalonId() {
        return salonId;
    }

    public Rol getRol() {
        return rol;
    }

    /** La autoridad es "ROLE_" + el rol. Por eso las reglas usan hasRole("DUENO"), sin el prefijo. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    /** Spring llama "username" al identificador de acceso; aqui es el correo. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
