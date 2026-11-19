package com.devsenior.sillon.pago.service;

import com.devsenior.sillon.compartido.excepcion.PasarelaPagoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.pago.config.StripeProperties;
import com.devsenior.sillon.pago.dto.PagoResponse;
import com.devsenior.sillon.pago.model.TipoCompra;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.service.SalonService;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Crea la sesión de Checkout. El backend decide el precio: el cliente solo pide "plan PRO" o "paquete".
 * El plan se activa después, cuando Stripe confirma el pago por el webhook.
 */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final StripeClient stripeClient;
    private final StripeProperties stripe;
    private final SalonService salonService;
    private final String urlPublica;

    public PagoService(StripeClient stripeClient, StripeProperties stripe, SalonService salonService,
                       @Value("${sillon.url-publica}") String urlPublica) {
        this.stripeClient = stripeClient;
        this.stripe = stripe;
        this.salonService = salonService;
        this.urlPublica = urlPublica.replaceAll("/+$", "");
    }

    public PagoResponse crearSesion(Long salonId, String correoDueno, TipoCompra tipo) {
        Salon salon = salonService.obtenerEntidad(salonId);
        if (tipo == TipoCompra.SUSCRIPCION_PRO && salon.getPlan() == Plan.PRO) {
            throw new ReglaNegocioException("El salón ya tiene el plan PRO activo");
        }

        boolean esSuscripcion = tipo == TipoCompra.SUSCRIPCION_PRO;
        String precio = esSuscripcion ? stripe.precioPro() : stripe.precioCreditos();
        String idSalon = String.valueOf(salonId);

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(esSuscripcion ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito")
                .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                .setCustomerEmail(correoDueno)
                .setClientReferenceId(idSalon)
                .putMetadata("salonId", idSalon)
                .putMetadata("tipo", tipo.name())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(precio)
                        .setQuantity(1L)
                        .build());
        if (esSuscripcion) {
            builder.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("salonId", idSalon)
                    .putMetadata("tipo", tipo.name())
                    .build());
        }

        try {
            Session sesion = stripeClient.v1().checkout().sessions().create(builder.build());
            if (sesion.getUrl() == null) {
                throw new PasarelaPagoException("Stripe no devolvió la URL de pago");
            }
            return new PagoResponse(tipo, sesion.getId(), sesion.getUrl());
        } catch (StripeException ex) {
            log.error("Stripe rechazó la creación de la sesión: {}", ex.getMessage());
            throw new PasarelaPagoException("No se pudo crear la sesión de pago en Stripe");
        }
    }
}
