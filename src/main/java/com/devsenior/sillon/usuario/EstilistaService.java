package com.devsenior.sillon.usuario;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.auth.AuthService;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PagoRequeridoException;
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
    private final int limiteGratis;

    public EstilistaService(UsuarioRepository usuarioRepository, SalonRepository salonRepository,
            ServicioRepository servicioRepository, PasswordEncoder passwordEncoder,
            @Value("${sillon.plan-gratis.limite-estilistas:2}") int limiteGratis) {
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.passwordEncoder = passwordEncoder;
        this.limiteGratis = limiteGratis;
    }

    /**
     * Orden de validación (ver SPEC): 1) límite del plan (402), 2) correo único (409),
     * 3) todos los servicios son del salón (404).
     */
    @Transactional
    public EstilistaRespuesta crear(Long salonId, EstilistaPeticion peticion) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("El salón no existe"));

        if (salon.getPlan() == Plan.GRATIS
                && usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA) >= limiteGratis) {
            throw new PagoRequeridoException(
                    "El plan GRATIS permite máximo " + limiteGratis + " estilistas. Pasa al plan PRO para agregar más");
        }

        String email = AuthService.normalizarEmail(peticion.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo");
        }

        Set<Long> idsPedidos = new LinkedHashSet<>(peticion.servicioIds());
        List<Servicio> servicios = servicioRepository.findAllByIdInAndSalonId(idsPedidos, salonId);
        if (servicios.size() != idsPedidos.size()) {
            throw new NoEncontradoException("Uno o más servicios no existen en tu salón");
        }

        Usuario estilista = new Usuario();
        estilista.setNombre(peticion.nombre().trim());
        estilista.setEmail(email);
        estilista.setPassword(passwordEncoder.encode(peticion.password()));
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        estilista.setSalon(salon);
        estilista.setServicios(new LinkedHashSet<>(servicios));
        return aRespuesta(usuarioRepository.save(estilista));
    }

    @Transactional(readOnly = true)
    public List<EstilistaRespuesta> listar(Long salonId) {
        return usuarioRepository.findAllBySalonIdAndRolOrderByIdAsc(salonId, Rol.ESTILISTA).stream()
                .map(EstilistaService::aRespuesta)
                .toList();
    }

    private static EstilistaRespuesta aRespuesta(Usuario u) {
        List<String> nombres = u.getServicios().stream().map(Servicio::getNombre).toList();
        return new EstilistaRespuesta(u.getId(), u.getNombre(), u.getEmail(), u.isActivo(), nombres);
    }
}
