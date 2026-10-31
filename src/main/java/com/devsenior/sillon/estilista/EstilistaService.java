package com.devsenior.sillon.estilista;

import com.devsenior.sillon.compartido.ConflictoExcepcion;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.estilista.dto.EstilistaRequestDto;
import com.devsenior.sillon.estilista.dto.EstilistaResponseDto;
import com.devsenior.sillon.estilista.dto.HorarioEstilistaDto;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.servicio.ServicioSalon;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EstilistaService {

    private final EstilistaRepository estilistaRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final HorarioEstilistaRepository horarioEstilistaRepository;

    public EstilistaService(
            EstilistaRepository estilistaRepository,
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            HorarioEstilistaRepository horarioEstilistaRepository) {
        this.estilistaRepository = estilistaRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.horarioEstilistaRepository = horarioEstilistaRepository;
    }

    @Transactional
    public EstilistaResponseDto crearEstilista(Long salonId, EstilistaRequestDto request) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salon no encontrado"));

        Estilista estilista = new Estilista();
        estilista.setNombre(request.nombre().trim());
        estilista.setEspecialidad(request.especialidad() != null ? request.especialidad().trim() : null);
        estilista.setActivo(true);
        estilista.setSalon(salon);

        if (request.serviciosIds() != null && !request.serviciosIds().isEmpty()) {
            List<ServicioSalon> servicios = servicioRepository.findAllById(request.serviciosIds());
            estilista.setServicios(servicios);
        }

        estilista = estilistaRepository.save(estilista);
        return mapearADto(estilista);
    }

    @Transactional(readOnly = true)
    public List<EstilistaResponseDto> listarEstilistas(Long salonId) {
        return estilistaRepository.findBySalonId(salonId).stream()
                .map(this::mapearADto)
                .toList();
    }

    @Transactional
    public EstilistaResponseDto actualizarEstilista(Long id, Long salonId, EstilistaRequestDto request) {
        Estilista estilista = estilistaRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("Estilista no encontrado en su salon"));

        estilista.setNombre(request.nombre().trim());
        estilista.setEspecialidad(request.especialidad() != null ? request.especialidad().trim() : null);

        if (request.serviciosIds() != null) {
            List<ServicioSalon> servicios = servicioRepository.findAllById(request.serviciosIds());
            estilista.setServicios(servicios);
        }

        estilista = estilistaRepository.save(estilista);
        return mapearADto(estilista);
    }

    @Transactional
    public void cambiarEstado(Long id, Long salonId, boolean activo) {
        Estilista estilista = estilistaRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("Estilista no encontrado en su salon"));

        estilista.setActivo(activo);
        estilistaRepository.save(estilista);
    }

    @Transactional
    public List<HorarioEstilistaDto> asignarHorarios(Long estilistaId, Long salonId, List<HorarioEstilistaDto> horariosDto) {
        Estilista estilista = estilistaRepository.findByIdAndSalonId(estilistaId, salonId)
                .orElseThrow(() -> new NoEncontradoException("Estilista no encontrado en su salon"));

        for (HorarioEstilistaDto h : horariosDto) {
            if (!h.horaInicio().isBefore(h.horaFin())) {
                throw new ConflictoExcepcion("La hora de inicio debe ser anterior a la hora de fin para el dia " + h.diaSemana());
            }
        }

        horarioEstilistaRepository.deleteByEstilistaId(estilistaId);

        List<HorarioEstilista> nuevosHorarios = horariosDto.stream()
                .map(dto -> new HorarioEstilista(dto.diaSemana(), dto.horaInicio(), dto.horaFin(), estilista))
                .toList();

        horarioEstilistaRepository.saveAll(nuevosHorarios);

        return horariosDto;
    }

    @Transactional(readOnly = true)
    public List<HorarioEstilistaDto> obtenerHorarios(Long estilistaId, Long salonId) {
        Estilista estilista = estilistaRepository.findByIdAndSalonId(estilistaId, salonId)
                .orElseThrow(() -> new NoEncontradoException("Estilista no encontrado en su salon"));

        return horarioEstilistaRepository.findByEstilistaId(estilista.getId()).stream()
                .map(h -> new HorarioEstilistaDto(h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin()))
                .toList();
    }

    private EstilistaResponseDto mapearADto(Estilista estilista) {
        List<Long> serviciosIds = estilista.getServicios() != null
                ? estilista.getServicios().stream().map(ServicioSalon::getId).toList()
                : new ArrayList<>();

        return new EstilistaResponseDto(
                estilista.getId(),
                estilista.getNombre(),
                estilista.getEspecialidad(),
                estilista.isActivo(),
                serviciosIds
        );
    }
}