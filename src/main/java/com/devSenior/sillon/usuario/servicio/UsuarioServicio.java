package com.devSenior.sillon.usuario.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.usuario.dto.SolicitudActivo;
import com.devSenior.sillon.usuario.dto.UsuarioRespuesta;
import com.devSenior.sillon.usuario.entidad.Usuario;
import com.devSenior.sillon.usuario.mapeador.UsuarioMapeador;
import com.devSenior.sillon.usuario.repositorio.UsuarioRepositorio;

@Service
public class UsuarioServicio {

	private final UsuarioRepositorio usuarioRepositorio;

	public UsuarioServicio(UsuarioRepositorio usuarioRepositorio) {
		this.usuarioRepositorio = usuarioRepositorio;
	}

	@Transactional(readOnly = true)
	public UsuarioRespuesta yo(Long usuarioId) {
		Usuario usuario = usuarioRepositorio.findById(usuarioId)
				.orElseThrow(() -> new NoEncontradoException("El usuario no existe"));
		return UsuarioMapeador.aRespuesta(usuario);
	}

	@Transactional(readOnly = true)
	public List<UsuarioRespuesta> listar(Long salonId) {
		return usuarioRepositorio.findBySalon_IdOrderByIdAsc(salonId).stream()
				.map(UsuarioMapeador::aRespuesta)
				.toList();
	}

	@Transactional
	public UsuarioRespuesta cambiarActivo(Long salonId, Long usuarioId, SolicitudActivo solicitud) {
		Usuario usuario = usuarioRepositorio.findByIdAndSalon_Id(usuarioId, salonId)
				.orElseThrow(() -> new NoEncontradoException("El usuario no existe"));
		usuario.setActivo(solicitud.activo());
		return UsuarioMapeador.aRespuesta(usuarioRepositorio.save(usuario));
	}

}
