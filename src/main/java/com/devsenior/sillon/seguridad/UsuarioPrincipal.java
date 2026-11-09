package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UsuarioPrincipal implements UserDetails {
    private final Long id;
    private final Long salonId;
    private final String email;
    private final String password;
    private final Rol rol;
    private final boolean activo;

    public UsuarioPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.salonId = usuario.getSalon().getId();
        this.email = usuario.getEmail();
        this.password = usuario.getPassword();
        this.rol = usuario.getRol();
        this.activo = Boolean.TRUE.equals(usuario.getActivo());
    }

    public Long getId() { return id; }
    public Long getSalonId() { return salonId; }
    public String getEmail() { return email; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return activo; }
}
