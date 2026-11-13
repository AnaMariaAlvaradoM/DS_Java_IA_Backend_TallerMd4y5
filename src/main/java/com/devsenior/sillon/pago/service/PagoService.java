package com.devsenior.sillon.pago.service;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.compartido.ServicioExternoException;
import com.devsenior.sillon.pago.config.StripeProperties;
import com.devsenior.sillon.pago.dto.PagoResponse;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PagoService {

    private final StripeClient stripeClient;
    private final StripeProperties stripeProperties;
    private final SalonRepository salonRepository;

    public PagoService(StripeClient stripeClient, StripeProperties stripeProperties, SalonRepository salonRepository) {
        this.stripeClient = stripeClient;
        this.stripeProperties = stripeProperties;
        this.salonRepository = salonRepository;
    }

    public PagoResponse crearSesionSuscripcion(String emailDueno, Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ReglaNegocioException("Salón no encontrado"));
        if (salon.isPro()) { // Ajusta "isPro()" según cómo se llame el método o propiedad en tu entidad Salon
            throw new ReglaNegocioException("El salón ya cuenta con una suscripción PRO activa");
        }
        Session session = crearSesionEnStripe(
                stripeProperties.getPrecioPro(),
                SessionCreateParams.Mode.SUBSCRIPTION,
                emailDueno,
                salonId,
                "SUSCRIPCION_PRO"
        );

        return new PagoResponse("SUSCRIPCION_PRO", session.getId(), session.getUrl());
    }

    public PagoResponse crearSesionCreditos(String emailDueno, Long salonId) {
        Session session = crearSesionEnStripe(
                stripeProperties.getPrecioPaquete(),
                SessionCreateParams.Mode.PAYMENT,
                emailDueno,
                salonId,
                "PAQUETE_CREDITOS"
        );

        return new PagoResponse("PAQUETE_CREDITOS", session.getId(), session.getUrl());
    }
    private Session crearSesionEnStripe(String priceId, SessionCreateParams.Mode modo, String emailDueno, Long salonId, String tipoCompra) {
        // 1. Crear el mapa de Metadata requerido
        Map<String, String> metadata = Map.of(
                "salonId", String.valueOf(salonId),
                "tipo", tipoCompra
        );

        // 2. Construir los parámetros base
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(modo)
                .setCustomerEmail(emailDueno) // El correo del dueño
                .setClientReferenceId(String.valueOf(salonId)) // El ID del salón como referencia
                .putAllMetadata(metadata) // Metadata principal de la sesión
                // Las URLs sobre tu URL pública configurada
                .setSuccessUrl(stripeProperties.getUrlPublica() + "/api/pagos/resultado?estado=exito")
                .setCancelUrl(stripeProperties.getUrlPublica() + "/api/pagos/resultado?estado=cancelado")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build()
                );

        // 3. Condición especial: Si es suscripción, la metadata también va en los datos de suscripción
        if (modo == SessionCreateParams.Mode.SUBSCRIPTION) {
            paramsBuilder.setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                            .putAllMetadata(metadata)
                            .build()
            );
        }

        // 4. Crear sesión en Stripe
        try {
            return stripeClient.checkout().sessions().create(paramsBuilder.build());
        } catch (StripeException e) {
            throw new ServicioExternoException("Error al conectar con la pasarela de pagos");
        }
    }
}
