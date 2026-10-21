package com.devsenior.sillon.usuario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.salon.*;
import com.devsenior.sillon.servicio.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class EstilistaServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final SalonRepository salones = mock(SalonRepository.class);
    private final ServicioRepository servicios = mock(ServicioRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final EstilistaService servicio = new EstilistaService(usuarios, salones, servicios, passwords, new EstilistaMapper(), 2);
    private final EstilistaRequest request = new EstilistaRequest("Sara", "sara@example.test", "secreta123", List.of(2L));

    @Test void tercerEstilistaGratisEs402() {
        Salon salon = new Salon(); salon.setId(1L); salon.setPlan(Plan.GRATIS);
        when(salones.findByIdParaActualizar(1L)).thenReturn(Optional.of(salon));
        when(usuarios.countBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(2L);
        var error = assertThrows(PagoRequeridoException.class, () -> servicio.crear(1L, request));
        assertEquals(HttpStatus.PAYMENT_REQUIRED, error.getEstado());
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test void tercerEstilistaProSeCrea() {
        Salon salon = new Salon(); salon.setId(1L); salon.setPlan(Plan.PRO);
        Servicio corte = new Servicio(); corte.setId(2L); corte.setNombre("Corte"); corte.setSalon(salon);
        when(salones.findByIdParaActualizar(1L)).thenReturn(Optional.of(salon));
        when(usuarios.countBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(2L);
        when(servicios.findAllByIdInAndSalonId(Set.of(2L), 1L)).thenReturn(List.of(corte));
        when(passwords.encode("secreta123")).thenReturn("hash-ficticio");
        when(usuarios.saveAndFlush(any())).thenAnswer(invocacion -> { Usuario usuario = invocacion.getArgument(0); usuario.setId(3L); return usuario; });
        assertEquals(3L, servicio.crear(1L, request).id());
        verify(usuarios).saveAndFlush(any());
    }
}
