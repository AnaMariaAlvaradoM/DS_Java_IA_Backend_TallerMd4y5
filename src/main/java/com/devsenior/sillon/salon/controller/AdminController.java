package com.devsenior.sillon.salon.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.service.SalonService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Equipo de Sillón (ADMIN). El primer ADMIN se crea cambiando el rol directamente en la base de datos. */
@Tag(name = "9 Administración", description = "Equipo de Sillón (rol ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SalonService salonService;

    public AdminController(SalonService salonService) {
        this.salonService = salonService;
    }

    @GetMapping("/salones")
    public List<SalonResponse> salones() {
        return salonService.listarTodos();
    }
}
