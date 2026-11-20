package com.devsenior.sillon.publico;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioMapper;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.servicio.ServicioRespuesta;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoService {
    private final SalonRepository salones;
    private final ServicioRepository servicios;
    private final UsuarioRepository usuarios;
    private final ServicioMapper servicioMapper;

    public CatalogoService(SalonRepository salones, ServicioRepository servicios,
                           UsuarioRepository usuarios, ServicioMapper servicioMapper) {
        this.salones = salones;
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.servicioMapper = servicioMapper;
    }

    @Transactional(readOnly = true)
    public CatalogoResponse obtener(String slug) {
        Salon salon = salones.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));

        List<Servicio> activos = servicios.findAllBySalonIdAndActivoTrueOrderByNombreAsc(salon.getId());
        Set<Long> idsPublicos = activos.stream().map(Servicio::getId).collect(Collectors.toSet());
        List<ServicioRespuesta> catalogo = activos.stream()
                .map(servicioMapper::aRespuesta).toList();
        List<EstilistaPublicoResponse> estilistas = usuarios
                .findBySalonIdAndRolOrderByNombreAsc(salon.getId(), Rol.ESTILISTA).stream()
                .filter(Usuario::isActivo)
                .map(usuario -> new EstilistaPublicoResponse(usuario.getId(), usuario.getNombre(),
                        usuario.getServicios().stream().map(Servicio::getId)
                                .filter(idsPublicos::contains).sorted().toList()))
                .toList();

        return new CatalogoResponse(salon.getNombre(), salon.getSlug(), salon.getCiudad(),
                salon.getDireccion(), salon.getTelefono(), horario(salon.getConfiguracion()),
                catalogo, estilistas);
    }

    private String horario(ConfiguracionSalon configuracion) {
        String dias = configuracion.isAbreDomingos()
                ? "Lunes a domingo"
                : "Lunes a sábado (domingos cerrado)";
        return dias + ", de " + hora(configuracion.getHoraApertura()) + " a "
                + hora(configuracion.getHoraCierre());
    }

    private String hora(LocalTime hora) {
        int horaDoce = hora.getHour() % 12;
        if (horaDoce == 0) {
            horaDoce = 12;
        }
        String sufijo = hora.getHour() < 12 ? "a. m." : "p. m.";
        return String.format(Locale.ROOT, "%d:%02d %s", horaDoce, hora.getMinute(), sufijo);
    }
}
