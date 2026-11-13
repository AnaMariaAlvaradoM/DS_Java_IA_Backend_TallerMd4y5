package com.devsenior.sillon.usuario.estilista;

import com.devsenior.sillon.compartido.LimitePlanExcedidoException;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.estilista.dto.EstilistaRequest;
import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import com.devsenior.sillon.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstilistaServiceTest {
    private static final Long SALON_ID = 17L;

    @Mock private ServicioRepository servicioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SalonRepository salonRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private EstilistaService estilistaService;
    private EstilistaRequest solicitud;
    private Salon salon;
    private Servicio servicio;

    @BeforeEach
    void prepararServicio() {
        estilistaService = new EstilistaService(
                servicioRepository, usuarioRepository, salonRepository, passwordEncoder);
        solicitud = new EstilistaRequest(
                "Valeria", "valeria@example.com", "clave-segura", List.of(42L));
        salon = new Salon();
        salon.setId(SALON_ID);
        servicio = new Servicio();
        servicio.setId(42L);
        servicio.setNombre("Corte");
    }

    @Test
    void tercerEstilistaEnPlanGratisLanza402YNoGuarda() {
        salon.setPlan(Plan.GRATIS);
        when(salonRepository.findById(SALON_ID)).thenReturn(Optional.of(salon));
        when(usuarioRepository.countBySalonIdAndRol(SALON_ID, Rol.ESTILISTA)).thenReturn(2L);

        assertThrows(LimitePlanExcedidoException.class,
                () -> estilistaService.crearEstilista(solicitud, SALON_ID));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void tercerEstilistaEnPlanProSeCreaYGuarda() {
        salon.setPlan(Plan.PRO);
        when(salonRepository.findById(SALON_ID)).thenReturn(Optional.of(salon));
        when(usuarioRepository.countBySalonIdAndRol(SALON_ID, Rol.ESTILISTA)).thenReturn(2L);
        when(usuarioRepository.existsByEmail(solicitud.email())).thenReturn(false);
        when(servicioRepository.findAllByIdInAndSalonId(Set.of(42L), SALON_ID))
                .thenReturn(List.of(servicio));
        when(passwordEncoder.encode(solicitud.password())).thenReturn("clave-encriptada");

        estilistaService.crearEstilista(solicitud, SALON_ID);

        ArgumentCaptor<Usuario> estilistaCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(estilistaCaptor.capture());
        Usuario guardado = estilistaCaptor.getValue();
        assertEquals("Valeria", guardado.getNombre());
        assertEquals("valeria@example.com", guardado.getEmail());
        assertEquals("clave-encriptada", guardado.getContrasena());
        assertEquals(Rol.ESTILISTA, guardado.getRol());
        assertEquals(salon, guardado.getSalon());
        assertEquals(Set.of(servicio), guardado.getServicios());
    }
}
