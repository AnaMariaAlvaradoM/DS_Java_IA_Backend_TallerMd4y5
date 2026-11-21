package com.devsenior.sillon.compartido;

import java.time.LocalDateTime;

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
}
