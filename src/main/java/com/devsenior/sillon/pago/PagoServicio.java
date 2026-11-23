package com.devsenior.sillon.pago;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ParametroInvalidoException;
import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class PagoServicio {

    private static final String MENSAJE_SALON_NO_ENCONTRADO = "Salón no encontrado";
    private static final String MENSAJE_YA_ES_PRO = "El salón ya está en el plan PRO";
    private static final String MENSAJE_ESTADO_INVALIDO = "Estado inválido: use exito o cancelado";

    private final SalonRepository salonRepository;
    private final StripeClient stripeClient;
    private final StripePropiedades propiedades;
    private final String urlPublica;

    public PagoServicio(SalonRepository salonRepository, StripeClient stripeClient,
            StripePropiedades propiedades, @Value("${url.publica}") String urlPublica) {
        this.salonRepository = salonRepository;
        this.stripeClient = stripeClient;
        this.propiedades = propiedades;
        this.urlPublica = urlPublica;
    }

    public CheckoutRespuesta crearSuscripcion(UsuarioPrincipal principal) {
        Salon salon = buscarSalon(principal.getSalonId());
        if (salon.getPlan() == Plan.PRO) {
            throw new ReglaNegocioException(MENSAJE_YA_ES_PRO);
        }
        return crearSesion(salon, principal.getUsername(), TipoCompra.SUSCRIPCION_PRO);
    }

    public CheckoutRespuesta crearCreditos(UsuarioPrincipal principal) {
        Salon salon = buscarSalon(principal.getSalonId());
        return crearSesion(salon, principal.getUsername(), TipoCompra.PAQUETE_CREDITOS);
    }

    public String mensajeResultado(String estado) {
        return switch (estado) {
            case "exito" -> "¡Pago recibido! Tu compra se aplicará en unos segundos.";
            case "cancelado" -> "Pago cancelado. Puedes intentarlo de nuevo cuando quieras.";
            default -> throw new ParametroInvalidoException(MENSAJE_ESTADO_INVALIDO);
        };
    }

    private CheckoutRespuesta crearSesion(Salon salon, String email, TipoCompra tipo) {
        boolean suscripcion = tipo == TipoCompra.SUSCRIPCION_PRO;
        String precio = suscripcion ? propiedades.precioPro() : propiedades.precioCreditos();
        String salonId = String.valueOf(salon.getId());
        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(suscripcion ? SessionCreateParams.Mode.SUBSCRIPTION
                        : SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito")
                .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                .setCustomerEmail(email)
                .putMetadata("salonId", salonId)
                .putMetadata("tipo", tipo.name())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(precio)
                        .setQuantity(1L)
                        .build());
        if (suscripcion) {
            builder.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("salonId", salonId)
                    .putMetadata("tipo", tipo.name())
                    .build());
        }
        try {
            Session sesion = stripeClient.checkout().sessions().create(builder.build());
            return new CheckoutRespuesta(tipo, sesion.getId(), sesion.getUrl());
        } catch (StripeException e) {
            // throw con causa
            throw new PasarelaException("No se pudo crear la sesión de pago", e);
        }
    }

    private Salon buscarSalon(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_SALON_NO_ENCONTRADO));
    }
}
