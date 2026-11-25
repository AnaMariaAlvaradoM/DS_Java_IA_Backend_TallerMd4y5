package com.devsenior.sillon.ia;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class CreditosIaServicio {

    private final SalonRepository salonRepository;

    public CreditosIaServicio(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    /**
     * Valida si el salón tiene créditos disponibles.
     * Si tiene 0 créditos, lanza 402 sin invocar a la IA.
     */
    public void verificarCreditos(Salon salon) {
        if (salon == null || salon.getCreditosIa() <= 0) {
            throw new PagoRequeridoException("El salón no cuenta con créditos de IA disponibles.");
        }
    }

    /**
     * Consume un crédito de forma atómica en la base de datos.
     * Si no quedaban créditos, lanza 402 y no decrementa por debajo de 0.
     * @return Los créditos restantes después de consumir.
     */
    @Transactional
    public int consumirCredito(Long salonId) {
        int filasAfectadas = salonRepository.descontarCredito(salonId);
        if (filasAfectadas == 0) {
            throw new PagoRequeridoException("El salón no cuenta con créditos de IA disponibles.");
        }

        return salonRepository.findById(salonId)
                .map(Salon::getCreditosIa)
                .orElse(0);
    }
}
