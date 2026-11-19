package com.devsenior.sillon.usuario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.compartido.config.NegocioProperties;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.service.SalonService;
import com.devsenior.sillon.servicio.model.Servicio;
import com.devsenior.sillon.servicio.repository.ServicioRepository;
import com.devsenior.sillon.usuario.dto.EstilistaRequest;
import com.devsenior.sillon.usuario.dto.EstilistaResponse;
import com.devsenior.sillon.usuario.mapper.EstilistaMapper;
import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import com.devsenior.sillon.usuario.repository.UsuarioRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class EstilistaServiceTest {

    private static final Long SALON_ID = 1L;

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private SalonService salonService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private EstilistaService estilistaService;
    private Salon salon;

    @BeforeEach
    void setUp() {
        NegocioProperties negocio = new NegocioProperties(10, 300, 200, 2);
        estilistaService = new EstilistaService(usuarioRepository, servicioRepository, salonService,
                new EstilistaMapper(), passwordEncoder, negocio);
        salon = new Salon();
        salon.setId(SALON_ID);
        salon.setPlan(Plan.GRATIS);
        when(salonService.obtenerEntidad(SALON_ID)).thenReturn(salon);
    }

    @Test
    void crear_tercerEstilistaEnPlanGratis_lanza402YNoGuarda() {
        when(usuarioRepository.countBySalonIdAndRol(SALON_ID, Rol.ESTILISTA)).thenReturn(2L);

        assertThrows(PagoRequeridoException.class, () -> estilistaService.crear(SALON_ID, request(10L)));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_tercerEstilistaEnPlanPro_loCrea() {
        salon.setPlan(Plan.PRO);
        when(usuarioRepository.countBySalonIdAndRol(SALON_ID, Rol.ESTILISTA)).thenReturn(2L);
        when(usuarioRepository.existsByEmail("luis@navaja.co")).thenReturn(false);
        when(servicioRepository.findByIdInAndSalonId(anyCollection(), anyLong())).thenReturn(List.of(servicio(10L, "Corte clásico")));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        EstilistaResponse respuesta = estilistaService.crear(SALON_ID, request(10L));

        assertEquals("luis@navaja.co", respuesta.email());
        assertEquals(List.of("Corte clásico"), respuesta.servicios());
    }

    @Test
    void crear_correoRepetido_lanza409() {
        when(usuarioRepository.countBySalonIdAndRol(SALON_ID, Rol.ESTILISTA)).thenReturn(0L);
        when(usuarioRepository.existsByEmail("luis@navaja.co")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> estilistaService.crear(SALON_ID, request(10L)));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_servicioDeOtroSalon_lanza404() {
        when(usuarioRepository.countBySalonIdAndRol(SALON_ID, Rol.ESTILISTA)).thenReturn(0L);
        when(usuarioRepository.existsByEmail("luis@navaja.co")).thenReturn(false);
        when(servicioRepository.findByIdInAndSalonId(anyCollection(), anyLong())).thenReturn(List.of(servicio(10L, "Corte clásico")));

        assertThrows(RecursoNoEncontradoException.class, () -> estilistaService.crear(SALON_ID, request(10L, 99L)));
        verify(usuarioRepository, never()).save(any());
    }

    private EstilistaRequest request(Long... servicioIds) {
        return new EstilistaRequest("Luis Pérez", " Luis@Navaja.co ", "luis123456", List.of(servicioIds));
    }

    private Servicio servicio(Long id, String nombre) {
        Servicio servicio = new Servicio();
        servicio.setId(id);
        servicio.setNombre(nombre);
        return servicio;
    }
}
