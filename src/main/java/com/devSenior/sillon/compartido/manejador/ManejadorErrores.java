package com.devSenior.sillon.compartido.manejador;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.devSenior.sillon.compartido.dto.ErrorRespuesta;
import com.devSenior.sillon.compartido.excepcion.ExcepcionDeApi;

@RestControllerAdvice
public class ManejadorErrores {

	private final Clock reloj;

	public ManejadorErrores(Clock reloj) {
		this.reloj = reloj;
	}

	@ExceptionHandler(ExcepcionDeApi.class)
	public ResponseEntity<ErrorRespuesta> manejarApi(ExcepcionDeApi excepcion) {
		return responder(excepcion.getCodigo(), excepcion.getError(), excepcion.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException excepcion) {
		String mensaje = excepcion.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return responder(400, "Bad Request", mensaje);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorRespuesta> manejarJsonMalFormado(HttpMessageNotReadableException excepcion) {
		return responder(400, "Bad Request", "El cuerpo JSON está mal formado");
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorRespuesta> manejarParametroFaltante(MissingServletRequestParameterException excepcion) {
		return responder(400, "Bad Request", "Falta el parámetro " + excepcion.getParameterName());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorRespuesta> manejarTipoInvalido(MethodArgumentTypeMismatchException excepcion) {
		return responder(400, "Bad Request", "El parámetro " + excepcion.getName() + " tiene un tipo inválido");
	}

	private ResponseEntity<ErrorRespuesta> responder(int codigo, String error, String mensaje) {
		ErrorRespuesta cuerpo = new ErrorRespuesta(codigo, error, mensaje, ahora());
		return ResponseEntity.status(codigo).body(cuerpo);
	}

	private LocalDateTime ahora() {
		return LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
	}

}
