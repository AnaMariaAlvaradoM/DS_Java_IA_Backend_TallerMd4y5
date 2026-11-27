package com.devsenior.sillon.salon.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.salon.dto.ConfiguracionSalonDto;
import com.devsenior.sillon.salon.dto.RespuestaSalon;
import com.devsenior.sillon.salon.mapper.SalonMapper;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;

@Service
public class SalonServicio {

    private final SalonRepositorio salonRepositorio;
    private final SalonMapper salonMapper;

    public SalonServicio(
            SalonRepositorio salonRepositorio,
            SalonMapper salonMapper) {

        this.salonRepositorio = salonRepositorio;
        this.salonMapper = salonMapper;
    }

    @Transactional(readOnly = true)
    public RespuestaSalon consultar(Long salonId) {

        Salon salon = buscarSalon(salonId);

        return salonMapper.aRespuesta(salon);
    }

    @Transactional
    public RespuestaSalon actualizarConfiguracion(
            Long salonId,
            ConfiguracionSalonDto solicitud) {

        if (!solicitud.horaApertura()
                .isBefore(solicitud.horaCierre())) {

            throw new ExcepcionConflicto(
                    "La hora de apertura debe ser anterior a la hora de cierre.");
        }

        Salon salon = buscarSalon(salonId);

        salon.setConfiguracion(salonMapper.aEntidad(solicitud));

        Salon actualizado = salonRepositorio.save(salon);

        return salonMapper.aRespuesta(actualizado);
    }

    private Salon buscarSalon(Long salonId) {

        return salonRepositorio.findById(salonId)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Salón no encontrado."));
    }

}