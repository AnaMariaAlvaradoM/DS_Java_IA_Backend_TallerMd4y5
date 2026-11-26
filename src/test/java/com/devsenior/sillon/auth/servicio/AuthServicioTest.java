package com.devsenior.sillon.auth.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.devsenior.sillon.auth.dto.RespuestaAuth;
import com.devsenior.sillon.auth.dto.SolicitudLogin;
import com.devsenior.sillon.auth.dto.SolicitudRegistro;
import com.devsenior.sillon.auth.mapper.AuthMapper;
import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionNoAutenticado;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.devsenior.sillon.salon.servicio.GeneradorSlug;
import com.devsenior.sillon.seguridad.ServicioJwt;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.devsenior.sillon.usuario.repositorio.UsuarioRepositorio;

class AuthServicioTest {

        private SalonRepositorio salonRepositorio;
        private UsuarioRepositorio usuarioRepositorio;
        private GeneradorSlug generadorSlug;
        private BCryptPasswordEncoder passwordEncoder;
        private ServicioJwt servicioJwt;
        private AuthServicio authServicio;

        @BeforeEach
        void preparar() {

                salonRepositorio = mock(SalonRepositorio.class);
                usuarioRepositorio = mock(UsuarioRepositorio.class);
                generadorSlug = mock(GeneradorSlug.class);
                servicioJwt = mock(ServicioJwt.class);

                passwordEncoder = new BCryptPasswordEncoder();

                Clock reloj = Clock.fixed(
                                Instant.parse("2026-11-19T15:00:00Z"),
                                ZoneId.of("America/Bogota"));

                authServicio = new AuthServicio(
                                salonRepositorio,
                                usuarioRepositorio,
                                generadorSlug,
                                passwordEncoder,
                                servicioJwt,
                                new AuthMapper(),
                                reloj);
        }

        @Test
        void registrarCreaSalonGratisConDiezCreditosYDueno() {

                SolicitudRegistro solicitud = new SolicitudRegistro(
                                "Barbería La Navaja",
                                "Manizales",
                                "Calle 23",
                                "3001234567",
                                "Camilo Rios",
                                "CAMILO@pruebas.example",
                                "PruebaSillon2026");

                when(usuarioRepositorio.existsByEmail(
                                "camilo@pruebas.example")).thenReturn(false);

                when(generadorSlug.generar(solicitud.salonNombre()))
                                .thenReturn("barberia-la-navaja");

                when(salonRepositorio.save(any(Salon.class)))
                                .thenAnswer(invocacion -> invocacion.getArgument(0));

                when(usuarioRepositorio.save(any(Usuario.class)))
                                .thenAnswer(invocacion -> invocacion.getArgument(0));

                when(servicioJwt.generarToken(any(UsuarioPrincipal.class)))
                                .thenReturn("token-prueba");

                when(servicioJwt.getExpiraEnMinutos()).thenReturn(60);

                RespuestaAuth respuesta = authServicio.registrar(solicitud);

                var captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);

                verify(usuarioRepositorio).save(captor.capture());

                Usuario usuario = captor.getValue();

                assertEquals(Rol.DUENO, usuario.getRol());
                assertTrue(usuario.isActivo());
                assertEquals("camilo@pruebas.example", usuario.getEmail());

                assertEquals(Plan.GRATIS, usuario.getSalon().getPlan());
                assertEquals(10, usuario.getSalon().getCreditosIa());

                assertNotEquals(solicitud.password(), usuario.getPassword());

                assertTrue(passwordEncoder.matches(
                                solicitud.password(), usuario.getPassword()));

                assertEquals("token-prueba", respuesta.token());
                assertEquals("Bearer", respuesta.tipo());
                assertEquals(60, respuesta.expiraEnMinutos());
        }

        @Test
        void registrarRechazaCorreoDuplicado() {

                SolicitudRegistro solicitud = new SolicitudRegistro(
                                "Barbería La Navaja",
                                "Manizales",
                                "Calle 23",
                                "3001234567",
                                "Camilo Rios",
                                "camilo@pruebas.example",
                                "PruebaSillon2026");

                when(usuarioRepositorio.existsByEmail(solicitud.email()))
                                .thenReturn(true);

                assertThrows(
                                ExcepcionConflicto.class,
                                () -> authServicio.registrar(solicitud));

                verify(salonRepositorio, never()).save(any(Salon.class));
                verify(usuarioRepositorio, never()).save(any(Usuario.class));
                verifyNoInteractions(servicioJwt);
        }

        @Test
        void loginCorrectoGeneraToken() {

                Usuario usuario = crearUsuario(true);

                when(usuarioRepositorio.findByEmail(usuario.getEmail()))
                                .thenReturn(Optional.of(usuario));

                when(servicioJwt.generarToken(any(UsuarioPrincipal.class)))
                                .thenReturn("token-prueba");

                when(servicioJwt.getExpiraEnMinutos()).thenReturn(60);

                RespuestaAuth respuesta = authServicio.login(
                                new SolicitudLogin(
                                                usuario.getEmail(),
                                                "PruebaSillon2026"));

                assertEquals("token-prueba", respuesta.token());
                assertEquals(Rol.DUENO, respuesta.rol());
                assertEquals(60, respuesta.expiraEnMinutos());
        }

        @Test
        void loginRechazaPasswordIncorrecto() {

                Usuario usuario = crearUsuario(true);

                when(usuarioRepositorio.findByEmail(usuario.getEmail()))
                                .thenReturn(Optional.of(usuario));

                assertThrows(
                                ExcepcionNoAutenticado.class,
                                () -> authServicio.login(
                                                new SolicitudLogin(
                                                                usuario.getEmail(),
                                                                "ClaveIncorrecta")));

                verifyNoInteractions(servicioJwt);
        }

        @Test
        void loginRechazaUsuarioInactivo() {

                Usuario usuario = crearUsuario(false);

                when(usuarioRepositorio.findByEmail(usuario.getEmail()))
                                .thenReturn(Optional.of(usuario));

                assertThrows(
                                ExcepcionNoAutenticado.class,
                                () -> authServicio.login(
                                                new SolicitudLogin(
                                                                usuario.getEmail(),
                                                                "PruebaSillon2026")));

                verifyNoInteractions(servicioJwt);
        }

        private Usuario crearUsuario(boolean activo) {

                Salon salon = new Salon();
                salon.setNombre("Barbería La Navaja");
                salon.setSlug("barberia-la-navaja");

                Usuario usuario = new Usuario();

                usuario.setNombre("Camilo Rios");
                usuario.setEmail("camilo@pruebas.example");
                usuario.setPassword(
                                passwordEncoder.encode("PruebaSillon2026"));

                usuario.setRol(Rol.DUENO);
                usuario.setActivo(activo);
                usuario.setSalon(salon);

                return usuario;
        }
}