package com.devsenior.sillon.usuario;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepositorio;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepositorio;
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
public class EstilistaServicio {
    private final UsuarioRepositorio usuarios;
    private final SalonRepositorio salones;
    private final ServicioRepositorio servicios;
    private final PasswordEncoder encoder;
    private final int limiteGratis;

    public EstilistaServicio(UsuarioRepositorio usuarios, SalonRepositorio salones,
            ServicioRepositorio servicios, PasswordEncoder encoder,
            @Value("${sillon.plan.gratis.max-estilistas}") int limiteGratis) {
        this.usuarios = usuarios;
        this.salones = salones;
        this.servicios = servicios;
        this.encoder = encoder;
        this.limiteGratis = limiteGratis;
    }

    @Transactional
    public EstilistaRespuesta crear(Long salonId, EstilistaSolicitud solicitud) {
        Salon salon = salones.buscarParaActualizar(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        if (salon.getPlan() == Plan.GRATIS
                && usuarios.countBySalon_IdAndRol(salonId, Rol.ESTILISTA) >= limiteGratis) {
            throw new PagoRequeridoException("El plan GRATIS permite máximo dos estilistas");
        }
        String email = solicitud.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmail(email)) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        Set<Servicio> asignados = new HashSet<>();
        for (Long servicioId : solicitud.servicioIds()) {
            Servicio servicio = servicios.findByIdAndSalon_Id(servicioId, salonId)
                    .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado"));
            asignados.add(servicio);
        }
        Usuario estilista = new Usuario();
        estilista.setNombre(solicitud.nombre());
        estilista.setEmail(email);
        estilista.setPassword(encoder.encode(solicitud.password()));
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        estilista.setSalon(salon);
        estilista.setServicios(asignados);
        try {
            return EstilistaRespuesta.desde(usuarios.saveAndFlush(estilista));
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
    }

    @Transactional(readOnly = true)
    public List<EstilistaRespuesta> listar(Long salonId) {
        return usuarios.findAllBySalon_IdAndRolOrderByIdAsc(salonId, Rol.ESTILISTA).stream()
                .map(EstilistaRespuesta::desde).toList();
    }
}
