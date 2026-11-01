package com.devsenior.sillon.salon;

import com.devsenior.sillon.compartido.ConflictoExcepcion;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.salon.dto.ActualizarConfiguracionDto;
import com.devsenior.sillon.salon.dto.SalonResponseDto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalonService {

    private final SalonRepository salonRepository;

    public SalonService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    @Transactional(readOnly = true)
    public SalonResponseDto obtenerMiSalon(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salon no encontrado"));

        return mapearADto(salon);
    }

    @Transactional
    public SalonResponseDto actualizarConfiguracion(Long salonId, ActualizarConfiguracionDto request) {
        // Regla de negocio: La hora de apertura no puede ser posterior ni igual a la hora de cierre (409)
        if (!request.horaApertura().isBefore(request.horaCierre())) {
            throw new ConflictoExcepcion("La hora de apertura debe ser anterior a la hora de cierre");
        }

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salon no encontrado"));

        ConfiguracionSalon config = salon.getConfiguracion();
        if (config == null) {
            config = new ConfiguracionSalon();
        }

        config.setHoraApertura(request.horaApertura());
        config.setHoraCierre(request.horaCierre());
        config.setAbreDomingos(request.abreDomingos());
        config.setIntervaloMinutos(request.intervaloMinutos());

        salon.setConfiguracion(config);
        salon = salonRepository.save(salon);

        return mapearADto(salon);
    }

    private SalonResponseDto mapearADto(Salon salon) {
        return new SalonResponseDto(
                salon.getId(),
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                salon.getPlan(),
                salon.getCreditosIa(),
                salon.getConfiguracion(),
                salon.getFechaCreacion()
        );
    }
}