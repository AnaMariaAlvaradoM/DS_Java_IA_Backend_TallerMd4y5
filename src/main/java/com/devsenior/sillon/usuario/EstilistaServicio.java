package com.devsenior.sillon.usuario;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;

@Service
public class EstilistaServicio {

    private static final int LIMITE_ESTILISTAS_GRATIS = 2;
    private static final String MENSAJE_CORREO_EXISTENTE = "El correo ya está registrado";
    private static final String MENSAJE_SERVICIO_INEXISTENTE = "Servicio no encontrado";

    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final PasswordEncoder passwordEncoder;

    public EstilistaServicio(UsuarioRepository usuarioRepository, SalonRepository salonRepository,
            ServicioRepository servicioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EstilistaRespuesta crear(Long salonId, EstilistaSolicitud solicitud) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        if (salon.getPlan() == Plan.GRATIS
                && usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA) >= LIMITE_ESTILISTAS_GRATIS) {
            throw new PagoRequeridoException(
                    "Tu plan permite máximo " + LIMITE_ESTILISTAS_GRATIS
                            + " estilistas; contrata el plan PRO para ampliar el equipo");
        }

        List<Servicio> servicios = cargarServiciosDelSalon(salonId, solicitud.servicioIds());

        String email = solicitud.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException(MENSAJE_CORREO_EXISTENTE);
        }

        Usuario estilista = new Usuario();
        estilista.setNombre(solicitud.nombre().trim());
        estilista.setEmail(email);
        estilista.setPassword(passwordEncoder.encode(solicitud.password()));
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        estilista.setSalon(salon);
        estilista.setServicios(new LinkedHashSet<>(servicios));
        estilista = usuarioRepository.save(estilista);
        return aRespuesta(estilista);
    }

    @Transactional(readOnly = true)
    public List<EstilistaRespuesta> listar(Long salonId) {
        return usuarioRepository.findBySalonIdAndRol(salonId, Rol.ESTILISTA).stream()
                .map(EstilistaServicio::aRespuesta)
                .toList();
    }

    private List<Servicio> cargarServiciosDelSalon(Long salonId, List<Long> servicioIds) {
        Set<Long> unicos = new LinkedHashSet<>(servicioIds);
        List<Servicio> servicios = new ArrayList<>();
        for (Long servicioId : unicos) {
            Servicio servicio = servicioRepository.findByIdAndSalonId(servicioId, salonId)
                    .orElseThrow(() -> new NoEncontradoException(MENSAJE_SERVICIO_INEXISTENTE));
            servicios.add(servicio);
        }
        return servicios;
    }

    private static EstilistaRespuesta aRespuesta(Usuario estilista) {
        List<String> nombres = estilista.getServicios().stream()
                .map(Servicio::getNombre)
                .toList();
        return new EstilistaRespuesta(
                estilista.getId(),
                estilista.getNombre(),
                estilista.getEmail(),
                estilista.isActivo(),
                nombres);
    }
}
