package com.devsenior.sillon.usuario.servicio;

import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepository;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.servicio.repositorio.ServicioRepository;
import com.devsenior.sillon.usuario.dto.EstilistaRequest;
import com.devsenior.sillon.usuario.dto.EstilistaResponse;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.devsenior.sillon.usuario.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EstilistaService {

    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final int limiteEstilistasGratis;

    public EstilistaService(
            UsuarioRepository usuarioRepository,
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${sillon.negocio.limite-estilistas-gratis}") int limiteEstilistasGratis
    ) {
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.limiteEstilistasGratis = limiteEstilistasGratis;
    }

    @Transactional
    public EstilistaResponse crear(Long salonId, EstilistaRequest request) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        // 1. Límite del plan (402)
        if (salon.getPlan() == Plan.GRATIS) {
            long totalEstilistas = usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA);
            if (totalEstilistas >= limiteEstilistasGratis) {
                throw new PagoRequeridoException("El plan GRATIS permite máximo " + limiteEstilistasGratis + " estilistas");
            }
        }

        // 2. Correo único (409)
        String emailNormalizado = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }

        // 3. Servicios pertenecen al salón (404)
        Set<Long> idsUnicos = new HashSet<>(request.servicioIds());
        List<Servicio> servicios = servicioRepository.findAllByIdInAndSalonId(idsUnicos, salonId);
        if (servicios.size() != idsUnicos.size()) {
            throw new RecursoNoEncontradoException("Uno o más servicios no pertenecen a este salón");
        }

        Usuario estilista = new Usuario();
        estilista.setNombre(request.nombre().trim());
        estilista.setEmail(emailNormalizado);
        estilista.setPassword(passwordEncoder.encode(request.password()));
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        estilista.setFechaCreacion(LocalDateTime.now(clock));
        estilista.setSalon(salon);
        estilista.setServicios(new HashSet<>(servicios));

        return EstilistaResponse.desde(usuarioRepository.save(estilista));
    }

    @Transactional(readOnly = true)
    public List<EstilistaResponse> listar(Long salonId) {
        return usuarioRepository.findBySalonIdAndRol(salonId, Rol.ESTILISTA).stream()
                .map(EstilistaResponse::desde)
                .toList();
    }
}

