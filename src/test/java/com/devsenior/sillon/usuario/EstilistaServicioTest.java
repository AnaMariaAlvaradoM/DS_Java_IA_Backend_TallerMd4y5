package com.devsenior.sillon.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.devsenior.sillon.compartido.NegocioConfig;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;

@ExtendWith(MockitoExtension.class)
class EstilistaServicioTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NegocioConfig negocioConfig;

    @InjectMocks
    private EstilistaServicio estilistaServicio;

    private Salon salonGratis;
    private Salon salonPro;
    private Servicio servicioCorte;
    private Servicio servicioBarba;

    @BeforeEach
    void setUp() {
        salonGratis = new Salon();
        salonGratis.setPlan(Plan.GRATIS);

        salonPro = new Salon();
        salonPro.setPlan(Plan.PRO);

        servicioCorte = new Servicio(1L, "Corte clásico", null, 45, 35000, true, salonGratis);
        servicioBarba = new Servicio(2L, "Arreglo de barba", null, 30, 20000, true, salonGratis);
    }

    @Test
    @DisplayName("Crear estilista exitoso con múltiples servicios devuelve DTO sin contraseña")
    void crear_estilistaValido_guardaYRetornaDtoConServicios() {
        Long salonId = 1L;
        EstilistaCreacionPeticion peticion = new EstilistaCreacionPeticion(
                "Mateo Gil",
                "mateo@navaja.co",
                "mateo12345",
                List.of(1L, 2L));

        when(salonRepository.findById(salonId)).thenReturn(Optional.of(salonGratis));
        when(negocioConfig.limiteEstilistasGratis()).thenReturn(2);
        when(usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA)).thenReturn(0L);
        when(usuarioRepository.existsByEmail("mateo@navaja.co")).thenReturn(false);
        when(servicioRepository.findByIdInAndSalonId(anyCollection(), eq(salonId)))
                .thenReturn(List.of(servicioCorte, servicioBarba));
        when(passwordEncoder.encode("mateo12345")).thenReturn("hashedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(5L);
            return u;
        });

        EstilistaRespuesta respuesta = estilistaServicio.crear(salonId, peticion);

        assertNotNull(respuesta);
        assertEquals(5L, respuesta.id());
        assertEquals("Mateo Gil", respuesta.nombre());
        assertEquals("mateo@navaja.co", respuesta.email());
        assertEquals(2, respuesta.servicios().size());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Tercer estilista en plan GRATIS lanza 402 (PagoRequeridoException)")
    void crear_tercerEstilistaEnPlanGratis_lanza402() {
        Long salonId = 1L;
        EstilistaCreacionPeticion peticion = new EstilistaCreacionPeticion(
                "Carlos Barber",
                "carlos@navaja.co",
                "carlos12345",
                List.of(1L));

        when(salonRepository.findById(salonId)).thenReturn(Optional.of(salonGratis));
        when(negocioConfig.limiteEstilistasGratis()).thenReturn(2);
        when(usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA)).thenReturn(2L);

        assertThrows(PagoRequeridoException.class, () -> estilistaServicio.crear(salonId, peticion));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Tercer estilista en plan PRO sí se crea exitosamente")
    void crear_tercerEstilistaEnPlanPro_seCreaExitosamente() {
        Long salonId = 1L;
        EstilistaCreacionPeticion peticion = new EstilistaCreacionPeticion(
                "Carlos Barber",
                "carlos@navaja.co",
                "carlos12345",
                List.of(1L));

        when(salonRepository.findById(salonId)).thenReturn(Optional.of(salonPro));
        when(usuarioRepository.existsByEmail("carlos@navaja.co")).thenReturn(false);
        when(servicioRepository.findByIdInAndSalonId(anyCollection(), eq(salonId)))
                .thenReturn(List.of(servicioCorte));
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(6L);
            return u;
        });

        EstilistaRespuesta respuesta = estilistaServicio.crear(salonId, peticion);

        assertNotNull(respuesta);
        assertEquals("Carlos Barber", respuesta.nombre());
    }

    @Test
    @DisplayName("Correo repetido lanza 409 (ReglaNegocioException)")
    void crear_correoDuplicado_lanza409() {
        Long salonId = 1L;
        EstilistaCreacionPeticion peticion = new EstilistaCreacionPeticion(
                "Mateo Gil",
                "mateo@navaja.co",
                "mateo12345",
                List.of(1L));

        when(salonRepository.findById(salonId)).thenReturn(Optional.of(salonGratis));
        when(negocioConfig.limiteEstilistasGratis()).thenReturn(2);
        when(usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA)).thenReturn(1L);
        when(usuarioRepository.existsByEmail("mateo@navaja.co")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> estilistaServicio.crear(salonId, peticion));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Servicio inexistente o de otro salón lanza 404 (RecursoNoEncontradoException)")
    void crear_servicioDeOtroSalon_lanza404() {
        Long salonId = 1L;
        EstilistaCreacionPeticion peticion = new EstilistaCreacionPeticion(
                "Mateo Gil",
                "mateo@navaja.co",
                "mateo12345",
                List.of(99L));

        when(salonRepository.findById(salonId)).thenReturn(Optional.of(salonGratis));
        when(negocioConfig.limiteEstilistasGratis()).thenReturn(2);
        when(usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA)).thenReturn(1L);
        when(usuarioRepository.existsByEmail("mateo@navaja.co")).thenReturn(false);
        when(servicioRepository.findByIdInAndSalonId(anyCollection(), eq(salonId)))
                .thenReturn(List.of()); // No lo encuentra para este salon

        assertThrows(RecursoNoEncontradoException.class, () -> estilistaServicio.crear(salonId, peticion));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Orden de validación: el límite del plan se verifica ANTES del correo y de los servicios")
    void crear_ordenValidacion_limitePlanPrimero() {
        Long salonId = 1L;
        EstilistaCreacionPeticion peticion = new EstilistaCreacionPeticion(
                "Mateo Gil",
                "mateo@navaja.co",
                "mateo12345",
                List.of(1L));

        when(salonRepository.findById(salonId)).thenReturn(Optional.of(salonGratis));
        when(negocioConfig.limiteEstilistasGratis()).thenReturn(2);
        when(usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA)).thenReturn(2L); // Limite alcanzado

        assertThrows(PagoRequeridoException.class, () -> estilistaServicio.crear(salonId, peticion));

        // Debe fallar en limite del plan SIN consultar correo ni servicios
        verify(usuarioRepository, never()).existsByEmail(any());
        verify(servicioRepository, never()).findByIdInAndSalonId(anyCollection(), anyLong());
    }

    @Test
    @DisplayName("Listar estilistas del salón")
    void listar_retornaEstilistasDelSalon() {
        Long salonId = 1L;
        Usuario e1 = new Usuario();
        e1.setNombre("Mateo Gil");
        e1.setEmail("mateo@navaja.co");
        e1.setServicios(java.util.Set.of(servicioCorte, servicioBarba));

        Usuario e2 = new Usuario();
        e2.setNombre("Sara Mejía");
        e2.setEmail("sara@navaja.co");
        e2.setServicios(java.util.Set.of(servicioCorte));

        when(usuarioRepository.findBySalonIdAndRol(salonId, Rol.ESTILISTA)).thenReturn(List.of(e1, e2));

        List<EstilistaRespuesta> lista = estilistaServicio.listar(salonId);

        assertEquals(2, lista.size());
        assertEquals("Mateo Gil", lista.get(0).nombre());
        assertEquals("Sara Mejía", lista.get(1).nombre());
    }
}
