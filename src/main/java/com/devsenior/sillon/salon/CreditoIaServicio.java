package com.devsenior.sillon.salon;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PagoRequeridoException;

@Service
public class CreditoIaServicio {

    private static final String MENSAJE_SALON_NO_ENCONTRADO = "Salón no encontrado";
    private static final String MENSAJE_SIN_CREDITOS = "Sin créditos de IA: compra un paquete o activa el plan PRO";

    private final SalonRepository salonRepository;

    public CreditoIaServicio(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    public void verificar(Long salonId) {
        Salon salon = buscarSalon(salonId);
        if (salon.getCreditosIa() <= 0) {
            throw new PagoRequeridoException(MENSAJE_SIN_CREDITOS);
        }
    }

    @Transactional
    public int consumir(Long salonId) {
        buscarSalon(salonId);
        int filas = salonRepository.consumirCredito(salonId);
        if (filas == 0) {
            throw new PagoRequeridoException(MENSAJE_SIN_CREDITOS);
        }
        return buscarSalon(salonId).getCreditosIa();
    }

    private Salon buscarSalon(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_SALON_NO_ENCONTRADO));
    }
}
