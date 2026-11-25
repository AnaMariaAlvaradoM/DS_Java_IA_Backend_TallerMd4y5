
package com.devsenior.sillon.pago.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.pago.configuracion.PropiedadesStripe;
import com.devsenior.sillon.pago.dto.RespuestaSesionPago;
import com.devsenior.sillon.pago.modelo.TipoCompra;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class PagoServicio {

    private final StripeClient stripeClient;
    private final PropiedadesStripe propiedades;
    private final SalonRepositorio salonRepositorio;
    private final String urlPublica;

    public PagoServicio(
            StripeClient stripeClient,
            PropiedadesStripe propiedades,
            SalonRepositorio salonRepositorio,
            @Value("${sillon.url-publica}") String urlPublica) {

        this.stripeClient = stripeClient;
        this.propiedades = propiedades;
        this.salonRepositorio = salonRepositorio;
        this.urlPublica = urlPublica.replaceAll("/+$", "");
    }

    public RespuestaSesionPago crearSesion(
            Long salonId,
            String correoDueno,
            TipoCompra tipo) {

        Salon salon = salonRepositorio.findById(salonId)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Salón no encontrado"));

        if (tipo == TipoCompra.SUSCRIPCION_PRO
                && salon.getPlan() == Plan.PRO) {
            throw new ExcepcionConflicto(
                    "El salón ya tiene activo el plan PRO");
        }

        String precioId = tipo == TipoCompra.SUSCRIPCION_PRO
                ? propiedades.precioPro()
                : propiedades.precioCreditos();

        SessionCreateParams.Mode modo = tipo == TipoCompra.SUSCRIPCION_PRO
                ? SessionCreateParams.Mode.SUBSCRIPTION
                : SessionCreateParams.Mode.PAYMENT;

        SessionCreateParams.Builder parametros = SessionCreateParams.builder()
                .setMode(modo)
                .setCustomerEmail(correoDueno)
                .setClientReferenceId(salonId.toString())
                .setSuccessUrl(urlPublica
                        + "/api/pagos/resultado?estado=exito")
                .setCancelUrl(urlPublica
                        + "/api/pagos/resultado?estado=cancelado")
                .putMetadata("salonId", salonId.toString())
                .putMetadata("tipoCompra", tipo.name())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(precioId)
                                .setQuantity(1L)
                                .build());

        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            parametros.setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                            .putMetadata("salonId", salonId.toString())
                            .putMetadata("tipoCompra", tipo.name())
                            .build());
        }

        try {
            Session sesion = stripeClient.checkout()
                    .sessions()
                    .create(parametros.build());

            return new RespuestaSesionPago(
                    tipo,
                    sesion.getId(),
                    sesion.getUrl());

        } catch (StripeException excepcion) {
            throw new ExcepcionIntegracion(
                    "No fue posible crear la sesión de pago con Stripe");
        }
    }
}
