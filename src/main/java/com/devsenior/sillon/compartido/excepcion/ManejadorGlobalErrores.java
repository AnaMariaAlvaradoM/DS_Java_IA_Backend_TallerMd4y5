package com.devsenior.sillon.compartido.excepcion;

import com.devsenior.sillon.compartido.dto.ErrorRespuesta;
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
public class ManejadorGlobalErrores {

    private final Clock clock;

    public ManejadorGlobalErrores(Clock clock) {
        this.clock = clock;
    }

    private ErrorRespuesta construirRespuesta(HttpStatus status, String mensaje) {
        return new ErrorRespuesta(
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                LocalDateTime.now(clock)
        );
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND; // 404
        return ResponseEntity.status(status).body(construirRespuesta(status, ex.getMessage()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorRespuesta> manejarReglaNegocio(ReglaNegocioException ex) {
        HttpStatus status = HttpStatus.CONFLICT; // 409
        return ResponseEntity.status(status).body(construirRespuesta(status, ex.getMessage()));
    }

    @ExceptionHandler(PagoRequeridoException.class)
    public ResponseEntity<ErrorRespuesta> manejarPagoRequerido(PagoRequeridoException ex) {
        HttpStatus status = HttpStatus.PAYMENT_REQUIRED; // 402
        return ResponseEntity.status(status).body(construirRespuesta(status, ex.getMessage()));
    }

    @ExceptionHandler(AccionNoPermitidaException.class)
    public ResponseEntity<ErrorRespuesta> manejarAccionNoPermitida(AccionNoPermitidaException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN; // 403
        return ResponseEntity.status(status).body(construirRespuesta(status, ex.getMessage()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorRespuesta> manejarCredencialesInvalidas(CredencialesInvalidasException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED; // 401
        return ResponseEntity.status(status).body(construirRespuesta(status, ex.getMessage()));
    }

    @ExceptionHandler(FirmaInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> manejarFirmaInvalida(FirmaInvalidaException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST; // 400
        return ResponseEntity.status(status).body(construirRespuesta(status, ex.getMessage()));
    }

    @ExceptionHandler(ErrorPasarelaException.class)
    public ResponseEntity<ErrorRespuesta> manejarErrorPasarela(ErrorPasarelaException ex) {
        HttpStatus status = HttpStatus.BAD_GATEWAY; // 502
        return ResponseEntity.status(status).body(construirRespuesta(status, ex.getMessage()));
    }

    // Validación de DTOs (@Valid): campos fallidos separados por punto y coma
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST; // 400
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(status).body(construirRespuesta(status, mensaje));
    }

    // JSON mal formado o tipos inválidos
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorRespuesta> manejarPeticionInvalida(Exception ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST; // 400
        return ResponseEntity.status(status).body(construirRespuesta(status, "La petición es inválida o tiene un formato incorrecto"));
    }

    // Error interno genérico (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> manejarErrorInterno(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR; // 500
        return ResponseEntity.status(status).body(construirRespuesta(status, "Ocurrió un error inesperado en el servidor"));
    }
}
