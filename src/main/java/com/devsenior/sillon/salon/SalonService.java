package com.devsenior.sillon.salon;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;

@Service
public class SalonService {

    private final SalonRepository salonRepository;

    public SalonService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    @Transactional(readOnly = true)
    public SalonRespuesta obtener(Long salonId) {
        return SalonMapper.aRespuesta(buscar(salonId));
    }

    @Transactional
    public SalonRespuesta configurar(Long salonId, ConfiguracionPeticion peticion) {
        if (!peticion.horaApertura().isBefore(peticion.horaCierre())) {
            throw new ReglaNegocioException("La hora de apertura debe ser anterior a la hora de cierre");
        }
        Salon salon = buscar(salonId);
        salon.setConfiguracion(new ConfiguracionSalon(peticion.horaApertura(), peticion.horaCierre(),
                peticion.abreDomingos(), peticion.intervaloMinutos()));
        return SalonMapper.aRespuesta(salonRepository.save(salon));
    }

    @Transactional(readOnly = true)
    public List<SalonRespuesta> listarTodos() {
        return salonRepository.findAll().stream().map(SalonMapper::aRespuesta).toList();
    }

    private Salon buscar(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("El salón no existe"));
    }
}
