package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PagoService {
    private final SalonRepository salones;
    private final StripeClient stripe;
    private final String precioPro;
    private final String precioCreditos;
    private final String urlPublica;

    public PagoService(SalonRepository salones, StripeClient stripe,
            @Value("${sillon.stripe.precio-pro}") String precioPro,
            @Value("${sillon.stripe.precio-creditos}") String precioCreditos,
            @Value("${sillon.url-publica}") String urlPublica) {
        this.salones = salones;
        this.stripe = stripe;
        this.precioPro = precioPro;
        this.precioCreditos = precioCreditos;
        this.urlPublica = urlPublica.replaceAll("/+$", "");
    }

    public SesionPagoResponse crearSuscripcion(UsuarioPrincipal dueno) {
        return crearSesion(dueno, TipoCompra.SUSCRIPCION_PRO);
    }

    public SesionPagoResponse comprarCreditos(UsuarioPrincipal dueno) {
        return crearSesion(dueno, TipoCompra.PAQUETE_CREDITOS);
    }

    private SesionPagoResponse crearSesion(UsuarioPrincipal dueno, TipoCompra tipo) {
        Salon salon = salones.findById(dueno.getSalonId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salón no encontrado"));
        if (tipo == TipoCompra.SUSCRIPCION_PRO && salon.getPlan() == Plan.PRO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El salón ya tiene el plan PRO");
        }

        String salonId = salon.getId().toString();
        String tipoNombre = tipo.name();
        SessionCreateParams.Builder parametros = SessionCreateParams.builder()
                .setMode(tipo == TipoCompra.SUSCRIPCION_PRO
                        ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(dueno.getUsername())
                .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito")
                .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                .setClientReferenceId(salonId)
                .putMetadata("salonId", salonId)
                .putMetadata("tipo", tipoNombre)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(tipo == TipoCompra.SUSCRIPCION_PRO ? precioPro : precioCreditos)
                        .setQuantity(1L).build());
        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            parametros.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("salonId", salonId)
                    .putMetadata("tipo", tipoNombre)
                    .build());
        }

        try {
            Session sesion = stripe.v1().checkout().sessions().create(parametros.build());
            if (sesion == null || sesion.getId() == null || sesion.getId().isBlank()
                    || sesion.getUrl() == null || sesion.getUrl().isBlank()) {
                throw new PasarelaException("No fue posible iniciar el pago con Stripe");
            }
            return new SesionPagoResponse(tipo, sesion.getId(), sesion.getUrl());
        } catch (StripeException exception) {
            throw new PasarelaException("No fue posible iniciar el pago con Stripe");
        }
    }
}
