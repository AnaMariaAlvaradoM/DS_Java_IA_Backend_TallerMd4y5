package com.devsenior.sillon.salon.service;

import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.repository.SalonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Créditos de IA: un crédito por respuesta.
 * Orden de toda funcionalidad con IA: validar → verificar créditos → llamar a la IA → consumir → leer JSON → responder.
 */
@Service
public class CreditosService {

    private final SalonRepository salonRepository;

    public CreditosService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    /** 402 si el salón no tiene créditos: la IA no se llama. */
    @Transactional(readOnly = true)
    public void verificar(Long salonId) {
        int creditos = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el salón"))
                .getCreditosIa();
        if (creditos <= 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA. Compre un paquete o active el plan PRO");
        }
    }

    /** Descuenta un crédito (la IA ya respondió: los tokens ya se gastaron) y devuelve los que quedan. */
    @Transactional
    public int consumir(Long salonId) {
        if (salonRepository.consumirCredito(salonId) == 0) {
            throw new PagoRequeridoException("El salón se quedó sin créditos de IA");
        }
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el salón"))
                .getCreditosIa();
    }
}
