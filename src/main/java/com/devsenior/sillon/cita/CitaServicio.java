package com.devsenior.sillon.cita;

import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteServicio;
import com.devsenior.sillon.compartido.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@Service
public class CitaServicio {

    private static final String MENSAJE_CITA_NO_ENCONTRADA = "Cita no encontrada";
    private static final String MENSAJE_SERVICIO_NO_ENCONTRADO = "Servicio no encontrado";
    private static final String MENSAJE_ESTILISTA_NO_ENCONTRADO = "Estilista no encontrado";

    private final CitaRepository citaRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final ClienteServicio clienteServicio;
    private final ReglasAgenda reglasAgenda;
    private final Clock reloj;

    public CitaServicio(CitaRepository citaRepository, ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository, SalonRepository salonRepository,
            ClienteServicio clienteServicio, ReglasAgenda reglasAgenda, Clock reloj) {
        this.citaRepository = citaRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.clienteServicio = clienteServicio;
        this.reglasAgenda = reglasAgenda;
        this.reloj = reloj;
    }

    @Transactional
    public CitaRespuesta crearDelSalon(Long salonId, CitaSolicitud solicitud) {
        return agendar(salonId, solicitud, OrigenCita.SALON, EstadoCita.CONFIRMADA);
    }

    @Transactional
    public CitaRespuesta reservarEnLinea(Long salonId, CitaSolicitud solicitud) {
        return agendar(salonId, solicitud, OrigenCita.EN_LINEA, EstadoCita.PENDIENTE);
    }

    @Transactional(readOnly = true)
    public List<CitaRespuesta> listar(Long salonId, LocalDate fecha, UsuarioPrincipal principal) {
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();
        List<Cita> citas = citaRepository
                .findBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                        salonId, desde, hasta);
        if (principal.getRol() == Rol.ESTILISTA) {
            citas = citas.stream()
                    .filter(cita -> cita.getEstilista().getId().equals(principal.getId()))
                    .toList();
        }
        return citas.stream().map(CitaServicio::aRespuesta).toList();
    }

    @Transactional
    public CitaRespuesta cambiarEstado(Long salonId, UsuarioPrincipal principal, Long citaId,
            EstadoCita destino) {
        Cita cita = citaRepository.findByIdAndSalonId(citaId, salonId)
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_CITA_NO_ENCONTRADA));
        if (principal.getRol() == Rol.ESTILISTA) {
            if (!cita.getEstilista().getId().equals(principal.getId())) {
                throw new NoEncontradoException(MENSAJE_CITA_NO_ENCONTRADA);
            }
            if (destino != EstadoCita.COMPLETADA && destino != EstadoCita.NO_ASISTIO) {
                throw new AccionNoPermitidaException(
                        "Un estilista solo puede marcar COMPLETADA o NO_ASISTIO");
            }
        }
        if (!cita.getEstado().puedeCambiarA(destino)) {
            throw new ReglaNegocioException(
                    "La cita no puede pasar de " + cita.getEstado() + " a " + destino);
        }
        cita.setEstado(destino);
        cita.setFechaActualizacion(LocalDateTime.now(reloj));
        cita = citaRepository.saveAndFlush(cita);
        return aRespuesta(cita);
    }

    @Transactional(readOnly = true)
    public DatosRecordatorio datosVerificados(Long salonId, Long citaId) {
        Cita cita = citaRepository.findByIdAndSalonId(citaId, salonId)
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_CITA_NO_ENCONTRADA));
        if (cita.getEstado() != EstadoCita.PENDIENTE && cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new ReglaNegocioException(
                    "El recordatorio solo está disponible para citas PENDIENTE o CONFIRMADA");
        }
        String texto = "Servicio: " + cita.getServicio().getNombre() + " ("
                + cita.getServicio().getDuracionMinutos() + " minutos)\n"
                + "Estilista: " + cita.getEstilista().getNombre() + "\n"
                + "Fecha: " + formatearFecha(cita.getInicio()) + "\n"
                + "Hora: " + formatearHora(cita.getInicio()) + "\n"
                + "Valor: " + formatearValor(cita.getPrecio()) + "\n"
                + "Dirección: " + cita.getSalon().getDireccion() + ", " + cita.getSalon().getCiudad();
        return new DatosRecordatorio(cita.getSalon().getNombre(), cita.getCliente().getNombre(),
                cita.getCliente().getTelefono(), texto);
    }

    private static String formatearFecha(LocalDateTime momento) {
        return momento.format(
                DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-CO")));
    }

    private static String formatearHora(LocalDateTime momento) {
        String hora = momento.format(
                DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-CO")));
        return hora.replace('\u00A0', ' ').replace('\u202F', ' ');
    }

    private static String formatearValor(long precio) {
        return "$" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(precio);
    }

    private CitaRespuesta agendar(Long salonId, CitaSolicitud solicitud, OrigenCita origen,
            EstadoCita estadoInicial) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));

        Servicio servicio = servicioRepository.findByIdAndSalonId(solicitud.servicioId(), salonId)
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_SERVICIO_NO_ENCONTRADO));
        if (!servicio.isActivo()) {
            throw new ReglaNegocioException("El servicio no está activo");
        }

        Usuario estilista = usuarioRepository.findById(solicitud.estilistaId())
                .filter(usuario -> usuario.getRol() == Rol.ESTILISTA)
                .filter(usuario -> usuario.getSalon().getId().equals(salonId))
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_ESTILISTA_NO_ENCONTRADO));
        if (!estilista.isActivo()) {
            throw new ReglaNegocioException("El estilista no está activo");
        }
        if (!estilista.ofrece(servicio)) {
            throw new ReglaNegocioException("El estilista no ofrece este servicio");
        }

        LocalDateTime inicio = solicitud.inicio();
        LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos());

        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (!inicio.isAfter(ahora)) {
            throw new ReglaNegocioException("La cita no se puede agendar en el pasado");
        }

        reglasAgenda.validarHorario(salon.getConfiguracion(), inicio, fin);

        List<EstadoCita> estadosQueOcupan = Arrays.stream(EstadoCita.values())
                .filter(EstadoCita::ocupaAgenda)
                .toList();
        if (citaRepository.hayCruce(salonId, estilista.getId(), estadosQueOcupan, inicio, fin)) {
            throw new ReglaNegocioException(
                    "El estilista ya tiene una cita que se cruza en ese horario");
        }

        Cliente cliente = clienteServicio.buscarOCrear(salonId, solicitud.clienteNombre(),
                solicitud.clienteTelefono());

        Cita cita = new Cita();
        cita.setSalon(salon);
        cita.setServicio(servicio);
        cita.setEstilista(estilista);
        cita.setCliente(cliente);
        cita.setInicio(inicio);
        cita.setFin(fin);
        cita.setEstado(estadoInicial);
        cita.setOrigen(origen);
        cita.setPrecio(servicio.getPrecio());
        cita.setNota(solicitud.nota() == null ? null : solicitud.nota().trim());
        cita.setFechaCreacion(ahora);
        cita.setFechaActualizacion(ahora);
        cita = citaRepository.save(cita);
        return aRespuesta(cita);
    }

    private static CitaRespuesta aRespuesta(Cita cita) {
        return new CitaRespuesta(
                cita.getId(),
                cita.getInicio(),
                cita.getFin(),
                cita.getEstado(),
                cita.getOrigen(),
                cita.getPrecio(),
                cita.getServicio().getId(),
                cita.getServicio().getNombre(),
                cita.getEstilista().getId(),
                cita.getEstilista().getNombre(),
                cita.getCliente().getNombre(),
                cita.getCliente().getTelefono(),
                cita.getNota(),
                cita.getFechaCreacion(),
                cita.getFechaActualizacion());
    }
}
