package com.devsenior.sillon.compartido.excepcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.devsenior.sillon.compartido.dto.RespuestaError;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

class ManejadorExcepcionesTest {

	private static final String FECHA = "2026-11-18T10:30:00";

	private ManejadorExcepciones manejador;

	@BeforeEach
	void preparar() {
		Clock reloj = Clock.fixed(Instant.parse("2026-11-18T15:30:00Z"), ZoneId.of("America/Bogota"));
		manejador = new ManejadorExcepciones(reloj);
	}

	@ParameterizedTest
	@MethodSource("excepcionesPropias")
	void excepcionPropiaUsaElFormatoUnico(ExcepcionSillon excepcion, HttpStatus estado) {
		ResponseEntity<RespuestaError> respuesta = manejador.manejarExcepcionSillon(excepcion);

		assertEquals(estado, respuesta.getStatusCode());
		RespuestaError cuerpo = respuesta.getBody();
		assertNotNull(cuerpo);
		assertEquals(estado.value(), cuerpo.codigo());
		assertEquals(estado.getReasonPhrase(), cuerpo.error());
		assertEquals(excepcion.getMessage(), cuerpo.mensaje());
		assertEquals(FECHA, cuerpo.fecha());
	}

	@Test
	void validacionUneLosCamposConPuntoYComa() throws NoSuchMethodException {
		BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "solicitud");
		resultado.addError(new FieldError("solicitud", "nombre", "no debe estar vacío"));
		resultado.addError(new FieldError("solicitud", "email", "debe ser un correo válido"));
		MethodArgumentNotValidException excepcion = new MethodArgumentNotValidException(
				new org.springframework.core.MethodParameter(
						ManejadorExcepcionesTest.class.getDeclaredMethod("metodoDePrueba", String.class), 0),
				resultado);

		ResponseEntity<RespuestaError> respuesta = manejador.manejarValidacion(excepcion);

		assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
		RespuestaError cuerpo = respuesta.getBody();
		assertNotNull(cuerpo);
		assertEquals(400, cuerpo.codigo());
		assertEquals("Bad Request", cuerpo.error());
		assertEquals("nombre: no debe estar vacío; email: debe ser un correo válido", cuerpo.mensaje());
		assertEquals(FECHA, cuerpo.fecha());
	}

	@Test
	void violacionesDeRestriccionUnenLosCampos() {
		Set<ConstraintViolation<?>> violaciones = new LinkedHashSet<>();
		violaciones.add(violacion("ciudad", "es obligatoria"));
		violaciones.add(violacion("telefono", "es demasiado largo"));

		ResponseEntity<RespuestaError> respuesta = manejador.manejarViolaciones(
				new ConstraintViolationException(violaciones));

		assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
		RespuestaError cuerpo = respuesta.getBody();
		assertNotNull(cuerpo);
		assertEquals(400, cuerpo.codigo());
		assertTrue(cuerpo.mensaje().contains("ciudad: es obligatoria"));
		assertTrue(cuerpo.mensaje().contains("telefono: es demasiado largo"));
		assertTrue(cuerpo.mensaje().contains("; "));
		assertEquals(FECHA, cuerpo.fecha());
	}

	@Test
	void jsonMalFormadoResponde400SinVolcarElCuerpo() {
		ResponseEntity<RespuestaError> respuesta = manejador.manejarJsonMalFormado(
				new HttpMessageNotReadableException(
						"cuerpo secreto {\"password\":\"clave\"}",
						mock(HttpInputMessage.class)));

		RespuestaError cuerpo = respuesta.getBody();
		assertNotNull(cuerpo);
		assertEquals(400, cuerpo.codigo());
		assertEquals("Bad Request", cuerpo.error());
		assertEquals("La solicitud contiene un JSON mal formado.", cuerpo.mensaje());
		assertFalse(cuerpo.mensaje().contains("password"));
		assertFalse(cuerpo.mensaje().contains("clave"));
		assertEquals(FECHA, cuerpo.fecha());
	}

	@Test
	void parametroFaltanteResponde400() {
		ResponseEntity<RespuestaError> respuesta = manejador.manejarParametroFaltante(
				new MissingServletRequestParameterException("fecha", "String"));

		RespuestaError cuerpo = respuesta.getBody();
		assertNotNull(cuerpo);
		assertEquals(400, cuerpo.codigo());
		assertEquals("Falta el parámetro obligatorio fecha.", cuerpo.mensaje());
		assertEquals(FECHA, cuerpo.fecha());
	}

	@Test
	void parametroConTipoInvalidoResponde400SinVolcarElValor() throws NoSuchMethodException {
		MethodArgumentTypeMismatchException excepcion = new MethodArgumentTypeMismatchException(
				"abc-invalido",
				LocalDate.class,
				"fecha",
				new org.springframework.core.MethodParameter(
						ManejadorExcepcionesTest.class.getDeclaredMethod("metodoDePrueba", String.class), 0),
				new IllegalArgumentException("no es una fecha"));

		ResponseEntity<RespuestaError> respuesta = manejador.manejarTipoInvalido(excepcion);

		assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
		RespuestaError cuerpo = respuesta.getBody();
		assertNotNull(cuerpo);
		assertEquals(400, cuerpo.codigo());
		assertEquals("Bad Request", cuerpo.error());
		assertEquals("El parámetro fecha tiene un tipo inválido.", cuerpo.mensaje());
		assertFalse(cuerpo.mensaje().contains("abc-invalido"));
		assertEquals(FECHA, cuerpo.fecha());
	}

	@Test
	void excepcionNoControladaResponde500SinDetalleTecnico() {
		ResponseEntity<RespuestaError> respuesta = manejador.manejarExcepcionNoControlada(
				new IllegalStateException("token-secreto-no-mostrar"));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
		RespuestaError cuerpo = respuesta.getBody();
		assertNotNull(cuerpo);
		assertEquals(500, cuerpo.codigo());
		assertEquals("Internal Server Error", cuerpo.error());
		assertEquals("Ocurrió un error interno.", cuerpo.mensaje());
		assertFalse(cuerpo.mensaje().contains("token-secreto-no-mostrar"));
		assertEquals(FECHA, cuerpo.fecha());
	}

	private static Stream<org.junit.jupiter.params.provider.Arguments> excepcionesPropias() {
		return Stream.of(
				argumento(new ExcepcionSolicitudInvalida("La solicitud contiene datos inválidos."), HttpStatus.BAD_REQUEST),
				argumento(new ExcepcionNoAutenticado("Credenciales incorrectas."), HttpStatus.UNAUTHORIZED),
				argumento(new ExcepcionPagoRequerido("No hay créditos de IA disponibles."), HttpStatus.PAYMENT_REQUIRED),
				argumento(new ExcepcionNoPermitida("La operación no está permitida para este rol."), HttpStatus.FORBIDDEN),
				argumento(new ExcepcionRecursoNoEncontrado("El recurso no existe."), HttpStatus.NOT_FOUND),
				argumento(new ExcepcionConflicto("El horario solicitado no está disponible."), HttpStatus.CONFLICT),
				argumento(new ExcepcionIntegracion("La respuesta del proveedor no es utilizable."), HttpStatus.BAD_GATEWAY),
				argumento(new ExcepcionServicioNoDisponible("Ningún proveedor de IA está disponible."), HttpStatus.SERVICE_UNAVAILABLE));
	}

	private static org.junit.jupiter.params.provider.Arguments argumento(ExcepcionSillon excepcion, HttpStatus estado) {
		return org.junit.jupiter.params.provider.Arguments.of(excepcion, estado);
	}

	@SuppressWarnings("unchecked")
	private ConstraintViolation<Object> violacion(String campo, String mensaje) {
		ConstraintViolation<Object> violacion = mock(ConstraintViolation.class);
		Path ruta = mock(Path.class);
		when(ruta.toString()).thenReturn(campo);
		when(violacion.getPropertyPath()).thenReturn(ruta);
		when(violacion.getMessage()).thenReturn(mensaje);
		return violacion;
	}

	@SuppressWarnings("unused")
	private void metodoDePrueba(String valor) {
	}

}
