
package com.devsenior.sillon.ia.servicio;

import org.springframework.stereotype.Service;

import com.devsenior.sillon.compartido.excepcion.ExcepcionPagoRequerido;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;

@Service
public class CreditoIaServicio {

    private final SalonRepositorio salonRepositorio;

    public CreditoIaServicio(
            SalonRepositorio salonRepositorio) {

        this.salonRepositorio = salonRepositorio;
    }

    public void verificarSaldo(Long salonId) {

        Salon salon = obtenerSalon(salonId);

        if (salon.getCreditosIa() <= 0) {
            throw new ExcepcionPagoRequerido(
                    "El salon no tiene creditos de IA disponibles");
        }
    }

    public int consumirCredito(Long salonId) {

        int filasActualizadas =
                salonRepositorio.descontarCreditoIa(salonId);

        if (filasActualizadas != 1) {

            Salon salon = obtenerSalon(salonId);

            if (salon.getCreditosIa() <= 0) {
                throw new ExcepcionPagoRequerido(
                        "El salon no tiene creditos de IA disponibles");
            }

            throw new IllegalStateException(
                    "No fue posible descontar el credito de IA");
        }

        Salon salonActualizado = obtenerSalon(salonId);

        return salonActualizado.getCreditosIa();
    }

    private Salon obtenerSalon(Long salonId) {

        return salonRepositorio.findById(salonId)
                .orElseThrow(
                        () -> new ExcepcionRecursoNoEncontrado(
                                "Salon no encontrado"));
    }
}
