package com.devsenior.sillon.usuario;

import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.service.SalonService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/salon")
@Tag(name = "9 Administración")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private SalonService salonService;

    public AdminController(SalonService salonService) {
        this.salonService = salonService;
    }
    @GetMapping
    public List<SalonResponse> obtenerTodosLosSalones() {
        return salonService.obtenerTodosLosSalones();
    }
}
