package com.devsenior.sillon.publico;

import java.util.List;

import com.devsenior.sillon.servicio.ServicioRespuesta;

/** Lo que ve el cliente final. Nunca correos ni contraseñas. */
public record CatalogoPublico(String nombre, String slug, String ciudad, String direccion, String telefono,
        String horario, List<ServicioRespuesta> servicios, List<EstilistaPublico> estilistas) {
}
