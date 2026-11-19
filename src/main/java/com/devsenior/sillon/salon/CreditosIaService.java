package com.devsenior.sillon.salon;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;

@Service
public class CreditosIaService {

    private final SalonRepository salones;

    public CreditosIaService(SalonRepository salones) {
        this.salones = salones;
    }

    @Transactional(readOnly = true)
    public void verificar(Long salonId) {
        Salon salon = salones.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        if (salon.getCreditosIa() <= 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA");
        }
    }

    @Transactional
    public int consumir(Long salonId) {
        if (salones.consumirCredito(salonId) == 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA");
        }
        return salones.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"))
                .getCreditosIa();
    }
}
