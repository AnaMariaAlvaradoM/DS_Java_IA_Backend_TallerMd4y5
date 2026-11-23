
package com.devsenior.sillon.servicio;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final SalonRepository salonRepository;
    private final ServicioMapper servicioMapper;

    public ServicioService(
            ServicioRepository servicioRepository,
            SalonRepository salonRepository,
            ServicioMapper servicioMapper) {

        this.servicioRepository = servicioRepository;
        this.salonRepository = salonRepository;
        this.servicioMapper = servicioMapper;
    }

    @Transactional
    public ServicioResponse crear(
            Long salonId,
            ServicioRequest request) {

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Salón no encontrado"
                ));

        Servicio servicio = servicioMapper.aEntidad(request, salon);

        Servicio guardado = servicioRepository.save(servicio);

        return servicioMapper.aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listar(Long salonId) {

        return servicioRepository.findBySalonId(salonId)
                .stream()
                .map(servicioMapper::aRespuesta)
                .toList();
    }

    @Transactional
    public ServicioResponse actualizar(
            Long salonId,
            Long servicioId,
            ServicioRequest request) {

        Servicio servicio = servicioRepository
                .findByIdAndSalonId(servicioId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Servicio no encontrado"
                ));

        servicioMapper.actualizarEntidad(request, servicio);

        Servicio actualizado = servicioRepository.save(servicio);

        return servicioMapper.aRespuesta(actualizado);
    }


        @Transactional(readOnly = true)
        public String obtenerCatalogoParaIA(Long salonId) {

                if (!salonRepository.existsById(salonId)) {
                        throw new RecursoNoEncontradoException(
                        "Salón no encontrado"
                        );
                }

                List<Servicio> servicios =
                        servicioRepository
                                .findBySalonIdAndActivoTrueOrderByNombreAsc(
                                        salonId
                        );

                if (servicios.isEmpty()) {
                        return "El salón no tiene servicios activos disponibles.";
                }

                return servicios.stream()
                        .map(servicio -> {

                                String precio = String.format(
                                        Locale.GERMANY,
                                        "%,d",
                                        servicio.getPrecio()
                                );

                                return "%s (%d minutos) - Precio: $%s"
                                        .formatted(
                                                servicio.getNombre(),
                                                servicio.getDuracionMinutos(),
                                                precio
                                        );
                        })
                        .collect(
                                java.util.stream.Collectors.joining("\n")
                        );
        }


        @Transactional(readOnly = true)
        public String obtenerHorarioParaIA(Long salonId) {

                Salon salon = salonRepository.findById(salonId)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                "Salón no encontrado"
                                )
                        );

                var configuracion = salon.getConfiguracion();

                if (configuracion == null
                        || configuracion.getHoraApertura() == null
                        || configuracion.getHoraCierre() == null) {

                   throw new IllegalStateException(
                                "El salón no tiene un horario configurado"
                        );
                }

        String dias = configuracion.isAbreDomingos()
                ? "lunes a domingo"
                : "lunes a sábado";

        String domingos = configuracion.isAbreDomingos()
                ? "Abierto"
                : "Cerrado";

        return """
                Días de atención: %s
                Hora de apertura: %s
                Hora de cierre: %s
                Domingos: %s
                Intervalo de reservas: %d minutos
                """.formatted(
                dias,
                configuracion.getHoraApertura(),
                configuracion.getHoraCierre(),
                domingos,
                configuracion.getIntervaloMinutos()
        ).trim();
        }

}
