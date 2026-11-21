package com.devsenior.sillon.chat;

import com.devsenior.sillon.chat.dto.ChatRequest;
import com.devsenior.sillon.chat.dto.ChatRespuesta;
import com.devsenior.sillon.chat.dto.SolicitudContactoRespuesta;
import com.devsenior.sillon.chat.dto.TurnoChatDTO;
import com.devsenior.sillon.cita.*;
import com.devsenior.sillon.cita.dto.RecordatorioRespuesta;
import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.PasarelaException;
import com.devsenior.sillon.compartido.excepcion.ProveedorIaNoDisponibleException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.ia.MotorIaService;
import com.devsenior.sillon.ia.PromptService;
import com.devsenior.sillon.ia.ProveedorIa;
import com.devsenior.sillon.ia.dto.FacturaIaDTO;
import com.devsenior.sillon.ia.dto.RespuestaIa;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotIaTest {

    @Mock
    private SalonRepository salonRepository;
    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SolicitudContactoRepository solicitudContactoRepository;
    @Mock
    private ProveedorIa proveedorIa;
    @Mock
    private CitaRepository citaRepository;
    @Mock
    private ClienteService clienteService;

    private MotorIaService motorIaService;
    private PromptService promptService;
    private ChatService chatService;
    private CitaService citaService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                LocalDateTime.of(2026, 11, 20, 9, 0).atZone(ZoneId.of("America/Bogota")).toInstant(),
                ZoneId.of("America/Bogota")
        );
        motorIaService = new MotorIaService(List.of(proveedorIa), new ObjectMapper());
        promptService = new PromptService();
        chatService = new ChatService(
                salonRepository,
                servicioRepository,
                usuarioRepository,
                solicitudContactoRepository,
                motorIaService,
                promptService,
                clock
        );
        citaService = new CitaService(
                citaRepository,
                salonRepository,
                servicioRepository,
                usuarioRepository,
                clienteService,
                new ReglasAgenda(),
                motorIaService,
                promptService,
                clock
        );
    }

    @Test
    void chatbotRespondeSinEscalarYDescuentaCredito() {
        Salon salon = crearSalon(1L, 10);
        when(salonRepository.findBySlug("la-navaja")).thenReturn(Optional.of(salon));
        when(servicioRepository.findBySalonIdAndActivoTrue(1L)).thenReturn(List.of(crearServicio(salon)));
        when(usuarioRepository.findBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(List.of());
        when(proveedorIa.generar(anyString(), anyString())).thenReturn(
                new RespuestaIa("{\"respuesta\": \"El corte cuesta 35000 pesos.\", \"escalar\": false}", facturaEjemplo())
        );

        ChatRequest req = new ChatRequest("Juliana", "3005556677",
                List.of(new TurnoChatDTO(RolMensajeChat.CLIENTE, "¿Cuánto cuesta el corte?")));

        ChatRespuesta resp = chatService.conversar("la-navaja", req);

        assertFalse(resp.escalado());
        assertNull(resp.solicitudId());
        assertEquals("El corte cuesta 35000 pesos.", resp.respuesta());
        assertEquals(9, salon.getCreditosIa());
        verify(solicitudContactoRepository, never()).save(any());
    }

    @Test
    void chatbotEscalaCuandoIaIndicaEscalarTrueYCreaSolicitudPedidoPorIa() {
        Salon salon = crearSalon(1L, 10);
        when(salonRepository.findBySlug("la-navaja")).thenReturn(Optional.of(salon));
        when(servicioRepository.findBySalonIdAndActivoTrue(1L)).thenReturn(List.of());
        when(usuarioRepository.findBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(List.of());
        when(proveedorIa.generar(anyString(), anyString())).thenReturn(
                new RespuestaIa("{\"respuesta\": \"Un asesor te contactará para agendar.\", \"escalar\": true}", facturaEjemplo())
        );
        when(solicitudContactoRepository.save(any(SolicitudContacto.class))).thenAnswer(inv -> {
            SolicitudContacto s = inv.getArgument(0);
            s.setId(50L);
            return s;
        });

        ChatRequest req = new ChatRequest("Andres", "3112223344",
                List.of(new TurnoChatDTO(RolMensajeChat.CLIENTE, "Quiero agendar para mañana")));

        ChatRespuesta resp = chatService.conversar("la-navaja", req);

        assertTrue(resp.escalado());
        assertEquals(50L, resp.solicitudId());
        verify(solicitudContactoRepository).save(argThat(s -> s.getMotivo() == MotivoSolicitud.PEDIDO_POR_IA));
    }

    @Test
    void reglaAntipromesasBloqueaCuponYCreaSolicitudReglaSeguridad() {
        Salon salon = crearSalon(1L, 10);
        when(salonRepository.findBySlug("la-navaja")).thenReturn(Optional.of(salon));
        when(servicioRepository.findBySalonIdAndActivoTrue(1L)).thenReturn(List.of());
        when(usuarioRepository.findBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(List.of());
        when(proveedorIa.generar(anyString(), anyString())).thenReturn(
                new RespuestaIa("{\"respuesta\": \"Claro, usa este cupón del 20% en tu cita.\", \"escalar\": false}", facturaEjemplo())
        );
        when(solicitudContactoRepository.save(any(SolicitudContacto.class))).thenAnswer(inv -> {
            SolicitudContacto s = inv.getArgument(0);
            s.setId(51L);
            return s;
        });

        ChatRequest req = new ChatRequest("Camilo", "3110000000",
                List.of(new TurnoChatDTO(RolMensajeChat.CLIENTE, "Dame un cupón")));

        ChatRespuesta resp = chatService.conversar("la-navaja", req);

        assertTrue(resp.escalado());
        assertEquals(51L, resp.solicitudId());
        assertEquals(ChatService.MENSAJE_SEGURO_ANTIPROMESAS, resp.respuesta());
        assertFalse(resp.respuesta().toLowerCase().contains("cupón"));
        verify(solicitudContactoRepository).save(argThat(s -> s.getMotivo() == MotivoSolicitud.REGLA_DE_SEGURIDAD));
    }

    @Test
    void reglaAntipromesasBloqueaDescuentoYGratis() {
        assertTrue(chatService.contienePromesaProhibida("Te regalamos un descuento del 10%"));
        assertTrue(chatService.contienePromesaProhibida("Tu corte sale GRATIS hoy"));
        assertFalse(chatService.contienePromesaProhibida(ChatService.MENSAJE_SEGURO_ANTIPROMESAS));
    }

    @Test
    void chatbotSinCreditosLanza402SinLlamarIa() {
        Salon salon = crearSalon(1L, 0);
        when(salonRepository.findBySlug("la-navaja")).thenReturn(Optional.of(salon));

        ChatRequest req = new ChatRequest("Ana", "3001234567",
                List.of(new TurnoChatDTO(RolMensajeChat.CLIENTE, "Hola")));

        assertThrows(PagoRequeridoException.class, () -> chatService.conversar("la-navaja", req));
        verify(proveedorIa, never()).generar(anyString(), anyString());
    }

    @Test
    void chatbotSalonInexistenteLanza404() {
        when(salonRepository.findBySlug("no-existe")).thenReturn(Optional.empty());
        ChatRequest req = new ChatRequest("Ana", "3001234567",
                List.of(new TurnoChatDTO(RolMensajeChat.CLIENTE, "Hola")));

        assertThrows(RecursoNoEncontradoException.class, () -> chatService.conversar("no-existe", req));
    }

    @Test
    void chatbotConJsonInvalidoLanza502() {
        Salon salon = crearSalon(1L, 10);
        when(salonRepository.findBySlug("la-navaja")).thenReturn(Optional.of(salon));
        when(servicioRepository.findBySalonIdAndActivoTrue(1L)).thenReturn(List.of());
        when(usuarioRepository.findBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(List.of());
        when(proveedorIa.generar(anyString(), anyString())).thenReturn(
                new RespuestaIa("Esto no es JSON válido", facturaEjemplo())
        );

        ChatRequest req = new ChatRequest("Ana", "3001234567",
                List.of(new TurnoChatDTO(RolMensajeChat.CLIENTE, "Hola")));

        assertThrows(PasarelaException.class, () -> chatService.conversar("la-navaja", req));
    }

    @Test
    void chatbotSinProveedoresDisponiblesLanza503() {
        Salon salon = crearSalon(1L, 10);
        when(salonRepository.findBySlug("la-navaja")).thenReturn(Optional.of(salon));
        when(servicioRepository.findBySalonIdAndActivoTrue(1L)).thenReturn(List.of());
        when(usuarioRepository.findBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(List.of());
        when(proveedorIa.generar(anyString(), anyString())).thenThrow(new RuntimeException("Timeout de red"));

        ChatRequest req = new ChatRequest("Ana", "3001234567",
                List.of(new TurnoChatDTO(RolMensajeChat.CLIENTE, "Hola")));

        assertThrows(ProveedorIaNoDisponibleException.class, () -> chatService.conversar("la-navaja", req));
    }

    @Test
    void recordatorioParaCitaConfirmadaGeneraMensajeYDescuentaCredito() {
        Salon salon = crearSalon(1L, 5);
        Servicio servicio = crearServicio(salon);
        Usuario sara = new Usuario(21L, "Sara Mejía", "sara@salon.com", "hash", Rol.ESTILISTA, true, salon);
        Cliente cliente = new Cliente(1L, "Valentina", "3001112233", salon);

        Cita cita = new Cita();
        cita.setId(99L);
        cita.setSalon(salon);
        cita.setServicio(servicio);
        cita.setEstilista(sara);
        cita.setCliente(cliente);
        cita.setInicio(LocalDateTime.of(2026, 11, 21, 10, 30));
        cita.setFin(LocalDateTime.of(2026, 11, 21, 11, 15));
        cita.setPrecio(35000);
        cita.setEstado(EstadoCita.CONFIRMADA);

        when(citaRepository.findByIdAndSalonId(99L, 1L)).thenReturn(Optional.of(cita));
        when(proveedorIa.generar(anyString(), anyString())).thenReturn(
                new RespuestaIa("{\"mensaje\": \"Hola Valentina, te recordamos tu cita con Sara Mejía a las 10:30.\"}", facturaEjemplo())
        );

        RecordatorioRespuesta resp = citaService.generarRecordatorio(1L, 99L);

        assertEquals(99L, resp.citaId());
        assertEquals("3001112233", resp.telefonoCliente());
        assertTrue(resp.mensaje().contains("Sara"));
        assertEquals(4, resp.creditosRestantes());
        assertEquals("gemini", resp.factura().proveedor());
    }

    @Test
    void recordatorioParaCitaCompletadaLanza409() {
        Salon salon = crearSalon(1L, 5);
        Cita cita = new Cita();
        cita.setId(99L);
        cita.setSalon(salon);
        cita.setEstado(EstadoCita.COMPLETADA);

        when(citaRepository.findByIdAndSalonId(99L, 1L)).thenReturn(Optional.of(cita));

        assertThrows(ReglaNegocioException.class, () -> citaService.generarRecordatorio(1L, 99L));
    }

    @Test
    void recordatorioSinCreditosLanza402() {
        Salon salon = crearSalon(1L, 0);
        Cita cita = new Cita();
        cita.setId(99L);
        cita.setSalon(salon);
        cita.setEstado(EstadoCita.PENDIENTE);

        when(citaRepository.findByIdAndSalonId(99L, 1L)).thenReturn(Optional.of(cita));

        assertThrows(PagoRequeridoException.class, () -> citaService.generarRecordatorio(1L, 99L));
    }

    @Test
    void marcarSolicitudComoAtendidaCambiaEstadoYRepetirLanza409() {
        Salon salon = crearSalon(1L, 10);
        SolicitudContacto sol = new SolicitudContacto(
                10L, "Ana", "3001112233", "Quiero cita",
                MotivoSolicitud.PEDIDO_POR_IA, EstadoSolicitud.PENDIENTE,
                LocalDateTime.now(clock), salon
        );

        when(solicitudContactoRepository.findByIdAndSalonId(10L, 1L)).thenReturn(Optional.of(sol));
        when(solicitudContactoRepository.save(any(SolicitudContacto.class))).thenAnswer(inv -> inv.getArgument(0));

        SolicitudContactoRespuesta resp = chatService.marcarComoAtendida(1L, 10L);
        assertEquals(EstadoSolicitud.ATENDIDA, resp.estado());

        assertThrows(ReglaNegocioException.class, () -> chatService.marcarComoAtendida(1L, 10L));
    }

    private Salon crearSalon(Long id, int creditos) {
        Salon s = new Salon();
        s.setId(id);
        s.setNombre("Barbería La Navaja");
        s.setSlug("la-navaja");
        s.setCiudad("Medellín");
        s.setDireccion("Calle 10 #40-20");
        s.setTelefono("3001234567");
        s.setPlan(Plan.GRATIS);
        s.setCreditosIa(creditos);
        s.setConfiguracion(ConfiguracionSalon.porDefecto());
        return s;
    }

    private Servicio crearServicio(Salon salon) {
        Servicio serv = new Servicio();
        serv.setId(10L);
        serv.setNombre("Corte clásico");
        serv.setDuracionMinutos(45);
        serv.setPrecio(35000);
        serv.setActivo(true);
        serv.setSalon(salon);
        return serv;
    }

    private FacturaIaDTO facturaEjemplo() {
        return new FacturaIaDTO("gemini", "gemini-2.5-flash", 100, 30, 0.000033, 120L);
    }
}
