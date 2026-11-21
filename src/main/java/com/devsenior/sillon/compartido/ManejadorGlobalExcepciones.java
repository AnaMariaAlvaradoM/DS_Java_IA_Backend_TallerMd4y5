
package com.devsenior.sillon.compartido;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Clock;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.stream.Collectors;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {


    private final Clock clock;

    public ManejadorGlobalExcepciones(Clock clock) {
        this.clock = clock;
}



    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarValidacion(
        MethodArgumentNotValidException ex) {

    String mensaje = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": "
                    + error.getDefaultMessage())
            .collect(Collectors.joining("; "));

        return crearRespuesta(HttpStatus.BAD_REQUEST, mensaje);
}


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> manejarJsonInvalido(
        HttpMessageNotReadableException ex) {

        return crearRespuesta(
            HttpStatus.BAD_REQUEST,
            "El cuerpo de la solicitud contiene un JSON inválido"
    );
}


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RespuestaError> manejarParametroFaltante(
        MissingServletRequestParameterException ex) {

        return crearRespuesta(
            HttpStatus.BAD_REQUEST,
            "Falta el parámetro obligatorio: " + ex.getParameterName()
    );
}


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> manejarTipoInvalido(
        MethodArgumentTypeMismatchException ex) {

        return crearRespuesta(
            HttpStatus.BAD_REQUEST,
            "El parámetro '" + ex.getName()
                    + "' tiene un valor o tipo inválido"
    );
}

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> manejarNoEncontrado(
            RecursoNoEncontradoException ex) {
        return crearRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<RespuestaError> manejarConflicto(
            ReglaNegocioException ex) {
        return crearRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PagoRequeridoException.class)
    public ResponseEntity<RespuestaError> manejarPagoRequerido(
            PagoRequeridoException ex) {
        return crearRespuesta(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler(AccionNoPermitidaException.class)
    public ResponseEntity<RespuestaError> manejarProhibido(
            AccionNoPermitidaException ex) {
        return crearRespuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<RespuestaError> manejarCredenciales(
            CredencialesInvalidasException ex) {
        return crearRespuesta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(FirmaInvalidaException.class)
    public ResponseEntity<RespuestaError> manejarFirmaInvalida(
            FirmaInvalidaException ex) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ErrorPasarelaException.class)
    public ResponseEntity<RespuestaError> manejarPasarela(
            ErrorPasarelaException ex) {
        return crearRespuesta(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    private ResponseEntity<RespuestaError> crearRespuesta(
            HttpStatus estado, String mensaje) {

        RespuestaError respuesta = new RespuestaError(
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                LocalDateTime.now(clock)
        );

        return ResponseEntity.status(estado).body(respuesta);
    }
}
