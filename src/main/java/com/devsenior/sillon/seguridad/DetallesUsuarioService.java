package com.devsenior.sillon.seguridad;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.usuario.UsuarioRepository;

@Service
public class DetallesUsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public DetallesUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return usuarioRepository.findByEmail(email)
                .map(UsuarioPrincipal::desde)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
