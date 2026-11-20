package com.devsenior.sillon.salon.servicio;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.dto.ConfiguracionSalonRequest;
import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalonService {

    private final SalonRepository salonRepository;

    public SalonService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    @Transactional(readOnly = true)
    public SalonResponse obtenerMiSalon(Long salonId) {
        Salon salon = buscarSalon(salonId);
        return SalonResponse.desde(salon);
    }

    @Transactional
    public SalonResponse actualizarConfiguracion(Long salonId, ConfiguracionSalonRequest request) {
        if (!request.horaApertura().isBefore(request.horaCierre())) {
            throw new ReglaNegocioException("La hora de apertura debe ser anterior a la hora de cierre");
        }

        Salon salon = buscarSalon(salonId);
        ConfiguracionSalon config = salon.getConfiguracion();
        config.setHoraApertura(request.horaApertura());
        config.setHoraCierre(request.horaCierre());
        config.setAbreDomingos(request.abreDomingos());
        config.setIntervaloMinutos(request.intervaloMinutos());

        salon = salonRepository.save(salon);
        return SalonResponse.desde(salon);
    }

    private Salon buscarSalon(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
    }
}