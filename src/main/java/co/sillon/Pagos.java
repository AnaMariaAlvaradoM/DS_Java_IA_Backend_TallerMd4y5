package co.sillon;

import tools.jackson.databind.*;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.Clock;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import static co.sillon.Fallos.fallo;

@RestController
@Transactional
class Pagos {
    private final Datos datos;
    private final ObjectMapper json;
    private final Clock clock;
    private final com.stripe.StripeClient stripe;
    private final String clave, secreto, pro, creditos, publica;
    Pagos(Datos datos, ObjectMapper json, Clock clock, com.stripe.StripeClient stripe,
          @Value("${sillon.stripe.clave}") String clave,
          @Value("${sillon.stripe.webhook}") String secreto, @Value("${sillon.stripe.pro}") String pro,
          @Value("${sillon.stripe.creditos}") String creditos, @Value("${sillon.url-publica}") String publica) {
        this.datos=datos; this.json=json; this.clock=clock; this.stripe=stripe;
        this.clave=clave; this.secreto=secreto; this.pro=pro;
        this.creditos=creditos; this.publica=publica.replaceFirst("/+$","");
    }
    record Checkout(String tipo, String sesionId, String urlPago) {}
    @PostMapping("/api/pagos/suscripcion") @SecurityRequirement(name="sesion")
    Checkout suscripcion(@AuthenticationPrincipal Actor jwt) {
        var s=datos.salon(jwt.salon(),false);
        if (s.plan.equals("PRO")) throw fallo(409,"El salón ya tiene plan PRO");
        return checkout(s,jwt.email(),true);
    }
    @PostMapping("/api/pagos/creditos") @SecurityRequirement(name="sesion")
    Checkout creditos(@AuthenticationPrincipal Actor jwt) { return checkout(datos.salon(jwt.salon(),false),jwt.email(),false); }
    private Checkout checkout(Salon s,String correo,boolean plan) {
        var precio=plan ? pro : creditos;
        if (clave.isBlank() || precio.isBlank()) throw fallo(502,"Configura Stripe y los precios antes de cobrar");
        String tipo=plan ? "SUSCRIPCION_PRO" : "PAQUETE_CREDITOS";
        var meta=Map.of("salonId",s.id.toString(),"tipo",tipo);
        var params=SessionCreateParams.builder().setMode(plan ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(correo).setClientReferenceId(s.id.toString()).putAllMetadata(meta)
                .setSuccessUrl(publica+"/api/pagos/resultado?estado=exito").setCancelUrl(publica+"/api/pagos/resultado?estado=cancelado")
                .addLineItem(SessionCreateParams.LineItem.builder().setPrice(precio).setQuantity(1L).build());
        if (plan) params.setSubscriptionData(SessionCreateParams.SubscriptionData.builder().putAllMetadata(meta).build());
        try {
            var sesion=stripe.v1().checkout().sessions().create(params.build());
            if (sesion.getUrl()==null || !sesion.getUrl().startsWith("https://")) throw fallo(502,"Stripe omitió la URL de pago");
            return new Checkout(tipo,sesion.getId(),sesion.getUrl());
        } catch (com.stripe.exception.StripeException e) { throw fallo(502,"No se pudo abrir la sesión de pago en Stripe"); }
    }
    @GetMapping("/api/pagos/resultado")
    Map<String,String> retorno(@RequestParam(defaultValue="") String estado) {
        return Map.of("mensaje", "exito".equals(estado) ? "Gracias. El salón recibirá la confirmación del pago de Stripe." : "Se cerró el proceso de pago.");
    }

    @PostMapping("/api/pagos/webhook")
    Map<String,Boolean> webhook(@RequestHeader(name="Stripe-Signature",required=false) String firma, @RequestBody byte[] cuerpo) {
        FirmaStripe.verificar(firma,cuerpo,secreto,clock);
        try {
            var evento=json.readTree(cuerpo);
            if (evento==null || !evento.isObject()) throw fallo(400,"Evento inválido");
            String id=campo(evento,"id"), tipo=campo(evento,"type");
            if (datos.em().find(EventoPago.class,id)!=null) return Map.of("recibido",true,"procesado",false);
            var objeto=evento.path("data").path("object");
            boolean procesado=switch (tipo) {
                case "checkout.session.completed","checkout.session.async_payment_succeeded" -> acreditar(id,objeto);
                case "customer.subscription.deleted" -> cancelar(id,objeto);
                default -> false;
            };
            return Map.of("recibido",true,"procesado",procesado);
        } catch (tools.jackson.core.JacksonException e) { throw fallo(400,"JSON de evento inválido"); }
    }
    private static String campo(JsonNode n, String nombre) {
        var valor=n.path(nombre).asText("");
        if (valor.isBlank() || valor.length()>190) throw fallo(400,"Falta el identificador "+nombre);
        return valor;
    }
    private boolean registrar(String id, String operacion) {
        var previos=datos.em().createQuery("select id from EventoPago where id=:id or operacion=:op",String.class)
                .setParameter("id",id).setParameter("op",operacion).getResultList();
        if (!previos.isEmpty()) return false;
        var evento=new EventoPago(); evento.id=id; evento.operacion=operacion; evento.tipo=operacion.startsWith("cancel/") ? "customer.subscription.deleted" : "checkout.session.completed"; evento.fechaProcesado=java.time.LocalDateTime.now(clock); datos.em().persist(evento);
        return true;
    }
    private boolean acreditar(String id, JsonNode o) {
        if (!"paid".equals(o.path("payment_status").asText())) return false;
        var meta=o.path("metadata");
        String tipo=meta.path("tipo").asText(), modo=o.path("mode").asText();
        boolean plan=tipo.equals("SUSCRIPCION_PRO") && modo.equals("subscription");
        if (!plan && !(tipo.equals("PAQUETE_CREDITOS") && modo.equals("payment"))) return false;
        long salonId=meta.path("salonId").asLong(-1);
        if (salonId<=0) throw fallo(400,"Falta el salón del pago");
        var s=datos.salon(salonId,true);
        String sub=plan ? campo(o,"subscription") : null;
        if (!registrar(id,"checkout/"+campo(o,"id"))) return false;
        if (plan) {
            if (s.plan.equals("PRO")) return false;
            s.plan="PRO"; s.stripeSuscripcionId=sub; s.creditosIa=s.creditosIa+300;
        } else s.creditosIa=s.creditosIa+200;
        return true;
    }
    private boolean cancelar(String id, JsonNode o) {
        String sub=campo(o,"id");
        var salones=datos.em().createQuery("from Salon where stripeSuscripcionId=:sub",Salon.class)
                .setParameter("sub",sub).setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE).getResultList();
        if (salones.isEmpty() || !registrar(id,"cancel/"+sub)) return false;
        var s=salones.get(0); s.plan="GRATIS"; s.stripeSuscripcionId=null;
        return true;
    }
}
