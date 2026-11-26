package com.devsenior.sillon.ia.service;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.SinCreditosException;
import com.devsenior.sillon.salon.repository.SalonRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditoIaService {
    private final SalonRepository salonRepository;

    public CreditoIaService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }
    @Transactional(readOnly = true)
    public void verificar(Long idSalon) {
        Integer creditos = salonRepository.obtenerCreditos(idSalon);
        if (creditos == null || creditos <= 0) {
            throw new SinCreditosException("No hay créditos disponibles para el salón con ID: " + idSalon);
        }

    }
    @Transactional
    public int consumirCredito(Long salonId) {
        int filasActualizadas = salonRepository.descontarCredito(salonId);
        if (filasActualizadas == 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA disponibles");
        }
        Integer restantes = salonRepository.obtenerCreditosActuales(salonId);
        return restantes != null ? restantes : 0;
    }
}
