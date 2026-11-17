package com.devSenior.sillon.usuario.servicio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devSenior.sillon.auth.servicio.AuthServicio;
import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devSenior.sillon.salon.entidad.Plan;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;
import com.devSenior.sillon.servicio.entidad.Servicio;
import com.devSenior.sillon.servicio.repositorio.ServicioRepositorio;
import com.devSenior.sillon.usuario.dto.EstilistaRespuesta;
import com.devSenior.sillon.usuario.dto.SolicitudEstilista;
import com.devSenior.sillon.usuario.dto.UsuarioRespuesta;
import com.devSenior.sillon.usuario.entidad.Rol;
import com.devSenior.sillon.usuario.entidad.Usuario;
import com.devSenior.sillon.usuario.mapeador.UsuarioMapeador;
import com.devSenior.sillon.usuario.repositorio.UsuarioRepositorio;

@Service
public class EstilistaServicio {

	private final UsuarioRepositorio usuarioRepositorio;
	private final SalonRepositorio salonRepositorio;
	private final ServicioRepositorio servicioRepositorio;
	private final PasswordEncoder passwordEncoder;
	private final int limiteGratis;

	public EstilistaServicio(UsuarioRepositorio usuarioRepositorio, SalonRepositorio salonRepositorio,
			ServicioRepositorio servicioRepositorio, PasswordEncoder passwordEncoder,
			@Value("${sillon.estilistas.limite-gratis}") int limiteGratis) {
		this.usuarioRepositorio = usuarioRepositorio;
		this.salonRepositorio = salonRepositorio;
		this.servicioRepositorio = servicioRepositorio;
		this.passwordEncoder = passwordEncoder;
		this.limiteGratis = limiteGratis;
	}

	@Transactional
	public EstilistaRespuesta crear(Long salonId, SolicitudEstilista solicitud) {
		Salon salon = salonRepositorio.findById(salonId)
				.orElseThrow(() -> new NoEncontradoException("El salón no existe"));
		if (salon.getPlan() == Plan.GRATIS
				&& usuarioRepositorio.countBySalon_IdAndRol(salonId, Rol.ESTILISTA) >= limiteGratis) {
			throw new PagoRequeridoException("El plan GRATIS admite como máximo " + limiteGratis + " estilistas");
		}
		String email = AuthServicio.normalizarCorreo(solicitud.email());
		if (usuarioRepositorio.existsByEmail(email)) {
			throw new ConflictoException("El correo ya está registrado");
		}
		List<Servicio> servicios = resolverActivos(salonId, solicitud.servicioIds());
		Usuario estilista = new Usuario();
		estilista.setNombre(solicitud.nombre().trim());
		estilista.setEmail(email);
		estilista.setPassword(passwordEncoder.encode(solicitud.password()));
		estilista.setRol(Rol.ESTILISTA);
		estilista.setActivo(true);
		estilista.setSalon(salon);
		estilista.setServicios(new HashSet<>(servicios));
		return UsuarioMapeador.aEstilista(usuarioRepositorio.save(estilista));
	}

	@Transactional(readOnly = true)
	public List<EstilistaRespuesta> listar(Long salonId) {
		return usuarioRepositorio.findBySalon_IdAndRolOrderByIdAsc(salonId, Rol.ESTILISTA).stream()
				.map(UsuarioMapeador::aEstilista)
				.toList();
	}

	@Transactional
	public UsuarioRespuesta asignarServicios(Long salonId, Long usuarioId, List<Long> servicioIds) {
		Usuario usuario = usuarioRepositorio.findByIdAndSalon_Id(usuarioId, salonId)
				.orElseThrow(() -> new NoEncontradoException("El usuario no existe"));
		List<Servicio> servicios = resolverActivos(salonId, servicioIds);
		usuario.getServicios().clear();
		usuario.getServicios().addAll(servicios);
		return UsuarioMapeador.aRespuesta(usuarioRepositorio.save(usuario));
	}

	private List<Servicio> resolverActivos(Long salonId, List<Long> servicioIds) {
		List<Servicio> servicios = new ArrayList<>();
		for (Long servicioId : servicioIds) {
			Servicio servicio = servicioRepositorio.findByIdAndSalon_Id(servicioId, salonId)
					.orElseThrow(() -> new NoEncontradoException("El servicio no existe"));
			if (!servicio.isActivo()) {
				throw new FirmaInvalidaException("El servicio está inactivo");
			}
			servicios.add(servicio);
		}
		return servicios;
	}

}
