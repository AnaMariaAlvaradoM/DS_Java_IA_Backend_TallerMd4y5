package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.Datos;
import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

/** Tests de agendar y cambiar estado, sin levantar Spring y con un reloj fijo (lunes 19 de octubre, 8:00). */
class CitaServiceTest {

    private final CitaRepository citaRepository = mock(CitaRepository.class);
    private final SalonRepository salonRepository = mock(SalonRepository.class);
    private final ServicioRepository servicioRepository = mock(ServicioRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ClienteService clienteService = mock(ClienteService.class);

    private CitaService citaService;
    private Salon salon;
    private Servicio corte;
    private Servicio barba;
    private Usuario sara;

    @BeforeEach
    void preparar() {
        citaService = new CitaService(citaRepository, salonRepository, servicioRepository, usuarioRepository,
                clienteService, new ReglasAgenda(), Datos.relojFijo());
        salon = Datos.salon(Plan.GRATIS);
        corte = Datos.servicio(10L, "Corte clásico", 45, 35000, salon);
        barba = Datos.servicio(11L, "Arreglo de barba", 30, 20000, salon);
        sara = Datos.estilista(20L, "Sara Mejía", salon, corte);

        Cliente andres = new Cliente();
        andres.setId(30L);
        andres.setNombre("Andrés Gómez");
        andres.setTelefono("3102223344");

        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(servicioRepository.findByIdAndSalonId(10L, 1L)).thenReturn(Optional.of(corte));
        when(servicioRepository.findByIdAndSalonId(11L, 1L)).thenReturn(Optional.of(barba));
        when(usuarioRepository.findByIdAndSalonIdAndRol(20L, 1L, Rol.ESTILISTA)).thenReturn(Optional.of(sara));
        when(clienteService.buscarOCrear(any(), anyString(), anyString())).thenReturn(andres);
        when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });
        when(citaRepository.saveAndFlush(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CitaPeticion peticion(Long servicioId, LocalDateTime inicio) {
        return new CitaPeticion("Andrés Gómez", "310 222 3344", servicioId, 20L, inicio, null);
    }

    @Test
    void citaValidaQuedaConfirmadaConElPrecioDelServicio() {
        CitaRespuesta cita = citaService.crearDesdeSalon(1L, peticion(10L, LocalDateTime.of(2026, 10, 20, 10, 30)));

        assertThat(cita.estado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(cita.origen()).isEqualTo(OrigenCita.SALON);
        assertThat(cita.precio()).isEqualTo(35000);
        assertThat(cita.fin()).isEqualTo(LocalDateTime.of(2026, 10, 20, 11, 15));
    }

    @Test
    void citaEnElPasadoNoSeGuarda() {
        assertThatThrownBy(() -> citaService.crearDesdeSalon(1L, peticion(10L, LocalDateTime.of(2026, 10, 18, 10, 0))))
                .isInstanceOf(ReglaNegocioException.class);
        verify(citaRepository, never()).save(any());
    }

    @Test
    void citaQueSeCruzaNoSeGuarda() {
        when(citaRepository.existsByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(eq(20L), anyCollection(),
                any(), any())).thenReturn(true);

        assertThatThrownBy(() -> citaService.crearDesdeSalon(1L, peticion(10L, LocalDateTime.of(2026, 10, 20, 10, 30))))
                .isInstanceOf(ReglaNegocioException.class);
        verify(citaRepository, never()).save(any());
    }

    @Test
    void estilistaQueNoOfreceElServicioEsRechazado() {
        assertThatThrownBy(() -> citaService.crearDesdeSalon(1L, peticion(11L, LocalDateTime.of(2026, 10, 20, 15, 0))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no ofrece");
        verify(citaRepository, never()).save(any());
    }

    @Test
    void servicioDeOtroSalonDa404() {
        when(servicioRepository.findByIdAndSalonId(anyLong(), eq(1L))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> citaService.crearDesdeSalon(1L, peticion(77L, LocalDateTime.of(2026, 10, 20, 10, 0))))
                .isInstanceOf(NoEncontradoException.class);
    }

    private Cita citaDe(Usuario estilista, EstadoCita estado) {
        Cita c = new Cita();
        c.setId(5L);
        c.setSalon(salon);
        c.setServicio(corte);
        c.setEstilista(estilista);
        Cliente cliente = new Cliente();
        cliente.setNombre("Valentina Ruiz");
        cliente.setTelefono("3001112233");
        c.setCliente(cliente);
        c.setInicio(LocalDateTime.of(2026, 10, 20, 10, 0));
        c.setFin(LocalDateTime.of(2026, 10, 20, 10, 45));
        c.setPrecio(35000); // la cita guarda el precio que tenía el servicio al agendar
        c.setEstado(estado);
        c.setOrigen(OrigenCita.EN_LINEA);
        c.setFechaCreacion(LocalDateTime.of(2026, 10, 19, 7, 0));
        c.setFechaActualizacion(LocalDateTime.of(2026, 10, 19, 7, 0));
        return c;
    }

    private UsuarioPrincipal mateo() {
        return new UsuarioPrincipal(21L, 1L, "Mateo Gil", "mateo@navaja.co", "x", Rol.ESTILISTA, true);
    }

    @Test
    void estilistaQueTocaUnaCitaAjenaRecibe404() {
        when(citaRepository.findByIdAndSalonId(5L, 1L)).thenReturn(Optional.of(citaDe(sara, EstadoCita.CONFIRMADA)));

        assertThatThrownBy(() -> citaService.cambiarEstado(mateo(), 5L, EstadoCita.COMPLETADA))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void estilistaQueIntentaCancelarRecibe403() {
        Usuario mateo = Datos.estilista(21L, "Mateo Gil", salon, corte, barba);
        when(citaRepository.findByIdAndSalonId(5L, 1L)).thenReturn(Optional.of(citaDe(mateo, EstadoCita.CONFIRMADA)));

        assertThatThrownBy(() -> citaService.cambiarEstado(mateo(), 5L, EstadoCita.CANCELADA))
                .isInstanceOf(AccionNoPermitidaException.class);
        verify(citaRepository, never()).saveAndFlush(any());
    }

    @Test
    void estilistaPuedeMarcarSuCitaCompletada() {
        Usuario mateo = Datos.estilista(21L, "Mateo Gil", salon, corte, barba);
        when(citaRepository.findByIdAndSalonId(5L, 1L)).thenReturn(Optional.of(citaDe(mateo, EstadoCita.CONFIRMADA)));

        CitaRespuesta r = citaService.cambiarEstado(mateo(), 5L, EstadoCita.COMPLETADA);

        assertThat(r.estado()).isEqualTo(EstadoCita.COMPLETADA);
        assertThat(r.fechaActualizacion()).isAfter(r.fechaCreacion());
    }

    @Test
    void transicionNoPermitidaNoSeGuarda() {
        UsuarioPrincipal dueno = new UsuarioPrincipal(1L, 1L, "Camilo Ríos", "camilo@navaja.co", "x", Rol.DUENO, true);
        when(citaRepository.findByIdAndSalonId(5L, 1L)).thenReturn(Optional.of(citaDe(sara, EstadoCita.CONFIRMADA)));

        assertThatThrownBy(() -> citaService.cambiarEstado(dueno, 5L, EstadoCita.PENDIENTE))
                .isInstanceOf(ReglaNegocioException.class);
        verify(citaRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordatorioDeCitaCompletadaDa409() {
        when(citaRepository.findByIdAndSalonId(5L, 1L)).thenReturn(Optional.of(citaDe(sara, EstadoCita.COMPLETADA)));
        assertThatThrownBy(() -> citaService.datosRecordatorio(1L, 5L)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void losDatosDelRecordatorioTraenUnaLineaPorDato() {
        when(citaRepository.findByIdAndSalonId(5L, 1L)).thenReturn(Optional.of(citaDe(sara, EstadoCita.CONFIRMADA)));

        DatosRecordatorio datos = citaService.datosRecordatorio(1L, 5L);

        assertThat(datos.datos()).contains("Servicio: Corte clásico (45 minutos)", "Estilista: Sara Mejía",
                "Hora: 10:00", "Valor: $35.000", "Dirección: Calle 10 # 43-20, Medellín");
        assertThat(datos.datos().lines()).hasSize(6);
    }
}
