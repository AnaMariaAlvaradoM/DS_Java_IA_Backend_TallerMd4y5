package com.devsenior.sillon.chat;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudControlador {

    private final SolicitudServicio solicitudServicio;

    public SolicitudControlador(SolicitudServicio solicitudServicio) {
        this.solicitudServicio = solicitudServicio;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SolicitudContactoRespuesta> listarSolicitudes(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return solicitudServicio.listarSolicitudes(principal.getSalonId());
    }

    @PatchMapping("/{id}/atendida")
    @ResponseStatus(HttpStatus.OK)
    public SolicitudContactoRespuesta marcarAtendida(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return solicitudServicio.marcarAtendida(id, principal.getSalonId());
    }
}
