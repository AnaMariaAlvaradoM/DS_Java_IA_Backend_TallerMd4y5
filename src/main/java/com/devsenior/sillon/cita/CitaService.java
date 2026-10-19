package com.devsenior.sillon.cita;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.excepcion.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CitaService {
    private final CitaRepository citas;
    private final ServicioRepository servicios;
    private final UsuarioRepository usuarios;
    private final ClienteService clientes;
    private final ReglasAgenda reglas;
    private final CitaMapper mapper;
    private final Clock reloj;

    public CitaService(CitaRepository citas, ServicioRepository servicios, UsuarioRepository usuarios,
            ClienteService clientes, ReglasAgenda reglas, CitaMapper mapper, Clock reloj) {
        this.citas = citas;
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.reglas = reglas;
        this.mapper = mapper;
        this.reloj = reloj;
    }

    @Transactional
    public CitaResponse crear(Long salonId, CitaRequest request) {
        return agendar(salonId, request, EstadoCita.CONFIRMADA, OrigenCita.SALON);
    }

    @Transactional
    public CitaResponse reservar(Long salonId, CitaRequest request) {
        return agendar(salonId, request, EstadoCita.PENDIENTE, OrigenCita.EN_LINEA);
    }

    private CitaResponse agendar(Long salonId, CitaRequest request, EstadoCita estado, OrigenCita origen) {
        Servicio servicio = servicios.findByIdAndSalonId(request.servicioId(), salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
        if (!servicio.isActivo()) { throw new ReglaNegocioException("El servicio no está activo"); }
        Usuario estilista = usuarios.findParaAgendar(request.estilistaId(), salonId, Rol.ESTILISTA)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estilista no encontrado"));
        if (!estilista.isActivo() || !estilista.ofreceServicio(servicio.getId())) {
            throw new ReglaNegocioException("El estilista no está activo o no ofrece el servicio");
        }
        LocalDateTime fin = request.inicio().plusMinutes(servicio.getDuracionMinutos());
        if (request.inicio().isBefore(LocalDateTime.now(reloj))) {
            throw new ReglaNegocioException("No se puede agendar en el pasado");
        }
        reglas.validarHorario(estilista.getSalon().getConfiguracion(), request.inicio(), fin);
        if (!citas.findAllBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                salonId, estilista.getId(), List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA), fin, request.inicio()).isEmpty()) {
            throw new ReglaNegocioException("El estilista ya tiene una cita en ese horario");
        }
        Cliente cliente = clientes.buscarOCrear(salonId, request.clienteNombre(), request.clienteTelefono());
        Cita cita = new Cita();
        cita.setSalon(estilista.getSalon());
        cita.setServicio(servicio);
        cita.setEstilista(estilista);
        cita.setCliente(cliente);
        cita.setInicio(request.inicio());
        cita.setFin(fin);
        cita.setPrecio(servicio.getPrecio());
        cita.setEstado(estado);
        cita.setOrigen(origen);
        cita.setNota(request.nota());
        return mapper.convertir(citas.saveAndFlush(cita));
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> listar(UsuarioPrincipal principal, LocalDate fecha) {
        LocalDate dia = fecha == null ? LocalDate.now(reloj) : fecha;
        LocalDateTime desde = dia.atStartOfDay();
        LocalDateTime hasta = dia.plusDays(1).atStartOfDay();
        List<Cita> agenda = principal.getRol() == Rol.ESTILISTA
                ? citas.findAllBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                        principal.getSalonId(), principal.getId(), desde, hasta)
                : citas.findAllBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                        principal.getSalonId(), desde, hasta);
        return agenda.stream().map(mapper::convertir).toList();
    }

    @Transactional
    public CitaResponse cambiarEstado(UsuarioPrincipal principal, Long id, EstadoCita destino) {
        Cita cita = citas.findByIdAndSalonId(id, principal.getSalonId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
        if (principal.getRol() == Rol.ESTILISTA) {
            if (!cita.getEstilista().getId().equals(principal.getId())) {
                throw new RecursoNoEncontradoException("Cita no encontrada");
            }
            if (destino != EstadoCita.COMPLETADA && destino != EstadoCita.NO_ASISTIO) {
                throw new AccionNoPermitidaException("El estilista solo puede completar o marcar no asistencia");
            }
        }
        if (!cita.getEstado().permite(destino)) { throw new ReglaNegocioException("Transición de estado no permitida"); }
        cita.setEstado(destino);
        return mapper.convertir(citas.saveAndFlush(cita));
    }

    @Transactional(readOnly = true)
    public DatosRecordatorio datosRecordatorio(Long salonId, Long citaId) {
        Cita cita = citas.findOneByIdAndSalonId(citaId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
        if (cita.getEstado() != EstadoCita.PENDIENTE && cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new ReglaNegocioException("Solo se generan recordatorios de citas pendientes o confirmadas");
        }
        Locale colombia = Locale.forLanguageTag("es-CO");
        String fecha = cita.getInicio().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", colombia));
        String hora = cita.getInicio().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(colombia))
                .replace('\u00a0', ' ').replace('\u202f', ' ');
        String valor = "$" + NumberFormat.getIntegerInstance(colombia).format(cita.getPrecio());
        long minutos = Duration.between(cita.getInicio(), cita.getFin()).toMinutes();
        String datos = "Servicio: " + cita.getServicio().getNombre() + " (" + minutos + " minutos)\n"
                + "Estilista: " + cita.getEstilista().getNombre() + "\n"
                + "Fecha: " + fecha + "\n"
                + "Hora: " + hora + "\n"
                + "Valor: " + valor + "\n"
                + "Dirección: " + cita.getSalon().getDireccion() + ", " + cita.getSalon().getCiudad();
        return new DatosRecordatorio(cita.getId(), cita.getSalon().getNombre(), cita.getCliente().getNombre(),
                cita.getCliente().getTelefono(), datos);
    }
}
