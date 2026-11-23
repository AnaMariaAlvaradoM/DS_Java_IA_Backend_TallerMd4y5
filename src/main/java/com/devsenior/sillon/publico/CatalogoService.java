
package com.devsenior.sillon.publico;

import org.springframework.stereotype.Service;


import java.util.List;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.UsuarioRepository;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.Rol;
import org.springframework.transaction.annotation.Transactional;


@Service
public class CatalogoService {

    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;

    public CatalogoService(
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
    }


    private Salon buscarSalonPorSlug(String slug) {

        return salonRepository.findBySlug(slug)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                            "Salón no encontrado"
                    )
            );
    }


    private List<ServicioPublicoResponse> obtenerServiciosActivos(
            Salon salon
    ) {
        return servicioRepository.findBySalonId(salon.getId())
                .stream()
                .filter(Servicio::isActivo)
                .map(servicio -> new ServicioPublicoResponse(
                        servicio.getId(),
                        servicio.getNombre(),
                        servicio.getDescripcion(),
                        servicio.getDuracionMinutos(),
                        servicio.getPrecio()
                ))
                .toList();
    }


    private List<EstilistaPublicoResponse> obtenerEstilistasActivos(
            Salon salon
    ) {
        return usuarioRepository
                .findBySalonIdAndRol(salon.getId(), Rol.ESTILISTA)
                .stream()
                .filter(Usuario::isActivo)
                .map(estilista -> new EstilistaPublicoResponse(
                        estilista.getId(),
                        estilista.getNombre(),
                        estilista.getServicios()
                                .stream()
                                .filter(Servicio::isActivo)
                                .map(Servicio::getId)
                                .toList()
                ))
                .toList();
    }


    @Transactional(readOnly = true)
    public CatalogoPublicoResponse obtenerCatalogo(String slug) {

        Salon salon = buscarSalonPorSlug(slug);

        var configuracion = salon.getConfiguracion();

        String dias = configuracion.isAbreDomingos()
                ? "Lunes a domingo"
                : "Lunes a sábado (domingos cerrado)";

        String horario = dias
                + ", de " + configuracion.getHoraApertura()
                + " a " + configuracion.getHoraCierre();

        return new CatalogoPublicoResponse(
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                horario,
                obtenerServiciosActivos(salon),
                obtenerEstilistasActivos(salon)
        );
    }


}
