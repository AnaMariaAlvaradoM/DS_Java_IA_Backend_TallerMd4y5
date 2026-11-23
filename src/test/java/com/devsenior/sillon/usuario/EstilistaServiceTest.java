
package com.devsenior.sillon.usuario;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EstilistaServiceTest {

    // PRUEBA 1
    // El plan GRATIS permite máximo dos estilistas.

    @Test
    void tercerEstilistaEnPlanGratisDebeRechazarse() {

        UsuarioRepository usuarios =
                mock(UsuarioRepository.class);

        SalonRepository salones =
                mock(SalonRepository.class);

        ServicioRepository servicios =
                mock(ServicioRepository.class);

        PasswordEncoder encoder =
                mock(PasswordEncoder.class);

        EstilistaService servicio = new EstilistaService(
                usuarios,
                salones,
                servicios,
                encoder,
                new EstilistaMapper()
        );

        Salon salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.GRATIS);

        when(salones.findById(1L))
                .thenReturn(Optional.of(salon));

        // El salón ya tiene dos estilistas.
        when(usuarios.countBySalonIdAndRol(
                1L,
                Rol.ESTILISTA
        )).thenReturn(2L);

        EstilistaRequest solicitud = new EstilistaRequest();

        solicitud.setNombre("Nuevo estilista");
        solicitud.setEmail("nuevo@ejemplo.com");
        solicitud.setPassword("Password123");
        solicitud.setServicioIds(List.of(10L));

        assertThrows(
                PagoRequeridoException.class,
                () -> servicio.crear(1L, solicitud)
        );

        // No debe guardar el tercer estilista.
        verify(usuarios, never())
                .save(any(Usuario.class));

        // No debe procesar la contraseña.
        verifyNoInteractions(encoder);
    }

    // PRUEBA 2
    // El plan PRO permite registrar un tercer estilista.

    @Test
    void tercerEstilistaEnPlanProDebePermitirse() {

        UsuarioRepository usuarios =
                mock(UsuarioRepository.class);

        SalonRepository salones =
                mock(SalonRepository.class);

        ServicioRepository servicios =
                mock(ServicioRepository.class);

        PasswordEncoder encoder =
                mock(PasswordEncoder.class);

        EstilistaService servicio = new EstilistaService(
                usuarios,
                salones,
                servicios,
                encoder,
                new EstilistaMapper()
        );

        Salon salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.PRO);

        when(salones.findById(1L))
                .thenReturn(Optional.of(salon));

        // El salón ya tiene dos estilistas.
        when(usuarios.countBySalonIdAndRol(
                1L,
                Rol.ESTILISTA
        )).thenReturn(2L);

        Servicio corte = new Servicio();
        corte.setId(10L);
        corte.setNombre("Corte clásico");
        corte.setActivo(true);

        when(servicios.findByIdAndSalonId(10L, 1L))
                .thenReturn(Optional.of(corte));

        when(encoder.encode("Password123"))
                .thenReturn("password-cifrada");

        when(usuarios.save(any(Usuario.class)))
                .thenAnswer(invocacion -> {
                    Usuario estilista =
                            invocacion.getArgument(0);

                    estilista.setId(3L);

                    return estilista;
                });

        EstilistaRequest solicitud = new EstilistaRequest();

        solicitud.setNombre("Nuevo estilista");
        solicitud.setEmail("nuevo@ejemplo.com");
        solicitud.setPassword("Password123");
        solicitud.setServicioIds(List.of(10L));

        EstilistaResponse respuesta =
                servicio.crear(1L, solicitud);

        assertNotNull(respuesta);

        assertEquals(
                3L,
                respuesta.getId()
        );

        assertEquals(
                "Nuevo estilista",
                respuesta.getNombre()
        );

        assertEquals(
                "nuevo@ejemplo.com",
                respuesta.getEmail()
        );

        assertTrue(respuesta.isActivo());

        assertTrue(
                respuesta.getServicios()
                        .contains("Corte clásico")
        );

        verify(usuarios, times(1))
                .save(any(Usuario.class));

        verify(encoder, times(1))
                .encode("Password123");
    }
}
