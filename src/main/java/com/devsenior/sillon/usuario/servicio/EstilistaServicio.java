package com.devsenior.sillon.usuario.servicio;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionPagoRequerido;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.compartido.excepcion.ExcepcionSolicitudInvalida;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.servicio.repositorio.ServicioRepositorio;
import com.devsenior.sillon.usuario.dto.RespuestaEstilista;
import com.devsenior.sillon.usuario.dto.SolicitudEstilista;
import com.devsenior.sillon.usuario.mapper.EstilistaMapper;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.devsenior.sillon.usuario.repositorio.UsuarioRepositorio;

@Service
public class EstilistaServicio {

	private final SalonRepositorio salonRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final ServicioRepositorio servicioRepositorio;
	private final PasswordEncoder passwordEncoder;
	private final EstilistaMapper estilistaMapper;
	private final int limiteGratis;

	public EstilistaServicio(
			SalonRepositorio salonRepositorio,
			UsuarioRepositorio usuarioRepositorio,
			ServicioRepositorio servicioRepositorio,
			PasswordEncoder passwordEncoder,
			EstilistaMapper estilistaMapper,
			@Value("${sillon.estilistas.limite-gratis:2}") int limiteGratis) {

		this.salonRepositorio = salonRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.servicioRepositorio = servicioRepositorio;
		this.passwordEncoder = passwordEncoder;
		this.estilistaMapper = estilistaMapper;
		this.limiteGratis = limiteGratis;
	}

	/**
	 * Registra un estilista en el salón siguiendo el orden de validaciones de SPEC §8.4:
	 * límite del plan (402), unicidad de correo (409) y pertenencia de servicios (404).
	 */
	@Transactional
	public RespuestaEstilista registrar(Long salonId, SolicitudEstilista solicitud) {
		if (solicitud.password().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ExcepcionSolicitudInvalida(
					"password: excede el límite de 72 bytes de BCrypt.");
		}

		Salon salon = salonRepositorio.findById(salonId)
				.orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Salón no encontrado."));

		// 1. Límite comercial del plan GRATIS
		if (salon.getPlan() == Plan.GRATIS
				&& usuarioRepositorio.countBySalonIdAndRol(salonId, Rol.ESTILISTA) >= limiteGratis) {
			throw new ExcepcionPagoRequerido(
					"El plan GRATIS permite un máximo de " + limiteGratis + " estilistas.");
		}

		// 2. Unicidad global del correo normalizado
		String emailNormalizado = solicitud.email().trim().toLowerCase(Locale.ROOT);
		if (usuarioRepositorio.existsByEmail(emailNormalizado)) {
			throw new ExcepcionConflicto("El correo electrónico ya está registrado.");
		}

		// 3. Existencia y pertenencia multi-inquilino de todos los servicios
		List<Long> idsUnicos = solicitud.servicioIds()
				.stream()
				.distinct()
				.toList();

		List<Servicio> servicios = servicioRepositorio.findAllByIdInAndSalonId(idsUnicos, salonId);
		if (servicios.size() != idsUnicos.size()) {
			throw new ExcepcionRecursoNoEncontrado(
					"Uno o más servicios no existen en este salón.");
		}

		Usuario estilista = new Usuario();
		estilista.setNombre(solicitud.nombre());
		estilista.setEmail(emailNormalizado);
		estilista.setPassword(passwordEncoder.encode(solicitud.password()));
		estilista.setRol(Rol.ESTILISTA);
		estilista.setActivo(true);
		estilista.setSalon(salon);
		estilista.setServicios(new ArrayList<>(servicios));

		try {
			Usuario guardado = usuarioRepositorio.saveAndFlush(estilista);
			return estilistaMapper.aRespuesta(guardado);
		} catch (DataIntegrityViolationException excepcion) {
			if (esViolacionUnicidadCorreo(excepcion, emailNormalizado)) {
				throw new ExcepcionConflicto("El correo electrónico ya está registrado.");
			}
			throw excepcion;
		}
	}

	/**
	 * Lista los estilistas del salón autenticado junto con los nombres de sus servicios.
	 */
	@Transactional(readOnly = true)
	public List<RespuestaEstilista> listar(Long salonId) {
		return usuarioRepositorio.buscarEstilistasConServicios(salonId, Rol.ESTILISTA)
				.stream()
				.map(estilistaMapper::aRespuesta)
				.toList();
	}

	private boolean esViolacionUnicidadCorreo(
			DataIntegrityViolationException excepcion,
			String emailNormalizado) {

		Throwable actual = excepcion;
		while (actual != null) {
			if (actual instanceof SQLException sqlExcepcion) {
				String estado = sqlExcepcion.getSQLState();
				int codigo = sqlExcepcion.getErrorCode();
				boolean esUnicidadPostgresOH2 = "23505".equals(estado);
				boolean esUnicidadMysql = "23000".equals(estado) && codigo == 1062;
				if (esUnicidadPostgresOH2 || esUnicidadMysql) {
					String mensaje = sqlExcepcion.getMessage() == null
							? ""
							: sqlExcepcion.getMessage().toLowerCase(Locale.ROOT);
					if (mensaje.contains(emailNormalizado) || mensaje.contains("email")) {
						return true;
					}
				}
			}
			actual = actual.getCause();
		}
		return false;
	}

}
