package com.devsenior.sillon.solicitud;

import com.devsenior.sillon.compartido.ConflictoExcepcion;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.solicitud.dto.SolicitudContactoResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SolicitudContactoService {

    private final SolicitudContactoRepository repositorio;

    public SolicitudContactoService(SolicitudContactoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<SolicitudContactoResponseDto> listarPorSalon(Long salonId) {
        return repositorio.findBySalonIdOrderByFechaCreacionDesc(salonId)
                .stream()
                .map(SolicitudContactoResponseDto::desde)
                .toList();
    }

    @Transactional
    public SolicitudContactoResponseDto marcarAtendida(Long id, Long salonId) {
        SolicitudContacto solicitud = repositorio.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("Solicitud no encontrada"));

        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ConflictoExcepcion("La solicitud ya se encuentra en estado ATENDIDA");
        }

        solicitud.marcarComoAtendida();
        return SolicitudContactoResponseDto.desde(repositorio.save(solicitud));
    }
}