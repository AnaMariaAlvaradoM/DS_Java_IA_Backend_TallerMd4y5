package com.devsenior.sillon.salon.controller;

import com.devsenior.sillon.salon.dto.ConfiRequest;
import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.service.SalonService;
import com.devsenior.sillon.security.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/salon")
@PreAuthorize("hasRole('DUENO')")
public class SalonController {
    private final SalonService salonService;

    public SalonController(SalonService salonService) {
        this.salonService = salonService;
    }
    @GetMapping
    public ResponseEntity<SalonResponse> obtenerSalonPorId(@AuthenticationPrincipal UsuarioPrincipal usuario){
        Long salonIdDelUsuario = usuario.getSalonId();

        SalonResponse respuesta = salonService.obtenerSalonPorId(salonIdDelUsuario);
        return ResponseEntity.ok(respuesta);
    }
    @PutMapping("/configuracion")
    public ResponseEntity<SalonResponse> actualizarConfiguracion(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ConfiRequest dto
    ) {
        // Se usa siempre el salonId del usuario autenticado
        Long salonIdDelUsuario = usuario.getSalonId();

        SalonResponse respuesta = salonService.actualizarSalon(salonIdDelUsuario, dto);
        return ResponseEntity.ok(respuesta);
    }
}



