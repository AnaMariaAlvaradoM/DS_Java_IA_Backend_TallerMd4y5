
package com.devsenior.sillon.pago;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServicioWebhookStripeTest {

    private static final String SECRETO =
            "whsec_simulado_para_pruebas_locales";

    private EventoStripeRepository eventos;
    private SalonRepository salones;
    private ServicioWebhookStripe servicio;
    private Salon salon;

    @BeforeEach
    void configurar() {

        eventos = mock(EventoStripeRepository.class);
        salones = mock(SalonRepository.class);

        ConfiguracionStripe.PropiedadesStripe propiedades =
                new ConfiguracionStripe.PropiedadesStripe(
                        "sk_test_simulada",
                        SECRETO,
                        "price_pro_simulado",
                        "price_creditos_simulado",
                        "http://localhost"
                );

        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-21T15:00:00Z"),
                ZoneId.of("America/Bogota")
        );

        servicio = new ServicioWebhookStripe(
                eventos,
                salones,
                propiedades,
                reloj
        );

        salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
    }

    // Construir una firma de Stripe válida
    // utilizando una clave exclusiva para pruebas.

    private String firmar(String cuerpo) throws Exception {

        long timestamp =
                System.currentTimeMillis() / 1000;

        String contenido =
                timestamp + "." + cuerpo;

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(
                SECRETO.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));

        byte[] firma = mac.doFinal(
                contenido.getBytes(StandardCharsets.UTF_8)
        );

        return "t=" + timestamp
                + ",v1=" + HexFormat.of().formatHex(firma);
    }

    // Simular un evento checkout.session.completed.

    private String eventoCompra(
            String eventoId,
            String tipo,
            String suscripcion
    ) {

        String subscriptionJson =
                suscripcion == null
                        ? "null"
                        : "\"" + suscripcion + "\"";

        return """
                {
                  "id": "%s",
                  "object": "event",
                  "api_version": "2024-06-20",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_simulada",
                      "object": "checkout.session",
                      "payment_status": "paid",
                      "subscription": %s,
                      "metadata": {
                        "salonId": "1",
                        "tipoCompra": "%s"
                      }
                    }
                  }
                }
                """.formatted(
                eventoId,
                subscriptionJson,
                tipo
        );
    }

    // Simular cancelación de una suscripción.

    private String eventoCancelacion(
            String eventoId,
            String suscripcionId
    ) {

        return """
                {
                  "id": "%s",
                  "object": "event",
                  "api_version": "2024-06-20",
                  "type": "customer.subscription.deleted",
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "subscription"
                    }
                  }
                }
                """.formatted(
                eventoId,
                suscripcionId
        );
    }

    // PRUEBA 1
    // Una suscripción PRO activa el plan
    // y agrega 300 créditos de IA.

    @Test
    void pagoProActivaPlanYSumaCreditos()
            throws Exception {

        when(salones.findByIdParaAgendar(1L))
                .thenReturn(Optional.of(salon));

        String cuerpo = eventoCompra(
                "evt_pro_001",
                "SUSCRIPCION_PRO",
                "sub_123"
        );

        boolean procesado = servicio.procesar(
                cuerpo,
                firmar(cuerpo)
        );

        assertTrue(procesado);

        assertEquals(
                Plan.PRO,
                salon.getPlan()
        );

        assertEquals(
                310,
                salon.getCreditosIa()
        );

        assertEquals(
                "sub_123",
                salon.getStripeSuscripcionId()
        );

        verify(salones, times(1))
                .save(salon);

        verify(eventos, times(1))
                .saveAndFlush(any(EventoStripe.class));
    }

    // PRUEBA 2
    // Comprar un paquete agrega 200 créditos
    // sin modificar el plan.

    @Test
    void paqueteDeCreditosSuma200()
            throws Exception {

        when(salones.findByIdParaAgendar(1L))
                .thenReturn(Optional.of(salon));

        String cuerpo = eventoCompra(
                "evt_creditos_001",
                "PAQUETE_CREDITOS",
                null
        );

        boolean procesado = servicio.procesar(
                cuerpo,
                firmar(cuerpo)
        );

        assertTrue(procesado);

        assertEquals(
                Plan.GRATIS,
                salon.getPlan()
        );

        assertEquals(
                210,
                salon.getCreditosIa()
        );

        verify(salones, times(1))
                .save(salon);

        verify(eventos, times(1))
                .saveAndFlush(any(EventoStripe.class));
    }

    // PRUEBA 3
    // Un evento ya procesado no puede
    // aplicar nuevamente sus efectos.

    @Test
    void eventoDuplicadoNoDebeAplicarseOtraVez()
            throws Exception {

        when(eventos.existsById("evt_duplicado"))
                .thenReturn(true);

        String cuerpo = eventoCompra(
                "evt_duplicado",
                "SUSCRIPCION_PRO",
                "sub_123"
        );

        boolean procesado = servicio.procesar(
                cuerpo,
                firmar(cuerpo)
        );

        assertFalse(procesado);

        assertEquals(
                Plan.GRATIS,
                salon.getPlan()
        );

        assertEquals(
                10,
                salon.getCreditosIa()
        );

        verifyNoInteractions(salones);

        verify(eventos, never())
                .saveAndFlush(any(EventoStripe.class));
    }

    // PRUEBA 4
    // Cancelar una suscripción cambia
    // el plan PRO a GRATIS.

    @Test
    void cancelacionDevuelvePlanGratis()
            throws Exception {

        salon.setPlan(Plan.PRO);
        salon.setCreditosIa(310);
        salon.setStripeSuscripcionId("sub_123");

        when(salones.findByStripeSuscripcionId(
                "sub_123"
        )).thenReturn(Optional.of(salon));

        String cuerpo = eventoCancelacion(
                "evt_cancelacion_001",
                "sub_123"
        );

        boolean procesado = servicio.procesar(
                cuerpo,
                firmar(cuerpo)
        );

        assertTrue(procesado);

        assertEquals(
                Plan.GRATIS,
                salon.getPlan()
        );

        assertNull(
                salon.getStripeSuscripcionId()
        );

        assertEquals(
                310,
                salon.getCreditosIa()
        );

        verify(salones, times(1))
                .save(salon);

        verify(eventos, times(1))
                .saveAndFlush(any(EventoStripe.class));
    }

    // PRUEBA 5
    // Una firma inválida debe rechazarse
    // antes de modificar el salón.

    @Test
    void firmaInvalidaDebeRechazarse() {

        String cuerpo = eventoCompra(
                "evt_firma_invalida",
                "SUSCRIPCION_PRO",
                "sub_123"
        );

        String firmaInvalida =
                "t=" + System.currentTimeMillis() / 1000
                        + ",v1=0000000000000000";

        assertThrows(
                FirmaInvalidaException.class,
                () -> servicio.procesar(
                        cuerpo,
                        firmaInvalida
                )
        );

        verifyNoInteractions(
                eventos,
                salones
        );
    }
}
