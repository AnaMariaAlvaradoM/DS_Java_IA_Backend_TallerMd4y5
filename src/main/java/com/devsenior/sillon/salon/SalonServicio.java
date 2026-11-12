package com.devsenior.sillon.salon;

import java.text.Normalizer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;

@Service
public class SalonServicio {

    private final SalonRepository salonRepository;

    public SalonServicio(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    @Transactional(readOnly = true)
    public SalonRespuesta obtener(Long salonId) {
        return SalonRespuesta.de(cargar(salonId));
    }

    @Transactional
    public SalonRespuesta configurar(Long salonId, ConfiguracionSolicitud solicitud) {
        if (!solicitud.horaApertura().isBefore(solicitud.horaCierre())) {
            throw new ReglaNegocioException(
                    "La hora de apertura debe ser anterior a la hora de cierre");
        }
        Salon salon = cargar(salonId);
        salon.setConfiguracion(new ConfiguracionSalon(
                solicitud.horaApertura(),
                solicitud.horaCierre(),
                solicitud.abreDomingos(),
                solicitud.intervaloMinutos()));
        salon = salonRepository.save(salon);
        return SalonRespuesta.de(salon);
    }

    public String generarSlug(String nombre) {
        String base = slugificar(nombre);
        String candidato = base;
        int sufijo = 2;
        while (salonRepository.existsBySlug(candidato)) {
            candidato = base + "-" + sufijo;
            sufijo++;
        }
        return candidato;
    }

    private Salon cargar(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
    }

    private String slugificar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String minusculas = sinTildes.toLowerCase();
        String conGuiones = minusculas.replaceAll("[^a-z0-9]+", "-");
        return conGuiones.replaceAll("^-+|-+$", "");
    }
}
