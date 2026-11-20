package com.devsenior.sillon.salon.controlador;

import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.repositorio.SalonRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SalonRepository salonRepository;

    public AdminController(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    @GetMapping("/salones")
    public ResponseEntity<List<SalonResponse>> listarSalones() {
        List<SalonResponse> salones = salonRepository.findAll().stream()
                .map(SalonResponse::desde)
                .toList();
        return ResponseEntity.ok(salones);
    }
}