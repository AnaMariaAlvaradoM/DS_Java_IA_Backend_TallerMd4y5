package com.devsenior.sillon.compartido;

import com.devsenior.sillon.compartido.dto.ErrorRespuesta;
import com.devsenior.sillon.compartido.excepcion.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.excepcion.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.PasarelaException;
import com.devsenior.sillon.compartido.excepcion.ProveedorIaNoDisponibleException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private final Clock clock;

    public ManejadorGlobalExcepciones(Clock clock) {
        this.clock = clock;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        if (mensaje.isBlank()) {
            mensaje = ex.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorRespuesta(HttpStatus.BAD_REQUEST.value(), "Bad Request", mensaje, ahora()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class, FirmaInvalidaException.class})
    public ResponseEntity<ErrorRespuesta> manejarBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorRespuesta(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), ahora()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorRespuesta> manejarCredencialesInvalidas(CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorRespuesta(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", ex.getMessage(), ahora()));
    }

    @ExceptionHandler(PagoRequeridoException.class)
    public ResponseEntity<ErrorRespuesta> manejarPagoRequerido(PagoRequeridoException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(new ErrorRespuesta(HttpStatus.PAYMENT_REQUIRED.value(), "Payment Required", ex.getMessage(), ahora()));
    }

    @ExceptionHandler(AccionNoPermitidaException.class)
    public ResponseEntity<ErrorRespuesta> manejarAccionNoPermitida(AccionNoPermitidaException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorRespuesta(HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage(), ahora()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorRespuesta(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), ahora()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorRespuesta> manejarReglaNegocio(ReglaNegocioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorRespuesta(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), ahora()));
    }

    @ExceptionHandler(PasarelaException.class)
    public ResponseEntity<ErrorRespuesta> manejarPasarela(PasarelaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorRespuesta(HttpStatus.BAD_GATEWAY.value(), "Bad Gateway", ex.getMessage(), ahora()));
    }

    @ExceptionHandler(ProveedorIaNoDisponibleException.class)
    public ResponseEntity<ErrorRespuesta> manejarIaNoDisponible(ProveedorIaNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorRespuesta(HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable", ex.getMessage(), ahora()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> manejarGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorRespuesta(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", ex.getMessage(), ahora()));
    }
}
