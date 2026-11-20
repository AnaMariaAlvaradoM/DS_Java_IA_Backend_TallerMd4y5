package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.usuario.Usuario;

class ReglasAgendaTest {

    private static final LocalDate SABADO = LocalDate.of(2026, 11, 21);
    private static final LocalDate DOMINGO = SABADO.plusDays(1);
    private static final Long ESTILISTA_ID = 10L;
    private final Salon salon = new Salon();
    private ReglasAgenda reglas;

    @BeforeEach
    void prepararHorario() {
        ZoneId bogota = ZoneId.of("America/Bogota");
        Clock clock = Clock.fixed(SABADO.atTime(8, 15).atZone(bogota).toInstant(), bogota);
        reglas = new ReglasAgenda(clock);
        salon.setId(1L);
        salon.setConfiguracion(new ConfiguracionSalon(hora(8, 0), hora(11, 0), false, 30));
    }

    @Test
    void permiteEmpezarEnLaAperturaOTerminarEnElCierre() {
        assertThatCode(() -> reglas.validarHorario(salon, SABADO.atTime(8, 0), SABADO.atTime(11, 0)))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaFueraDelHorarioYOtroDiaConElHorarioEnElMensaje() {
        assertThatThrownBy(() -> reglas.validarHorario(salon, SABADO.atTime(7, 59), SABADO.atTime(9, 0)))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("08:00 a 11:00");
        assertThatThrownBy(() -> reglas.validarHorario(salon, SABADO.atTime(10, 0), SABADO.atTime(11, 1)))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("08:00 a 11:00");
        assertThatThrownBy(() -> reglas.validarHorario(salon, SABADO.atTime(10, 0), DOMINGO.atTime(10, 30)))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("08:00 a 11:00");
    }

    @Test
    void aplicaLaConfiguracionDeDomingosEnAmbasOperaciones() {
        assertThatThrownBy(() -> reglas.validarHorario(salon, DOMINGO.atTime(9, 0), DOMINGO.atTime(9, 30)))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("domingos cerrado");
        assertThat(reglas.calcularHorariosLibres(salon, ESTILISTA_ID, DOMINGO, 120, List.of())).isEmpty();

        salon.getConfiguracion().setAbreDomingos(true);
        assertThatCode(() -> reglas.validarHorario(salon, DOMINGO.atTime(9, 0), DOMINGO.atTime(9, 30)))
                .doesNotThrowAnyException();
        assertThat(reglas.calcularHorariosLibres(salon, ESTILISTA_ID, DOMINGO, 120, List.of()))
                .containsExactly(hora(8, 0), hora(8, 30), hora(9, 0));
    }

    @Test
    void descartaElPasadoYLosIniciosQueNoDejanCompletarElServicio() {
        assertThat(reglas.calcularHorariosLibres(salon, ESTILISTA_ID, SABADO.minusDays(1), 45, List.of()))
                .isEmpty();
        assertThat(reglas.calcularHorariosLibres(salon, ESTILISTA_ID, SABADO, 45, List.of()))
                .containsExactly(hora(8, 30), hora(9, 0), hora(9, 30), hora(10, 0));
    }

    @Test
    void descartaCrucesSoloConCitasOcupadasDelSalonYEstilista() {
        Salon otroSalon = new Salon();
        otroSalon.setId(2L);
        List<Cita> citas = List.of(
                cita(salon, ESTILISTA_ID, EstadoCita.PENDIENTE, hora(9, 0), hora(9, 30)),
                cita(salon, ESTILISTA_ID, EstadoCita.CONFIRMADA, hora(10, 0), hora(10, 30)),
                cita(salon, ESTILISTA_ID, EstadoCita.CANCELADA, hora(8, 30), hora(9, 0)),
                cita(salon, ESTILISTA_ID, EstadoCita.COMPLETADA, hora(9, 30), hora(10, 0)),
                cita(salon, ESTILISTA_ID, EstadoCita.NO_ASISTIO, hora(10, 30), hora(11, 0)),
                cita(salon, 11L, EstadoCita.CONFIRMADA, hora(8, 30), hora(9, 0)),
                cita(otroSalon, ESTILISTA_ID, EstadoCita.CONFIRMADA, hora(10, 30), hora(11, 0)));

        assertThat(reglas.calcularHorariosLibres(salon, ESTILISTA_ID, SABADO, 30, citas))
                .containsExactly(hora(8, 30), hora(9, 30), hora(10, 30));
        assertThat(reglas.calcularHorariosLibres(salon, ESTILISTA_ID, SABADO, 45, citas)).isEmpty();
    }

    private Cita cita(Salon propietario, Long estilistaId, EstadoCita estado, LocalTime inicio, LocalTime fin) {
        Usuario estilista = new Usuario();
        estilista.setId(estilistaId);
        Cita cita = new Cita();
        cita.setSalon(propietario);
        cita.setEstilista(estilista);
        cita.setEstado(estado);
        cita.setInicio(SABADO.atTime(inicio));
        cita.setFin(SABADO.atTime(fin));
        return cita;
    }

    private static LocalTime hora(int horas, int minutos) {
        return LocalTime.of(horas, minutos);
    }
}
