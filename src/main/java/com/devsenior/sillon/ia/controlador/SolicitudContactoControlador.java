
package com.devsenior.sillon.ia.controlador;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.ia.dto.RespuestaSolicitudContacto;
import com.devsenior.sillon.ia.servicio.SolicitudContactoServicio;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/solicitudes")
@Tag(name = "8 Solicitudes", description = "Gestión y seguimiento de solicitudes de contacto")
@SecurityRequirement(name = "Bearer JWT")
public class SolicitudContactoControlador {

    private final SolicitudContactoServicio solicitudContactoServicio;

    public SolicitudContactoControlador(
            SolicitudContactoServicio solicitudContactoServicio) {

        this.solicitudContactoServicio = solicitudContactoServicio;
    }

    @GetMapping
    public ResponseEntity<List<RespuestaSolicitudContacto>> listar(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        List<RespuestaSolicitudContacto> solicitudes = solicitudContactoServicio.listar(
                usuario.getSalonId());

        return ResponseEntity.ok(solicitudes);
    }

    @PatchMapping("/{id}/atendida")
    public ResponseEntity<RespuestaSolicitudContacto> marcarAtendida(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id) {

        RespuestaSolicitudContacto respuesta = solicitudContactoServicio.marcarAtendida(
                usuario.getSalonId(),
                id);

        return ResponseEntity.ok(respuesta);
    }
}
