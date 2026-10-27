package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class FabricaErrorApi {
    private final Clock reloj;

    public FabricaErrorApi(Clock reloj) {
        this.reloj = reloj;
    }

    public ErrorApi crear(HttpStatus estado, String mensaje) {
        return new ErrorApi(estado.value(), estado.getReasonPhrase(), mensaje,
                LocalDateTime.now(reloj));
    }
}
