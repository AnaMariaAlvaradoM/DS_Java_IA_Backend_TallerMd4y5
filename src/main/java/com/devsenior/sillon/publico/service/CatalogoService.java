package com.devsenior.sillon.publico.service;

import com.devsenior.sillon.publico.dto.CatalogoResponse;
import com.devsenior.sillon.publico.dto.EstilistaPublicoResponse;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.service.SalonService;
import com.devsenior.sillon.servicio.dto.ServicioResponse;
import com.devsenior.sillon.servicio.mapper.ServicioMapper;
import com.devsenior.sillon.servicio.model.Servicio;
import com.devsenior.sillon.servicio.repository.ServicioRepository;
import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import com.devsenior.sillon.usuario.repository.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arma, por slug, los datos públicos del salón: ubicación, horario en texto, servicios y estilistas activos. */
@Service
public class CatalogoService {

    private final SalonService salonService;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioMapper servicioMapper;

    public CatalogoService(SalonService salonService, ServicioRepository servicioRepository,
                           UsuarioRepository usuarioRepository, ServicioMapper servicioMapper) {
        this.salonService = salonService;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioMapper = servicioMapper;
    }

    @Transactional(readOnly = true)
    public CatalogoResponse catalogo(String slug) {
        Salon salon = salonService.obtenerPorSlug(slug);

        List<ServicioResponse> servicios = serviciosActivos(salon.getId()).stream()
                .map(servicioMapper::toResponse)
                .toList();

        List<EstilistaPublicoResponse> estilistas = usuarioRepository
                .findBySalonIdAndRolAndActivoTrueOrderByNombreAsc(salon.getId(), Rol.ESTILISTA).stream()
                .map(this::estilistaPublico)
                .toList();

        return new CatalogoResponse(salon.getNombre(), salon.getSlug(), salon.getCiudad(), salon.getDireccion(),
                salon.getTelefono(), salon.getConfiguracion().descripcion(), servicios, estilistas);
    }

    @Transactional(readOnly = true)
    public List<Servicio> serviciosActivos(Long salonId) {
        return servicioRepository.findBySalonIdAndActivoTrueOrderByNombreAsc(salonId);
    }

    private EstilistaPublicoResponse estilistaPublico(Usuario estilista) {
        List<Long> servicioIds = estilista.getServicios().stream()
                .filter(Servicio::isActivo)
                .map(Servicio::getId)
                .sorted()
                .toList();
        return new EstilistaPublicoResponse(estilista.getId(), estilista.getNombre(), servicioIds);
    }
}
