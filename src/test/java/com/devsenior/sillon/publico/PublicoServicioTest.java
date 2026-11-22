package com.devsenior.sillon.publico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.cita.CitaCreacionPeticion;
import com.devsenior.sillon.cita.CitaRepository;
import com.devsenior.sillon.cita.CitaRespuesta;
import com.devsenior.sillon.cita.CitaServicio;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.OrigenCita;
import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioMapper;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PublicoServicioTest {

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private CitaServicio citaServicio;

    @Spy
    private ReglasAgenda reglasAgenda;

    @Spy
    private ServicioMapper servicioMapper;

    private Clock relojFijo;
    private PublicoServicio publicoServicio;

    private Salon salon;
    private Servicio servicioCorte;
    private Servicio servicioBarba;
    private Usuario estilistaMateo;
    private Usuario estilistaSara;

    private static final LocalDateTime AHORA_FIJO = LocalDateTime.of(2026, 10, 20, 8, 0, 0);

    @BeforeEach
    void setUp() {
        ZoneId zonaBogota = ZoneId.of("America/Bogota");
        Instant instantFijo = AHORA_FIJO.atZone(zonaBogota).toInstant();
        relojFijo = Clock.fixed(instantFijo, zonaBogota);

        publicoServicio = new PublicoServicio(
                salonRepository,
                servicioRepository,
                usuarioRepository,
                citaRepository,
                citaServicio,
                reglasAgenda,
                servicioMapper,
                relojFijo);

        salon = new Salon();
        salon.setNombre("Barbería La Navaja");
        salon.setSlug("barberia-la-navaja");
        salon.setCiudad("Medellín");
        salon.setDireccion("Calle 10 # 43-20");
        salon.setTelefono("604 555 1234");
        salon.setPlan(Plan.GRATIS);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());

        servicioCorte = new Servicio(1L, "Corte clásico", "Corte con tijera", 45, 35000, true, salon);
        servicioBarba = new Servicio(2L, "Arreglo de barba", "Perfilado", 30, 20000, true, salon);

        estilistaMateo = new Usuario();
        estilistaMateo.setId(20L);
        estilistaMateo.setNombre("Mateo Gil");
        estilistaMateo.setEmail("mateo@navaja.co");
        estilistaMateo.setRol(Rol.ESTILISTA);
        estilistaMateo.setActivo(true);
        estilistaMateo.setSalon(salon);
        estilistaMateo.setServicios(Set.of(servicioCorte, servicioBarba));

        estilistaSara = new Usuario();
        estilistaSara.setId(21L);
        estilistaSara.setNombre("Sara Mejía");
        estilistaSara.setEmail("sara@navaja.co");
        estilistaSara.setRol(Rol.ESTILISTA);
        estilistaSara.setActivo(true);
        estilistaSara.setSalon(salon);
        estilistaSara.setServicios(Set.of(servicioCorte));
    }

    @Test
    @DisplayName("Catálogo público muestra 2 servicios, 2 estilistas y ningún correo")
    void obtenerCatalogo_retornaCatalogoSinCorreos() {
        String slug = "barberia-la-navaja";
        when(salonRepository.findBySlug(slug)).thenReturn(Optional.of(salon));
        when(servicioRepository.findBySalonIdAndActivoTrue(any())).thenReturn(List.of(servicioCorte, servicioBarba));
        when(usuarioRepository.findBySalonIdAndRol(any(), eq(Rol.ESTILISTA))).thenReturn(List.of(estilistaMateo, estilistaSara));

        CatalogoPublicoRespuesta catalogo = publicoServicio.obtenerCatalogo(slug);

        assertNotNull(catalogo);
        assertEquals("Barbería La Navaja", catalogo.nombre());
        assertEquals(slug, catalogo.slug());
        assertEquals("Lunes a sábado (domingos cerrado), de 8:00 a. m. a 7:00 p. m.", catalogo.horario());
        assertEquals(2, catalogo.servicios().size());
        assertEquals(2, catalogo.estilistas().size());

        // Verificar que los estilistas solo traen id, nombre y servicioIds
        EstilistaPublicoRespuesta mateoDto = catalogo.estilistas().get(0);
        assertEquals(20L, mateoDto.id());
        assertEquals("Mateo Gil", mateoDto.nombre());
        assertTrue(mateoDto.servicioIds().containsAll(List.of(1L, 2L)));
    }

    @Test
    @DisplayName("Catálogo público de salón inexistente lanza 404")
    void obtenerCatalogo_salonInexistente_lanza404() {
        when(salonRepository.findBySlug("no-existe")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> publicoServicio.obtenerCatalogo("no-existe"));
    }

    @Test
    @DisplayName("Disponibilidad de Mateo para corte incluye 10:00")
    void consultarDisponibilidad_incluye1000() {
        String slug = "barberia-la-navaja";
        LocalDate fecha = LocalDate.of(2026, 10, 20); // Martes

        when(salonRepository.findBySlug(slug)).thenReturn(Optional.of(salon));
        when(servicioRepository.findByIdAndSalonId(1L, salon.getId())).thenReturn(Optional.of(servicioCorte));
        when(usuarioRepository.findByIdAndSalonIdAndRol(20L, salon.getId(), Rol.ESTILISTA)).thenReturn(Optional.of(estilistaMateo));
        when(citaRepository.findOcupadasPorEstilistaYFecha(any(), any(), anyCollection(), any(), any())).thenReturn(List.of());

        DisponibilidadRespuesta resp = publicoServicio.consultarDisponibilidad(slug, 1L, 20L, fecha);

        assertNotNull(resp);
        assertEquals(fecha, resp.fecha());
        assertEquals("Corte clásico", resp.servicio());
        assertEquals("Mateo Gil", resp.estilista());
        assertEquals(45, resp.duracionMinutos());
        assertTrue(resp.horariosLibres().contains(LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("La disponibilidad se actualiza descartando cruces (9:30, 10:00 y 10:30) e incluye 11:00")
    void consultarDisponibilidad_actualizadaConCita() {
        String slug = "barberia-la-navaja";
        LocalDate fecha = LocalDate.of(2026, 10, 20);

        when(salonRepository.findBySlug(slug)).thenReturn(Optional.of(salon));
        when(servicioRepository.findByIdAndSalonId(1L, salon.getId())).thenReturn(Optional.of(servicioCorte));
        when(usuarioRepository.findByIdAndSalonIdAndRol(20L, salon.getId(), Rol.ESTILISTA)).thenReturn(Optional.of(estilistaMateo));

        // Supongamos que Valentina reservó a las 10:00 (termina a las 10:45)
        Cita citaValentina = new Cita();
        citaValentina.setInicio(LocalDateTime.of(2026, 10, 20, 10, 0));
        citaValentina.setFin(LocalDateTime.of(2026, 10, 20, 10, 45));
        citaValentina.setEstado(EstadoCita.PENDIENTE);

        when(citaRepository.findOcupadasPorEstilistaYFecha(any(), any(), anyCollection(), any(), any()))
                .thenReturn(List.of(citaValentina));

        DisponibilidadRespuesta resp = publicoServicio.consultarDisponibilidad(slug, 1L, 20L, fecha);

        // Para un corte de 45 min:
        // 9:30 choca (9:30 + 45 = 10:15 > 10:00)
        assertFalse(resp.horariosLibres().contains(LocalTime.of(9, 30)));
        // 10:00 choca
        assertFalse(resp.horariosLibres().contains(LocalTime.of(10, 0)));
        // 10:30 choca (10:30 < 10:45)
        assertFalse(resp.horariosLibres().contains(LocalTime.of(10, 30)));
        // 11:00 esta libre
        assertTrue(resp.horariosLibres().contains(LocalTime.of(11, 0)));
    }

    @Test
    @DisplayName("Reserva en línea delega a CitaServicio con origen EN_LINEA")
    void reservarEnLinea_delegaCorrectamente() {
        String slug = "barberia-la-navaja";
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 10, 0);
        CitaCreacionPeticion peticion = new CitaCreacionPeticion(
                "Valentina Ruiz", "300 111 2233", 1L, 20L, inicio, null);

        when(salonRepository.findBySlug(slug)).thenReturn(Optional.of(salon));
        CitaRespuesta mockRespuesta = new CitaRespuesta(
                50L, inicio, inicio.plusMinutes(45), EstadoCita.PENDIENTE, OrigenCita.EN_LINEA,
                35000, 1L, "Corte clásico", 20L, "Mateo Gil", "Valentina Ruiz", "3001112233", null,
                AHORA_FIJO, AHORA_FIJO);

        when(citaServicio.reservarEnLinea(salon.getId(), peticion)).thenReturn(mockRespuesta);

        CitaRespuesta respuesta = publicoServicio.reservarEnLinea(slug, peticion);

        assertNotNull(respuesta);
        assertEquals(EstadoCita.PENDIENTE, respuesta.estado());
        assertEquals(OrigenCita.EN_LINEA, respuesta.origen());
        verify(citaServicio).reservarEnLinea(salon.getId(), peticion);
    }
}
