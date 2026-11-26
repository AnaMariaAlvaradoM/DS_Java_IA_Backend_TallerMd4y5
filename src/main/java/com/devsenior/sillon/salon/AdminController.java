package com.devsenior.sillon.salon;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Solo ADMIN (regla en SeguridadConfig): ve todos los salones de la plataforma. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SalonService salonService;

    public AdminController(SalonService salonService) {
        this.salonService = salonService;
    }

    @GetMapping("/salones")
    public List<SalonRespuesta> salones() {
        return salonService.listarTodos();
    }
}
