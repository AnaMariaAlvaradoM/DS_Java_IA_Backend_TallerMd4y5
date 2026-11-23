
package com.devsenior.sillon.usuario;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private final EstilistaMapper estilistaMapper;

    public EstilistaService(
            UsuarioRepository usuarioRepository,
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            PasswordEncoder passwordEncoder,
            EstilistaMapper estilistaMapper) {

        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.passwordEncoder = passwordEncoder;
        this.estilistaMapper = estilistaMapper;
    }

    @Transactional
    public EstilistaResponse crear(
            Long salonId,
            EstilistaRequest request) {

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Salón no encontrado"
                ));

        // 1. Validar el límite del plan
        long cantidad = usuarioRepository.countBySalonIdAndRol(
                salonId, Rol.ESTILISTA
        );

        if (salon.getPlan() == Plan.GRATIS && cantidad >= 2) {
            throw new PagoRequeridoException(
                    "El plan GRATIS permite máximo 2 estilistas"
            );
        }

        // 2. Validar correo duplicado
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException(
                    "El correo ya está registrado"
            );
        }

        // 3. Validar servicios asociados
        Set<Servicio> servicios = new HashSet<>();

        for (Long servicioId : request.getServicioIds()) {

            Servicio servicio = servicioRepository
                    .findByIdAndSalonId(servicioId, salonId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Servicio no encontrado"
                    ));

            servicios.add(servicio);
        }

        // Crear el estilista
        Usuario estilista = new Usuario();

        estilista.setNombre(request.getNombre());
        estilista.setEmail(email);
        estilista.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        estilista.setSalon(salon);
        estilista.setServicios(servicios);

        Usuario guardado = usuarioRepository.save(estilista);

        return estilistaMapper.aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public List<EstilistaResponse> listar(Long salonId) {

        return usuarioRepository
                .findBySalonIdAndRol(salonId, Rol.ESTILISTA)
                .stream()
                .map(estilistaMapper::aRespuesta)
                .toList();
    }
}
