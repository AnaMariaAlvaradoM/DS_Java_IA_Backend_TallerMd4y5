package co.sillon;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static co.sillon.Fallos.fallo;

final class FirmaStripe {
    static void verificar(String cabecera, byte[] cuerpo, String secreto, Clock clock) {
        if (secreto.isBlank()) throw fallo(503,"Falta configurar el secreto de webhooks");
        if (cabecera==null) throw fallo(400,"Falta la firma de Stripe");
        String marca=null;
        var firmas=new ArrayList<String>();
        for (String parte : cabecera.split(",")) {
            var par=parte.trim().split("=",2);
            if (par.length!=2) continue;
            if (par[0].equals("t")) marca=par[1];
            if (par[0].equals("v1")) firmas.add(par[1]);
        }
        try {
            var ahora=clock.instant().getEpochSecond();
            long t=Long.parseLong(marca);
            if (t<ahora-300 || t>ahora+300) throw fallo(400,"Firma fuera de vigencia");
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            mac.update((marca+".").getBytes(StandardCharsets.UTF_8));
            byte[] esperado=mac.doFinal(cuerpo);
            for (String firma : firmas) {
                if (firma.matches("[a-fA-F0-9]{64}") && MessageDigest.isEqual(esperado,HexFormat.of().parseHex(firma))) return;
            }
        } catch (NumberFormatException e) { throw fallo(400,"Marca temporal inválida"); }
          catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
        throw fallo(400,"Firma de Stripe inválida");
    }
}
