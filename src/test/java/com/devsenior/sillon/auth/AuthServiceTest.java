package com.devsenior.sillon.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.devsenior.sillon.auth.dto.LoginRequest;
import com.devsenior.sillon.auth.dto.RegistroRequest;
import com.devsenior.sillon.auth.dto.RespuestaAuth;
import com.devsenior.sillon.compartido.excepcion.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
import com.devsenior.sillon.salon.GeneradorSlug;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.ServicioJwt;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Instant AHORA = Instant.parse("2026-10-20T15:30:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SalonRepository salonRepository;
    @Mock
    private GeneradorSlug generadorSlug;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ServicioJwt servicioJwt;

    private AuthService authService;

    @BeforeEach
    void configurar() {
        authService = new AuthService(usuarioRepository, salonRepository, generadorSlug, passwordEncoder,
                servicioJwt, Clock.fixed(AHORA, ZONA), 10);
    }

    private RegistroRequest peticionRegistro() {
        return new RegistroRequest("Barbería La Navaja", "Medellín", "Calle 10 # 43-20", "604 555 1234",
                "Camilo Ríos", "  CAMILO@Navaja.co ", "secreta123");
    }

    @Test
    void elRegistroCreaElSalonEnGratisCon10CreditosYHorarioPorDefecto() {
        when(usuarioRepository.existsByEmail("camilo@navaja.co")).thenReturn(false);
        when(generadorSlug.generar("Barbería La Navaja")).thenReturn("barberia-la-navaja");
        when(passwordEncoder.encode("secreta123")).thenReturn("hash-bcrypt");
        when(salonRepository.save(any(Salon.class))).thenAnswer(inv -> {
            Salon salon = inv.getArgument(0);
            salon.setId(1L);
            return salon;
        });
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario usuario = inv.getArgument(0);
            usuario.setId(10L);
            return usuario;
        });
        when(servicioJwt.generarToken("camilo@navaja.co", "DUENO", 1L)).thenReturn("token-jwt");
        when(servicioJwt.getExpiracionMinutos()).thenReturn(1440L);

        RespuestaAuth respuesta = authService.registrar(peticionRegistro());

        ArgumentCaptor<Salon> capturaSalon = ArgumentCaptor.forClass(Salon.class);
        verify(salonRepository).save(capturaSalon.capture());
        Salon salon = capturaSalon.getValue();
        assertThat(salon.getPlan()).isEqualTo(Plan.GRATIS);
        assertThat(salon.getCreditosIa()).isEqualTo(10);
        assertThat(salon.getSlug()).isEqualTo("barberia-la-navaja");
        assertThat(salon.getConfiguracion().getHoraApertura()).isEqualTo(LocalTime.of(8, 0));
        assertThat(salon.getConfiguracion().getHoraCierre()).isEqualTo(LocalTime.of(19, 0));
        assertThat(salon.getConfiguracion().isAbreDomingos()).isFalse();
        assertThat(salon.getConfiguracion().getIntervaloMinutos()).isEqualTo(30);
        assertThat(salon.getFechaCreacion()).isEqualTo(LocalDateTime.ofInstant(AHORA, ZONA));

        ArgumentCaptor<Usuario> capturaUsuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturaUsuario.capture());
        Usuario dueno = capturaUsuario.getValue();
        assertThat(dueno.getRol()).isEqualTo(Rol.DUENO);
        assertThat(dueno.isActivo()).isTrue();
        assertThat(dueno.getEmail()).isEqualTo("camilo@navaja.co");
        assertThat(dueno.getPassword()).isEqualTo("hash-bcrypt");

        assertThat(respuesta.token()).isEqualTo("token-jwt");
        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(respuesta.expiraEnMinutos()).isEqualTo(1440L);
        assertThat(respuesta.usuarioId()).isEqualTo(10L);
        assertThat(respuesta.rol()).isEqualTo("DUENO");
        assertThat(respuesta.salonId()).isEqualTo(1L);
        assertThat(respuesta.salonSlug()).isEqualTo("barberia-la-navaja");
    }

    @Test
    void elRegistroConCorreoExistenteLanzaReglaDeNegocio() {
        when(usuarioRepository.existsByEmail("camilo@navaja.co")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(peticionRegistro()))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    private Usuario usuarioExistente() {
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setSlug("barberia-la-navaja");
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNombre("Camilo Ríos");
        usuario.setEmail("camilo@navaja.co");
        usuario.setPassword("hash-bcrypt");
        usuario.setRol(Rol.DUENO);
        usuario.setActivo(true);
        usuario.setSalon(salon);
        return usuario;
    }

    @Test
    void elLoginCorrectoDevuelveElToken() {
        when(usuarioRepository.findByEmail("camilo@navaja.co")).thenReturn(Optional.of(usuarioExistente()));
        when(passwordEncoder.matches("secreta123", "hash-bcrypt")).thenReturn(true);
        when(servicioJwt.generarToken("camilo@navaja.co", "DUENO", 1L)).thenReturn("token-jwt");
        when(servicioJwt.getExpiracionMinutos()).thenReturn(1440L);

        RespuestaAuth respuesta = authService.login(new LoginRequest("Camilo@Navaja.co", "secreta123"));

        assertThat(respuesta.token()).isEqualTo("token-jwt");
        assertThat(respuesta.salonSlug()).isEqualTo("barberia-la-navaja");
    }

    @Test
    void elLoginConContrasenaIncorrectaRespondeConMensajeGenerico() {
        when(usuarioRepository.findByEmail("camilo@navaja.co")).thenReturn(Optional.of(usuarioExistente()));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("camilo@navaja.co", "otra")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void elLoginConCorreoInexistenteRespondeConElMismoMensajeGenerico() {
        when(usuarioRepository.findByEmail("nadie@navaja.co")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie@navaja.co", "secreta123")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas");
    }
}
