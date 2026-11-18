package com.devsenior.sillon.salon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
import com.devsenior.sillon.salon.dto.ConfiguracionRequest;
import com.devsenior.sillon.salon.dto.SalonResponse;

@ExtendWith(MockitoExtension.class)
class SalonServiceTest {

    @Mock
    private SalonRepository salonRepository;

    @InjectMocks
    private SalonService salonService;

    private Salon salonDePrueba() {
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setNombre("Barbería La Navaja");
        salon.setSlug("barberia-la-navaja");
        salon.setCiudad("Medellín");
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(LocalDateTime.of(2026, 10, 20, 10, 30));
        return salon;
    }

    @Test
    void obtenerMiSalonArmaElDtoConPlanYCreditos() {
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salonDePrueba()));

        SalonResponse respuesta = salonService.obtenerMiSalon(1L);

        assertThat(respuesta.plan()).isEqualTo("GRATIS");
        assertThat(respuesta.creditosIa()).isEqualTo(10);
        assertThat(respuesta.slug()).isEqualTo("barberia-la-navaja");
        assertThat(respuesta.configuracion().intervaloMinutos()).isEqualTo(30);
    }

    @Test
    void obtenerUnSalonInexistenteResponde404() {
        when(salonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salonService.obtenerMiSalon(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void laAperturaIgualOPosteriorAlCierreLanzaReglaDeNegocio() {
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salonDePrueba()));
        ConfiguracionRequest invertida = new ConfiguracionRequest(
                LocalTime.of(20, 0), LocalTime.of(8, 0), false, 30);
        ConfiguracionRequest igual = new ConfiguracionRequest(
                LocalTime.of(8, 0), LocalTime.of(8, 0), false, 30);

        assertThatThrownBy(() -> salonService.actualizarConfiguracion(1L, invertida))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> salonService.actualizarConfiguracion(1L, igual))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void laConfiguracionValidaSeGuardaYSeDevuelveActualizada() {
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salonDePrueba()));
        ConfiguracionRequest peticion = new ConfiguracionRequest(
                LocalTime.of(9, 0), LocalTime.of(18, 0), true, 15);

        SalonResponse respuesta = salonService.actualizarConfiguracion(1L, peticion);

        assertThat(respuesta.configuracion().horaApertura()).isEqualTo(LocalTime.of(9, 0));
        assertThat(respuesta.configuracion().horaCierre()).isEqualTo(LocalTime.of(18, 0));
        assertThat(respuesta.configuracion().abreDomingos()).isTrue();
        assertThat(respuesta.configuracion().intervaloMinutos()).isEqualTo(15);
    }
}
