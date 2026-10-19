package com.devsenior.sillon.compartido;

import com.devsenior.sillon.compartido.excepcion.ExcepcionSillon;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class ManejadorGlobalErrores extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);
    private final FabricaErrorApi fabrica;

    public ManejadorGlobalErrores(FabricaErrorApi fabrica) {
        this.fabrica = fabrica;
    }

    @ExceptionHandler(ExcepcionSillon.class)
    public ResponseEntity<ErrorApi> manejarNegocio(ExcepcionSillon excepcion) {
        return ResponseEntity.status(excepcion.getEstado())
                .body(fabrica.crear(excepcion.getEstado(), excepcion.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String mensaje = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error instanceof org.springframework.validation.FieldError campo
                        ? campo.getField() + ": " + campo.getDefaultMessage() : error.getDefaultMessage())
                .sorted().collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, mensaje, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "El JSON está mal formado o contiene un valor inválido", headers);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + ex.getParameterName(), headers);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "Un parámetro tiene un tipo o formato inválido", headers);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respuesta(HttpStatus.valueOf(status.value()), "No se pudo procesar la solicitud", headers);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> manejarInesperado(Exception excepcion) {
        LOG.error("Error inesperado al procesar la solicitud", excepcion);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(fabrica.crear(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno del servidor"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApi> manejarAccesoDenegado(AccessDeniedException excepcion) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(fabrica.crear(HttpStatus.FORBIDDEN, "No tiene permiso para realizar esta acción"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorApi> manejarDatosDuplicados(DataIntegrityViolationException excepcion) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(fabrica.crear(HttpStatus.CONFLICT, "Los datos entran en conflicto con un registro existente"));
    }

    private ResponseEntity<Object> respuesta(HttpStatus estado, String mensaje, HttpHeaders headers) {
        return new ResponseEntity<>(fabrica.crear(estado, mensaje), headers, estado);
    }
}
