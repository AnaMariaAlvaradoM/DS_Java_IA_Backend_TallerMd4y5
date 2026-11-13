package com.devsenior.sillon.security;
import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final Long salonId;
    private final String email;
    private final String password;
    private final Rol rol;
    private final boolean activo;
    private final Collection<? extends GrantedAuthority> authorities;

    public UsuarioPrincipal(Long id, Long salonId, String email, String password, Rol rol, boolean activo, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.salonId = salonId;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.activo = activo;
        this.authorities = authorities;
    }

    /**
     * Método de fábrica para construir UsuarioPrincipal desde la entidad Usuario.
     */
    public static UsuarioPrincipal build(Usuario usuario) {
        String nombreRol = usuario.getRol().name();
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + nombreRol);

        return new UsuarioPrincipal(
                usuario.getId(),
                usuario.getSalon() != null ? usuario.getSalon().getId() : null,
                usuario.getEmail(),
                usuario.getContrasena(),
                usuario.getRol(),
                true, // o el estado activo del usuario si existe atributo en la entidad
                List.of(authority)
        );
    }

    // --- Métodos de UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
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

    // --- Getters propios para el multitenancy y auditoría ---

    public Long getId() {
        return id;
    }

    public Long getSalonId() {
        return salonId;
    }

    public String getEmail() {
        return email;
    }

    public Rol getRol() {
        return rol;
    }
}