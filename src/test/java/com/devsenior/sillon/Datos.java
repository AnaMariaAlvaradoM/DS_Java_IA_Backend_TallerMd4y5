package com.devsenior.sillon;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;

import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;

/** Datos de prueba compartidos (los del anexo del taller). */
public final class Datos {

    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    /** "Ahora" fijo: lunes 19 de octubre de 2026, 8:00 a. m. en Bogotá. */
    public static Clock relojFijo() {
        return Clock.fixed(LocalDateTime.of(2026, 10, 19, 8, 0).atZone(BOGOTA).toInstant(), BOGOTA);
    }

    private Datos() {
    }

    public static Salon salon(Plan plan) {
        Salon s = new Salon();
        s.setId(1L);
        s.setNombre("Barbería La Navaja");
        s.setSlug("barberia-la-navaja");
        s.setCiudad("Medellín");
        s.setDireccion("Calle 10 # 43-20");
        s.setTelefono("604 555 1234");
        s.setPlan(plan);
        s.setCreditosIa(10);
        s.setConfiguracion(new ConfiguracionSalon(LocalTime.of(8, 0), LocalTime.of(19, 0), false, 30));
        return s;
    }

    public static Servicio servicio(Long id, String nombre, int minutos, int precio, Salon salon) {
        Servicio s = new Servicio();
        s.setId(id);
        s.setNombre(nombre);
        s.setDuracionMinutos(minutos);
        s.setPrecio(precio);
        s.setActivo(true);
        s.setSalon(salon);
        return s;
    }

    public static Usuario estilista(Long id, String nombre, Salon salon, Servicio... servicios) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(nombre.toLowerCase().replace(' ', '.') + "@navaja.co");
        u.setPassword("x");
        u.setRol(Rol.ESTILISTA);
        u.setActivo(true);
        u.setSalon(salon);
        Set<Servicio> set = new LinkedHashSet<>(Set.of(servicios));
        u.setServicios(set);
        return u;
    }
}
