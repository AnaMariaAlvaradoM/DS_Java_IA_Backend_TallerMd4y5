package co.sillon;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;

@RestControllerAdvice
class Fallos {
    static ResponseStatusException fallo(int estado, String mensaje) {
        return new ResponseStatusException(HttpStatus.valueOf(estado), mensaje);
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> negocio(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("mensaje", ex.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> campos(MethodArgumentNotValidException ex) {
        String campos = ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField())
                .distinct().sorted().collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("mensaje","Revisa los campos " + campos));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
                       MissingServletRequestParameterException.class})
    ResponseEntity<?> formato() {
        return ResponseEntity.badRequest().body(Map.of("mensaje","Revisa el formato y los parámetros de la petición"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> duplicado() {
        return ResponseEntity.status(409).body(Map.of("mensaje","El registro entra en conflicto con datos existentes"));
    }
}
