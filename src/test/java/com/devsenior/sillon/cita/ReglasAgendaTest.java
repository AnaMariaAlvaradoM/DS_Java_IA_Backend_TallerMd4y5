package com.devsenior.sillon.cita;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;

class ReglasAgendaTest {

    private ReglasAgenda reglasAgenda;
    private ConfiguracionSalon configuracion;

    @BeforeEach
    void setUp() {
        reglasAgenda = new ReglasAgenda();
        // Horario estandar: 8:00 a 19:00, no abre domingos, intervalo 30 minutos
        configuracion = ConfiguracionSalon.porDefecto();
    }

    @Test
    @DisplayName("Cita dentro del horario y día hábil es válida")
    void validarHorario_dentroDelHorario_esValida() {
        // Martes a las 10:00 (duracion 45 minutos -> termina 10:45)
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 10, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 10, 20, 10, 45);

        assertDoesNotThrow(() -> reglasAgenda.validarHorario(inicio, fin, configuracion));
    }

    @Test
    @DisplayName("Cita que empieza antes de la apertura lanza 409")
    void validarHorario_empiezaAntesDeApertura_lanza409() {
        // Inicia a las 7:30 (apertura es 8:00)
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 7, 30);
        LocalDateTime fin = LocalDateTime.of(2026, 10, 20, 8, 15);

        assertThrows(ReglaNegocioException.class, () -> reglasAgenda.validarHorario(inicio, fin, configuracion));
    }

    @Test
    @DisplayName("Cita que termina después del cierre lanza 409")
    void validarHorario_terminaDespuesDelCierre_lanza409() {
        // Inicia a las 18:45 con 45 min -> termina a las 19:30 (cierre es 19:00)
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 18, 45);
        LocalDateTime fin = LocalDateTime.of(2026, 10, 20, 19, 30);

        assertThrows(ReglaNegocioException.class, () -> reglasAgenda.validarHorario(inicio, fin, configuracion));
    }

    @Test
    @DisplayName("Cita un domingo cuando el salón no abre lanza 409")
    void validarHorario_domingoCerrado_lanza409() {
        // Domingo 25 de octubre de 2026
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 25, 10, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 10, 25, 10, 45);

        assertThrows(ReglaNegocioException.class, () -> reglasAgenda.validarHorario(inicio, fin, configuracion));
    }

    @Test
    @DisplayName("Cruce de citas: inicio1 < fin2 y fin1 > inicio2")
    void seCruzan_reglaExacta() {
        LocalDateTime i1 = LocalDateTime.of(2026, 10, 20, 10, 0);
        LocalDateTime f1 = LocalDateTime.of(2026, 10, 20, 10, 45);

        // Caso 1: empieza a las 10:30 y termina a las 11:00 -> cruzan
        LocalDateTime i2 = LocalDateTime.of(2026, 10, 20, 10, 30);
        LocalDateTime f2 = LocalDateTime.of(2026, 10, 20, 11, 0);
        assertTrue(reglasAgenda.seCruzan(i1, f1, i2, f2));

        // Caso 2: empieza exactamente cuando termina la anterior (10:45 a 11:30) -> NO cruzan
        LocalDateTime i3 = LocalDateTime.of(2026, 10, 20, 10, 45);
        LocalDateTime f3 = LocalDateTime.of(2026, 10, 20, 11, 30);
        assertFalse(reglasAgenda.seCruzan(i1, f1, i3, f3));

        // Caso 3: termina exactamente cuando empieza la otra (9:15 a 10:00) -> NO cruzan
        LocalDateTime i4 = LocalDateTime.of(2026, 10, 20, 9, 15);
        LocalDateTime f4 = LocalDateTime.of(2026, 10, 20, 10, 0);
        assertFalse(reglasAgenda.seCruzan(i1, f1, i4, f4));
    }

    @Test
    @DisplayName("Calcular horarios libres excluye cruces con citas ocupadas")
    void calcularHorariosLibres_excluyeCruces() {
        LocalDate fecha = LocalDate.of(2026, 10, 20); // Martes
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 20, 7, 0); // Temprano en la manana

        // Cita ocupada de 10:00 a 10:45
        Cita citaOcupada = new Cita();
        citaOcupada.setInicio(LocalDateTime.of(2026, 10, 20, 10, 0));
        citaOcupada.setFin(LocalDateTime.of(2026, 10, 20, 10, 45));

        // Servicio de 45 minutos
        List<LocalTime> libres = reglasAgenda.calcularHorariosLibres(
                fecha, 45, configuracion, List.of(citaOcupada), ahora);

        // 9:30 se descarta porque 9:30 + 45 = 10:15 (choca con 10:00)
        assertFalse(libres.contains(LocalTime.of(9, 30)));
        // 10:00 se descarta
        assertFalse(libres.contains(LocalTime.of(10, 0)));
        // 10:30 se descarta porque 10:30 < 10:45
        assertFalse(libres.contains(LocalTime.of(10, 30)));
        // 11:00 esta libre
        assertTrue(libres.contains(LocalTime.of(11, 0)));
        // 9:00 esta libre (termina a las 9:45)
        assertTrue(libres.contains(LocalTime.of(9, 0)));
    }

    @Test
    @DisplayName("Un día cerrado no ofrece ningún horario")
    void calcularHorariosLibres_diaCerrado_retornaListaVacia() {
        LocalDate domingo = LocalDate.of(2026, 10, 25);
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 25, 7, 0);

        List<LocalTime> libres = reglasAgenda.calcularHorariosLibres(
                domingo, 45, configuracion, List.of(), ahora);

        assertTrue(libres.isEmpty());
    }

    @Test
    @DisplayName("Hoy no ofrece horas que ya pasaron")
    void calcularHorariosLibres_hoyNoOfreceHorasPasadas() {
        LocalDate fecha = LocalDate.of(2026, 10, 20);
        // Ahora es 14:15
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 20, 14, 15);

        List<LocalTime> libres = reglasAgenda.calcularHorariosLibres(
                fecha, 30, configuracion, List.of(), ahora);

        // Horas anteriores a 14:15 no deben aparecer
        assertFalse(libres.contains(LocalTime.of(10, 0)));
        assertFalse(libres.contains(LocalTime.of(14, 0)));
        // 14:30 si debe estar disponible
        assertTrue(libres.contains(LocalTime.of(14, 30)));
    }
}
