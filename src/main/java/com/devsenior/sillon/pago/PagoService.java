package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.SalonResponse;
import com.devsenior.sillon.salon.SalonService;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PagoService {
    private final SalonService salones;
    private final PasarelaStripe pasarela;
    private final StripeProperties properties;
    private final String urlPublica;

    public PagoService(SalonService salones, PasarelaStripe pasarela, StripeProperties properties,
            @Value("${sillon.url-publica}") String urlPublica) {
        this.salones = salones;
        this.pasarela = pasarela;
        this.properties = properties;
        this.urlPublica = urlPublica.replaceAll("/+$", "");
    }

    public PagoResponse crear(UsuarioPrincipal principal, TipoCompra tipo) {
        SalonResponse salon = salones.consultar(principal.getSalonId());
        boolean pro = tipo == TipoCompra.SUSCRIPCION_PRO;
        if (pro && salon.plan() == Plan.PRO) { throw new ReglaNegocioException("El salón ya tiene el plan PRO"); }
        String salonId = salon.id().toString();
        var builder = SessionCreateParams.builder()
                .setMode(pro ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(principal.getUsername()).setClientReferenceId(salonId)
                .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito")
                .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                .putMetadata("salonId", salonId).putMetadata("tipoCompra", tipo.name())
                .addLineItem(SessionCreateParams.LineItem.builder().setPrice(
                        pro ? properties.getPrecioPro() : properties.getPrecioCreditos()).setQuantity(1L).build());
        if (pro) {
            builder.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("salonId", salonId).putMetadata("tipoCompra", tipo.name()).build());
        }
        Session sesion = pasarela.crearSesion(builder.build());
        return new PagoResponse(tipo, sesion.getId(), sesion.getUrl());
    }
}
