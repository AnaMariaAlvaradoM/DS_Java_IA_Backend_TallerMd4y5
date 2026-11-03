package com.devsenior.sillon.salon;

import com.devsenior.sillon.salon.dto.SalonResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SalonService salonService;

    public AdminController(SalonService salonService) {
        this.salonService = salonService;
    }

    @GetMapping("/salones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SalonResponseDto>> listarSalones() {
        List<SalonResponseDto> salones = salonService.listarTodos();
        return ResponseEntity.ok(salones);
    }
}
