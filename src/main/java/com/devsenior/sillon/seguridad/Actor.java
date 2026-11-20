package com.devsenior.sillon.seguridad;

public record Actor(long id, long salon, String rol, String email) {
    public boolean dueno() {
        return "DUENO".equals(rol);
    }
}
