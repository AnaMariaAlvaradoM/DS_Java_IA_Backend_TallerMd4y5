package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.time.LocalDateTime;
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

@RestControllerAdvice
public class ManejadorErrores {
    private final Clock reloj;

    public ManejadorErrores(Clock reloj) {
        this.reloj = reloj;
    }

    @ExceptionHandler(ExcepcionApi.class)
    public ResponseEntity<ErrorApi> manejarNegocio(ExcepcionApi exception) {
        return responder(exception.getEstado(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> manejarValidacion(MethodArgumentNotValidException exception) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> manejarJson(HttpMessageNotReadableException exception) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no contiene un JSON válido");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorApi> manejarParametroFaltante(MissingServletRequestParameterException exception) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro " + exception.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorApi> manejarTipoParametro(MethodArgumentTypeMismatchException exception) {
        return responder(HttpStatus.BAD_REQUEST, "El parámetro " + exception.getName() + " tiene un valor inválido");
    }

    private ResponseEntity<ErrorApi> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado)
                .body(new ErrorApi(estado.value(), estado.getReasonPhrase(), mensaje, LocalDateTime.now(reloj)));
    }
}
