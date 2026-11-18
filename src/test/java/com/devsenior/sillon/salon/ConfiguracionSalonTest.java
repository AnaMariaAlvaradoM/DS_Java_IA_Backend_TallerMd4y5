package com.devsenior.sillon.salon;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class ConfiguracionSalonTest {

    @Test
    void laConfiguracionPorDefectoEsDe8A19SinDomingosYCada30Minutos() {
        ConfiguracionSalon configuracion = ConfiguracionSalon.porDefecto();

        assertThat(configuracion.getHoraApertura()).isEqualTo(LocalTime.of(8, 0));
        assertThat(configuracion.getHoraCierre()).isEqualTo(LocalTime.of(19, 0));
        assertThat(configuracion.isAbreDomingos()).isFalse();
        assertThat(configuracion.getIntervaloMinutos()).isEqualTo(30);
    }
}
