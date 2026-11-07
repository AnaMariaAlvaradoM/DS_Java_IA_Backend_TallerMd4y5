
package com.devsenior.sillon.salon;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;

@Service
public class ServicioSalon {

    private final SalonRepository salonRepository;

    public ServicioSalon(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    @Transactional(readOnly = true)
    public RespuestaSalon consultar(Long salonId) {

        // Buscar únicamente el salón del usuario autenticado
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Salón no encontrado"
                ));

        // Convertir la entidad en la respuesta de la API
        return RespuestaSalon.desde(salon);
    }


@Transactional
public RespuestaSalon actualizarConfiguracion(
        Long salonId,
        SolicitudConfiguracionSalon solicitud) {

    // Verificar que la apertura sea anterior al cierre
    if (!solicitud.horaApertura()
            .isBefore(solicitud.horaCierre())) {

        throw new ReglaNegocioException(
                "La hora de apertura debe ser anterior a la hora de cierre"
        );
    }

    // Buscar el salón del usuario autenticado
    Salon salon = salonRepository.findById(salonId)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                    "Salón no encontrado"
            ));

    // Actualizar su configuración
    ConfiguracionSalon configuracion = salon.getConfiguracion();

    configuracion.setHoraApertura(solicitud.horaApertura());
    configuracion.setHoraCierre(solicitud.horaCierre());
    configuracion.setAbreDomingos(solicitud.abreDomingos());
    configuracion.setIntervaloMinutos(solicitud.intervaloMinutos());

    return RespuestaSalon.desde(salon);
}

}
