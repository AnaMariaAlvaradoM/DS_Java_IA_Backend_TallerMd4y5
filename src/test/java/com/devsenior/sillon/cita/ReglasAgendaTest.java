package com.devsenior.sillon.cita;

import com.devsenior.sillon.compartido.ConflictoExcepcion;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Timeout(value = 3, unit = TimeUnit.SECONDS)
class ReglasAgendaTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZONA);

    private ConfiguracionSalon configuracion;
    private ReglasAgenda reglasAgenda;

    @BeforeEach
    void setUp() {
        reglasAgenda = new ReglasAgenda();
        // Usamos una instancia real para evitar bucles de 0 minutos por atributos no stubbeados
        configuracion = ConfiguracionSalon.crearPorDefecto();
    }

    @Test
    @DisplayName("1. Cita en horario válido (Lunes 10:00 a 10:30): no lanza excepción")
    void citaEnHorarioValido() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 9, 10, 0);
        LocalDateTime fin = inicio.plusMinutes(30);

        assertDoesNotThrow(() -> reglasAgenda.validarHorario(configuracion, inicio, fin));
    }

    @Test
    @DisplayName("2. Domingo: lanza ConflictoExcepcion por salón cerrado")
    void citaEnDomingoFalla() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 15, 10, 0);
        LocalDateTime fin = inicio.plusMinutes(30);

        assertThrows(ConflictoExcepcion.class, () -> reglasAgenda.validarHorario(configuracion, inicio, fin));
    }

    @Test
    @DisplayName("3. Cita que empieza antes de la hora de apertura: lanza ConflictoExcepcion")
    void citaAntesDeAperturaFalla() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 9, 7, 30);
        LocalDateTime fin = inicio.plusMinutes(30);

        assertThrows(ConflictoExcepcion.class, () -> reglasAgenda.validarHorario(configuracion, inicio, fin));
    }

    @Test
    @DisplayName("4. Cita que termina después del cierre: lanza ConflictoExcepcion")
    void citaDespuesDeCierreFalla() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 9, 18, 45);
        LocalDateTime fin = inicio.plusMinutes(30);

        assertThrows(ConflictoExcepcion.class, () -> reglasAgenda.validarHorario(configuracion, inicio, fin));
    }

    @Test
    @DisplayName("5. Cita que empieza y termina en días distintos: lanza ConflictoExcepcion")
    void citaEnDiasDistintosFalla() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 9, 23, 45);
        LocalDateTime fin = inicio.plusMinutes(45);

        assertThrows(ConflictoExcepcion.class, () -> reglasAgenda.validarHorario(configuracion, inicio, fin));
    }

    @Test
    @DisplayName("6. Cita que termina exactamente a la hora de cierre: debe ser válida")
    void citaTerminaExactoAlCierreEsValida() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 9, 18, 30);
        LocalDateTime fin = inicio.plusMinutes(30);

        assertDoesNotThrow(() -> reglasAgenda.validarHorario(configuracion, inicio, fin));
    }

    @Test
    @DisplayName("7. Horarios libres de un día sin citas: inicia a las 08:00:00 y no supera el cierre")
    void horariosLibresDiaSinCitas() {
        LocalDate fechaLunes = LocalDate.of(2026, 11, 9);
        LocalDateTime ahoraPasado = LocalDateTime.of(2026, 11, 9, 7, 0);

        List<String> libres = reglasAgenda.calcularHorariosLibres(
                configuracion,
                fechaLunes,
                30,
                List.of(),
                ahoraPasado
        );

        assertFalse(libres.isEmpty(), "La lista de horarios no debe estar vacía");
        assertTrue(libres.contains("08:00:00"), "Debe incluir el primer turno de las 08:00:00");
    }

    @Test
    @DisplayName("8. Con una cita ocupada de 10:00 a 10:30: '10:00:00' no debe aparecer en la lista")
    void horarioOcupadoEsExcluido() {
        LocalDate fechaLunes = LocalDate.of(2026, 11, 9);
        LocalDateTime ahoraPasado = LocalDateTime.of(2026, 11, 9, 7, 0);

        Cita citaOcupada = mock(Cita.class);
        when(citaOcupada.getInicio()).thenReturn(LocalDateTime.of(2026, 11, 9, 10, 0));
        when(citaOcupada.getFin()).thenReturn(LocalDateTime.of(2026, 11, 9, 10, 30));

        List<String> libres = reglasAgenda.calcularHorariosLibres(
                configuracion,
                fechaLunes,
                30,
                List.of(citaOcupada),
                ahoraPasado
        );

        assertFalse(libres.contains("10:00:00"), "El turno de las 10:00:00 debe estar excluido");
    }

    @Test
    @DisplayName("9. Con ahora a media mañana: ninguna hora anterior a ahora aparece")
    void noMuestraHorariosPasados() {
        LocalDate fechaLunes = LocalDate.of(2026, 11, 9);
        LocalDateTime ahora = LocalDateTime.now(RELOJ); // 10:00 AM Bogota

        List<String> libres = reglasAgenda.calcularHorariosLibres(
                configuracion,
                fechaLunes,
                30,
                List.of(),
                ahora
        );

        assertFalse(libres.contains("08:00:00"), "No debe incluir las 08:00:00 si son las 10:00 AM");
        assertFalse(libres.contains("09:30:00"), "No debe incluir las 09:30:00 si son las 10:00 AM");
    }

    @Test
    @DisplayName("10. Domingo: lista de horarios libres debe ser vacía")
    void domingoListaHorariosVacia() {
        LocalDate fechaDomingo = LocalDate.of(2026, 11, 15);
        LocalDateTime ahoraPasado = LocalDateTime.of(2026, 11, 15, 7, 0);

        List<String> libres = reglasAgenda.calcularHorariosLibres(
                configuracion,
                fechaDomingo,
                30,
                List.of(),
                ahoraPasado
        );

        assertTrue(libres.isEmpty(), "La lista de slots en domingo debe estar vacía");
    }
}