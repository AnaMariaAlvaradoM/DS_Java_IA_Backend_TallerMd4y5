package com.devsenior.sillon.usuario;

import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.dto.CrearEstilistaRequest;
import com.devsenior.sillon.usuario.dto.EstilistaRespuesta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EstilistaService {

    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final PasswordEncoder passwordEncoder;
    private final int limiteEstilistasGratis;

    public EstilistaService(
            UsuarioRepository usuarioRepository,
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${sillon.limite.estilistas-gratis:2}") int limiteEstilistasGratis
    ) {
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.passwordEncoder = passwordEncoder;
        this.limiteEstilistasGratis = limiteEstilistasGratis;
    }

    @Transactional
    public EstilistaRespuesta crearEstilista(Long salonId, CrearEstilistaRequest request) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        // 1. Validar límite del plan GRATIS (HTTP 402)
        if (salon.getPlan() == Plan.GRATIS) {
            long totalEstilistas = usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA);
            if (totalEstilistas >= limiteEstilistasGratis) {
                throw new PagoRequeridoException("El plan GRATIS permite un máximo de " + limiteEstilistasGratis + " estilistas. Actualice al plan PRO.");
            }
        }

        // 2. Validar correo único (HTTP 409)
        String emailNormalizado = request.email().toLowerCase().trim();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ReglaNegocioException("El correo ya está registrado en la plataforma");
        }

        // 3. Validar que los servicios existan y pertenezcan al salón (HTTP 404)
        List<Servicio> serviciosAsignados = new ArrayList<>();
        for (Long servicioId : request.servicioIds()) {
            Servicio servicio = servicioRepository.findByIdAndSalonId(servicioId, salonId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Servicio con ID " + servicioId + " no encontrado en este salón"));
            serviciosAsignados.add(servicio);
        }

        Usuario estilista = new Usuario();
        estilista.setNombre(request.nombre().trim());
        estilista.setEmail(emailNormalizado);
        estilista.setPassword(passwordEncoder.encode(request.password()));
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        estilista.setSalon(salon);
        estilista.setServicios(serviciosAsignados);

        estilista = usuarioRepository.save(estilista);

        return mapearARespuesta(estilista);
    }

    @Transactional(readOnly = true)
    public List<EstilistaRespuesta> listarEstilistas(Long salonId) {
        return usuarioRepository.findBySalonIdAndRol(salonId, Rol.ESTILISTA).stream()
                .map(this::mapearARespuesta)
                .toList();
    }

    public EstilistaRespuesta mapearARespuesta(Usuario usuario) {
        List<String> nombresServicios = usuario.getServicios().stream()
                .map(Servicio::getNombre)
                .toList();
        List<Long> idsServicios = usuario.getServicios().stream()
                .map(Servicio::getId)
                .toList();

        return new EstilistaRespuesta(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.isActivo(),
                nombresServicios,
                idsServicios
        );
    }
}
