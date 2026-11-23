
package com.devsenior.sillon.ia;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConcurrenciaCreditosIATest {

    @Autowired
    private SalonRepository salonRepository;

    @Autowired
    private ServicioCreditosIA servicioCreditos;

    @Test
    void dosSolicitudesNoConsumenElMismoCredito()
            throws Exception {

        Salon salon = new Salon();
        salon.setNombre("Prueba concurrencia IA");
        salon.setSlug("prueba-ia-" + UUID.randomUUID());
        salon.setCiudad("Bogotá");
        salon.setDireccion("Dirección de prueba");
        salon.setTelefono("3000000000");
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(1);
        salon.setConfiguracion(
                ConfiguracionSalon.porDefecto()
        );
        salon.setFechaCreacion(LocalDateTime.now());

        Long salonId = salonRepository
                .saveAndFlush(salon)
                .getId();

        var ejecutor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);

        try {
            java.util.concurrent.Callable<Boolean> intento =
                    () -> {
                        inicio.await();

                        try {
                            int restantes =
                                    servicioCreditos.consumir(salonId);

                            return restantes == 0;

                        } catch (PagoRequeridoException ex) {
                            return false;
                        }
                    };

            Future<Boolean> primera =
                    ejecutor.submit(intento);

            Future<Boolean> segunda =
                    ejecutor.submit(intento);

            inicio.countDown();

            boolean resultado1 =
                    primera.get(15, TimeUnit.SECONDS);

            boolean resultado2 =
                    segunda.get(15, TimeUnit.SECONDS);

            int consumosExitosos =
                    (resultado1 ? 1 : 0)
                    + (resultado2 ? 1 : 0);

            assertEquals(1, consumosExitosos);

            Salon actualizado = salonRepository
                    .findById(salonId)
                    .orElseThrow();

            assertEquals(
                    0,
                    actualizado.getCreditosIa()
            );

        } finally {
            ejecutor.shutdownNow();
            salonRepository.deleteById(salonId);
        }
    }
}
