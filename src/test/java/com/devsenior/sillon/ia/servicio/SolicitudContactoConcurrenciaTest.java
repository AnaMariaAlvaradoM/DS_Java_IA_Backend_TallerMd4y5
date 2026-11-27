
package com.devsenior.sillon.ia.servicio;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.ia.modelo.EstadoSolicitud;
import com.devsenior.sillon.ia.modelo.MotivoSolicitud;
import com.devsenior.sillon.ia.modelo.SolicitudContacto;
import com.devsenior.sillon.ia.repositorio.SolicitudContactoRepositorio;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;

@SpringBootTest
class SolicitudContactoConcurrenciaTest {

    @Autowired
    private SolicitudContactoServicio solicitudServicio;

    @Autowired
    private SolicitudContactoRepositorio solicitudRepositorio;

    @Autowired
    private SalonRepositorio salonRepositorio;

    private Long salonId;
    private Long solicitudId;

    @AfterEach
    void limpiar() {

        if (solicitudId != null) {
            solicitudRepositorio.deleteById(solicitudId);
        }

        if (salonId != null) {
            salonRepositorio.deleteById(salonId);
        }
    }

    @Test
    void soloUnaOperacionPuedeMarcarLaSolicitudComoAtendida()
            throws Exception {

        crearDatosDePrueba();

        ExecutorService ejecutor = Executors.newFixedThreadPool(2);

        CountDownLatch inicio = new CountDownLatch(1);

        try {

            Future<Boolean> primera = ejecutor.submit(() -> {
                inicio.await();

                try {
                    solicitudServicio.marcarAtendida(
                            salonId, solicitudId);

                    return true;

                } catch (ExcepcionConflicto excepcion) {
                    return false;
                }
            });

            Future<Boolean> segunda = ejecutor.submit(() -> {
                inicio.await();

                try {
                    solicitudServicio.marcarAtendida(
                            salonId, solicitudId);

                    return true;

                } catch (ExcepcionConflicto excepcion) {
                    return false;
                }
            });

            inicio.countDown();

            boolean resultadoPrimera = primera.get(30, TimeUnit.SECONDS);

            boolean resultadoSegunda = segunda.get(30, TimeUnit.SECONDS);

            int operacionesExitosas = (resultadoPrimera ? 1 : 0)
                    + (resultadoSegunda ? 1 : 0);

            assertEquals(
                    1,
                    operacionesExitosas,
                    "Solo una operacion debe marcar "
                            + "la solicitud como atendida");

            SolicitudContacto actualizada = solicitudRepositorio.findById(solicitudId)
                    .orElseThrow();

            assertEquals(
                    EstadoSolicitud.ATENDIDA,
                    actualizada.getEstado());

        } finally {

            ejecutor.shutdownNow();

            assertTrue(
                    ejecutor.awaitTermination(
                            30, TimeUnit.SECONDS),
                    "Los hilos deben finalizar correctamente");
        }
    }

    private void crearDatosDePrueba() {

        Salon salon = new Salon();

        salon.setNombre("Salon Prueba Concurrencia");
        salon.setSlug("concurrencia-" + UUID.randomUUID());
        salon.setCiudad("Bogota");
        salon.setDireccion("Calle 100 # 20-30");
        salon.setTelefono("3001234567");
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(5);

        salon.setConfiguracion(
                ConfiguracionSalon.predeterminada());

        salon.setFechaCreacion(LocalDateTime.now());

        Salon salonGuardado = salonRepositorio.saveAndFlush(salon);

        salonId = salonGuardado.getId();

        SolicitudContacto solicitud = new SolicitudContacto();

        solicitud.setSalon(salonGuardado);
        solicitud.setClienteNombre("Ana");
        solicitud.setClienteContacto("3009876543");

        solicitud.setMensajes(
                "[{\"rol\":\"CLIENTE\","
                        + "\"texto\":\"Quiero reservar una cita\"}]");

        solicitud.setMotivo(
                MotivoSolicitud.PEDIDO_POR_IA);

        solicitud.setEstado(
                EstadoSolicitud.PENDIENTE);

        SolicitudContacto guardada = solicitudRepositorio.saveAndFlush(solicitud);

        solicitudId = guardada.getId();
    }
}
