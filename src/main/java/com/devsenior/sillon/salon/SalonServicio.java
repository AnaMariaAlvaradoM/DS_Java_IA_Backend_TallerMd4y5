package com.devsenior.sillon.salon;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalonServicio {
    private final SalonRepositorio salones;

    public SalonServicio(SalonRepositorio salones) { this.salones = salones; }

    @Transactional(readOnly = true)
    public SalonRespuesta consultar(Long salonId) { return SalonRespuesta.desde(buscar(salonId)); }

    @Transactional
    public SalonRespuesta configurar(Long salonId, ConfiguracionSolicitud solicitud) {
        if (!solicitud.horaApertura().isBefore(solicitud.horaCierre())) {
            throw new ReglaNegocioException("La hora de apertura debe ser anterior a la hora de cierre");
        }
        Salon salon = buscar(salonId);
        ConfiguracionSalon configuracion = salon.getConfiguracion();
        configuracion.setHoraApertura(solicitud.horaApertura());
        configuracion.setHoraCierre(solicitud.horaCierre());
        configuracion.setAbreDomingos(solicitud.abreDomingos());
        configuracion.setIntervaloMinutos(solicitud.intervaloMinutos());
        return SalonRespuesta.desde(salones.save(salon));
    }

    private Salon buscar(Long id) {
        return salones.findById(id).orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
    }
}
