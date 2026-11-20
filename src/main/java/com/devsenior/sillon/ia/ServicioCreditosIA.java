
package com.devsenior.sillon.ia;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class ServicioCreditosIA {

    private final SalonRepository salonRepository;

    public ServicioCreditosIA(
            SalonRepository salonRepository
    ) {
        this.salonRepository = salonRepository;
    }

    @Transactional(readOnly = true)
    public void verificar(Long salonId) {

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Salón no encontrado"
                ));

        if (salon.getCreditosIa() <= 0) {
            throw new PagoRequeridoException(
                    "El salón no tiene créditos de IA disponibles"
            );
        }
    }

    @Transactional
    public int consumir(Long salonId) {

        int filasActualizadas =
                salonRepository.consumirCreditoIa(salonId);

        if (filasActualizadas == 0) {

            if (!salonRepository.existsById(salonId)) {
                throw new RecursoNoEncontradoException(
                        "Salón no encontrado"
                );
            }

            throw new PagoRequeridoException(
                    "El salón no tiene créditos de IA disponibles"
            );
        }

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Salón no encontrado"
                ));

        return salon.getCreditosIa();
    }
}
