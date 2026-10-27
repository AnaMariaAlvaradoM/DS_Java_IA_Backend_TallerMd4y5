package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.SalonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditosService {
    private final SalonRepository salones;

    public CreditosService(SalonRepository salones) {
        this.salones = salones;
    }

    @Transactional(readOnly = true)
    public void verificar(Long salonId) {
        if (saldo(salonId) <= 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA disponibles");
        }
    }

    @Transactional
    public int consumir(Long salonId) {
        if (salones.consumirCredito(salonId) == 0) {
            saldo(salonId);
            throw new PagoRequeridoException("El salón no tiene créditos de IA disponibles");
        }
        return saldo(salonId);
    }

    private int saldo(Long salonId) {
        return salones.consultarCreditos(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
    }
}
