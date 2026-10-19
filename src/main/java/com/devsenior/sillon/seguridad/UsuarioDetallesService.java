package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.usuario.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetallesService implements UserDetailsService {
    private final UsuarioRepository usuarios;

    public UsuarioDetallesService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioPrincipal loadUserByUsername(String email) {
        return usuarios.findByEmail(email).map(UsuarioPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
    }
}
