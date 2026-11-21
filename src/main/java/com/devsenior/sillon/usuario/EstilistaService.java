package com.devsenior.sillon.usuario;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstilistaService {
    private final UsuarioRepository usuarios;
    private final SalonRepository salones;
    private final ServicioRepository servicios;
    private final PasswordEncoder encoder;
    private final int limiteGratis;

    public EstilistaService(UsuarioRepository usuarios, SalonRepository salones,
                           ServicioRepository servicios, PasswordEncoder encoder,
                           @Value("${sillon.limite-estilistas-gratis}") int limiteGratis) {
        this.usuarios = usuarios;
        this.salones = salones;
        this.servicios = servicios;
        this.encoder = encoder;
        this.limiteGratis = limiteGratis;
    }

    @Transactional
    public EstilistaResponse crear(Long salonId, EstilistaRequest request) {
        Salon salon = salones.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("El salón no existe"));
        if (salon.getPlan() == Plan.GRATIS && usuarios.countBySalonIdAndRol(salonId, Rol.ESTILISTA) >= limiteGratis) {
            throw new PagoRequeridoException("El plan GRATIS admite como máximo dos estilistas");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmail(email)) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        Set<Long> ids = new HashSet<>(request.servicioIds());
        List<Servicio> ofrecidos = servicios.findAllByIdInAndSalonId(ids, salonId);
        if (ofrecidos.size() != ids.size()) {
            throw new NoEncontradoException("Uno o más servicios no existen en este salón");
        }
        Usuario estilista = new Usuario();
        estilista.setSalon(salon);
        estilista.setNombre(request.nombre().trim());
        estilista.setEmail(email);
        estilista.setPassword(encoder.encode(request.password()));
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        estilista.setServicios(new HashSet<>(ofrecidos));
        try {
            return mapear(usuarios.saveAndFlush(estilista));
        } catch (DataIntegrityViolationException exception) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
    }

    @Transactional(readOnly = true)
    public List<EstilistaResponse> listar(Long salonId) {
        return usuarios.findBySalonIdAndRolOrderByNombreAsc(salonId, Rol.ESTILISTA).stream()
                .map(this::mapear).toList();
    }

    private EstilistaResponse mapear(Usuario estilista) {
        List<String> nombres = estilista.getServicios().stream()
                .map(Servicio::getNombre).sorted().toList();
        return new EstilistaResponse(estilista.getId(), estilista.getNombre(), estilista.getEmail(),
                estilista.isActivo(), nombres);
    }
}
