package co.sillon;

import java.time.*;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
class Fallos {
    private final Clock reloj;
    Fallos(Clock reloj) { this.reloj=reloj; }
    record ErrorApi(int codigo,String error,String mensaje,LocalDateTime fecha) {}
    static ErrorApi error(Clock reloj,int estado,String mensaje) {
        return new ErrorApi(estado,HttpStatus.valueOf(estado).getReasonPhrase(),mensaje,LocalDateTime.now(reloj));
    }
    static ResponseStatusException fallo(int estado, String mensaje) {
        return new ResponseStatusException(HttpStatus.valueOf(estado), mensaje);
    }
    private ResponseEntity<ErrorApi> respuesta(int codigo,String mensaje) {
        return ResponseEntity.status(codigo).body(error(reloj,codigo,mensaje));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorApi> negocio(ResponseStatusException ex) { return respuesta(ex.getStatusCode().value(),ex.getReason()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorApi> campos(MethodArgumentNotValidException ex) {
        var campos=ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField()+" "+e.getDefaultMessage())
                .distinct().sorted().collect(Collectors.joining("; "));
        return respuesta(400,campos);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,MissingServletRequestParameterException.class})
    ResponseEntity<ErrorApi> formato() { return respuesta(400,"Revisa el formato y los parámetros de la petición"); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorApi> duplicado() { return respuesta(409,"Ya existe un registro con esos datos"); }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorApi> ruta() { return respuesta(404,"La ruta solicitada no existe"); }
}
