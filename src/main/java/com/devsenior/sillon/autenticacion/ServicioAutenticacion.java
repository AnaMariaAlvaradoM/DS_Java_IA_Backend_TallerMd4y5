
package com.devsenior.sillon.autenticacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.GeneradorSlug;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.ServicioJwt;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import com.devsenior.sillon.compartido.CredencialesInvalidasException;

@Service
public class ServicioAutenticacion {

    private final SalonRepository salonRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeneradorSlug generadorSlug;
    private final PasswordEncoder passwordEncoder;
    private final ServicioJwt servicioJwt;
    private final Clock reloj;
    private final int creditosRegistro;
    private final long expiracionMinutos;

    public ServicioAutenticacion(
            SalonRepository salonRepository,
            UsuarioRepository usuarioRepository,
            GeneradorSlug generadorSlug,
            PasswordEncoder passwordEncoder,
            ServicioJwt servicioJwt,
            Clock reloj,
            @Value("${sillon.creditos-registro:10}") int creditosRegistro,
            @Value("${sillon.jwt.expiracion-horas:24}") long expiracionHoras) {

        this.salonRepository = salonRepository;
        this.usuarioRepository = usuarioRepository;
        this.generadorSlug = generadorSlug;
        this.passwordEncoder = passwordEncoder;
        this.servicioJwt = servicioJwt;
        this.reloj = reloj;
        this.creditosRegistro = creditosRegistro;
        this.expiracionMinutos = Math.multiplyExact(expiracionHoras, 60);
    }

    @Transactional
    public RespuestaAutenticacion registrar(SolicitudRegistro solicitud) {
        

        // Normalizar el correo electrónico
        String email = solicitud.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Verificar que el correo no esté registrado
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException(
                    "El correo ya está registrado"
            );

            
            
        }

        // Crear el salón
        Salon salon = new Salon();

        salon.setNombre(solicitud.salonNombre());
        salon.setSlug(generadorSlug.generar(solicitud.salonNombre()));
        salon.setCiudad(solicitud.ciudad());
        salon.setDireccion(solicitud.direccion());
        salon.setTelefono(solicitud.telefono());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(creditosRegistro);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(LocalDateTime.now(reloj));

        salon = salonRepository.save(salon);

        // Crear al propietario del salón
        Usuario usuario = new Usuario();

        usuario.setNombre(solicitud.nombre());
        usuario.setEmail(email);
        usuario.setPassword(
                passwordEncoder.encode(solicitud.password())
        );
        usuario.setRol(Rol.DUENO);
        usuario.setActivo(true);
        usuario.setSalon(salon);

        usuario = usuarioRepository.save(usuario);

        // Generar el token JWT
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        String token = servicioJwt.generarToken(principal);

        // Devolver los datos de autenticación
        return new RespuestaAutenticacion(
                token,
                "Bearer",
                expiracionMinutos,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol(),
                salon.getId(),
                salon.getSlug()
        );
    }


@Transactional(readOnly = true)
public RespuestaAutenticacion login(SolicitudLogin solicitud) {

    // Normalizar el correo
    String email = solicitud.email()
            .trim()
            .toLowerCase(Locale.ROOT);

    // Buscar el usuario
    Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new CredencialesInvalidasException(
                    "Correo o contraseña incorrectos"
            ));

    // Verificar la contraseña y el estado de la cuenta
    if (!passwordEncoder.matches(
            solicitud.password(),
            usuario.getPassword()
    ) || !usuario.isActivo()) {

        throw new CredencialesInvalidasException(
                "Correo o contraseña incorrectos"
        );
    }

    // Generar el token JWT
    UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
    String token = servicioJwt.generarToken(principal);

    // Obtener el salón del usuario
    Salon salon = usuario.getSalon();

    // Devolver los datos de autenticación
    return new RespuestaAutenticacion(
            token,
            "Bearer",
            expiracionMinutos,
            usuario.getId(),
            usuario.getNombre(),
            usuario.getRol(),
            salon.getId(),
            salon.getSlug()
    );
}

}
