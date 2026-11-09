package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.salon.SalonRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditosIaServicio {
    private final SalonRepositorio salones;

    public CreditosIaServicio(SalonRepositorio salones) { this.salones = salones; }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public int verificar(Long salonId) {
        int creditos = salones.consultarCreditosIa(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        if (creditos <= 0) throw new PagoRequeridoException("El salón no tiene créditos de IA");
        return creditos;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int consumir(Long salonId) {
        if (salones.consumirCreditoIa(salonId) != 1)
            throw new PagoRequeridoException("El salón no tiene créditos de IA");
        return salones.consultarCreditosIa(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
    }
}
