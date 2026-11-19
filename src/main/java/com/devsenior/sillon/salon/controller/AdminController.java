package com.devsenior.sillon.salon.controller;

import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.service.SalonService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Equipo de Sillón (ADMIN). El primer ADMIN se crea cambiando el rol directamente en la base de datos. */
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
