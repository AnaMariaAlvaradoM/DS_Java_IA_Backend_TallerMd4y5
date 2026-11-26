package com.devsenior.sillon.salon;

public final class SalonMapper {

    private SalonMapper() {
    }

    public static SalonRespuesta aRespuesta(Salon salon) {
        ConfiguracionSalon c = salon.getConfiguracion();
        ConfiguracionRespuesta configuracion = new ConfiguracionRespuesta(c.getHoraApertura(), c.getHoraCierre(),
                c.isAbreDomingos(), c.getIntervaloMinutos());
        return new SalonRespuesta(salon.getId(), salon.getNombre(), salon.getSlug(), salon.getCiudad(),
                salon.getDireccion(), salon.getTelefono(), salon.getPlan(), salon.getCreditosIa(), configuracion,
                salon.getFechaCreacion());
    }
}
