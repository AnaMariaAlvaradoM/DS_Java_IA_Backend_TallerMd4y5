
package com.devsenior.sillon.publico;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.time.LocalTime;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.UsuarioRepository;
import com.devsenior.sillon.cita.CitaRepository;
import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.Rol;



@Service
public class DisponibilidadPublicaService {

    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final ReglasAgenda reglasAgenda;
    private final Clock reloj;

    public DisponibilidadPublicaService(
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository,
            CitaRepository citaRepository,
            ReglasAgenda reglasAgenda,
            Clock reloj
    ) {
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.reglasAgenda = reglasAgenda;
        this.reloj = reloj;
    }


    private Salon buscarSalonPorSlug(String slug) {

        return salonRepository.findBySlug(slug)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                            "Salón no encontrado"
                        )
                );
    }


    private Servicio validarServicio(
            Long salonId,
            Long servicioId
    ) {
        Servicio servicio = servicioRepository
                .findByIdAndSalonId(servicioId, salonId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                            "Servicio no encontrado en este salón"
                        )
                );

        if (!servicio.isActivo()) {
            throw new ReglaNegocioException(
                "El servicio solicitado no está activo"
            );
        }

        return servicio;
    }


    private Usuario validarEstilista(
            Long salonId,
            Long estilistaId,
            Long servicioId
    ) {
        Usuario estilista = usuarioRepository
                .findById(estilistaId)
                .filter(usuario ->
                        usuario.getSalon() != null
                        && salonId.equals(usuario.getSalon().getId())
                        && usuario.getRol() == Rol.ESTILISTA
                )
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                            "Estilista no encontrado en este salón"
                        )
                );

        if (!estilista.isActivo()) {
            throw new ReglaNegocioException(
                "El estilista solicitado no está activo"
            );
        }

        if (!estilista.ofreceServicio(servicioId)) {
            throw new ReglaNegocioException(
                "El estilista no ofrece el servicio solicitado"
            );
        }

        return estilista;
    }


    private List<Cita> obtenerCitasOcupadas(
            Long salonId,
            Long estilistaId,
            LocalDate fecha
    ) {
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);

        return citaRepository
                .findBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                        salonId,
                        estilistaId,
                        inicioDia,
                        finDia
                )
                .stream()
                .filter(cita ->
                        cita.getEstado() != null
                        && cita.getEstado().ocupaAgenda()
                )
                .toList();
    }


    @Transactional(readOnly = true)
    public DisponibilidadPublicaResponse consultarDisponibilidad(
            String slug,
            Long servicioId,
            Long estilistaId,
            LocalDate fecha
    ) {
        // 1. Buscar el salón
        Salon salon = buscarSalonPorSlug(slug);

        // 2. Validar el servicio
        Servicio servicio = validarServicio(
                salon.getId(),
                servicioId
        );

        // 3. Validar el estilista
        Usuario estilista = validarEstilista(
                salon.getId(),
                estilistaId,
                servicioId
        );

        // 4. Consultar las citas ocupadas
        List<Cita> citasOcupadas = obtenerCitasOcupadas(
                salon.getId(),
                estilistaId,
                fecha
        );

        // 5. Calcular los horarios libres
        List<LocalTime> horariosLibres =
                reglasAgenda.calcularHorariosLibres(
                        fecha,
                        servicio.getDuracionMinutos(),
                        salon.getConfiguracion(),
                        citasOcupadas,
                        reloj
                );

        // 6. Construir la respuesta pública
        return new DisponibilidadPublicaResponse(
                fecha,
                servicio.getNombre(),
                estilista.getNombre(),
                servicio.getDuracionMinutos(),
                horariosLibres
        );
    }

}
