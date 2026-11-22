package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;

/**
 * Formato unico de error de Sillon (contrato, seccion 4).
 * Todos los errores, incluidos los de seguridad (401 y 403), responden con estos cuatro campos.
 *
 * @param codigo  numero del codigo HTTP (por ejemplo 409)
 * @param error   nombre estandar del codigo (por ejemplo "Conflict")
 * @param mensaje explicacion en espanol; en validacion, "campo: mensaje" separados por punto y coma
 * @param fecha   fecha y hora del error (AAAA-MM-DDTHH:mm:ss), tomada del Clock del proyecto
 */
public record ErrorRespuesta(int codigo, String error, String mensaje, LocalDateTime fecha) {

    /**
     * Unico lugar donde se arma un error: lo usan el manejador global y las respuestas 401/403 de seguridad.
     * La fecha se corta a segundos para que salga como AAAA-MM-DDTHH:mm:ss (sin fracciones).
     */
    public static ErrorRespuesta de(HttpStatus estado, String mensaje, Clock reloj) {
        return new ErrorRespuesta(estado.value(), estado.getReasonPhrase(), mensaje,
                LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS));
    }
}
