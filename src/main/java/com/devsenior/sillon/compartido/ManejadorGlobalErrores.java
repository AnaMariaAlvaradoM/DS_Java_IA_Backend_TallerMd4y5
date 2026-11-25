package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Convierte cada excepcion en el formato unico de error (codigo, error, mensaje, fecha).
 * Las excepciones de negocio llegan desde los servicios; las de validacion las lanza Spring.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private final Clock reloj;

    public ManejadorGlobalErrores(Clock reloj) {
        this.reloj = reloj;
    }

    // ---------- Excepciones propias: una por cada codigo de la tabla del contrato ----------

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorRespuesta> reglaDeNegocio(ReglaNegocioException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PagoRequeridoException.class)
    public ResponseEntity<ErrorRespuesta> pagoRequerido(PagoRequeridoException ex) {
        return responder(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler(AccionNoPermitidaException.class)
    public ResponseEntity<ErrorRespuesta> accionNoPermitida(AccionNoPermitidaException ex) {
        return responder(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorRespuesta> credencialesInvalidas(CredencialesInvalidasException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(FirmaInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> firmaInvalida(FirmaInvalidaException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PasarelaException.class)
    public ResponseEntity<ErrorRespuesta> errorDePasarela(PasarelaException ex) {
        return responder(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(IaNoDisponibleException.class)
    public ResponseEntity<ErrorRespuesta> iaNoDisponible(IaNoDisponibleException ex) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    // ---------- Errores de la peticion que detecta Spring (siempre 400) ----------

    /** @Valid fallo: "campo: mensaje; campo: mensaje", ordenados por campo para que el texto sea estable. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (mensaje.isBlank()) {
            mensaje = "Los datos enviados no son válidos.";
        }
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonMalFormado(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición está vacío o no es un JSON válido.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> parametroFaltante(MissingServletRequestParameterException ex) {
        return responder(HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> parametroConTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido: '" + ex.getValue() + "'.");
    }

    // ---------- Construccion del cuerpo ----------

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(ErrorRespuesta.de(estado, mensaje, reloj));
    }
}
