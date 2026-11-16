package com.devSenior.sillon.salon.repositorio;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import com.devSenior.sillon.salon.entidad.ConfiguracionSalon;
import com.devSenior.sillon.salon.entidad.Plan;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.usuario.entidad.Rol;
import com.devSenior.sillon.usuario.entidad.Usuario;
import com.devSenior.sillon.usuario.repositorio.UsuarioRepositorio;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UnicidadSalonUsuarioTest {

	@Autowired
	private SalonRepositorio salonRepositorio;

	@Autowired
	private UsuarioRepositorio usuarioRepositorio;

	@Test
	void noPermiteDosSalonesConElMismoSlug() {
		salonRepositorio.saveAndFlush(salon("unico"));

		assertThrows(DataIntegrityViolationException.class, () -> salonRepositorio.saveAndFlush(salon("unico")));
	}

	@Test
	void noPermiteDosUsuariosConElMismoEmail() {
		Salon salon = salonRepositorio.saveAndFlush(salon("correo-unico"));
		usuarioRepositorio.saveAndFlush(usuario(salon, "ana@sillon.test"));

		assertThrows(DataIntegrityViolationException.class,
				() -> usuarioRepositorio.saveAndFlush(usuario(salon, "ana@sillon.test")));
	}

	private Salon salon(String slug) {
		Salon salon = new Salon();
		salon.setNombre("Sillón");
		salon.setSlug(slug);
		salon.setPlan(Plan.GRATIS);
		salon.setCreditosIa(10);
		salon.setConfiguracion(ConfiguracionSalon.porDefecto());
		salon.setFechaCreacion(LocalDateTime.of(2026, 11, 11, 9, 0));
		return salon;
	}

	private Usuario usuario(Salon salon, String email) {
		Usuario usuario = new Usuario();
		usuario.setNombre("Ana");
		usuario.setEmail(email);
		usuario.setPassword("hash");
		usuario.setRol(Rol.DUENO);
		usuario.setActivo(true);
		usuario.setSalon(salon);
		return usuario;
	}

}
