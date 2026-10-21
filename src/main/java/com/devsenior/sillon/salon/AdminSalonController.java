package com.devsenior.sillon.salon;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "9 Administración")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/salones")
public class AdminSalonController {
    private final SalonService salones;

    public AdminSalonController(SalonService salones) { this.salones = salones; }

    @GetMapping
    public List<SalonResponse> listar() { return salones.listarTodos(); }
}
