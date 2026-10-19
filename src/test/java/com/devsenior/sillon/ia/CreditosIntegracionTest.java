package com.devsenior.sillon.ia;

import static org.junit.jupiter.api.Assertions.*;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CreditosIntegracionTest {
    @Autowired CreditosService creditos;
    @Autowired SalonRepository salones;
    private Long salonId;

    @BeforeEach void crearSalon() {
        Salon salon = new Salon();
        salon.setNombre("Prueba créditos"); salon.setSlug("creditos-" + UUID.randomUUID());
        salon.setCiudad("Medellín"); salon.setDireccion("Calle prueba"); salon.setTelefono("6045550000");
        salon.setPlan(Plan.GRATIS); salon.setCreditosIa(1); salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salonId = salones.saveAndFlush(salon).getId();
    }

    @AfterEach void limpiar() { if (salonId != null) { salones.deleteById(salonId); } }

    @Test void verificarNoDescuentaYConsumirDevuelveSaldo() {
        creditos.verificar(salonId);
        assertEquals(1, salones.consultarCreditos(salonId).orElseThrow());
        assertEquals(0, creditos.consumir(salonId));
        assertThrows(PagoRequeridoException.class, () -> creditos.verificar(salonId));
        assertThrows(PagoRequeridoException.class, () -> creditos.consumir(salonId));
        assertEquals(0, salones.consultarCreditos(salonId).orElseThrow());
    }

    @Test void dosConsumosSimultaneosConUnCreditoSoloPermitenUno() throws Exception {
        var ejecutor = Executors.newFixedThreadPool(2);
        var preparados = new CountDownLatch(2);
        var salida = new CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Boolean> consumir = () -> {
                preparados.countDown();
                if (!salida.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("Sin inicio concurrente"); }
                try { creditos.consumir(salonId); return true; }
                catch (PagoRequeridoException ex) { return false; }
            };
            var primero = ejecutor.submit(consumir); var segundo = ejecutor.submit(consumir);
            assertTrue(preparados.await(5, TimeUnit.SECONDS)); salida.countDown();
            List<Boolean> resultados = List.of(primero.get(15, TimeUnit.SECONDS), segundo.get(15, TimeUnit.SECONDS));
            assertEquals(1, resultados.stream().filter(Boolean::booleanValue).count());
            assertEquals(0, salones.consultarCreditos(salonId).orElseThrow());
        } finally { salida.countDown(); ejecutor.shutdownNow(); }
    }

    @Test void consumoYaConfirmadoNoSeReviertePorErrorPosteriorDeJson() {
        creditos.consumir(salonId);
        var lector = new LectorJsonIA(tools.jackson.databind.json.JsonMapper.builder().build());
        var respuesta = new RespuestaIA("texto no JSON", "simulado", "prueba", 10, 2, java.math.BigDecimal.ZERO, 1);
        assertThrows(RespuestaIAInvalidaException.class, () -> lector.leer(respuesta, Object.class));
        assertEquals(0, salones.consultarCreditos(salonId).orElseThrow());
    }

    @Test void salonInexistenteEs404() {
        assertThrows(RecursoNoEncontradoException.class, () -> creditos.verificar(-1L));
        assertThrows(RecursoNoEncontradoException.class, () -> creditos.consumir(-1L));
    }
}
