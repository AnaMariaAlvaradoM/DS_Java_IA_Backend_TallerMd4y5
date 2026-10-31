package com.devsenior.sillon.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.salon.SalonRepository;

@Service
public class AdminService {

    private final SalonRepository salonRepository;

    public AdminService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminSalonRespuesta> listarSalones() {
        return salonRepository.findAll().stream()
                .map(AdminMapper::aRespuesta)
                .toList();
    }
}
