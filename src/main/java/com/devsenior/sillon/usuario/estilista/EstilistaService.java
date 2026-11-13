package com.devsenior.sillon.usuario.estilista;

import com.devsenior.sillon.compartido.LimitePlanExcedidoException;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.estilista.dto.EstilistaRequest;
import com.devsenior.sillon.usuario.estilista.dto.EstilistaResponse;
import com.devsenior.sillon.usuario.estilista.mapper.EstilistaMapper;
import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import com.devsenior.sillon.usuario.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EstilistaService {
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final PasswordEncoder PasswordEncoder;

    public EstilistaService(ServicioRepository servicioRepository, UsuarioRepository usuarioRepository, SalonRepository salonRepository, PasswordEncoder passwordEncoder) {
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        PasswordEncoder = passwordEncoder;
    }

    @Transactional
    public EstilistaResponse crearEstilista(EstilistaRequest request, Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        System.out.println("Plan del salón: " + salon.getPlan());
        long estilistasActuales = usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA);
        System.out.println("Estilistas actuales: " + estilistasActuales + ", Plan del salón: " + salon.getPlan());
        // Si el plan es GRATIS y ya tiene 2 o más estilistas, rechaza con 402
        if (Plan.GRATIS.equals(salon.getPlan()) && estilistasActuales >= 2) {
            throw new LimitePlanExcedidoException("Has alcanzado el límite de estilistas permitidos para el plan GRATIS");
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ReglaNegocioException("El correo electrónico ya se encuentra registrado");
        }
        Set<Long> idsSet = new HashSet<>(request.serviciosId());
        List<Servicio> serviciosValidos = servicioRepository.findAllByIdInAndSalonId(idsSet, salonId);
        if (serviciosValidos.size() != idsSet.size()) {
            throw new NoEncontradoException("Uno o más servicios no existen o no pertenecen al salón");
        }

        Set<Servicio> servicios = new HashSet<>(serviciosValidos);

        List<String> nombresServicios = serviciosValidos.stream()
                .map(Servicio::getNombre)
                .toList();
        Usuario estilista = new Usuario();
        estilista.setNombre(request.nombre());
        estilista.setEmail(request.email());
       estilista.setContrasena(PasswordEncoder.encode(request.password()));
       estilista.setServicios(servicios);
       estilista.setRol(Rol.ESTILISTA);
       estilista.setSalon(salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado con ID: " + salonId)));
        usuarioRepository.save(estilista);
        return EstilistaMapper.estilistaToEstilistaResponse(estilista);
    }
    public List<EstilistaResponse> obtenerEstilistasPorSalon(Long salonId) {
        List<Usuario> estilistas = usuarioRepository.findBySalonIdAndRol(salonId, Rol.ESTILISTA);
        return estilistas.stream()
                .map(EstilistaMapper::estilistaToEstilistaResponse)
                .collect(Collectors.toList());
    }
}
