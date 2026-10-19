package com.devsenior.sillon.publico;

import com.devsenior.sillon.cita.CitaRepository;
import com.devsenior.sillon.cita.CitaRequest;
import com.devsenior.sillon.cita.CitaResponse;
import com.devsenior.sillon.cita.CitaService;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.FormatoHorarioSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioMapper;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoService {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final SalonRepository salones;
    private final ServicioRepository servicios;
    private final UsuarioRepository usuarios;
    private final CitaRepository citas;
    private final CitaService agenda;
    private final ReglasAgenda reglas;
    private final ServicioMapper mapper;
    private final FormatoHorarioSalon horario;

    public CatalogoService(SalonRepository salones, ServicioRepository servicios, UsuarioRepository usuarios,
            CitaRepository citas, CitaService agenda, ReglasAgenda reglas, ServicioMapper mapper, FormatoHorarioSalon horario) {
        this.salones = salones;
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.citas = citas;
        this.agenda = agenda;
        this.reglas = reglas;
        this.mapper = mapper;
        this.horario = horario;
    }

    @Transactional(readOnly = true)
    public CatalogoResponse consultar(String slug) {
        Salon salon = buscarSalon(slug);
        var catalogo = servicios.findAllBySalonIdAndActivoTrue(salon.getId()).stream().map(mapper::convertir).toList();
        var equipo = usuarios.findAllBySalonIdAndRolAndActivoTrue(salon.getId(), Rol.ESTILISTA).stream()
                .map(usuario -> new EstilistaPublicoResponse(usuario.getId(), usuario.getNombre(),
                        usuario.getServicios().stream().filter(Servicio::isActivo).map(Servicio::getId).toList()))
                .toList();
        return new CatalogoResponse(salon.getNombre(), salon.getSlug(), salon.getCiudad(), salon.getDireccion(),
                salon.getTelefono(), horario.describir(salon.getConfiguracion()), catalogo, equipo);
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse disponibilidad(String slug, Long servicioId, Long estilistaId, LocalDate fecha) {
        Salon salon = buscarSalon(slug);
        Servicio servicio = servicios.findByIdAndSalonId(servicioId, salon.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
        if (!servicio.isActivo()) { throw new ReglaNegocioException("El servicio no está activo"); }
        Usuario estilista = usuarios.findByIdAndSalonIdAndRol(estilistaId, salon.getId(), Rol.ESTILISTA)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estilista no encontrado"));
        if (!estilista.isActivo() || !estilista.ofreceServicio(servicioId)) {
            throw new ReglaNegocioException("El estilista no está activo o no ofrece el servicio");
        }
        var ocupadas = citas.findAllBySalonIdAndEstilistaIdAndEstadoInAndInicioGreaterThanEqualAndInicioLessThan(
                salon.getId(), estilistaId, List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA),
                fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        var libres = reglas.horariosLibres(salon.getConfiguracion(), fecha, servicio.getDuracionMinutos(), ocupadas)
                .stream().map(HORA::format).toList();
        return new DisponibilidadResponse(fecha, servicio.getNombre(), estilista.getNombre(), servicio.getDuracionMinutos(), libres);
    }

    @Transactional
    public CitaResponse reservar(String slug, CitaRequest request) {
        return agenda.reservar(buscarSalon(slug).getId(), request);
    }

    private Salon buscarSalon(String slug) {
        return salones.findBySlug(slug).orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
    }
}
