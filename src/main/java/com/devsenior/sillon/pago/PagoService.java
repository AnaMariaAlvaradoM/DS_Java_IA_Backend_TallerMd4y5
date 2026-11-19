package com.devsenior.sillon.pago;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.devsenior.sillon.compartido.excepcion.ErrorDePasarelaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
import com.devsenior.sillon.pago.dto.SesionPagoResponse;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class PagoService {

    private final SalonRepository salones;
    private final StripeClient stripe;
    private final ConfiguracionStripe configuracion;
    private final String urlPublica;

    public PagoService(SalonRepository salones, StripeClient stripe, ConfiguracionStripe configuracion,
            @Value("${sillon.url-publica}") String urlPublica) {
        this.salones = salones;
        this.stripe = stripe;
        this.configuracion = configuracion;
        this.urlPublica = urlPublica.replaceAll("/+$", "");
    }

    public SesionPagoResponse crearSesion(UsuarioPrincipal principal, TipoCompra tipo) {
        Salon salon = salones.findById(principal.getSalonId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        boolean suscripcion = tipo == TipoCompra.SUSCRIPCION_PRO;
        if (suscripcion && salon.getPlan() == Plan.PRO) {
            throw new ReglaDeNegocioException("El salón ya tiene el plan PRO");
        }

        Map<String, String> metadata = Map.of("salonId", salon.getId().toString(), "tipo", tipo.name());
        var parametros = SessionCreateParams.builder()
                .setMode(suscripcion ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(principal.getUsername())
                .setClientReferenceId(salon.getId().toString())
                .putAllMetadata(metadata)
                .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito")
                .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(suscripcion ? configuracion.precioPro() : configuracion.precioCreditos())
                        .setQuantity(1L).build());
        if (suscripcion) {
            parametros.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .putAllMetadata(metadata).build());
        }

        Session sesion;
        try {
            sesion = stripe.checkout().sessions().create(parametros.build());
        } catch (StripeException ex) {
            // No se propaga el mensaje del proveedor: puede contener datos de la solicitud.
            throw new ErrorDePasarelaException("Stripe no pudo crear la sesión de pago");
        } catch (NullPointerException ex) {
            // stripe-java 34.1 lanza NPE al deserializar una respuesta JSON null.
            throw new ErrorDePasarelaException("Stripe no devolvió una sesión de prueba válida");
        }
        if (sesion == null || sesion.getId() == null || sesion.getId().isBlank()
                || !urlValida(sesion.getUrl()) || Boolean.TRUE.equals(sesion.getLivemode())) {
            throw new ErrorDePasarelaException("Stripe no devolvió una sesión de prueba válida");
        }
        return new SesionPagoResponse(tipo, sesion.getId(), sesion.getUrl());
    }

    private static boolean urlValida(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}
