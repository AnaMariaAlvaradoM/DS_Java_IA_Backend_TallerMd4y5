package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.usuario.UsuarioRepositorio;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ServicioUsuariosSeguridad implements UserDetailsService {
    private final UsuarioRepositorio usuarios;

    public ServicioUsuariosSeguridad(UsuarioRepositorio usuarios) { this.usuarios = usuarios; }

    @Override public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarios.findByEmail(email).map(UsuarioPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
