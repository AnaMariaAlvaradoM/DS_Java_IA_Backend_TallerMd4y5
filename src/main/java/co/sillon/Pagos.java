package co.sillon;

import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final RestClient http;
    private final String api, clave, secreto, pro, creditos, publica;
    Pagos(Datos datos, ObjectMapper json, Clock clock, RestClient http,
          @Value("${sillon.stripe.api}") String api, @Value("${sillon.stripe.clave}") String clave,
          @Value("${sillon.stripe.webhook}") String secreto, @Value("${sillon.stripe.pro}") String pro,
          @Value("${sillon.stripe.creditos}") String creditos, @Value("${sillon.url-publica}") String publica) {
        this.datos=datos; this.json=json; this.clock=clock; this.http=http;
        this.api=api.replaceFirst("/+$",""); this.clave=clave; this.secreto=secreto; this.pro=pro;
        this.creditos=creditos; this.publica=publica.replaceFirst("/+$","");
    }
    record Checkout(String urlPago) {}
    @PostMapping("/api/pagos/suscripcion") @SecurityRequirement(name="sesion")
    Checkout suscripcion(@AuthenticationPrincipal Jwt jwt) {
        var s=datos.salon(Actor.de(jwt).salon(),false);
        if (s.plan.equals("PRO")) throw fallo(409,"El salón ya tiene plan PRO");
        return checkout(s,true);
    }
    @PostMapping("/api/pagos/creditos") @SecurityRequirement(name="sesion")
    Checkout creditos(@AuthenticationPrincipal Jwt jwt) { return checkout(datos.salon(Actor.de(jwt).salon(),false),false); }
    private Checkout checkout(Salon s, boolean plan) {
        var precio=plan ? pro : creditos;
        if (clave.isBlank() || precio.isBlank()) throw fallo(503,"Configura Stripe y los precios antes de cobrar");
        var form=new LinkedMultiValueMap<String,String>();
        form.add("mode",plan ? "subscription":"payment"); form.add("line_items[0][price]",precio);
        form.add("line_items[0][quantity]","1"); form.add("metadata[salonId]",s.id.toString());
        form.add("metadata[tipo]",plan ? "SUSCRIPCION_PRO":"PAQUETE_CREDITOS");
        form.add("success_url",publica+"/pagos/retorno?resultado=exito"); form.add("cancel_url",publica+"/pagos/retorno?resultado=cancelado");
        try {
            var respuesta=http.post().uri(api+"/v1/checkout/sessions").headers(h -> h.setBasicAuth(clave,""))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
            if (respuesta==null || !respuesta.path("url").asText("").startsWith("https://")) throw fallo(502,"Stripe omitió la URL de pago");
            return new Checkout(respuesta.get("url").asText());
        } catch (RestClientException e) { throw fallo(502,"No se pudo abrir la sesión de pago en Stripe"); }
    }
    @GetMapping(value="/pagos/retorno",produces=MediaType.TEXT_PLAIN_VALUE)
    String retorno(@RequestParam(defaultValue="") String resultado) {
        return "exito".equals(resultado) ? "Gracias. El salón recibirá la confirmación del pago de Stripe." : "Se cerró el proceso de pago.";
    }

    @PostMapping("/api/pagos/webhook")
    Map<String,Boolean> webhook(@RequestHeader(name="Stripe-Signature",required=false) String firma, @RequestBody byte[] cuerpo) {
        FirmaStripe.verificar(firma,cuerpo,secreto,clock);
        try {
            var evento=json.readTree(cuerpo);
            if (evento==null || !evento.isObject()) throw fallo(400,"Evento inválido");
            String id=campo(evento,"id"), tipo=campo(evento,"type");
            if (datos.em().find(EventoPago.class,id)!=null) return Map.of("procesado",false);
            var objeto=evento.path("data").path("object");
            boolean procesado=switch (tipo) {
                case "checkout.session.completed","checkout.session.async_payment_succeeded" -> acreditar(id,objeto);
                case "customer.subscription.deleted" -> cancelar(id,objeto);
                default -> false;
            };
            return Map.of("procesado",procesado);
        } catch (IOException e) { throw fallo(400,"JSON de evento inválido"); }
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
        var evento=new EventoPago(); evento.id=id; evento.operacion=operacion; datos.em().persist(evento);
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
            s.plan="PRO"; s.suscripcion=sub; s.creditosIa+=300;
        } else s.creditosIa+=200;
        return true;
    }
    private boolean cancelar(String id, JsonNode o) {
        String sub=campo(o,"id");
        var salones=datos.em().createQuery("from Salon where suscripcion=:sub",Salon.class)
                .setParameter("sub",sub).setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE).getResultList();
        if (salones.isEmpty() || !registrar(id,"cancel/"+sub)) return false;
        var s=salones.getFirst(); s.plan="GRATIS"; s.suscripcion=null;
        return true;
    }
}
