package com.devsenior.sillon.salon;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalonService {
    private final SalonRepository salones;

    public SalonService(SalonRepository salones) {
        this.salones = salones;
    }

    @Transactional(readOnly = true)
    public SalonResponse obtener(Long salonId) {
        return mapear(buscar(salonId));
    }

    @Transactional
    public SalonResponse configurar(Long salonId, ConfiguracionRequest request) {
        if (!request.horaApertura().isBefore(request.horaCierre())) {
            throw new ReglaNegocioException("La hora de apertura debe ser anterior a la de cierre");
        }
        Salon salon = buscar(salonId);
        salon.setConfiguracion(new ConfiguracionSalon(request.horaApertura(), request.horaCierre(),
                request.abreDomingos(), request.intervaloMinutos()));
        return mapear(salones.saveAndFlush(salon));
    }

    private Salon buscar(Long salonId) {
        return salones.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("El salón no existe"));
    }

    private SalonResponse mapear(Salon salon) {
        ConfiguracionSalon c = salon.getConfiguracion();
        return new SalonResponse(salon.getId(), salon.getNombre(), salon.getSlug(), salon.getCiudad(),
                salon.getDireccion(), salon.getTelefono(), salon.getPlan(), salon.getCreditosIa(),
                new SalonResponse.ConfiguracionResponse(c.getHoraApertura(), c.getHoraCierre(),
                        c.isAbreDomingos(), c.getIntervaloMinutos()), salon.getFechaCreacion());
    }
}
