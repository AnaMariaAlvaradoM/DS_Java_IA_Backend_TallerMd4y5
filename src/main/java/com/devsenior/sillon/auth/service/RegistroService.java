package com.devsenior.sillon.auth.service;

import com.devsenior.sillon.auth.dto.RegistroRequest;
import com.devsenior.sillon.auth.dto.RegistroResponse;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.model.ConfiguracionSalon;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.devsenior.sillon.salon.service.GeneradorSlugService;
import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import com.devsenior.sillon.usuario.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class RegistroService {

    private final SalonRepository salonRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeneradorSlugService generadorSlugService;

    public RegistroService(
            SalonRepository salonRepository,
            UsuarioRepository usuarioRepository,
            GeneradorSlugService generadorSlugService
    ) {
        this.salonRepository = salonRepository;
        this.usuarioRepository = usuarioRepository;
        this.generadorSlugService = generadorSlugService;
    }

    @Transactional
    public RegistroResponse registrar(RegistroRequest request) {
        String emailNormalizado = request.email() == null ? null : request.email().trim();

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ReglaNegocioException("El correo ya está registrado en la plataforma");
        }

        String nombreSalon = request.nombreSalon() == null ? "" : request.nombreSalon().trim();
        String slug = generadorSlugService.generarSlugUnico(nombreSalon);

        Salon salon = new Salon();
        salon.setSlug(slug);
        salon.setCiudad("Bogotá");
        salon.setDireccion("Por definir");
        salon.setTelefono("0000000000");
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
        salon.setStripeSuscripcionId("SIN_SUSCRIPCION");
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setNombresalon(nombreSalon);

        Salon salonGuardado = salonRepository.save(salon);

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(emailNormalizado);
        usuario.setContrasena(request.contrasena());
        usuario.setRol(Rol.DUENO);
        usuario.setActivo(true);
        usuario.setSalon(salonGuardado);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        return new RegistroResponse(
                salonGuardado.getId(),
                usuarioGuardado.getId(),
                salonGuardado.getSlug(),
                usuarioGuardado.getEmail(),
                usuarioGuardado.getRol().name(),
                "Registro exitoso"
        );
    }
}
