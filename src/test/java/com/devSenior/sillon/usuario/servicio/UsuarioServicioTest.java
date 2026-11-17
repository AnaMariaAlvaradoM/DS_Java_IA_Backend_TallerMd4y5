package com.devSenior.sillon.usuario.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.usuario.dto.SolicitudActivo;
import com.devSenior.sillon.usuario.dto.UsuarioRespuesta;
import com.devSenior.sillon.usuario.entidad.Rol;
import com.devSenior.sillon.usuario.entidad.Usuario;
import com.devSenior.sillon.usuario.repositorio.UsuarioRepositorio;

@ExtendWith(MockitoExtension.class)
class UsuarioServicioTest {

	@Mock
	private UsuarioRepositorio usuarioRepositorio;

	@InjectMocks
	private UsuarioServicio usuarioServicio;

	@Test
	void cambiaElActivoDelPropioSalonYOcultaLaContrasena() {
		Usuario usuario = usuario();
		when(usuarioRepositorio.findByIdAndSalon_Id(5L, 1L)).thenReturn(Optional.of(usuario));
		when(usuarioRepositorio.save(usuario)).thenReturn(usuario);

		UsuarioRespuesta apagado = usuarioServicio.cambiarActivo(1L, 5L, new SolicitudActivo(false));
		assertFalse(apagado.activo());
		assertEquals("ana@sillon.test", apagado.email());

		when(usuarioRepositorio.findByIdAndSalon_Id(5L, 2L)).thenReturn(Optional.empty());
		assertThrows(NoEncontradoException.class, () -> usuarioServicio.cambiarActivo(2L, 5L, new SolicitudActivo(true)));
		assertThrows(NoEncontradoException.class, () -> usuarioServicio.cambiarActivo(1L, 99L, new SolicitudActivo(true)));
	}

	private Usuario usuario() {
		Usuario usuario = new Usuario();
		usuario.setId(5L);
		usuario.setNombre("Ana");
		usuario.setEmail("ana@sillon.test");
		usuario.setPassword("$2hash");
		usuario.setRol(Rol.DUENO);
		usuario.setActivo(true);
		return usuario;
	}

}
