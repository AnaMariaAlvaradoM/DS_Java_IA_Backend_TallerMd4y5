package com.devsenior.sillon.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;

@ExtendWith(MockitoExtension.class)
class EstilistaServiceTest {

    private static final Long SALON_ID = 1L;
    private static final int LIMITE_GRATIS = 2;

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SalonRepository salonRepository;
    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private EstilistaService estilistaService;

    private Salon salon;
    private Servicio servicio;

    @BeforeEach
    void setUp() {
        estilistaService = new EstilistaService(usuarioRepository, salonRepository, servicioRepository,
                passwordEncoder, LIMITE_GRATIS);

        salon = new Salon();
        salon.setId(SALON_ID);

        servicio = new Servicio();
        servicio.setId(5L);
        servicio.setNombre("Corte");

        when(salonRepository.findById(SALON_ID)).thenReturn(Optional.of(salon));
    }

    private EstilistaRequest request(String email) {
        return new EstilistaRequest("Nuevo Estilista", email, "password123", List.of(servicio.getId()));
    }

    @Test
    void elTercerEstilistaEnPlanGratisDaPagoRequerido() {
        salon.setPlan(Plan.GRATIS);
        when(usuarioRepository.countBySalonIdAndRol(SALON_ID, Rol.ESTILISTA)).thenReturn(2L);

        assertThatThrownBy(() -> estilistaService.crear(SALON_ID, request("nuevo@correo.com")))
                .isInstanceOf(PagoRequeridoException.class);

        verify(usuarioRepository, never()).existsByEmail(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void enPlanProSiSeCreaAunqueHayaDosEstilistas() {
        salon.setPlan(Plan.PRO);
        when(usuarioRepository.existsByEmail("nuevo@correo.com")).thenReturn(false);
        when(servicioRepository.findByIdInAndSalonId(Set.of(servicio.getId()), SALON_ID))
                .thenReturn(List.of(servicio));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EstilistaRespuesta respuesta = estilistaService.crear(SALON_ID, request("nuevo@correo.com"));

        assertThat(respuesta.email()).isEqualTo("nuevo@correo.com");
        verify(usuarioRepository, never()).countBySalonIdAndRol(any(), any());
    }

    @Test
    void correoRepetidoDaConflicto() {
        salon.setPlan(Plan.PRO);
        when(usuarioRepository.existsByEmail("repetido@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> estilistaService.crear(SALON_ID, request("repetido@correo.com")))
                .isInstanceOf(ReglaNegocioException.class);

        verify(usuarioRepository, never()).save(any());
    }
}
