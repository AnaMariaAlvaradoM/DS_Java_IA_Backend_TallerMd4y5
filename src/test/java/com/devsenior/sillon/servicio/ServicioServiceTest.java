package com.devsenior.sillon.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.dto.ServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioResponse;

@ExtendWith(MockitoExtension.class)
class ServicioServiceTest {

    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private SalonRepository salonRepository;

    private ServicioService servicioService;

    @BeforeEach
    void configurar() {
        servicioService = new ServicioService(servicioRepository, salonRepository, new ServicioMapper());
    }

    private Salon salonDePrueba() {
        Salon salon = new Salon();
        salon.setId(1L);
        return salon;
    }

    @Test
    void crearCopiaLosCamposYNaceActivoSiElCampoNoLlega() {
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salonDePrueba()));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> {
            Servicio servicio = inv.getArgument(0);
            servicio.setId(5L);
            return servicio;
        });
        ServicioRequest peticion = new ServicioRequest("Corte clásico", "Con tijera", 45, 35000, null);

        ServicioResponse respuesta = servicioService.crear(1L, peticion);

        assertThat(respuesta.id()).isEqualTo(5L);
        assertThat(respuesta.nombre()).isEqualTo("Corte clásico");
        assertThat(respuesta.duracionMinutos()).isEqualTo(45);
        assertThat(respuesta.precio()).isEqualTo(35000);
        assertThat(respuesta.activo()).isTrue();
    }

    @Test
    void crearRespetaElCampoActivoCuandoLlegaEnFalso() {
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salonDePrueba()));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));
        ServicioRequest peticion = new ServicioRequest("Corte clásico", null, 45, 35000, false);

        assertThat(servicioService.crear(1L, peticion).activo()).isFalse();
    }

    @Test
    void listarDevuelveSoloLosServiciosDelSalon() {
        Servicio servicio = new Servicio();
        servicio.setId(5L);
        servicio.setNombre("Corte clásico");
        servicio.setDuracionMinutos(45);
        servicio.setPrecio(35000);
        servicio.setActivo(true);
        when(servicioRepository.findAllBySalonId(1L)).thenReturn(List.of(servicio));

        List<ServicioResponse> lista = servicioService.listar(1L);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).nombre()).isEqualTo("Corte clásico");
    }

    @Test
    void actualizarUnServicioDeOtroSalonResponde404YNoGuarda() {
        when(servicioRepository.findByIdAndSalonId(5L, 2L)).thenReturn(Optional.empty());
        ServicioRequest peticion = new ServicioRequest("Corte clásico", null, 45, 35000, null);

        assertThatThrownBy(() -> servicioService.actualizar(2L, 5L, peticion))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(servicioRepository, never()).save(any());
    }

    @Test
    void actualizarCambiaLosCamposDelServicioDelMismoSalon() {
        Servicio existente = new Servicio();
        existente.setId(5L);
        existente.setNombre("Corte clásico");
        existente.setDuracionMinutos(45);
        existente.setPrecio(35000);
        existente.setActivo(true);
        when(servicioRepository.findByIdAndSalonId(5L, 1L)).thenReturn(Optional.of(existente));
        ServicioRequest peticion = new ServicioRequest("Corte premium", "Con ritual", 60, 50000, false);

        ServicioResponse respuesta = servicioService.actualizar(1L, 5L, peticion);

        assertThat(respuesta.nombre()).isEqualTo("Corte premium");
        assertThat(respuesta.duracionMinutos()).isEqualTo(60);
        assertThat(respuesta.precio()).isEqualTo(50000);
        assertThat(respuesta.activo()).isFalse();
        verify(servicioRepository).save(existente);
    }
}
