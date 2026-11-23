
package com.devsenior.sillon.solicitud;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/solicitudes")
@Tag(
    name = "Solicitudes de contacto",
    description = "Consulta y gestión de solicitudes escaladas por el chatbot a atención humana"
)
public class ControladorSolicitudesContacto {

    private final ServicioSolicitudesContacto servicio;

    public ControladorSolicitudesContacto(
            ServicioSolicitudesContacto servicio
    ) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RespuestaSolicitudContacto> listar(
            @AuthenticationPrincipal UsuarioPrincipal usuario
    ) {

        return servicio.listar(usuario.getSalonId())
                .stream()
                .map(RespuestaSolicitudContacto::desde)
                .toList();
    }

    @PatchMapping("/{id}/atendida")
    public RespuestaSolicitudContacto marcarAtendida(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id
    ) {

        SolicitudContacto solicitud = servicio.marcarAtendida(
                usuario.getSalonId(),
                id
        );

        return RespuestaSolicitudContacto.desde(solicitud);
    }
}
