package com.devsenior.sillon.publico;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.cita.CitaRepository;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisponibilidadService {
    private final SalonRepository salones;
    private final ServicioRepository servicios;
    private final UsuarioRepository usuarios;
    private final CitaRepository citas;
    private final ReglasAgenda reglas;

    public DisponibilidadService(SalonRepository salones, ServicioRepository servicios,
                                 UsuarioRepository usuarios, CitaRepository citas, ReglasAgenda reglas) {
        this.salones = salones;
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.citas = citas;
        this.reglas = reglas;
    }

    @Transactional(readOnly = true)
    public DisponibilidadRespuesta consultar(String slug, Long servicioId, Long estilistaId, LocalDate fecha) {
        Salon salon = salones.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("El salón no existe"));
        Servicio servicio = servicios.findByIdAndSalonId(servicioId, salon.getId())
                .orElseThrow(() -> new NoEncontradoException("El servicio no existe en este salón"));
        Usuario estilista = usuarios.findByIdAndSalonIdAndRol(estilistaId, salon.getId(), Rol.ESTILISTA)
                .orElseThrow(() -> new NoEncontradoException("El estilista no existe en este salón"));
        if (!servicio.isActivo() || !estilista.isActivo() || !estilista.ofreceServicio(servicioId)) {
            throw new ReglaNegocioException("El servicio no está disponible con este estilista");
        }
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay().minusNanos(1);
        List<Cita> ocupadas = citas.findBySalonIdAndEstilistaIdAndEstadoInAndInicioBetweenOrderByInicioAsc(
                salon.getId(), estilistaId, List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA), desde, hasta);
        List<LocalTime> libres = reglas.horariosLibres(salon.getConfiguracion(), fecha,
                servicio.getDuracionMinutos(), ocupadas);
        return new DisponibilidadRespuesta(fecha, servicio.getNombre(), estilista.getNombre(),
                servicio.getDuracionMinutos(), libres);
    }
}
