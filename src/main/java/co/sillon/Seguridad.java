package co.sillon;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(info=@Info(title="Sillón · Agenda de salones",version="1.0"))
@SecurityScheme(name="sesion",type=SecuritySchemeType.HTTP,scheme="bearer",bearerFormat="JWT")
class Seguridad {
    @Bean SecretKey clave(@Value("${sillon.jwt.secreto}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        }
        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRETO requiere al menos 32 bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean JwtEncoder encoder(SecretKey clave) { return new NimbusJwtEncoder(new ImmutableSecret<>(clave)); }
    @Bean JwtDecoder decoder(SecretKey clave) {
        var decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("sillon"));
        return decoder;
    }
    @Bean BCryptPasswordEncoder passwords() { return new BCryptPasswordEncoder(); }

    @Bean SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        return http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**","/api/publico/**","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html",
                                 "/pagos/retorno","/error").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/pagos/webhook").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("SCOPE_ADMIN")
                .requestMatchers("/api/salon/configuracion","/api/pagos/**","/api/clientes","/api/solicitudes").hasAuthority("SCOPE_DUENO")
                .requestMatchers(HttpMethod.POST,"/api/estilistas","/api/servicios","/api/citas").hasAuthority("SCOPE_DUENO")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> {})).build();
    }
}
