package com.devsenior.sillon.compartido;
import com.devsenior.sillon.dto.ErrorRespuesta;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // --- 1. EXCEPCIONES PROPIAS DEL DOMINIO ---

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> manejarNoEncontrado(NoEncontradoException ex) {
        return crearRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorRespuesta> manejarReglaNegocio(ReglaNegocioException ex) {
        return crearRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PagoRequeridoException.class)
    public ResponseEntity<ErrorRespuesta> manejarPagoRequerido(PagoRequeridoException ex) {
        return crearRespuesta(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler(AccionNoPermitidaException.class)
    public ResponseEntity<ErrorRespuesta> manejarAccionNoPermitida(AccionNoPermitidaException ex) {
        return crearRespuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorRespuesta> manejarCredencialesInvalidas(CredencialesInvalidasException ex) {
        return crearRespuesta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(FirmaInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> manejarFirmaInvalida(FirmaInvalidaException ex) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ErrorPasarelaException.class)
    public ResponseEntity<ErrorRespuesta> manejarErrorPasarela(ErrorPasarelaException ex) {
        return crearRespuesta(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    // --- 2. ERRORES DE VALIDACIÓN Y PETICIONES HTTP (400 BAD REQUEST) ---

    // Errores de validación de Jakarta (@Valid en DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Error de validación en la petición");
        return crearRespuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    // JSON mal formado en el body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> manejarJsonMalFormado(HttpMessageNotReadableException ex) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, "Cuerpo de la petición JSON mal formado o ausente");
    }

    // Parámetro requerido faltante en la URL / Query
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> manejarParametroFaltante(MissingServletRequestParameterException ex) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, "Falta el parámetro requerido: " + ex.getParameterName());
    }

    // Parámetro con tipo de dato inválido (ej. enviar texto en lugar de número)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> manejarTipoParametroInvalido(MethodArgumentTypeMismatchException ex) {
        String mensaje = String.format("El parámetro '%s' debe ser de tipo %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "válido");
        return crearRespuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    // Error genérico no controlado (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> manejarExcepcionGenerica(Exception ex) {
        return crearRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error interno no esperado");
    }

    // --- MÉTODO AUXILIAR PARA CONSTRUIR EL DTO DE 4 CAMPOS ---

    private ResponseEntity<ErrorRespuesta> crearRespuesta(HttpStatus status, String mensaje) {
        ErrorRespuesta error = new ErrorRespuesta(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje
        );
        return new ResponseEntity<>(error, status);
    }
}
