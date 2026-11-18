package com.devsenior.sillon.salon;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
import com.devsenior.sillon.salon.dto.ConfiguracionRequest;
import com.devsenior.sillon.salon.dto.ConfiguracionResponse;
import com.devsenior.sillon.salon.dto.SalonResponse;

/**
 * Mi salón (paso 2.5): siempre se trabaja con el salonId del usuario
 * autenticado, nunca con un id que llegue en la URL o en el cuerpo.
 */
@Service
public class SalonService {

    private final SalonRepository salonRepository;

    public SalonService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    @Transactional(readOnly = true)
    public SalonResponse obtenerMiSalon(Long salonId) {
        return aRespuesta(buscarSalon(salonId));
    }

    @Transactional
    public SalonResponse actualizarConfiguracion(Long salonId, ConfiguracionRequest peticion) {
        Salon salon = buscarSalon(salonId);
        if (!peticion.horaApertura().isBefore(peticion.horaCierre())) {
            throw new ReglaDeNegocioException("La hora de apertura debe ser anterior a la hora de cierre");
        }
        ConfiguracionSalon configuracion = new ConfiguracionSalon(
                peticion.horaApertura(),
                peticion.horaCierre(),
                peticion.abreDomingos(),
                peticion.intervaloMinutos());
        salon.setConfiguracion(configuracion);
        salonRepository.save(salon);
        return aRespuesta(salon);
    }

    private Salon buscarSalon(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El salón no existe"));
    }

    private SalonResponse aRespuesta(Salon salon) {
        ConfiguracionSalon configuracion = salon.getConfiguracion();
        return new SalonResponse(
                salon.getId(),
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                salon.getPlan().name(),
                salon.getCreditosIa(),
                new ConfiguracionResponse(
                        configuracion.getHoraApertura(),
                        configuracion.getHoraCierre(),
                        configuracion.isAbreDomingos(),
                        configuracion.getIntervaloMinutos()),
                salon.getFechaCreacion());
    }
}
