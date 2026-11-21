package com.devsenior.sillon.salon;

import com.devsenior.sillon.salon.dto.SalonRespuesta;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SalonRepository salonRepository;
    private final SalonService salonService;

    public AdminController(SalonRepository salonRepository, SalonService salonService) {
        this.salonRepository = salonRepository;
        this.salonService = salonService;
    }

    @GetMapping("/salones")
    public ResponseEntity<List<SalonRespuesta>> listarTodosLosSalones() {
        List<SalonRespuesta> salones = salonRepository.findAll().stream()
                .map(salonService::mapearARespuesta)
                .toList();
        return ResponseEntity.ok(salones);
    }
}
