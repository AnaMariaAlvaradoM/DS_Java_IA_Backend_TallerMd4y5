package com.devsenior.sillon.ia.servicio;

import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditosIaService {

    private final SalonRepository salonRepository;

    public CreditosIaService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    @Transactional(readOnly = true)
    public void verificar(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        if (salon.getCreditosIa() <= 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA disponibles");
        }
    }

    @Transactional
    public int consumir(Long salonId) {
        int actualizados = salonRepository.descontarCreditoAtomico(salonId);
        if (actualizados == 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA disponibles");
        }
        return salonRepository.findById(salonId)
                .map(Salon::getCreditosIa)
                .orElse(0);
    }
}