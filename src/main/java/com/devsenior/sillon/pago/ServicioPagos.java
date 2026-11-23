
package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.ErrorPasarelaException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServicioPagos {

    private final StripeClient stripeClient;
    private final SalonRepository salonRepository;
    private final ConfiguracionStripe.PropiedadesStripe propiedades;
    private final String urlPublica;

    public ServicioPagos(
            StripeClient stripeClient,
            SalonRepository salonRepository,
            ConfiguracionStripe.PropiedadesStripe propiedades,
            @Value("${sillon.url-publica}") String urlPublica
    ) {
        this.stripeClient = stripeClient;
        this.salonRepository = salonRepository;
        this.propiedades = propiedades;
        this.urlPublica = urlPublica.replaceAll("/+$", "");
    }

    public RespuestaCheckout crearSesion(
            Long salonId,
            String correoDueno,
            TipoCompra tipo
    ) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Salón no encontrado"
                ));

        if (tipo == TipoCompra.SUSCRIPCION_PRO
                && salon.getPlan() == Plan.PRO) {
            throw new ReglaNegocioException(
                    "El salón ya tiene el plan PRO"
            );
        }

        String precio = tipo == TipoCompra.SUSCRIPCION_PRO
                ? propiedades.precioPro()
                : propiedades.precioCreditos();

        SessionCreateParams.Mode modo =
                tipo == TipoCompra.SUSCRIPCION_PRO
                        ? SessionCreateParams.Mode.SUBSCRIPTION
                        : SessionCreateParams.Mode.PAYMENT;

        String identificadorSalon = salon.getId().toString();

        SessionCreateParams.Builder parametros =
                SessionCreateParams.builder()
                        .setMode(modo)
                        .setCustomerEmail(correoDueno)
                        .setClientReferenceId(identificadorSalon)
                        .setSuccessUrl(
                                urlPublica
                                        + "/api/pagos/resultado?estado=exito"
                        )
                        .setCancelUrl(
                                urlPublica
                                        + "/api/pagos/resultado?estado=cancelado"
                        )
                        .putMetadata("salonId", identificadorSalon)
                        .putMetadata("tipoCompra", tipo.name())
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPrice(precio)
                                        .setQuantity(1L)
                                        .build()
                        );

        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            parametros.setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                            .putMetadata(
                                    "salonId",
                                    identificadorSalon
                            )
                            .putMetadata(
                                    "tipoCompra",
                                    tipo.name()
                            )
                            .build()
            );
        }

        try {
            Session sesion = stripeClient.v1()
                    .checkout()
                    .sessions()
                    .create(parametros.build());

            return new RespuestaCheckout(
                    tipo,
                    sesion.getId(),
                    sesion.getUrl()
            );

        
        } catch (StripeException ex) {
            System.err.println("Error de Stripe: " + ex.getMessage());

            throw new ErrorPasarelaException(
            "No fue posible crear la sesión de pago con Stripe"
            );
        }

    }
}
