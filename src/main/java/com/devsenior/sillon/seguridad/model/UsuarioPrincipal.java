package com.devsenior.sillon.seguridad.model;

import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * El usuario autenticado. Lleva su salonId: es la llave del multi-inquilino.
 * Los controladores lo reciben con @AuthenticationPrincipal y le pasan el salonId al servicio.
 */
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final Long salonId;
    private final String nombre;
    private final String email;
    private final String password;
    private final Rol rol;
    private final boolean activo;

    public UsuarioPrincipal(Long id, Long salonId, String nombre, String email, String password, Rol rol, boolean activo) {
        this.id = id;
        this.salonId = salonId;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.activo = activo;
    }

    public static UsuarioPrincipal desde(Usuario usuario) {
        return new UsuarioPrincipal(usuario.getId(), usuario.getSalon().getId(), usuario.getNombre(),
                usuario.getEmail(), usuario.getPassword(), usuario.getRol(), usuario.isActivo());
    }

    public Long getId() {
        return id;
    }

    public Long getSalonId() {
        return salonId;
    }

    public String getNombre() {
        return nombre;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean esEstilista() {
        return rol == Rol.ESTILISTA;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
