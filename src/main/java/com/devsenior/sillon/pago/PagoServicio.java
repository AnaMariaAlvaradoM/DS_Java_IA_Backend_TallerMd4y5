package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.compartido.SolicitudInvalidaException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepositorio;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepositorio;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PagoServicio {
    private final SalonRepositorio salones;
    private final UsuarioRepositorio usuarios;
    private final CheckoutGateway checkout;
    private final StripeConfiguracion stripe;
    private final String urlPublica;

    public PagoServicio(SalonRepositorio salones, UsuarioRepositorio usuarios, CheckoutGateway checkout,
            StripeConfiguracion stripe, @Value("${sillon.url-publica}") String urlPublica) {
        this.salones = salones;
        this.usuarios = usuarios;
        this.checkout = checkout;
        this.stripe = stripe;
        this.urlPublica = urlPublica.replaceAll("/+$", "");
    }

    public SesionPagoRespuesta suscripcion(UsuarioPrincipal principal) {
        return crearSesion(principal, TipoCompra.SUSCRIPCION_PRO);
    }

    public SesionPagoRespuesta creditos(UsuarioPrincipal principal) {
        return crearSesion(principal, TipoCompra.PAQUETE_CREDITOS);
    }

    private SesionPagoRespuesta crearSesion(UsuarioPrincipal principal, TipoCompra tipo) {
        Salon salon = salones.findById(principal.getSalonId())
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        if (tipo == TipoCompra.SUSCRIPCION_PRO && salon.getPlan() == Plan.PRO) {
            throw new ReglaNegocioException("El salón ya tiene el plan PRO");
        }
        Usuario dueno = usuarios.findByIdAndSalon_IdAndRol(principal.getId(), principal.getSalonId(), Rol.DUENO)
                .orElseThrow(() -> new NoEncontradoException("Dueño no encontrado"));
        String salonId = salon.getId().toString();
        String precio = tipo == TipoCompra.SUSCRIPCION_PRO ? stripe.precioPro() : stripe.precioCreditos();
        SessionCreateParams.Builder parametros = SessionCreateParams.builder()
                .setMode(tipo == TipoCompra.SUSCRIPCION_PRO
                        ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(dueno.getEmail())
                .setClientReferenceId(salonId)
                .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito")
                .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                .putMetadata("salonId", salonId)
                .putMetadata("tipoCompra", tipo.name())
                .addLineItem(SessionCreateParams.LineItem.builder().setPrice(precio).setQuantity(1L).build());
        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            parametros.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("salonId", salonId)
                    .putMetadata("tipoCompra", tipo.name()).build());
        }
        SesionCheckout sesion = checkout.crear(parametros.build());
        if (sesion == null || sesion.id() == null || sesion.id().isBlank()
                || sesion.url() == null || !sesion.url().startsWith("https://")) {
            throw new PasarelaException("Stripe no devolvió una sesión de pago válida");
        }
        return new SesionPagoRespuesta(tipo, sesion.id(), sesion.url());
    }

    public ResultadoPagoRespuesta resultado(String estado) {
        return switch (estado) {
            case "exito" -> new ResultadoPagoRespuesta("El pago se está procesando. Consulte su salón para confirmar el resultado.");
            case "cancelado" -> new ResultadoPagoRespuesta("El proceso de pago fue cancelado.");
            default -> throw new SolicitudInvalidaException("El estado debe ser exito o cancelado");
        };
    }
}
