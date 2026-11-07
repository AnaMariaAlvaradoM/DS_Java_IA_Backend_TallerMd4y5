package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {
    private final Clock clock;

    public ManejadorGlobalExcepciones(Clock clock) {
        this.clock = clock;
    }

    private ResponseEntity<ErrorRespuesta> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado)
                .body(new ErrorRespuesta(estado.value(), estado.getReasonPhrase(), mensaje,
                        OffsetDateTime.now(clock)));
    }

    @ExceptionHandler(ExcepcionApi.class)
    public ResponseEntity<ErrorRespuesta> manejarApi(ExcepcionApi excepcion) {
        return respuesta(excepcion.getEstado(), excepcion.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException excepcion) {
        String mensaje = excepcion.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> manejarJson(HttpMessageNotReadableException excepcion) {
        return respuesta(HttpStatus.BAD_REQUEST, "JSON mal formado o dato inválido");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> manejarParametroFaltante(MissingServletRequestParameterException excepcion) {
        return respuesta(HttpStatus.BAD_REQUEST, "Falta el parámetro " + excepcion.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> manejarTipoInvalido(MethodArgumentTypeMismatchException excepcion) {
        return respuesta(HttpStatus.BAD_REQUEST, "Tipo inválido para el parámetro " + excepcion.getName());
    }
}
