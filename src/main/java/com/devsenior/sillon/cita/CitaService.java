
package com.devsenior.sillon.cita;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.UsuarioRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.compartido.AccionNoPermitidaException;

import java.util.List;

@Service
public class CitaService {

    private final CitaRepository citaRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteService clienteService;
    private final ReglasAgenda reglasAgenda;
    private final CitaMapper citaMapper;
    private final Clock reloj;

    public CitaService(
            CitaRepository citaRepository,
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository,
            ClienteService clienteService,
            ReglasAgenda reglasAgenda,
            CitaMapper citaMapper,
            Clock reloj
    ) {
        this.citaRepository = citaRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteService = clienteService;
        this.reglasAgenda = reglasAgenda;
        this.citaMapper = citaMapper;
        this.reloj = reloj;
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
                    "No se pueden agendar citas para un servicio inactivo"
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
                    "No se pueden agendar citas con un estilista inactivo"
            );
        }

        if (!estilista.ofreceServicio(servicioId)) {
            throw new ReglaNegocioException(
                    "El estilista no ofrece el servicio solicitado"
            );
        }

        return estilista;
    }


    private void validarFechaFutura(LocalDateTime inicio) {

        if (inicio == null) {
            throw new ReglaNegocioException(
                    "La fecha y hora de la cita son obligatorias"
            );
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);

        if (!inicio.isAfter(ahora)) {
            throw new ReglaNegocioException(
                    "No se pueden agendar citas en fechas u horas pasadas"
            );
        }
    }

    private void validarDisponibilidad(
            Long salonId,
            Long estilistaId,
            LocalDateTime inicio,
            LocalDateTime fin
    ) {

        boolean existeCruce = citaRepository
                .existsBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                        salonId,
                        estilistaId,
                        List.of(
                                EstadoCita.PENDIENTE,
                                EstadoCita.CONFIRMADA
                        ),
                        fin,
                        inicio
                );

        if (existeCruce) {
            throw new ReglaNegocioException(
                    "El estilista ya tiene una cita en ese horario"
            );
        }
    }


    @Transactional
    public CitaResponse crear(Long salonId, CitaRequest request) {

        // 1. Verificar el salón

        Salon salon = salonRepository.findByIdParaAgendar(salonId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                        "Salón no encontrado"
                )
        );


        // 2. Validar el servicio
        Servicio servicio = validarServicio(
                salonId,
                request.servicioId()
        );

        // 3. Validar el estilista
        Usuario estilista = validarEstilista(
                salonId,
                request.estilistaId(),
                servicio.getId()
        );

        // 4. Validar que la fecha sea futura
        LocalDateTime inicio = request.inicio();
        validarFechaFutura(inicio);

        // 5. Calcular la hora de finalización
        LocalDateTime fin = inicio.plusMinutes(
                servicio.getDuracionMinutos()
        );

        // 6. Verificar el horario de atención
        reglasAgenda.validarHorario(
                inicio,
                fin,
                salon.getConfiguracion()
        );

        // 7. Comprobar que no existan citas cruzadas
        validarDisponibilidad(
                salonId,
                estilista.getId(),
                inicio,
                fin
        );

        // 8. Buscar o registrar al cliente
        Cliente cliente = clienteService.buscarOCrear(
                request.clienteNombre(),
                request.clienteTelefono(),
                salon
        );

        // 9. Construir la cita
        Cita cita = new Cita();

        cita.setSalon(salon);
        cita.setCliente(cliente);
        cita.setServicio(servicio);
        cita.setEstilista(estilista);
        cita.setInicio(inicio);
        cita.setFin(fin);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setOrigen(OrigenCita.SALON);
        cita.setPrecio(servicio.getPrecio());
        cita.setNota(request.nota());

        // 10. Guardar la cita
        Cita guardada = citaRepository.saveAndFlush(cita);

        return citaMapper.toResponse(guardada);
    }


    @Transactional(readOnly = true)
    public List<CitaResponse> listarPorFecha(
            Long salonId,
            Long usuarioId,
            Rol rol,
            LocalDate fecha
    ) {

        LocalDate dia = fecha != null
                ? fecha
                : LocalDate.now(reloj);

        LocalDateTime inicioDia = dia.atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);

        List<Cita> citas;

        if (rol == Rol.DUENO) {

            citas = citaRepository
                    .findBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                            salonId,
                            inicioDia,
                            finDia
                    );

        } else if (rol == Rol.ESTILISTA) {

            citas = citaRepository
                    .findBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                            salonId,
                            usuarioId,
                            inicioDia,
                            finDia
                    );

        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para consultar las citas"
            );
        }

        return citas.stream()
                .map(citaMapper::toResponse)
                .toList();
    }


    @Transactional
    public CitaResponse actualizarEstado(
            Long salonId,
            Long usuarioId,
            Rol rol,
            Long citaId,
            CambiarEstadoCitaRequest request
    ) {

        // 1. Buscar la cita dentro del salón
        Cita cita = citaRepository
                .findByIdAndSalonId(citaId, salonId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Cita no encontrada"
                        )
                );

        // 2. Validar permisos del estilista
        if (rol == Rol.ESTILISTA) {

            if (!usuarioId.equals(cita.getEstilista().getId())) {
                throw new RecursoNoEncontradoException(
                        "Cita no encontrada"
                );
            }

            if (request.estado() != EstadoCita.COMPLETADA
                    && request.estado() != EstadoCita.NO_ASISTIO) {

                throw new AccionNoPermitidaException(
                        "El estilista no puede establecer ese estado"
                );
            }

        } else if (rol != Rol.DUENO) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para modificar citas"
            );
        }

        // 3. Comprobar la transición de estado
        if (!cita.getEstado().puedeCambiarA(request.estado())) {
            throw new ReglaNegocioException(
                    "No se permite cambiar la cita de "
                    + cita.getEstado()
                    + " a "
                    + request.estado()
            );
        }

        // 4. Actualizar el estado
        cita.setEstado(request.estado());

        // 5. Guardar y devolver la cita actualizada
        Cita actualizada = citaRepository.saveAndFlush(cita);

        return citaMapper.toResponse(actualizada);
    }


}
