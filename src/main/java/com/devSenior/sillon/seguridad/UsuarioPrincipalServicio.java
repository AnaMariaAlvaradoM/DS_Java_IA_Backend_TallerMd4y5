package com.devSenior.sillon.seguridad;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.devSenior.sillon.usuario.repositorio.UsuarioRepositorio;

@Service
public class UsuarioPrincipalServicio implements UserDetailsService {

	private final UsuarioRepositorio usuarioRepositorio;

	public UsuarioPrincipalServicio(UsuarioRepositorio usuarioRepositorio) {
		this.usuarioRepositorio = usuarioRepositorio;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		return usuarioRepositorio.findByEmail(email)
				.map(UsuarioPrincipal::desde)
				.orElseThrow(() -> new UsernameNotFoundException("No autenticado"));
	}

}
