package com.devsenior.sillon.seguridad;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;

public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final Long salonId;
    private final String email;
    private final String password;
    private final Rol rol;
    private final boolean activo;

    private UsuarioPrincipal(Long id, Long salonId, String email, String password, Rol rol, boolean activo) {
        this.id = id;
        this.salonId = salonId;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.activo = activo;
    }

    public static UsuarioPrincipal de(Usuario usuario) {
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
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
