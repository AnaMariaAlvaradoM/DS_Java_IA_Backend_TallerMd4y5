package com.devsenior.sillon.publico;

import com.devsenior.sillon.cita.CitaRespuesta;
import com.devsenior.sillon.cita.CitaService;
import com.devsenior.sillon.cita.CitaSolicitud;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.salon.SalonRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico/salones/{slug}")
public class PublicoController {
    private final CatalogoService catalogo;
    private final DisponibilidadService disponibilidad;
    private final SalonRepository salones;
    private final CitaService citas;

    public PublicoController(CatalogoService catalogo, DisponibilidadService disponibilidad,
                             SalonRepository salones, CitaService citas) {
        this.catalogo = catalogo;
        this.disponibilidad = disponibilidad;
        this.salones = salones;
        this.citas = citas;
    }

    @GetMapping
    public CatalogoResponse obtener(@PathVariable String slug) {
        return catalogo.obtener(slug);
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadRespuesta disponibilidad(@PathVariable String slug,
            @RequestParam Long servicioId, @RequestParam Long estilistaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return disponibilidad.consultar(slug, servicioId, estilistaId, fecha);
    }

    @PostMapping("/reservas")
    public ResponseEntity<CitaRespuesta> reservar(@PathVariable String slug,
                                                  @Valid @RequestBody CitaSolicitud solicitud) {
        Long salonId = salones.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("El salón no existe"))
                .getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(citas.agendarEnLinea(salonId, solicitud));
    }
}
