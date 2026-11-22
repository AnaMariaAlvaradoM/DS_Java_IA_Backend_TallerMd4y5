package com.devsenior.sillon.seguridad;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.devsenior.sillon.usuario.UsuarioRepository;

/** Carga el usuario desde la base de datos para el filtro JWT. */
@Service
public class UsuarioDetallesServicio implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetallesServicio(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
                .map(UsuarioPrincipal::desde)
                .orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con ese correo."));
    }
}
