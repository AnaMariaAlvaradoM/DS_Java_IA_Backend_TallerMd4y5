package com.devsenior.sillon.salon;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;

@Service
public class SalonService {

    private final SalonRepository salonRepository;

    public SalonService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    public SalonRespuesta obtenerPorSalonId(Long salonId) {
        Salon salon = buscar(salonId);
        return SalonMapper.aRespuesta(salon);
    }

    @Transactional
    public SalonRespuesta actualizarConfiguracion(Long salonId, ConfiguracionSalonRequest request) {
        Salon salon = buscar(salonId);

        if (!request.horaApertura().isBefore(request.horaCierre())) {
            throw new ReglaNegocioException("La hora de apertura debe ser anterior a la hora de cierre");
        }

        ConfiguracionSalon configuracion = new ConfiguracionSalon(request.horaApertura(), request.horaCierre(),
                request.abreDomingos(), request.intervaloMinutos());
        salon.setConfiguracion(configuracion);
        Salon actualizado = salonRepository.save(salon);

        return SalonMapper.aRespuesta(actualizado);
    }

    private Salon buscar(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salon no encontrado"));
    }
}
