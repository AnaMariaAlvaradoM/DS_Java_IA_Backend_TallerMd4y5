package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.salon.Salon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditosIaService {
    private final CreditoIaRepository salones;

    public CreditosIaService(CreditoIaRepository salones) {
        this.salones = salones;
    }

    @Transactional(readOnly = true)
    public void verificar(Long salonId) {
        Salon salon = salones.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        if (salon.getCreditosIa() <= 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA");
        }
    }

    @Transactional
    public int consumir(Long salonId) {
        if (salones.consumirSiDisponible(salonId) != 1) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA");
        }
        return salones.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"))
                .getCreditosIa();
    }
}
