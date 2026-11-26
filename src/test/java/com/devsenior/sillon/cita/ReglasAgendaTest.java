package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;

class ReglasAgendaTest {

    private final ReglasAgenda reglas = new ReglasAgenda();
    private final ConfiguracionSalon config = ConfiguracionSalon.porDefecto(); // 8:00 a 19:00, sin domingos, 30 min
    private final LocalDate martes = LocalDate.of(2026, 10, 20);
    private final LocalDate domingo = LocalDate.of(2026, 10, 25);
    private final LocalDateTime antesDeTodo = LocalDateTime.of(2026, 10, 1, 0, 0);

    @Test
    void citaDentroDelHorarioEsValida() {
        assertThatCode(() -> reglas.validarHorario(config, martes.atTime(10, 30), martes.atTime(11, 15)))
                .doesNotThrowAnyException();
    }

    @Test
    void citaQueTerminaDespuesDelCierreEsRechazada() {
        assertThatThrownBy(() -> reglas.validarHorario(config, martes.atTime(18, 45), martes.atTime(19, 30)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void citaQueEmpiezaAntesDeAbrirEsRechazada() {
        assertThatThrownBy(() -> reglas.validarHorario(config, martes.atTime(7, 30), martes.atTime(8, 15)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void citaEnDomingoCerradoEsRechazada() {
        assertThatThrownBy(() -> reglas.validarHorario(config, domingo.atTime(10, 0), domingo.atTime(10, 45)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("domingos");
    }

    @Test
    void horariosLibresExcluyenLosCruces() {
        Cita valentina = new Cita();
        valentina.setInicio(martes.atTime(10, 0));
        valentina.setFin(martes.atTime(10, 45));

        List<LocalTime> libres = reglas.horariosLibres(config, martes, 45, List.of(valentina), antesDeTodo);

        assertThat(libres).doesNotContain(LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30));
        assertThat(libres).contains(LocalTime.of(9, 0), LocalTime.of(11, 0));
    }

    @Test
    void elServicioDebeCaberCompletoAntesDelCierre() {
        List<LocalTime> libres = reglas.horariosLibres(config, martes, 45, List.of(), antesDeTodo);
        assertThat(libres).first().isEqualTo(LocalTime.of(8, 0));
        assertThat(libres).last().isEqualTo(LocalTime.of(18, 0));
        assertThat(libres).doesNotContain(LocalTime.of(18, 30));
    }

    @Test
    void unDiaCerradoNoTieneHorarios() {
        assertThat(reglas.horariosLibres(config, domingo, 45, List.of(), antesDeTodo)).isEmpty();
    }

    @Test
    void hoyNoOfreceHorasPasadas() {
        LocalDateTime ahora = martes.atTime(12, 10);
        List<LocalTime> libres = reglas.horariosLibres(config, martes, 30, List.of(), ahora);
        assertThat(libres).first().isEqualTo(LocalTime.of(12, 30));
        assertThat(libres).doesNotContain(LocalTime.of(12, 0), LocalTime.of(8, 0));
    }

    @Test
    void unaCitaQueTerminaALas1045NoSeCruzaConOtraQueEmpiezaALas1045() {
        assertThat(ReglasAgenda.seCruzan(martes.atTime(10, 0), martes.atTime(10, 45), martes.atTime(10, 45),
                martes.atTime(11, 15))).isFalse();
        assertThat(ReglasAgenda.seCruzan(martes.atTime(10, 0), martes.atTime(10, 45), martes.atTime(10, 30),
                martes.atTime(11, 0))).isTrue();
    }

    @Test
    void elHorarioEnTextoUsaEspaciosNormales() {
        String texto = ReglasAgenda.horarioTexto(config);
        assertThat(texto).startsWith("Lunes a sábado (domingos cerrado), de 8:00").contains("7:00");
        assertThat(texto).doesNotContain("\u202F").doesNotContain("\u00A0");
    }
}
