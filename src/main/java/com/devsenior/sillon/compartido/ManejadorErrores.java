package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Convierte cualquier excepción en el formato único {codigo, error, mensaje, fecha}. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    private final Clock clock;

    public ManejadorErrores(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorRespuesta> propia(ApiException ex) {
        return responder(ex.getEstado(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorRespuesta> validacionParametros(HandlerMethodValidationException ex) {
        String mensaje = ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonMalFormado(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido o tiene un valor con formato incorrecto");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> parametroFaltante(MissingServletRequestParameterException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + ex.getParameterName());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorRespuesta> cabeceraFaltante(MissingRequestHeaderException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Falta la cabecera obligatoria: " + ex.getHeaderName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El parámetro " + ex.getName() + " tiene un valor inválido");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> rutaInexistente(NoResourceFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, "La ruta no existe");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido en esta ruta");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException ex) {
        return responder(HttpStatus.CONFLICT, "El dato ya existe o viola una restricción");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception ex) {
        log.error("Error inesperado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor");
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(estado.value(), estado.getReasonPhrase(), mensaje,
                LocalDateTime.now(clock));
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
