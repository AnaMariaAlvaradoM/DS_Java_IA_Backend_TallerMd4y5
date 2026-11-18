package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.devsenior.sillon.compartido.excepcion.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.excepcion.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.excepcion.ErrorDePasarelaException;
import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;

/**
 * Manejador global: convierte toda excepción en el formato único de error
 * de cuatro campos con el código HTTP que le corresponde (SPEC.md §7.3 y §7.4).
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private final Clock clock;

    public ManejadorGlobalErrores(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<RespuestaError> manejarReglaDeNegocio(ReglaDeNegocioException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PagoRequeridoException.class)
    public ResponseEntity<RespuestaError> manejarPagoRequerido(PagoRequeridoException ex) {
        return construir(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler(AccionNoPermitidaException.class)
    public ResponseEntity<RespuestaError> manejarAccionNoPermitida(AccionNoPermitidaException ex) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<RespuestaError> manejarCredencialesInvalidas(CredencialesInvalidasException ex) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(FirmaInvalidaException.class)
    public ResponseEntity<RespuestaError> manejarFirmaInvalida(FirmaInvalidaException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ErrorDePasarelaException.class)
    public ResponseEntity<RespuestaError> manejarErrorDePasarela(ErrorDePasarelaException ex) {
        return construir(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarValidacion(MethodArgumentNotValidException ex) {
        // Campos que fallaron con su mensaje, separados por punto y coma (SPEC.md §7.3)
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return construir(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RespuestaError> manejarParametroFaltante(MissingServletRequestParameterException ex) {
        return construir(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> manejarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return construir(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' tiene un tipo inválido");
    }

    private ResponseEntity<RespuestaError> construir(HttpStatus estado, String mensaje) {
        RespuestaError cuerpo = new RespuestaError(
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                LocalDateTime.now(clock));
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
