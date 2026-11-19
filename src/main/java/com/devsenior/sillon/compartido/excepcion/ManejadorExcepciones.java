package com.devsenior.sillon.compartido.excepcion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.devsenior.sillon.compartido.dto.RespuestaError;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ManejadorExcepciones {

	private static final Logger REGISTRO = LoggerFactory.getLogger(ManejadorExcepciones.class);

	private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private final Clock reloj;

	public ManejadorExcepciones(Clock reloj) {
		this.reloj = reloj;
	}

	@ExceptionHandler(ExcepcionSillon.class)
	public ResponseEntity<RespuestaError> manejarExcepcionSillon(ExcepcionSillon excepcion) {
		return responder(excepcion.getEstado(), excepcion.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<RespuestaError> manejarValidacion(MethodArgumentNotValidException excepcion) {
		List<String> partes = new ArrayList<>();
		excepcion.getBindingResult().getFieldErrors().forEach(error -> partes.add(
				error.getField() + ": " + texto(error.getDefaultMessage())));
		if (partes.isEmpty()) {
			return responder(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos.");
		}
		return responder(HttpStatus.BAD_REQUEST, String.join("; ", partes));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<RespuestaError> manejarViolaciones(ConstraintViolationException excepcion) {
		List<String> partes = new ArrayList<>();
		for (ConstraintViolation<?> violacion : excepcion.getConstraintViolations()) {
			String campo = violacion.getPropertyPath() == null ? "" : violacion.getPropertyPath().toString();
			String mensaje = texto(violacion.getMessage());
			if (campo.isBlank()) {
				partes.add(mensaje);
			}
			else {
				partes.add(campo + ": " + mensaje);
			}
		}
		if (partes.isEmpty()) {
			return responder(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos.");
		}
		return responder(HttpStatus.BAD_REQUEST, String.join("; ", partes));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<RespuestaError> manejarJsonMalFormado(HttpMessageNotReadableException excepcion) {
		return responder(HttpStatus.BAD_REQUEST, "La solicitud contiene un JSON mal formado.");
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<RespuestaError> manejarParametroFaltante(MissingServletRequestParameterException excepcion) {
		return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio " + excepcion.getParameterName() + ".");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<RespuestaError> manejarTipoInvalido(MethodArgumentTypeMismatchException excepcion) {
		String nombre = excepcion.getName();
		String mensaje = nombre == null || nombre.isBlank()
				? "Un parámetro tiene un tipo inválido."
				: "El parámetro " + nombre + " tiene un tipo inválido.";
		return responder(HttpStatus.BAD_REQUEST, mensaje);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<RespuestaError> manejarExcepcionNoControlada(Exception excepcion) {
		REGISTRO.error("Error no controlado: {}", excepcion.getClass().getName());
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno.");
	}

	private ResponseEntity<RespuestaError> responder(HttpStatus estado, String mensaje) {
		RespuestaError respuesta = new RespuestaError(
				estado.value(),
				estado.getReasonPhrase(),
				mensaje,
				fechaActual());
		return ResponseEntity.status(estado).body(respuesta);
	}

	private String fechaActual() {
		return LocalDateTime.now(reloj).format(FORMATO_FECHA);
	}

	private String texto(String mensaje) {
		return mensaje == null ? "dato inválido" : mensaje;
	}

}
