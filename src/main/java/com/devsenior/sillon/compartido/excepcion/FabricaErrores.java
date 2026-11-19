package com.devsenior.sillon.compartido.excepcion;

import com.devsenior.sillon.compartido.dto.ErrorRespuesta;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Arma el objeto de error con la hora del salón (Clock del proyecto). */
@Component
public class FabricaErrores {

    private final Clock clock;

    public FabricaErrores(Clock clock) {
        this.clock = clock;
    }

    public ErrorRespuesta crear(HttpStatus estado, String mensaje) {
        return new ErrorRespuesta(estado.value(), estado.getReasonPhrase(), mensaje, LocalDateTime.now(clock));
    }
}
