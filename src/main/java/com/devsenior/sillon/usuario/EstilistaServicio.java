package com.devsenior.sillon.usuario;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.NegocioConfig;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;

@Service
public class EstilistaServicio {

    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final PasswordEncoder passwordEncoder;
    private final NegocioConfig negocioConfig;

    public EstilistaServicio(UsuarioRepository usuarioRepository,
                             SalonRepository salonRepository,
                             ServicioRepository servicioRepository,
                             PasswordEncoder passwordEncoder,
                             NegocioConfig negocioConfig) {
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.passwordEncoder = passwordEncoder;
        this.negocioConfig = negocioConfig;
    }

    /**
     * Orden estricto de validacion exigido por la SPEC:
     * 1. Limite del plan (402): En plan GRATIS, maximo 2 estilistas. Va primero porque es una regla comercial.
     * 2. Correo unico (409): No se puede reutilizar ningun correo existente en la plataforma.
     * 3. Servicios del salon (404): Todos los servicios deben existir y pertenecer al salon.
     */
    @Transactional
    public EstilistaRespuesta crear(Long salonId, EstilistaCreacionPeticion peticion) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        // 1. Limite del plan (402)
        if (salon.getPlan() == Plan.GRATIS) {
            long totalEstilistas = usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA);
            if (totalEstilistas >= negocioConfig.limiteEstilistasGratis()) {
                throw new PagoRequeridoException("El plan GRATIS permite máximo "
                        + negocioConfig.limiteEstilistasGratis() + " estilistas. Actualiza al plan PRO para agregar más.");
            }
        }

        // 2. Correo unico (409)
        String emailNormalizado = NormalizadorCorreo.normalizar(peticion.email());
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ReglaNegocioException("El correo ya está registrado en la plataforma.");
        }

        // 3. Servicios del salon (404)
        Set<Long> idsUnicos = new HashSet<>(peticion.servicioIds());
        List<Servicio> servicios = servicioRepository.findByIdInAndSalonId(idsUnicos, salonId);
        if (servicios.size() != idsUnicos.size()) {
            throw new RecursoNoEncontradoException("Uno o más servicios no existen o pertenecen a otro salón.");
        }

        Usuario estilista = new Usuario();
        estilista.setNombre(peticion.nombre().trim());
        estilista.setEmail(emailNormalizado);
        estilista.setPassword(passwordEncoder.encode(peticion.password()));
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        estilista.setSalon(salon);
        estilista.setServicios(new HashSet<>(servicios));

        Usuario guardado = usuarioRepository.save(estilista);
        return aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public List<EstilistaRespuesta> listar(Long salonId) {
        return usuarioRepository.findBySalonIdAndRol(salonId, Rol.ESTILISTA).stream()
                .map(this::aRespuesta)
                .toList();
    }

    private EstilistaRespuesta aRespuesta(Usuario estilista) {
        List<String> nombresServicios = estilista.getServicios().stream()
                .map(Servicio::getNombre)
                .toList();
        return new EstilistaRespuesta(
                estilista.getId(),
                estilista.getNombre(),
                estilista.getEmail(),
                estilista.isActivo(),
                nombresServicios);
    }
}
