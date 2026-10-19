package com.devsenior.sillon.usuario;

import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstilistaService {
    private final UsuarioRepository usuarios;
    private final SalonRepository salones;
    private final ServicioRepository servicios;
    private final PasswordEncoder passwords;
    private final EstilistaMapper mapper;
    private final int limiteGratis;

    public EstilistaService(UsuarioRepository usuarios, SalonRepository salones, ServicioRepository servicios,
            PasswordEncoder passwords, EstilistaMapper mapper,
            @Value("${sillon.limite-estilistas-gratis}") int limiteGratis) {
        this.usuarios = usuarios;
        this.salones = salones;
        this.servicios = servicios;
        this.passwords = passwords;
        this.mapper = mapper;
        this.limiteGratis = limiteGratis;
    }

    @Transactional
    public EstilistaResponse crear(Long salonId, EstilistaRequest request) {
        Salon salon = salones.findByIdParaActualizar(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        if (salon.getPlan() == Plan.GRATIS && usuarios.countBySalonIdAndRol(salonId, Rol.ESTILISTA) >= limiteGratis) {
            throw new PagoRequeridoException("El plan GRATIS permite como máximo " + limiteGratis + " estilistas");
        }
        if (usuarios.existsByEmail(request.email())) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        Set<Long> ids = new LinkedHashSet<>(request.servicioIds());
        List<Servicio> seleccionados = servicios.findAllByIdInAndSalonId(ids, salonId);
        if (seleccionados.size() != ids.size()) {
            throw new RecursoNoEncontradoException("Servicio no encontrado");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPassword(passwords.encode(request.password()));
        usuario.setRol(Rol.ESTILISTA);
        usuario.setActivo(true);
        usuario.setSalon(salon);
        usuario.setServicios(new LinkedHashSet<>(seleccionados));
        return mapper.convertir(usuarios.saveAndFlush(usuario));
    }

    @Transactional(readOnly = true)
    public List<EstilistaResponse> listar(Long salonId) {
        return usuarios.findAllBySalonIdAndRol(salonId, Rol.ESTILISTA).stream().map(mapper::convertir).toList();
    }
}
