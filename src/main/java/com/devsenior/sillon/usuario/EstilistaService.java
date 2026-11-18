package com.devsenior.sillon.usuario;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.dto.EstilistaRequest;
import com.devsenior.sillon.usuario.dto.EstilistaResponse;

@Service
public class EstilistaService {

    private static final int MAX_ESTILISTAS_GRATIS = 2;
    private static final String CORREO_DUPLICADO = "El correo ya está registrado en la plataforma";

    private final UsuarioRepository usuarios;
    private final SalonRepository salones;
    private final ServicioRepository servicios;
    private final PasswordEncoder passwordEncoder;

    public EstilistaService(UsuarioRepository usuarios, SalonRepository salones, ServicioRepository servicios,
            PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.salones = salones;
        this.servicios = servicios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EstilistaResponse crear(Long salonId, EstilistaRequest peticion) {
        // El bloqueo por salón evita que dos altas concurrentes excedan el cupo.
        Salon salon = salones.findOneById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        if (salon.getPlan() == Plan.GRATIS
                && usuarios.countBySalonIdAndRol(salonId, Rol.ESTILISTA) >= MAX_ESTILISTAS_GRATIS) {
            throw new PagoRequeridoException("El plan GRATIS admite máximo dos estilistas");
        }

        String email = peticion.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmail(email)) {
            throw new ReglaDeNegocioException(CORREO_DUPLICADO);
        }

        Set<Long> ids = new LinkedHashSet<>(peticion.servicioIds());
        List<Servicio> serviciosDelSalon = servicios.findAllBySalonIdAndIdIn(salonId, ids);
        if (serviciosDelSalon.size() != ids.size()) {
            throw new RecursoNoEncontradoException("Servicio no encontrado");
        }

        Usuario estilista = new Usuario();
        estilista.setNombre(peticion.nombre());
        estilista.setEmail(email);
        estilista.setPassword(passwordEncoder.encode(peticion.password()));
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        estilista.setSalon(salon);
        estilista.setServicios(new LinkedHashSet<>(serviciosDelSalon));
        try {
            // Con IDENTITY, save inserta el usuario aquí; su único campo único aportado es el correo.
            return aRespuesta(usuarios.save(estilista));
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof ConstraintViolationException violacion
                    && violacion.getKind() == ConstraintKind.UNIQUE) {
                throw new ReglaDeNegocioException(CORREO_DUPLICADO);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<EstilistaResponse> listar(Long salonId) {
        return usuarios.findAllBySalonIdAndRol(salonId, Rol.ESTILISTA).stream()
                .map(EstilistaService::aRespuesta)
                .toList();
    }

    private static EstilistaResponse aRespuesta(Usuario estilista) {
        return new EstilistaResponse(estilista.getId(), estilista.getNombre(), estilista.getEmail(),
                estilista.isActivo(), estilista.getServicios().stream().map(Servicio::getNombre).toList());
    }
}
