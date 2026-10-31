package com.devsenior.sillon.ia;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class CreditosService {

    private final SalonRepository salonRepository;

    public CreditosService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    public void verificar(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        if (salon.getCreditosIa() <= 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA disponibles");
        }
    }

    @Transactional
    public int consumir(Long salonId) {
        int filas = salonRepository.restarCredito(salonId);
        if (filas == 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA disponibles");
        }
        return salonRepository.findById(salonId)
                .map(Salon::getCreditosIa)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
    }
}
