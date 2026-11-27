
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

import com.devsenior.sillon.compartido.excepcion.ExcepcionPagoRequerido;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;

@SpringBootTest
class CreditoIaServicioIntegracionTest {

    @Autowired
    private CreditoIaServicio creditoIaServicio;

    @Autowired
    private SalonRepositorio salonRepositorio;

    private Long salonId;

    @AfterEach
    void limpiar() {

        if (salonId != null
                && salonRepositorio.existsById(salonId)) {

            salonRepositorio.deleteById(salonId);
        }
    }

    @Test
    void descuentaUnCreditoYDevuelveSaldoActualizado() {

        salonId = crearSalon(10);

        creditoIaServicio.verificarSaldo(salonId);

        int saldoRestante =
                creditoIaServicio.consumirCredito(salonId);

        assertEquals(9, saldoRestante);

        Salon actualizado = salonRepositorio.findById(salonId)
                .orElseThrow();

        assertEquals(9, actualizado.getCreditosIa());
    }

    @Test
    void rechazaConsumoCuandoNoHayCreditos() {

        salonId = crearSalon(0);

        assertThrows(
                ExcepcionPagoRequerido.class,
                () -> creditoIaServicio.verificarSaldo(salonId));

        assertThrows(
                ExcepcionPagoRequerido.class,
                () -> creditoIaServicio.consumirCredito(salonId));

        Salon actualizado = salonRepositorio.findById(salonId)
                .orElseThrow();

        assertEquals(0, actualizado.getCreditosIa());
    }

    @Test
    void dosSolicitudesConcurrentesNoConsumenElMismoCredito()
            throws Exception {

        salonId = crearSalon(1);

        ExecutorService ejecutor =
                Executors.newFixedThreadPool(2);

        CountDownLatch inicio = new CountDownLatch(1);

        try {

            Future<Boolean> primera = ejecutor.submit(() -> {
                inicio.await();

                try {
                    creditoIaServicio.consumirCredito(salonId);
                    return true;

                } catch (ExcepcionPagoRequerido excepcion) {
                    return false;
                }
            });

            Future<Boolean> segunda = ejecutor.submit(() -> {
                inicio.await();

                try {
                    creditoIaServicio.consumirCredito(salonId);
                    return true;

                } catch (ExcepcionPagoRequerido excepcion) {
                    return false;
                }
            });

            inicio.countDown();

            boolean resultadoPrimera =
                    primera.get(20, TimeUnit.SECONDS);

            boolean resultadoSegunda =
                    segunda.get(20, TimeUnit.SECONDS);

            int exitos = 0;

            if (resultadoPrimera) {
                exitos++;
            }

            if (resultadoSegunda) {
                exitos++;
            }

            assertEquals(
                    1,
                    exitos,
                    "Solo una solicitud debe consumir el crédito");

            Salon actualizado = salonRepositorio
                    .findById(salonId)
                    .orElseThrow();

            assertEquals(
                    0,
                    actualizado.getCreditosIa(),
                    "El saldo nunca debe ser negativo");

        } finally {

            ejecutor.shutdownNow();

            assertTrue(
                    ejecutor.awaitTermination(
                            20, TimeUnit.SECONDS),
                    "Los hilos de prueba deben finalizar");
        }
    }

    private Long crearSalon(int creditos) {

        Salon salon = new Salon();

        salon.setNombre("Salon Prueba Creditos IA");

        salon.setSlug(
                "prueba-ia-" + UUID.randomUUID());

        salon.setCiudad("Bogota");

        salon.setDireccion(
                "Direccion de prueba 123");

        salon.setTelefono("3001234567");

        salon.setPlan(Plan.GRATIS);

        salon.setCreditosIa(creditos);

        salon.setConfiguracion(
                ConfiguracionSalon.predeterminada());

        salon.setFechaCreacion(
                LocalDateTime.of(2026, 11, 25, 20, 0));

        return salonRepositorio.saveAndFlush(salon).getId();
    }
}
