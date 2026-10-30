package com.devsenior.sillon.cita;

import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cita.dto.CitaResquest;
import com.devsenior.sillon.cita.mapper.CitaMapper;
import com.devsenior.sillon.cita.model.Cita;
import com.devsenior.sillon.cita.model.EstadoCita;
import com.devsenior.sillon.cita.model.OrigenCita;
import com.devsenior.sillon.cita.repository.CitaRepository;
import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteRepository;
import com.devsenior.sillon.compartido.AccesoDenegadoException;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;

import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import com.devsenior.sillon.usuario.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CitaService {
    private final CitaRepository citaRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ReglasAgenda reglasAgenda;
    private final Clock clock;

    public CitaService(CitaRepository citaRepository, SalonRepository salonRepository,
                       ServicioRepository servicioRepository, UsuarioRepository usuarioRepository,
                       ClienteRepository clienteRepository, ReglasAgenda reglasAgenda,
                       Clock  clock) {
        this.citaRepository = citaRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.reglasAgenda = reglasAgenda;
        this.clock = clock;
    }
    @Transactional
    public CitaResponse agendarDesdeSalon(CitaResquest dto, Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));

        // Desde el salón, la cita arranca confirmada directamente
        Cita cita = procesarAgendamiento(dto, salon, OrigenCita.SALON, EstadoCita.PENDIENTE);
        return CitaMapper.toResponse(cita);
    }
    private Cita procesarAgendamiento(CitaResquest dto, Salon salon, OrigenCita origen, EstadoCita estadoInicial) {

        // 1. El servicio existe en el salón y está activo
        Servicio servicio = servicioRepository.findByIdAndSalonId(dto.servicioId(), salon.getId())
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado en el salón"));
        if (!servicio.getActivo()) {
            throw new ReglaNegocioException("El servicio no está activo");
        }

        // 2. El estilista existe en el salón, está activo y ofrece ese servicio
        Usuario estilista = usuarioRepository.findByIdAndSalonIdAndRol(dto.estilistaId(), salon.getId(), Rol.ESTILISTA)
                .orElseThrow(() -> new NoEncontradoException("Estilista no encontrado en el salón"));
        if (!estilista.isActivo()) {
            throw new ReglaNegocioException("El estilista no está activo");
        }
        if (!estilista.getServicios().contains(servicio)) {
            throw new ReglaNegocioException("El estilista no ofrece el servicio solicitado");
        }

        // 3. Se calcula el fin con la duración del servicio
        LocalDateTime inicio = dto.inicio();
        LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos());

        // 4. La cita no está en el pasado (usando relojProyecto para tests)
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (inicio.isBefore(ahora)) {
            throw new ReglaNegocioException("No se pueden agendar citas en el pasado");
        }

        // 5. Cumple el horario del salón
        boolean abreDomingo = Boolean.TRUE.equals(salon.getConfiguracion().isAbreDomingos()); // Dependiendo de tu modelo
        reglasAgenda.validarHorario(inicio, servicio.getDuracionMinutos(),
                salon.getConfiguracion().getHoraApertura(), salon.getConfiguracion().getHoraCierre(), abreDomingo);

        // 6. No se cruza con otra cita del estilista que ocupe agenda
        LocalDateTime inicioDia = inicio.toLocalDate().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);
        List<Cita> citasExistentes = citaRepository.findCitasQueOcupanAgenda(
                estilista.getId(), List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA), inicioDia, finDia);

        for (Cita ocupada : citasExistentes) {
            LocalDateTime inicioOcupada = ocupada.getFechaHora();
            LocalDateTime finOcupada = inicioOcupada.plusMinutes(ocupada.getServicio().getDuracionMinutos());

            if (reglasAgenda.seCruzan(inicio, fin, inicioOcupada, finOcupada)) {
                throw new ReglaNegocioException("El horario solicitado se cruza con otra cita del estilista");
            }
        }

        // 7. Se busca o crea el cliente, se copia el precio y se guarda
        String telNormalizado = Cliente.normalizarTelefono(dto.clienteTelefono());
        Cliente cliente = clienteRepository.findBySalonIdAndTelefono(salon.getId(), telNormalizado)
                .orElseGet(() -> {
                    Cliente nuevoCliente = new Cliente(dto.clienteNombre(),salon, telNormalizado );
                    return clienteRepository.save(nuevoCliente);
                });

        Cita nuevaCita = new Cita(salon, cliente, estilista, servicio, servicio.getPrecio(), inicio, origen);
        nuevaCita.setEstadoCita(estadoInicial);
         nuevaCita.setNota(dto.nota());

        return citaRepository.save(nuevaCita);
    }
    @Transactional(readOnly = true)
    public List<CitaResponse> listarPorDia(Long salonId, LocalDate fecha, Long usuarioAutenticadoId, Rol rol) {
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);

        List<Cita> citas;

        if (rol == Rol.DUENO) {
            citas = citaRepository.findBySalonIdAndDia(salonId, inicioDia, finDia);
        } else if (rol == Rol.ESTILISTA) {

            citas = citaRepository.findBySalonIdAndEstilistaIdAndDia(salonId, usuarioAutenticadoId, inicioDia, finDia);
        } else {
            throw new AccesoDenegadoException("Rol no autorizado para consultar la agenda");
        }

        // Usamos el método construirRespuesta que ya creaste en el paso 4.4
        return citas.stream()
                .map(CitaMapper::toResponse)
                .toList();
    }
    @Transactional
    public CitaResponse cambiarEstado(Long citaId, Long salonId, EstadoCita nuevoEstado, Long usuarioAutenticadoId, Rol rol) {

        // 1. Busca por id y salón (Si no existe o no es de ese salón -> 404)
        Cita cita = citaRepository.findByIdAndSalonId(citaId, salonId)
                .orElseThrow(() -> new NoEncontradoException("Cita no encontrada"));

        // 2. Validaciones específicas si es ESTILISTA
        if (rol == Rol.ESTILISTA) {

            if (!cita.getEstilista().getId().equals(usuarioAutenticadoId)) {
                throw new NoEncontradoException("Cita no encontrada");
            }
            // El destino solo puede ser COMPLETADA o NO_ASISTIO (Si es otro -> 403)
            if (nuevoEstado != EstadoCita.COMPLETADA && nuevoEstado != EstadoCita.NO_ASISTIO) {
                throw new AccesoDenegadoException("Los estilistas solo pueden marcar citas como COMPLETADA o NO_ASISTIO");
            }
        }

        // 3. La transición debe ser válida según la máquina de estados (Si no -> 409)
        if (!cita.getEstadoCita().puedeCambiarA(nuevoEstado)) {
            throw new ReglaNegocioException("Transición no permitida: Una cita " + cita.getEstadoCita() + " no puede pasar a " + nuevoEstado);
        }

        // 4. Cambiar el estado y guardar
        cita.setEstadoCita(nuevoEstado);

        Cita actualizada = citaRepository.saveAndFlush(cita);

        return CitaMapper.toResponse(actualizada);
    }
}
