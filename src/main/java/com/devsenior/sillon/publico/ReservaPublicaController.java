
package com.devsenior.sillon.publico;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.cita.CitaRequest;
import com.devsenior.sillon.cita.CitaResponse;
import com.devsenior.sillon.cita.CitaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publico/salones")
public class ReservaPublicaController {

    private final CitaService citaService;

    public ReservaPublicaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @PostMapping("/{slug}/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse crearReserva(
            @PathVariable String slug,
            @Valid @RequestBody CitaRequest request
    ) {
        return citaService.crearPublica(slug, request);
    }
}
