package com.devsenior.sillon.usuario.service;

import com.devsenior.sillon.auth.service.AuthService;
import com.devsenior.sillon.compartido.config.NegocioProperties;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.service.SalonService;
import com.devsenior.sillon.servicio.model.Servicio;
import com.devsenior.sillon.servicio.repository.ServicioRepository;
import com.devsenior.sillon.usuario.dto.EstilistaRequest;
import com.devsenior.sillon.usuario.dto.EstilistaResponse;
import com.devsenior.sillon.usuario.mapper.EstilistaMapper;
import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import com.devsenior.sillon.usuario.repository.UsuarioRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstilistaService {

    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;
    private final SalonService salonService;
    private final EstilistaMapper estilistaMapper;
    private final PasswordEncoder passwordEncoder;
    private final NegocioProperties negocio;

    public EstilistaService(UsuarioRepository usuarioRepository, ServicioRepository servicioRepository,
                            SalonService salonService, EstilistaMapper estilistaMapper,
                            PasswordEncoder passwordEncoder, NegocioProperties negocio) {
        this.usuarioRepository = usuarioRepository;
        this.servicioRepository = servicioRepository;
        this.salonService = salonService;
        this.estilistaMapper = estilistaMapper;
        this.passwordEncoder = passwordEncoder;
        this.negocio = negocio;
    }

    /**
     * Orden de validación (SPEC R10):
     * 1. Límite del plan (402): no depende de los datos enviados; si el salón no puede crear más, lo demás sobra.
     * 2. Correo único en toda la plataforma (409).
     * 3. Todos los servicios son del salón (404).
     */
    @Transactional
    public EstilistaResponse crear(Long salonId, EstilistaRequest request) {
        Salon salon = salonService.obtenerEntidad(salonId);

        long estilistasActuales = usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA);
        if (salon.getPlan() == Plan.GRATIS && estilistasActuales >= negocio.limiteEstilistasGratis()) {
            throw new PagoRequeridoException("El plan GRATIS permite máximo "
                    + negocio.limiteEstilistasGratis() + " estilistas. Active el plan PRO para agregar más");
        }

        String email = AuthService.normalizarCorreo(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo");
        }

        Set<Long> idsPedidos = new HashSet<>(request.servicioIds());
        List<Servicio> servicios = servicioRepository.findByIdInAndSalonId(idsPedidos, salonId);
        if (servicios.size() != idsPedidos.size()) {
            throw new RecursoNoEncontradoException("Uno o más servicios no existen en este salón");
        }

        Usuario estilista = new Usuario();
        estilista.setNombre(request.nombre().trim());
        estilista.setEmail(email);
        estilista.setPassword(passwordEncoder.encode(request.password()));
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        estilista.setSalon(salon);
        estilista.setServicios(new HashSet<>(servicios));
        return estilistaMapper.toResponse(usuarioRepository.save(estilista));
    }

    @Transactional(readOnly = true)
    public List<EstilistaResponse> listar(Long salonId) {
        return usuarioRepository.findBySalonIdAndRolOrderByNombreAsc(salonId, Rol.ESTILISTA).stream()
                .map(estilistaMapper::toResponse)
                .toList();
    }

    /** El estilista debe existir en el salón y estar activo (404 si no). */
    @Transactional(readOnly = true)
    public Usuario obtenerActivo(Long salonId, Long estilistaId) {
        return usuarioRepository.findByIdAndSalonIdAndRol(estilistaId, salonId, Rol.ESTILISTA)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el estilista con id " + estilistaId));
    }
}
