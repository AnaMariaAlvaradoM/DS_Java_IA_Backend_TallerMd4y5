
package com.devsenior.sillon.salon;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/salon")
public class ControladorSalon {

    private final ServicioSalon servicioSalon;

    public ControladorSalon(ServicioSalon servicioSalon) {
        this.servicioSalon = servicioSalon;
    }

    @GetMapping
    public ResponseEntity<RespuestaSalon> consultar(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        RespuestaSalon respuesta =
                servicioSalon.consultar(usuario.getSalonId());

        return ResponseEntity.ok(respuesta);
    }


@PutMapping("/configuracion")
public ResponseEntity<RespuestaSalon> actualizarConfiguracion(
        @AuthenticationPrincipal UsuarioPrincipal usuario,
        @Valid @RequestBody SolicitudConfiguracionSalon solicitud) {

    RespuestaSalon respuesta =
            servicioSalon.actualizarConfiguracion(
                    usuario.getSalonId(),
                    solicitud
            );

    return ResponseEntity.ok(respuesta);
}

}
