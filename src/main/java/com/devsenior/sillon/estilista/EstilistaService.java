package com.devsenior.sillon.estilista;

import com.devsenior.sillon.compartido.ConflictoExcepcion;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.estilista.dto.EstilistaRequestDto;
import com.devsenior.sillon.estilista.dto.EstilistaResponseDto;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
public class EstilistaService {

    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${sillon.limite-estilistas-gratis:2}")
    private int limiteGratis;

    public EstilistaService(
            UsuarioRepository usuarioRepository,
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EstilistaResponseDto crearEstilista(Long salonId, EstilistaRequestDto request) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salon no encontrado"));

        if (salon.getPlan() == Plan.GRATIS) {
            long actuales = usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA);
            if (actuales >= limiteGratis) {
                throw new PagoRequeridoException("El plan GRATIS solo permite un maximo de " + limiteGratis + " estilistas");
            }
        }

        String emailLimpio = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(emailLimpio)) {
            throw new ConflictoExcepcion("El email ya esta registrado");
        }

        List<Long> idsUnicos = request.servicioIds().stream().distinct().toList();
        List<Servicio> servicios = servicioRepository.findByIdInAndSalonId(idsUnicos, salonId);

        if (servicios.size() != idsUnicos.size()) {
            throw new NoEncontradoException("Uno o mas servicios no pertenecen al salon o no existen");
        }

        Usuario estilista = new Usuario(
                request.nombre().trim(),
                emailLimpio,
                passwordEncoder.encode(request.password()),
                Rol.ESTILISTA,
                salon
        );
        estilista.setServicios(new HashSet<>(servicios));

        estilista = usuarioRepository.save(estilista);
        return mapearADto(estilista);
    }

    @Transactional(readOnly = true)
    public List<EstilistaResponseDto> listarEstilistas(Long salonId) {
        return usuarioRepository.findBySalonIdAndRol(salonId, Rol.ESTILISTA).stream()
                .map(this::mapearADto)
                .toList();
    }

    private EstilistaResponseDto mapearADto(Usuario estilista) {
        List<String> nombresServicios = estilista.getServicios() != null
                ? estilista.getServicios().stream().map(Servicio::getNombre).toList()
                : List.of();

        return new EstilistaResponseDto(
                estilista.getId(),
                estilista.getNombre(),
                estilista.getEmail(),
                estilista.isActivo(),
                nombresServicios
        );
    }
}