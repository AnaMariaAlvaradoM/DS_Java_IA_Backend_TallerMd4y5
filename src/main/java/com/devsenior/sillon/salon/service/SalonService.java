package com.devsenior.sillon.salon.service;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.dto.ConfiRequest;
import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.mapper.SalonMapper;
import com.devsenior.sillon.salon.model.ConfiguracionSalon;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class SalonService {
    private final SalonRepository salonRepository;

    public SalonService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }
    public SalonResponse obtenerSalonPorId(Long salonId) {
        if (salonId == null) {
            throw new NoEncontradoException("El usuario autenticado no tiene un salón asociado");
        }

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado con ID: " + salonId));;
        return SalonMapper.toResponse(salon);
    }
    public SalonResponse actualizarSalon(Long id,ConfiRequest request){
        if (!request.horaApertura().isBefore(request.horaCierre())) {
            throw new ReglaNegocioException("La hora de apertura debe ser estrictamente anterior a la hora de cierre");
        }
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado con ID: " + id));
        ConfiguracionSalon configuracion = salon.getConfiguracion();
        configuracion.setHoraApertura(request.horaApertura());
        configuracion.setHoraCierre(request.horaCierre());
        configuracion.setIntervaloMinutos(request.intervaloMinutos());
        configuracion.setAbreDomingos(request.abreDomingo());
        salon.setConfiguracion(configuracion);
        salonRepository.save(salon);
        return SalonMapper.toResponse(salon);
    }

}
