package com.devsenior.sillon.salon;

public class SalonMapper {

    private SalonMapper() {
    }

    public static SalonRespuesta aRespuesta(Salon salon) {
        ConfiguracionSalon configuracion = salon.getConfiguracion();
        ConfiguracionRespuesta configuracionRespuesta = new ConfiguracionRespuesta(
                configuracion.getHoraApertura(), configuracion.getHoraCierre(), configuracion.isAbreDomingos(),
                configuracion.getIntervaloMinutos());

        return new SalonRespuesta(salon.getId(), salon.getNombre(), salon.getSlug(), salon.getCiudad(),
                salon.getDireccion(), salon.getTelefono(), salon.getPlan(), salon.getCreditosIa(),
                configuracionRespuesta, salon.getFechaCreacion());
    }
}
