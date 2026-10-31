package com.devsenior.sillon.compartido;

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

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    private final Clock reloj;

    public ManejadorGlobalExcepciones() {
        this.reloj = Clock.systemUTC();
    }

    public ManejadorGlobalExcepciones(Clock reloj) {
        this.reloj = reloj;
    }

    private ResponseEntity<ErrorApi> construir(HttpStatus estado, String mensaje) {
        ErrorApi cuerpo = new ErrorApi(
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                LocalDateTime.now(reloj));
        return ResponseEntity.status(estado).body(cuerpo);
    }

    // MANEJADOR FALTANTE PARA 409 CONFLICT (Registro duplicado y horarios imposibles)
    @ExceptionHandler(ConflictoExcepcion.class)
    public ResponseEntity<ErrorApi> manejarConflicto(ConflictoExcepcion ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<ErrorApi> manejarNoEncontrado(NoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorApi> manejarReglaNegocio(ReglaNegocioException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PagoRequeridoException.class)
    public ResponseEntity<ErrorApi> manejarPagoRequerido(PagoRequeridoException ex) {
        return construir(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler(AccionNoPermitidaException.class)
    public ResponseEntity<ErrorApi> manejarAccionNoPermitida(AccionNoPermitidaException ex) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasExcepcion.class)
    public ResponseEntity<ErrorApi> manejarCredencialesInvalidas(CredencialesInvalidasExcepcion ex) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(FirmaInvalidaException.class)
    public ResponseEntity<ErrorApi> manejarFirmaInvalida(FirmaInvalidaException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ErrorPasarelaException.class)
    public ResponseEntity<ErrorApi> manejarErrorPasarela(ErrorPasarelaException ex) {
        return construir(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return construir(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> manejarJsonMalFormado(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST, "JSON mal formado o inválido");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorApi> manejarParametroFaltante(MissingServletRequestParameterException ex) {
        return construir(HttpStatus.BAD_REQUEST, "Parámetro faltante: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorApi> manejarTipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return construir(HttpStatus.BAD_REQUEST, "Tipo de parámetro inválido para: " + ex.getName());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> manejarGlobal(Exception ex) {
        log.error("Error no controlado en el servidor", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error inesperado en el servidor");
    }
}