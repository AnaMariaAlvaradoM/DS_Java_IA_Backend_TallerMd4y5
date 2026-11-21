package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.compartido.excepcion.PasarelaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.pago.dto.ResultadoPagoRespuesta;
import com.devsenior.sillon.pago.dto.SesionPagoRespuesta;
import com.devsenior.sillon.pago.dto.WebhookRespuesta;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PagoService {

    private final StripeClient stripeClient;
    private final SalonRepository salonRepository;
    private final EventoStripeRepository eventoStripeRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private final String precioPro;
    private final String precioCreditos;
    private final String secretoWebhook;
    private final String urlPublica;
    private final int creditosPlanPro;
    private final int creditosPaquete;

    public PagoService(
            StripeClient stripeClient,
            SalonRepository salonRepository,
            EventoStripeRepository eventoStripeRepository,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${sillon.stripe.precio-pro}") String precioPro,
            @Value("${sillon.stripe.precio-creditos}") String precioCreditos,
            @Value("${sillon.stripe.secreto-webhook}") String secretoWebhook,
            @Value("${sillon.url-publica:http://localhost:8080}") String urlPublica,
            @Value("${sillon.creditos.plan-pro:300}") int creditosPlanPro,
            @Value("${sillon.creditos.paquete:200}") int creditosPaquete
    ) {
        this.stripeClient = stripeClient;
        this.salonRepository = salonRepository;
        this.eventoStripeRepository = eventoStripeRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.precioPro = precioPro;
        this.precioCreditos = precioCreditos;
        this.secretoWebhook = secretoWebhook;
        this.urlPublica = urlPublica;
        this.creditosPlanPro = creditosPlanPro;
        this.creditosPaquete = creditosPaquete;
    }

    @Transactional(readOnly = true)
    public SesionPagoRespuesta crearSesionSuscripcion(UsuarioPrincipal principal) {
        Salon salon = salonRepository.findById(principal.getSalonId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        if (salon.getPlan() == Plan.PRO) {
            throw new ReglaNegocioException("El salón ya cuenta con el plan PRO activo");
        }

        String salonIdStr = String.valueOf(salon.getId());
        String tipoStr = TipoCompra.SUSCRIPCION_PRO.name();

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomerEmail(principal.getUsername())
                    .setClientReferenceId(salonIdStr)
                    .putMetadata("salonId", salonIdStr)
                    .putMetadata("salon_id", salonIdStr)
                    .putMetadata("tipoCompra", tipoStr)
                    .putMetadata("tipo_compra", tipoStr)
                    .putMetadata("tipo", tipoStr)
                    .setSubscriptionData(
                            SessionCreateParams.SubscriptionData.builder()
                                    .putMetadata("salonId", salonIdStr)
                                    .putMetadata("salon_id", salonIdStr)
                                    .putMetadata("tipoCompra", tipoStr)
                                    .putMetadata("tipo_compra", tipoStr)
                                    .putMetadata("tipo", tipoStr)
                                    .build()
                    )
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(precioPro)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                    .build();

            Session session = stripeClient.checkout().sessions().create(params);
            return new SesionPagoRespuesta(TipoCompra.SUSCRIPCION_PRO, session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new PasarelaException("Error al comunicarse con Stripe: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public SesionPagoRespuesta crearSesionCreditos(UsuarioPrincipal principal) {
        Salon salon = salonRepository.findById(principal.getSalonId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        String salonIdStr = String.valueOf(salon.getId());
        String tipoStr = TipoCompra.PAQUETE_CREDITOS.name();

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomerEmail(principal.getUsername())
                    .setClientReferenceId(salonIdStr)
                    .putMetadata("salonId", salonIdStr)
                    .putMetadata("salon_id", salonIdStr)
                    .putMetadata("tipoCompra", tipoStr)
                    .putMetadata("tipo_compra", tipoStr)
                    .putMetadata("tipo", tipoStr)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(precioCreditos)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                    .build();

            Session session = stripeClient.checkout().sessions().create(params);
            return new SesionPagoRespuesta(TipoCompra.PAQUETE_CREDITOS, session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new PasarelaException("Error al comunicarse con Stripe: " + e.getMessage());
        }
    }

    @Transactional
    public WebhookRespuesta procesarWebhook(String payload, String sigHeader) {
        if (sigHeader == null || sigHeader.isBlank()) {
            throw new FirmaInvalidaException("Firma de webhook de Stripe ausente o inválida");
        }

        Event evento;
        try {
            evento = Webhook.constructEvent(payload, sigHeader, secretoWebhook);
        } catch (SignatureVerificationException e) {
            throw new FirmaInvalidaException("Firma de webhook de Stripe no verificada: " + e.getMessage());
        } catch (Exception e) {
            throw new FirmaInvalidaException("Error al procesar firma de webhook: " + e.getMessage());
        }

        if (eventoStripeRepository.existsById(evento.getId())) {
            return new WebhookRespuesta(true, false);
        }

        JsonNode dataObj = extraerDataObject(payload);

        if ("checkout.session.completed".equals(evento.getType()) && dataObj != null) {
            procesarCheckoutCompleted(dataObj);
        } else if ("customer.subscription.deleted".equals(evento.getType()) && dataObj != null) {
            procesarSubscriptionDeleted(dataObj);
        }

        eventoStripeRepository.save(new EventoStripe(evento.getId(), evento.getType(), LocalDateTime.now(clock)));
        return new WebhookRespuesta(true, true);
    }

    private void procesarCheckoutCompleted(JsonNode dataObj) {
        String paymentStatus = getString(dataObj, "payment_status");
        if (paymentStatus != null && !"paid".equalsIgnoreCase(paymentStatus)) {
            return;
        }

        JsonNode metadata = dataObj.has("metadata") && dataObj.get("metadata").isObject()
                ? dataObj.get("metadata")
                : null;

        String salonIdStr = extraerSalonId(dataObj, metadata);
        String tipoCompraStr = extraerTipoCompra(dataObj, metadata);
        String subscriptionId = extraerSubscriptionId(dataObj);

        if (salonIdStr == null || salonIdStr.isBlank()) {
            return;
        }

        long salonId;
        try {
            salonId = Long.parseLong(salonIdStr.trim());
        } catch (NumberFormatException e) {
            return;
        }

        Salon salon = salonRepository.findById(salonId).orElse(null);
        if (salon == null) {
            return;
        }

        if (TipoCompra.SUSCRIPCION_PRO.name().equalsIgnoreCase(tipoCompraStr)
                || "PRO".equalsIgnoreCase(tipoCompraStr)) {
            salon.setPlan(Plan.PRO);
            if (subscriptionId != null && !subscriptionId.isBlank()) {
                salon.setStripeSuscripcionId(subscriptionId);
            }
            salon.setCreditosIa(salon.getCreditosIa() + creditosPlanPro);
            salonRepository.saveAndFlush(salon);
        } else if (TipoCompra.PAQUETE_CREDITOS.name().equalsIgnoreCase(tipoCompraStr)
                || "CREDITOS".equalsIgnoreCase(tipoCompraStr)) {
            salon.setCreditosIa(salon.getCreditosIa() + creditosPaquete);
            salonRepository.saveAndFlush(salon);
        }
    }

    private void procesarSubscriptionDeleted(JsonNode dataObj) {
        String subId = getString(dataObj, "id");
        if (subId == null || subId.isBlank()) {
            subId = getString(dataObj, "subscription");
        }

        Salon salon = null;
        if (subId != null && !subId.isBlank()) {
            salon = salonRepository.findByStripeSuscripcionId(subId).orElse(null);
        }

        if (salon == null && dataObj.has("metadata") && dataObj.get("metadata").isObject()) {
            JsonNode metadata = dataObj.get("metadata");
            String salonIdStr = getPrimerValor(metadata, "salonId", "salon_id", "idSalon", "salon");
            if (salonIdStr != null && !salonIdStr.isBlank()) {
                try {
                    salon = salonRepository.findById(Long.parseLong(salonIdStr.trim())).orElse(null);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (salon != null) {
            salon.setPlan(Plan.GRATIS);
            salon.setStripeSuscripcionId(null);
            salonRepository.saveAndFlush(salon);
        }
    }

    private JsonNode extraerDataObject(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root != null && root.has("data") && root.get("data").isObject()) {
                JsonNode data = root.get("data");
                if (data.has("object") && data.get("object").isObject()) {
                    return data.get("object");
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extraerSalonId(JsonNode dataObj, JsonNode metadata) {
        String valor = getPrimerValor(metadata, "salonId", "salon_id", "idSalon", "salon");
        if (valor != null && !valor.isBlank()) {
            return valor;
        }
        return getString(dataObj, "client_reference_id");
    }

    private String extraerTipoCompra(JsonNode dataObj, JsonNode metadata) {
        String valor = getPrimerValor(metadata, "tipoCompra", "tipo_compra", "tipo", "plan", "producto");
        if (valor != null && !valor.isBlank()) {
            return valor;
        }
        if (metadata != null && metadata.isObject()) {
            for (Map.Entry<String, JsonNode> entry : metadata.properties()) {
                if (entry.getValue() != null && entry.getValue().isValueNode()) {
                    String val = entry.getValue().asText();
                    if (TipoCompra.SUSCRIPCION_PRO.name().equalsIgnoreCase(val)
                            || TipoCompra.PAQUETE_CREDITOS.name().equalsIgnoreCase(val)) {
                        return val;
                    }
                }
            }
        }
        String mode = getString(dataObj, "mode");
        if ("subscription".equalsIgnoreCase(mode)) {
            return TipoCompra.SUSCRIPCION_PRO.name();
        }
        if ("payment".equalsIgnoreCase(mode)) {
            return TipoCompra.PAQUETE_CREDITOS.name();
        }
        return null;
    }

    private String extraerSubscriptionId(JsonNode dataObj) {
        if (!dataObj.has("subscription") || dataObj.get("subscription").isNull()) {
            return null;
        }
        JsonNode subElem = dataObj.get("subscription");
        if (subElem.isValueNode()) {
            return subElem.asText();
        }
        if (subElem.isObject()) {
            return getString(subElem, "id");
        }
        return null;
    }

    private String getPrimerValor(JsonNode obj, String... keys) {
        if (obj == null) {
            return null;
        }
        for (String key : keys) {
            String val = getString(obj, key);
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return null;
    }

    private String getString(JsonNode obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isNull()) {
            return null;
        }
        JsonNode elem = obj.get(key);
        return elem.isValueNode() ? elem.asText() : null;
    }

    public ResultadoPagoRespuesta obtenerResultado(String estado) {
        String mensaje = "exito".equalsIgnoreCase(estado)
                ? "¡Pago procesado exitosamente! Tu cuenta ha sido actualizada."
                : "El proceso de pago fue cancelado. No se realizó ningún cobro.";
        return new ResultadoPagoRespuesta(estado, mensaje);
    }
}
