package com.devsenior.sillon.salon;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SalonService {
    private final SalonRepository salones;
    private final SalonMapper mapper;

    public SalonService(SalonRepository salones, SalonMapper mapper) {
        this.salones = salones;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public SalonResponse consultar(Long salonId) {
        return mapper.convertir(buscar(salonId));
    }

    @Transactional(readOnly = true)
    public List<SalonResponse> listarTodos() {
        return salones.findAll().stream().map(mapper::convertir).toList();
    }

    @Transactional
    public SalonResponse configurar(Long salonId, ConfiguracionSalonRequest request) {
        Salon salon = buscar(salonId);
        if (!request.horaApertura().isBefore(request.horaCierre())) {
            throw new ReglaNegocioException("La hora de apertura debe ser anterior a la hora de cierre");
        }
        ConfiguracionSalon configuracion = salon.getConfiguracion();
        configuracion.setHoraApertura(request.horaApertura());
        configuracion.setHoraCierre(request.horaCierre());
        configuracion.setAbreDomingos(request.abreDomingos());
        configuracion.setIntervaloMinutos(request.intervaloMinutos());
        salones.saveAndFlush(salon);
        return mapper.convertir(salon);
    }

    private Salon buscar(Long salonId) {
        return salones.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
    }
}
