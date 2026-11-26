
package com.devsenior.sillon.ia.repositorio;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.devsenior.sillon.ia.modelo.EstadoSolicitud;
import com.devsenior.sillon.ia.modelo.MotivoSolicitud;
import com.devsenior.sillon.ia.modelo.SolicitudContacto;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;

@SpringBootTest
class SolicitudContactoRepositorioIntegracionTest {

    @Autowired
    private SolicitudContactoRepositorio solicitudRepositorio;

    @Autowired
    private SalonRepositorio salonRepositorio;

    private final List<Long> solicitudesCreadas = new ArrayList<>();

    private final List<Long> salonesCreados = new ArrayList<>();

    @AfterEach
    void limpiarDatos() {

        for (Long id : solicitudesCreadas) {
            solicitudRepositorio.deleteById(id);
        }

        for (Long id : salonesCreados) {
            salonRepositorio.deleteById(id);
        }
    }

    @Test
    void guardaSolicitudConHistorialYEstadoPendiente() {

        Salon salon = crearSalon();

        String historial = """
                [
                  {
                    "rol": "CLIENTE",
                    "texto": "Quiero reservar una cita"
                  },
                  {
                    "rol": "ASISTENTE",
                    "texto": "Te contactaremos para ayudarte"
                  }
                ]
                """;

        SolicitudContacto solicitud = crearSolicitud(
                salon,
                "Ana",
                "3001234567",
                historial,
                MotivoSolicitud.PEDIDO_POR_IA);

        SolicitudContacto recuperada = solicitudRepositorio
                .buscarPorIdYSalon(
                        solicitud.getId(),
                        salon.getId())
                .orElseThrow();

        assertNotNull(recuperada.getId());
        assertNotNull(recuperada.getFechaCreacion());

        assertEquals(
                "Ana",
                recuperada.getClienteNombre());

        assertEquals(
                "3001234567",
                recuperada.getClienteContacto());

        assertEquals(
                historial,
                recuperada.getMensajes());

        assertEquals(
                MotivoSolicitud.PEDIDO_POR_IA,
                recuperada.getMotivo());

        assertEquals(
                EstadoSolicitud.PENDIENTE,
                recuperada.getEstado());
    }

    @Test
    void impideConsultarSolicitudesDeOtroSalon() {

        Salon salonA = crearSalon();
        Salon salonB = crearSalon();

        SolicitudContacto solicitud = crearSolicitud(
                salonA,
                "Carlos",
                "3009876543",
                "[{\"rol\":\"CLIENTE\",\"texto\":\"Necesito ayuda\"}]",
                MotivoSolicitud.REGLA_DE_SEGURIDAD);

        assertTrue(
                solicitudRepositorio
                        .buscarPorIdYSalon(
                                solicitud.getId(),
                                salonA.getId())
                        .isPresent());

        assertTrue(
                solicitudRepositorio
                        .buscarPorIdYSalon(
                                solicitud.getId(),
                                salonB.getId())
                        .isEmpty());

        assertTrue(
                solicitudRepositorio
                        .buscarPorSalonOrdenadas(salonB.getId())
                        .isEmpty());
    }

    @Test
    void listaSolicitudesDelSalonOrdenadas() {

        Salon salon = crearSalon();

        crearSolicitud(
                salon,
                "Cliente Uno",
                "3001111111",
                "[{\"rol\":\"CLIENTE\",\"texto\":\"Primera consulta\"}]",
                MotivoSolicitud.PEDIDO_POR_IA);

        crearSolicitud(
                salon,
                "Cliente Dos",
                "3002222222",
                "[{\"rol\":\"CLIENTE\",\"texto\":\"Segunda consulta\"}]",
                MotivoSolicitud.REGLA_DE_SEGURIDAD);

        List<SolicitudContacto> resultado = solicitudRepositorio
                .buscarPorSalonOrdenadas(salon.getId());

        assertEquals(2, resultado.size());

        for (int i = 1; i < resultado.size(); i++) {

            SolicitudContacto anterior = resultado.get(i - 1);
            SolicitudContacto siguiente = resultado.get(i);

            int comparacion = anterior.getFechaCreacion()
                    .compareTo(siguiente.getFechaCreacion());

            assertTrue(
                    comparacion > 0
                            || (comparacion == 0
                                    && anterior.getId() > siguiente.getId()),
                    "Las solicitudes deben ordenarse "
                            + "de la mas reciente a la mas antigua");
        }
    }

    private Salon crearSalon() {

        Salon salon = new Salon();

        salon.setNombre("Salon Prueba Solicitudes");

        salon.setSlug(
                "prueba-solicitudes-" + UUID.randomUUID());

        salon.setCiudad("Bogota");
        salon.setDireccion("Calle 123 # 45-67");
        salon.setTelefono("3000000000");
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(5);

        salon.setConfiguracion(
                ConfiguracionSalon.predeterminada());

        salon.setFechaCreacion(LocalDateTime.now());

        Salon guardado = salonRepositorio.saveAndFlush(salon);

        salonesCreados.add(guardado.getId());

        return guardado;
    }

    private SolicitudContacto crearSolicitud(
            Salon salon,
            String nombre,
            String contacto,
            String historial,
            MotivoSolicitud motivo) {

        SolicitudContacto solicitud = new SolicitudContacto();

        solicitud.setSalon(salon);
        solicitud.setClienteNombre(nombre);
        solicitud.setClienteContacto(contacto);
        solicitud.setMensajes(historial);
        solicitud.setMotivo(motivo);

        SolicitudContacto guardada = solicitudRepositorio.saveAndFlush(solicitud);

        solicitudesCreadas.add(guardada.getId());

        return guardada;
    }
}
