package com.devsenior.sillon.salon;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.dto.ActualizarConfiguracionRequest;
import com.devsenior.sillon.salon.dto.ConfiguracionSalonDTO;
import com.devsenior.sillon.salon.dto.SalonRespuesta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalonService {

    private final SalonRepository salonRepository;

    public SalonService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    @Transactional(readOnly = true)
    public SalonRespuesta obtenerMiSalon(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        return mapearARespuesta(salon);
    }

    @Transactional
    public SalonRespuesta actualizarConfiguracion(Long salonId, ActualizarConfiguracionRequest request) {
        if (!request.horaApertura().isBefore(request.horaCierre())) {
            throw new ReglaNegocioException("La hora de apertura debe ser anterior a la de cierre");
        }

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        ConfiguracionSalon conf = new ConfiguracionSalon(
                request.horaApertura(),
                request.horaCierre(),
                request.abreDomingos(),
                request.intervaloMinutos()
        );

        salon.setConfiguracion(conf);
        salon = salonRepository.save(salon);

        return mapearARespuesta(salon);
    }

    public SalonRespuesta mapearARespuesta(Salon salon) {
        ConfiguracionSalonDTO confDto = null;
        if (salon.getConfiguracion() != null) {
            confDto = new ConfiguracionSalonDTO(
                    salon.getConfiguracion().getHoraApertura(),
                    salon.getConfiguracion().getHoraCierre(),
                    salon.getConfiguracion().isAbreDomingos(),
                    salon.getConfiguracion().getIntervaloMinutos()
            );
        }

        return new SalonRespuesta(
                salon.getId(),
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                salon.getPlan(),
                salon.getCreditosIa(),
                confDto,
                salon.getFechaCreacion()
        );
    }
}
