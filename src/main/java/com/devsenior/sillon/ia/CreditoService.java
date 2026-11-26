package com.devsenior.sillon.ia;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.salon.SalonRepository;

/** Un crédito = una respuesta de la IA. */
@Service
public class CreditoService {

    private final SalonRepository salonRepository;

    public CreditoService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    /** Antes de llamar a la IA: sin créditos → 402 y la IA no se llama. */
    @Transactional(readOnly = true)
    public void verificar(Long salonId) {
        int creditos = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("El salón no existe"))
                .getCreditosIa();
        if (creditos <= 0) {
            throw new PagoRequeridoException("El salón no tiene créditos de IA. Compra un paquete o pasa al plan PRO");
        }
    }

    /**
     * Después de que la IA respondió: resta 1 con una sola actualización en la base de datos
     * (solo si quedan créditos), así dos peticiones simultáneas nunca dejan créditos negativos.
     */
    @Transactional
    public int consumir(Long salonId) {
        if (salonRepository.descontarCredito(salonId) == 0) {
            throw new PagoRequeridoException("El salón se quedó sin créditos de IA");
        }
        return salonRepository.findById(salonId).orElseThrow().getCreditosIa();
    }
}
