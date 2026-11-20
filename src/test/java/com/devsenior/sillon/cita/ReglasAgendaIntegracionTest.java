package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.devsenior.sillon.compartido.ConfiguracionReloj;
import com.devsenior.sillon.compartido.ManejadorGlobalErrores;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;

@SpringJUnitConfig({ReglasAgenda.class, ConfiguracionReloj.class, ManejadorGlobalErrores.class})
@TestPropertySource(properties = "sillon.zona-horaria=America/Bogota")
class ReglasAgendaIntegracionTest {

    @Autowired private ReglasAgenda reglas;
    @Autowired private ManejadorGlobalErrores manejador;

    @Test
    void elManejadorExistenteTraduceLaValidacionDeHorarioA409() {
        Salon salon = new Salon();
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        LocalDate dia = LocalDate.of(2026, 11, 21);

        var error = assertThrows(ReglaDeNegocioException.class,
                () -> reglas.validarHorario(salon, dia.atTime(18, 45), dia.atTime(19, 30)));
        var respuesta = manejador.manejarReglaDeNegocio(error);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().codigo()).isEqualTo(409);
        assertThat(respuesta.getBody().mensaje()).contains("08:00 a 19:00");
    }
}
