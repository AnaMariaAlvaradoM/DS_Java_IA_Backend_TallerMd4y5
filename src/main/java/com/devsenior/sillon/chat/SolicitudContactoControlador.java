package com.devsenior.sillon.chat;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudContactoControlador {
    private final SolicitudContactoServicio solicitudes;

    public SolicitudContactoControlador(SolicitudContactoServicio solicitudes) {
        this.solicitudes = solicitudes;
    }

    @GetMapping
    public List<SolicitudContactoRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return solicitudes.listar(usuario.getSalonId());
    }

    @PatchMapping("/{id}/atendida")
    public SolicitudContactoRespuesta marcarAtendida(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                     @PathVariable Long id) {
        return solicitudes.marcarAtendida(id, usuario.getSalonId());
    }
}
