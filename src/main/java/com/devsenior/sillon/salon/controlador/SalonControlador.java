package com.devsenior.sillon.salon.controlador;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.salon.dto.ConfiguracionSalonDto;
import com.devsenior.sillon.salon.dto.RespuestaSalon;
import com.devsenior.sillon.salon.servicio.SalonServicio;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/salon")
public class SalonControlador {

    private final SalonServicio salonServicio;

    public SalonControlador(SalonServicio salonServicio) {
        this.salonServicio = salonServicio;
    }

    @GetMapping
    public ResponseEntity<RespuestaSalon> consultar(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        return ResponseEntity.ok(
                salonServicio.consultar(usuario.getSalonId()));
    }

    @PutMapping("/configuracion")
    public ResponseEntity<RespuestaSalon> actualizarConfiguracion(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ConfiguracionSalonDto solicitud) {

        return ResponseEntity.ok(
                salonServicio.actualizarConfiguracion(
                        usuario.getSalonId(),
                        solicitud));
    }

}