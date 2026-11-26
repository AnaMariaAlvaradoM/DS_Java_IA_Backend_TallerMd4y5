package com.devsenior.sillon.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.devsenior.sillon.Datos;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.ServicioRepository;

class EstilistaServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final SalonRepository salonRepository = mock(SalonRepository.class);
    private final ServicioRepository servicioRepository = mock(ServicioRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final EstilistaService service = new EstilistaService(usuarioRepository, salonRepository,
            servicioRepository, passwordEncoder, 2);

    private final EstilistaPeticion luis = new EstilistaPeticion("Luis Pérez", "luis@navaja.co", "luis123456",
            List.of(10L));

    private void conSalonYDosEstilistas(Salon salon) {
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(usuarioRepository.countBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(2L);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(servicioRepository.findAllByIdInAndSalonId(anyCollection(), eq(1L)))
                .thenReturn(List.of(Datos.servicio(10L, "Corte clásico", 45, 35000, salon)));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void tercerEstilistaEnPlanGratisDa402() {
        conSalonYDosEstilistas(Datos.salon(Plan.GRATIS));

        assertThatThrownBy(() -> service.crear(1L, luis)).isInstanceOf(PagoRequeridoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void tercerEstilistaEnPlanProSiSeCrea() {
        conSalonYDosEstilistas(Datos.salon(Plan.PRO));

        EstilistaRespuesta r = service.crear(1L, luis);

        assertThat(r.servicios()).containsExactly("Corte clásico");
        assertThat(r.email()).isEqualTo("luis@navaja.co");
    }

    @Test
    void servicioDeOtroSalonDa404() {
        conSalonYDosEstilistas(Datos.salon(Plan.PRO));
        when(servicioRepository.findAllByIdInAndSalonId(anyCollection(), eq(1L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.crear(1L, luis)).isInstanceOf(NoEncontradoException.class);
    }
}
