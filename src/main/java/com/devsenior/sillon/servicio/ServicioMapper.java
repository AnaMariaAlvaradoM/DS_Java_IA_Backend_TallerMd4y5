package com.devsenior.sillon.servicio;

public final class ServicioMapper {

    private ServicioMapper() {
    }

    public static ServicioRespuesta aRespuesta(Servicio s) {
        return new ServicioRespuesta(s.getId(), s.getNombre(), s.getDescripcion(), s.getDuracionMinutos(),
                s.getPrecio(), s.isActivo());
    }

    /** Copia los datos de la petición a la entidad (sirve para crear y para editar). */
    public static void copiar(ServicioPeticion p, Servicio s) {
        s.setNombre(p.nombre().trim());
        s.setDescripcion(p.descripcion() == null ? null : p.descripcion().trim());
        s.setDuracionMinutos(p.duracionMinutos());
        s.setPrecio(p.precio());
        s.setActivo(p.activo() == null || p.activo());
    }
}
