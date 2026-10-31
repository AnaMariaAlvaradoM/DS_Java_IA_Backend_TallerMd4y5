package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;

class ReglasAgendaTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    // martes 2026-03-10, 10:00 a.m. hora Bogota
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T15:00:00Z"), ZONA);

    private ReglasAgenda reglasAgenda;
    private ConfiguracionSalon configuracion;

    @BeforeEach
    void setUp() {
        reglasAgenda = new ReglasAgenda(CLOCK);
        configuracion = ConfiguracionSalon.porDefecto();
    }

    @Test
    void noLanzaExcepcionDentroDelHorario() {
        LocalDateTime inicio = LocalDateTime.of(2026, 3, 10, 9, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 3, 10, 10, 0);

        assertThatCode(() -> reglasAgenda.validarHorario(configuracion, inicio, fin)).doesNotThrowAnyException();
    }

    @Test
    void lanzaExcepcionSiTerminaDespuesDelCierre() {
        LocalDateTime inicio = LocalDateTime.of(2026, 3, 10, 18, 30);
        LocalDateTime fin = LocalDateTime.of(2026, 3, 10, 19, 30);

        assertThatThrownBy(() -> reglasAgenda.validarHorario(configuracion, inicio, fin))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void lanzaExcepcionSiEmpiezaAntesDeLaApertura() {
        LocalDateTime inicio = LocalDateTime.of(2026, 3, 10, 7, 30);
        LocalDateTime fin = LocalDateTime.of(2026, 3, 10, 8, 30);

        assertThatThrownBy(() -> reglasAgenda.validarHorario(configuracion, inicio, fin))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void lanzaExcepcionDomingoCerrado() {
        LocalDateTime inicio = LocalDateTime.of(2026, 3, 8, 10, 0); // domingo
        LocalDateTime fin = LocalDateTime.of(2026, 3, 8, 11, 0);

        assertThatThrownBy(() -> reglasAgenda.validarHorario(configuracion, inicio, fin))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void permiteDomingoSiElSalonAbreDomingos() {
        ConfiguracionSalon conAbreDomingos = new ConfiguracionSalon(LocalTime.of(8, 0), LocalTime.of(19, 0), true,
                30);
        LocalDateTime inicio = LocalDateTime.of(2026, 3, 8, 10, 0); // domingo
        LocalDateTime fin = LocalDateTime.of(2026, 3, 8, 11, 0);

        assertThatCode(() -> reglasAgenda.validarHorario(conAbreDomingos, inicio, fin)).doesNotThrowAnyException();
    }

    @Test
    void calcularHorariosLibresExcluyeLosQueSeCruzanConUnaCitaOcupada() {
        LocalDate fecha = LocalDate.of(2026, 3, 11); // miercoles, fecha futura
        Cita ocupada = new Cita();
        ocupada.setInicio(LocalDateTime.of(2026, 3, 11, 10, 0));
        ocupada.setFin(LocalDateTime.of(2026, 3, 11, 10, 30));

        List<LocalTime> libres = reglasAgenda.calcularHorariosLibres(configuracion, fecha, 30, List.of(ocupada));

        assertThat(libres).doesNotContain(LocalTime.of(10, 0));
    }

    @Test
    void unaCitaQueTerminaALas1045NoCruzaConOtraQueEmpiezaALas1045() {
        // intervalo de 15 minutos para que exista un slot exactamente en 10:45
        ConfiguracionSalon conIntervaloDe15 = new ConfiguracionSalon(LocalTime.of(8, 0), LocalTime.of(19, 0), false,
                15);
        LocalDate fecha = LocalDate.of(2026, 3, 11);
        Cita ocupada = new Cita();
        ocupada.setInicio(LocalDateTime.of(2026, 3, 11, 10, 15));
        ocupada.setFin(LocalDateTime.of(2026, 3, 11, 10, 45));

        List<LocalTime> libres = reglasAgenda.calcularHorariosLibres(conIntervaloDe15, fecha, 30, List.of(ocupada));

        assertThat(libres).contains(LocalTime.of(10, 45));
    }

    @Test
    void unDiaCerradoNoTieneHorariosLibres() {
        LocalDate domingo = LocalDate.of(2026, 3, 8);

        List<LocalTime> libres = reglasAgenda.calcularHorariosLibres(configuracion, domingo, 30, List.of());

        assertThat(libres).isEmpty();
    }

    @Test
    void hoyNoOfreceHorasPasadas() {
        LocalDate hoy = LocalDate.of(2026, 3, 10); // segun CLOCK, son las 10:00 a.m.

        List<LocalTime> libres = reglasAgenda.calcularHorariosLibres(configuracion, hoy, 30, List.of());

        assertThat(libres).doesNotContain(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(9, 30));
        assertThat(libres).contains(LocalTime.of(10, 0));
    }
}
