package com.devsenior.sillon.cita;

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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteServicio;
import com.devsenior.sillon.compartido.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CitaServicioTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteServicio clienteServicio;

    @Spy
    private ReglasAgenda reglasAgenda;

    @Spy
    private CitaMapper citaMapper;

    private Clock relojFijo;
    private CitaServicio citaServicio;

    private Salon salon;
    private Servicio servicioCorte;
    private Servicio servicioBarba;
    private Usuario estilistaMateo;
    private Usuario estilistaSara;
    private Cliente clienteValentina;

    // Martes 20 de octubre de 2026, 08:00:00 (America/Bogota)
    private static final LocalDateTime AHORA_FIJO = LocalDateTime.of(2026, 10, 20, 8, 0, 0);

    @BeforeEach
    void setUp() {
        ZoneId zonaBogota = ZoneId.of("America/Bogota");
        Instant instantFijo = AHORA_FIJO.atZone(zonaBogota).toInstant();
        relojFijo = Clock.fixed(instantFijo, zonaBogota);

        citaServicio = new CitaServicio(
                citaRepository,
                salonRepository,
                servicioRepository,
                usuarioRepository,
                clienteServicio,
                reglasAgenda,
                citaMapper,
                relojFijo);

        salon = new Salon();
        salon.setPlan(Plan.GRATIS);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());

        servicioCorte = new Servicio(1L, "Corte clásico", "Corte con tijera", 45, 35000, true, salon);
        servicioBarba = new Servicio(2L, "Arreglo de barba", "Perfilado", 30, 20000, true, salon);

        estilistaMateo = new Usuario();
        estilistaMateo.setId(20L);
        estilistaMateo.setNombre("Mateo Gil");
        estilistaMateo.setRol(Rol.ESTILISTA);
        estilistaMateo.setActivo(true);
        estilistaMateo.setSalon(salon);
        estilistaMateo.setServicios(Set.of(servicioCorte, servicioBarba));

        estilistaSara = new Usuario();
        estilistaSara.setId(21L);
        estilistaSara.setNombre("Sara Mejía");
        estilistaSara.setRol(Rol.ESTILISTA);
        estilistaSara.setActivo(true);
        estilistaSara.setSalon(salon);
        estilistaSara.setServicios(Set.of(servicioCorte)); // Sara solo hace corte

        clienteValentina = new Cliente(30L, "Valentina Ruiz", "3001112233", salon);
    }

    @Test
    @DisplayName("Cita válida queda CONFIRMADA, origen SALON y con el precio actual del servicio")
    void crearPorSalon_citaValida_quedaConfirmadaConPrecioYOrigenSalon() {
        Long salonId = 1L;
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 10, 30);
        CitaCreacionPeticion peticion = new CitaCreacionPeticion(
                "Valentina Ruiz", "3001112233", 1L, 20L, inicio, "Cliente puntual");

        when(servicioRepository.findByIdAndSalonId(1L, salonId)).thenReturn(Optional.of(servicioCorte));
        when(usuarioRepository.findByIdAndSalonIdAndRol(20L, salonId, Rol.ESTILISTA))
                .thenReturn(Optional.of(estilistaMateo));
        when(salonRepository.findById(salonId)).thenReturn(Optional.of(salon));
        when(citaRepository.findCruce(eq(salonId), eq(20L), anyCollection(), eq(inicio), eq(inicio.plusMinutes(45))))
                .thenReturn(List.of());
        when(clienteServicio.buscarOCrear(salonId, "Valentina Ruiz", "3001112233"))
                .thenReturn(clienteValentina);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> {
            Cita c = invocation.getArgument(0);
            c.setId(100L);
            return c;
        });

        CitaRespuesta respuesta = citaServicio.crearPorSalon(salonId, peticion);

        assertNotNull(respuesta);
        assertEquals(100L, respuesta.id());
        assertEquals(EstadoCita.CONFIRMADA, respuesta.estado());
        assertEquals(OrigenCita.SALON, respuesta.origen());
        assertEquals(35000, respuesta.precio());
        assertEquals(inicio.plusMinutes(45), respuesta.fin());
        verify(citaRepository).save(any(Cita.class));
    }

    @Test
    @DisplayName("Cita en el pasado lanza 409")
    void crearPorSalon_citaEnElPasado_lanza409() {
        Long salonId = 1L;
        // Ayer respecto a AHORA_FIJO (2026-10-20T08:00)
        LocalDateTime inicioPasado = LocalDateTime.of(2026, 10, 19, 10, 0);
        CitaCreacionPeticion peticion = new CitaCreacionPeticion(
                "Valentina Ruiz", "3001112233", 1L, 20L, inicioPasado, null);

        when(servicioRepository.findByIdAndSalonId(1L, salonId)).thenReturn(Optional.of(servicioCorte));
        when(usuarioRepository.findByIdAndSalonIdAndRol(20L, salonId, Rol.ESTILISTA))
                .thenReturn(Optional.of(estilistaMateo));

        assertThrows(ReglaNegocioException.class, () -> citaServicio.crearPorSalon(salonId, peticion));
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Cita con cruce de horario lanza 409")
    void crearPorSalon_conCruce_lanza409() {
        Long salonId = 1L;
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 10, 30);
        CitaCreacionPeticion peticion = new CitaCreacionPeticion(
                "Valentina Ruiz", "3001112233", 1L, 20L, inicio, null);

        when(servicioRepository.findByIdAndSalonId(1L, salonId)).thenReturn(Optional.of(servicioCorte));
        when(usuarioRepository.findByIdAndSalonIdAndRol(20L, salonId, Rol.ESTILISTA))
                .thenReturn(Optional.of(estilistaMateo));
        when(salonRepository.findById(salonId)).thenReturn(Optional.of(salon));

        // Cita existente que se cruza
        Cita citaExistente = new Cita();
        citaExistente.setInicio(LocalDateTime.of(2026, 10, 20, 10, 0));
        citaExistente.setFin(LocalDateTime.of(2026, 10, 20, 10, 45));

        when(citaRepository.findCruce(eq(salonId), eq(20L), anyCollection(), any(), any()))
                .thenReturn(List.of(citaExistente));

        assertThrows(ReglaNegocioException.class, () -> citaServicio.crearPorSalon(salonId, peticion));
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Estilista que no ofrece el servicio lanza 409")
    void crearPorSalon_estilistaNoOfreceServicio_lanza409() {
        Long salonId = 1L;
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 10, 30);
        // Sara (21L) no ofrece barba (2L)
        CitaCreacionPeticion peticion = new CitaCreacionPeticion(
                "Andrés Gómez", "3102223344", 2L, 21L, inicio, null);

        when(servicioRepository.findByIdAndSalonId(2L, salonId)).thenReturn(Optional.of(servicioBarba));
        when(usuarioRepository.findByIdAndSalonIdAndRol(21L, salonId, Rol.ESTILISTA))
                .thenReturn(Optional.of(estilistaSara));

        assertThrows(ReglaNegocioException.class, () -> citaServicio.crearPorSalon(salonId, peticion));
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Estilista que toca cita de otro estilista lanza 404")
    void cambiarEstado_estilistaTocaCitaAjena_lanza404() {
        Long salonId = 1L;
        Long citaId = 50L;
        // Usuario autenticado es Sara (21L)
        UsuarioPrincipal usuarioSara = new UsuarioPrincipal(21L, salonId, "sara@navaja.co", "pass", Rol.ESTILISTA, true);

        // La cita pertenece a Mateo (20L)
        Cita citaDeMateo = new Cita();
        citaDeMateo.setId(citaId);
        citaDeMateo.setEstilista(estilistaMateo);
        citaDeMateo.setEstado(EstadoCita.CONFIRMADA);

        when(citaRepository.findByIdAndSalonId(citaId, salonId)).thenReturn(Optional.of(citaDeMateo));

        assertThrows(RecursoNoEncontradoException.class,
                () -> citaServicio.cambiarEstado(usuarioSara, citaId, EstadoCita.COMPLETADA));
    }

    @Test
    @DisplayName("Estilista que intenta cancelar su propia cita lanza 403")
    void cambiarEstado_estilistaIntentaCancelar_lanza403() {
        Long salonId = 1L;
        Long citaId = 50L;
        // Usuario autenticado es Mateo (20L)
        UsuarioPrincipal usuarioMateo = new UsuarioPrincipal(20L, salonId, "mateo@navaja.co", "pass", Rol.ESTILISTA, true);

        Cita citaDeMateo = new Cita();
        citaDeMateo.setId(citaId);
        citaDeMateo.setEstilista(estilistaMateo);
        citaDeMateo.setEstado(EstadoCita.CONFIRMADA);

        when(citaRepository.findByIdAndSalonId(citaId, salonId)).thenReturn(Optional.of(citaDeMateo));

        // El estilista solo puede pasar a COMPLETADA o NO_ASISTIO
        assertThrows(AccionNoPermitidaException.class,
                () -> citaServicio.cambiarEstado(usuarioMateo, citaId, EstadoCita.CANCELADA));
    }

    @Test
    @DisplayName("Estilista puede marcar su cita como COMPLETADA")
    void cambiarEstado_estilistaMarcaCompletada_exitoso() {
        Long salonId = 1L;
        Long citaId = 50L;
        UsuarioPrincipal usuarioMateo = new UsuarioPrincipal(20L, salonId, "mateo@navaja.co", "pass", Rol.ESTILISTA, true);

        Cita citaDeMateo = new Cita();
        citaDeMateo.setId(citaId);
        citaDeMateo.setEstilista(estilistaMateo);
        citaDeMateo.setServicio(servicioCorte);
        citaDeMateo.setCliente(clienteValentina);
        citaDeMateo.setEstado(EstadoCita.CONFIRMADA);
        citaDeMateo.setInicio(LocalDateTime.of(2026, 10, 20, 10, 0));
        citaDeMateo.setFin(LocalDateTime.of(2026, 10, 20, 10, 45));
        citaDeMateo.setPrecio(35000);
        citaDeMateo.setOrigen(OrigenCita.SALON);
        citaDeMateo.setFechaCreacion(AHORA_FIJO);
        citaDeMateo.setFechaActualizacion(AHORA_FIJO);

        when(citaRepository.findByIdAndSalonId(citaId, salonId)).thenReturn(Optional.of(citaDeMateo));
        when(citaRepository.saveAndFlush(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitaRespuesta respuesta = citaServicio.cambiarEstado(usuarioMateo, citaId, EstadoCita.COMPLETADA);

        assertEquals(EstadoCita.COMPLETADA, respuesta.estado());
        verify(citaRepository).saveAndFlush(citaDeMateo);
    }

    @Test
    @DisplayName("Transición de estado no permitida lanza 409")
    void cambiarEstado_transicionNoPermitida_lanza409() {
        Long salonId = 1L;
        Long citaId = 50L;
        UsuarioPrincipal usuarioDueno = new UsuarioPrincipal(1L, salonId, "camilo@navaja.co", "pass", Rol.DUENO, true);

        Cita cita = new Cita();
        cita.setId(citaId);
        cita.setEstado(EstadoCita.CONFIRMADA);

        when(citaRepository.findByIdAndSalonId(citaId, salonId)).thenReturn(Optional.of(cita));

        // CONFIRMADA -> PENDIENTE no es permitida
        assertThrows(ReglaNegocioException.class,
                () -> citaServicio.cambiarEstado(usuarioDueno, citaId, EstadoCita.PENDIENTE));
    }

    @Test
    @DisplayName("Dueño ve todas las citas del día; estilista solo las suyas")
    void listarPorDia_duenoVeTodasYEstilistaSoloLasSuyas() {
        Long salonId = 1L;
        LocalDate fecha = LocalDate.of(2026, 10, 20);
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();

        UsuarioPrincipal dueno = new UsuarioPrincipal(1L, salonId, "camilo@navaja.co", "pass", Rol.DUENO, true);
        UsuarioPrincipal estilista = new UsuarioPrincipal(20L, salonId, "mateo@navaja.co", "pass", Rol.ESTILISTA, true);

        Cita c1 = new Cita();
        c1.setId(1L);
        c1.setEstilista(estilistaMateo);
        c1.setServicio(servicioCorte);
        c1.setCliente(clienteValentina);
        c1.setPrecio(35000);
        c1.setEstado(EstadoCita.CONFIRMADA);
        c1.setOrigen(OrigenCita.SALON);

        Cita c2 = new Cita();
        c2.setId(2L);
        c2.setEstilista(estilistaSara);
        c2.setServicio(servicioCorte);
        c2.setCliente(clienteValentina);
        c2.setPrecio(35000);
        c2.setEstado(EstadoCita.CONFIRMADA);
        c2.setOrigen(OrigenCita.SALON);

        // Para el dueno se consultan todas las citas del salon
        when(citaRepository.findBySalonIdAndInicioBetweenOrderByInicioAsc(salonId, inicioDia, finDia))
                .thenReturn(List.of(c1, c2));

        List<CitaRespuesta> resultadoDueno = citaServicio.listarPorDia(dueno, fecha);
        assertEquals(2, resultadoDueno.size());

        // Para el estilista se consultan solo las suyas
        when(citaRepository.findBySalonIdAndEstilistaIdAndInicioBetweenOrderByInicioAsc(salonId, 20L, inicioDia, finDia))
                .thenReturn(List.of(c1));

        List<CitaRespuesta> resultadoEstilista = citaServicio.listarPorDia(estilista, fecha);
        assertEquals(1, resultadoEstilista.size());
        assertEquals(20L, resultadoEstilista.get(0).estilistaId());
    }
}
