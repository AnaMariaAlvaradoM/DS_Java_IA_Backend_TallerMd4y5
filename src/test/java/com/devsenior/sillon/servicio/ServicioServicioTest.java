package com.devsenior.sillon.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@ExtendWith(MockitoExtension.class)
class ServicioServicioTest {

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private SalonRepository salonRepository;

    @Spy
    private ServicioMapper servicioMapper;

    @InjectMocks
    private ServicioServicio servicioServicio;

    private Salon salon;

    @BeforeEach
    void setUp() {
        salon = new Salon();
        salon.setPlan(Plan.GRATIS);
    }

    @Test
    @DisplayName("Crear servicio válido asigna salón y persiste")
    void crear_servicioValido_guardaYRetornaDto() {
        Long salonId = 1L;
        ServicioCreacionPeticion peticion = new ServicioCreacionPeticion(
                "Corte clásico",
                "Corte con tijera",
                45,
                35000,
                true);

        when(salonRepository.findById(salonId)).thenReturn(Optional.of(salon));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(invocation -> {
            Servicio s = invocation.getArgument(0);
            s.setId(10L);
            return s;
        });

        ServicioRespuesta respuesta = servicioServicio.crear(salonId, peticion);

        assertNotNull(respuesta);
        assertEquals(10L, respuesta.id());
        assertEquals("Corte clásico", respuesta.nombre());
        assertEquals(45, respuesta.duracionMinutos());
        assertEquals(35000, respuesta.precio());
        assertTrue(respuesta.activo());
        verify(servicioRepository).save(any(Servicio.class));
    }

    @Test
    @DisplayName("Crear servicio en salón inexistente lanza 404")
    void crear_salonInexistente_lanza404() {
        Long salonId = 999L;
        ServicioCreacionPeticion peticion = new ServicioCreacionPeticion("Corte clásico", null, 45, 35000, true);
        when(salonRepository.findById(salonId)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicioServicio.crear(salonId, peticion));
    }

    @Test
    @DisplayName("Listar servicios del salón")
    void listar_retornaServiciosDelSalon() {
        Long salonId = 1L;
        Servicio s1 = new Servicio(1L, "Corte clásico", null, 45, 35000, true, salon);
        Servicio s2 = new Servicio(2L, "Arreglo de barba", null, 30, 20000, true, salon);

        when(servicioRepository.findBySalonId(salonId)).thenReturn(List.of(s1, s2));

        List<ServicioRespuesta> lista = servicioServicio.listar(salonId);

        assertEquals(2, lista.size());
        assertEquals("Corte clásico", lista.get(0).nombre());
        assertEquals("Arreglo de barba", lista.get(1).nombre());
    }

    @Test
    @DisplayName("Actualizar servicio existente en el salón")
    void actualizar_servicioExistente_actualizaCampos() {
        Long salonId = 1L;
        Long servicioId = 10L;
        Servicio servicioExistente = new Servicio(servicioId, "Corte", null, 30, 30000, true, salon);

        when(servicioRepository.findByIdAndSalonId(servicioId, salonId)).thenReturn(Optional.of(servicioExistente));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServicioCreacionPeticion peticion = new ServicioCreacionPeticion("Corte premium", "Incluye lavado", 45, 40000, true);

        ServicioRespuesta respuesta = servicioServicio.actualizar(salonId, servicioId, peticion);

        assertEquals("Corte premium", respuesta.nombre());
        assertEquals(45, respuesta.duracionMinutos());
        assertEquals(40000, respuesta.precio());
    }

    @Test
    @DisplayName("Actualizar servicio de otro salón lanza 404")
    void actualizar_servicioDeOtroSalon_lanza404() {
        Long salonId = 1L;
        Long servicioId = 99L;

        when(servicioRepository.findByIdAndSalonId(servicioId, salonId)).thenReturn(Optional.empty());

        ServicioCreacionPeticion peticion = new ServicioCreacionPeticion("Corte", null, 30, 25000, true);

        assertThrows(RecursoNoEncontradoException.class, () -> servicioServicio.actualizar(salonId, servicioId, peticion));
    }
}
