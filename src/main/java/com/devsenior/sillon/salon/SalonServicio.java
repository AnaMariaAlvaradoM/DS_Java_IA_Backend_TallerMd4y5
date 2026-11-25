package com.devsenior.sillon.salon;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;

/**
 * Todo metodo recibe el salonId del usuario autenticado (nunca uno que llegue en la URL o el cuerpo),
 * asi un dueno solo puede ver y editar su propio salon.
 */
@Service
public class SalonServicio {

    private final SalonRepository salonRepository;
    private final SalonMapper mapper;

    public SalonServicio(SalonRepository salonRepository, SalonMapper mapper) {
        this.salonRepository = salonRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public SalonRespuesta obtener(Long salonId) {
        return mapper.aRespuesta(buscar(salonId));
    }

    @Transactional
    public SalonRespuesta actualizarConfiguracion(Long salonId, ConfiguracionPeticion peticion) {
        // 409: el formato es valido (paso la validacion), pero choca con una regla de negocio.
        if (!peticion.horaApertura().isBefore(peticion.horaCierre())) {
            throw new ReglaNegocioException("La hora de apertura debe ser anterior a la hora de cierre.");
        }
        Salon salon = buscar(salonId);
        ConfiguracionSalon configuracion = salon.getConfiguracion();
        configuracion.setHoraApertura(peticion.horaApertura());
        configuracion.setHoraCierre(peticion.horaCierre());
        configuracion.setAbreDomingos(peticion.abreDomingos());
        configuracion.setIntervaloMinutos(peticion.intervaloMinutos());
        // Sin save(): la entidad esta en la transaccion y Hibernate guarda el cambio al terminar el metodo.
        return mapper.aRespuesta(salon);
    }

    private Salon buscar(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El salón no existe."));
    }
}
