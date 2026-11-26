package com.devsenior.sillon.salon.controller;

import com.devsenior.sillon.salon.dto.SalonAdminResponse;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.devsenior.sillon.salon.service.SalonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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
    private SalonRepository salonRepository;

    public AdminController(SalonService salonService, SalonRepository salonRepository) {
        this.salonService = salonService;
        this.salonRepository = salonRepository;
    }

    @GetMapping("/salones")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos los salones registrados")
    public ResponseEntity<List<SalonAdminResponse>> listarSalones() {
        List<SalonAdminResponse> salones = salonRepository.findAll()
                .stream()
                .map(SalonAdminResponse::desdeEntidad)
                .toList();
        return ResponseEntity.ok(salones);
    }
}
