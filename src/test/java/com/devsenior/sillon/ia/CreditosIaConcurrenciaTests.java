package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepositorio;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CreditosIaConcurrenciaTests {
    @Autowired SalonRepositorio salones;
    @Autowired CreditosIaServicio creditos;

    @Test void dosConsumosSimultaneosNoDejanSaldoNegativo() throws Exception {
        Salon salon = new Salon();
        salon.setNombre("Prueba IA");
        salon.setSlug("ia-" + UUID.randomUUID());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(1);
        Long id = salones.saveAndFlush(salon).getId();
        var ejecutor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            var primera = ejecutor.submit(() -> consumir(inicio, id));
            var segunda = ejecutor.submit(() -> consumir(inicio, id));
            inicio.countDown();
            assertEquals(1, primera.get(30, TimeUnit.SECONDS) + segunda.get(30, TimeUnit.SECONDS));
            assertEquals(0, salones.consultarCreditosIa(id).orElseThrow());
        } finally {
            ejecutor.shutdownNow();
            salones.deleteById(id);
        }
    }

    private int consumir(CountDownLatch inicio, Long id) throws InterruptedException {
        inicio.await();
        try {
            creditos.consumir(id);
            return 1;
        } catch (PagoRequeridoException error) {
            return 0;
        }
    }
}
