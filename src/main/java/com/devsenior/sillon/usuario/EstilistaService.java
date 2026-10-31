package com.devsenior.sillon.usuario;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;

@Service
public class EstilistaService {

    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final PasswordEncoder passwordEncoder;
    private final int limiteEstilistasGratis;

    public EstilistaService(UsuarioRepository usuarioRepository, SalonRepository salonRepository,
            ServicioRepository servicioRepository, PasswordEncoder passwordEncoder,
            @Value("${sillon.plan.limite-estilistas-gratis}") int limiteEstilistasGratis) {
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.passwordEncoder = passwordEncoder;
        this.limiteEstilistasGratis = limiteEstilistasGratis;
    }

    @Transactional
    public EstilistaRespuesta crear(Long salonId, EstilistaRequest request) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salon no encontrado"));

        if (salon.getPlan() == Plan.GRATIS) {
            long cantidadEstilistas = usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA);
            if (cantidadEstilistas >= limiteEstilistasGratis) {
                throw new PagoRequeridoException("El plan GRATIS alcanzo el limite de estilistas");
            }
        }

        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo");
        }

        Set<Long> idsSolicitados = new HashSet<>(request.servicioIds());
        List<Servicio> servicios = servicioRepository.findByIdInAndSalonId(idsSolicitados, salonId);
        if (servicios.size() != idsSolicitados.size()) {
            throw new RecursoNoEncontradoException("Alguno de los servicios no pertenece al salon");
        }

        Usuario estilista = new Usuario();
        estilista.setNombre(request.nombre());
        estilista.setEmail(request.email());
        estilista.setPassword(passwordEncoder.encode(request.password()));
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        estilista.setSalon(salon);
        estilista.setServicios(new HashSet<>(servicios));

        Usuario guardado = usuarioRepository.save(estilista);
        return EstilistaMapper.aRespuesta(guardado);
    }

    @Transactional
    public List<EstilistaRespuesta> listarPorSalon(Long salonId) {
        return usuarioRepository.findBySalonIdAndRol(salonId, Rol.ESTILISTA).stream()
                .map(EstilistaMapper::aRespuesta)
                .toList();
    }
}
