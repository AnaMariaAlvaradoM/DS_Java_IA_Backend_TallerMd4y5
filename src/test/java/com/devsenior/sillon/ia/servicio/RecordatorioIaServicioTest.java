
package com.devsenior.sillon.ia.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.cita.modelo.EstadoCita;
import com.devsenior.sillon.cita.repositorio.CitaRepositorio;
import com.devsenior.sillon.cliente.modelo.Cliente;
import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionPagoRequerido;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.compartido.excepcion.ExcepcionServicioNoDisponible;
import com.devsenior.sillon.ia.dto.FacturaIa;
import com.devsenior.sillon.ia.dto.RespuestaRecordatorioIa;
import com.devsenior.sillon.ia.dto.ResultadoProveedorIa;
import com.devsenior.sillon.ia.proveedor.AsistenteIa;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.usuario.modelo.Usuario;

@ExtendWith(MockitoExtension.class)
class RecordatorioIaServicioTest {

        private static final Long SALON_ID = 10L;
        private static final Long CITA_ID = 25L;

        @Mock
        private CitaRepositorio citaRepositorio;

        @Mock
        private CreditoIaServicio creditoIaServicio;

        @Mock
        private AsistenteIa asistenteIa;

        @Mock
        private ValidadorJsonIa validadorJsonIa;

        @InjectMocks
        private RecordatorioIaServicio recordatorioIaServicio;

        private Cita cita;

        @BeforeEach
        void preparar() {
                cita = mock(Cita.class);
        }

        @Test
        void rechazaCitaQueNoPerteneceAlSalon() {

                when(citaRepositorio.buscarPorIdYSalon(CITA_ID, SALON_ID))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ExcepcionRecursoNoEncontrado.class,
                                () -> recordatorioIaServicio.generar(
                                                SALON_ID, CITA_ID));

                verifyNoInteractions(
                                creditoIaServicio,
                                asistenteIa);
        }

        @Test
        void rechazaCitaCanceladaSinConsumirCreditos() {

                when(citaRepositorio.buscarPorIdYSalon(CITA_ID, SALON_ID))
                                .thenReturn(Optional.of(cita));

                when(cita.getEstado())
                                .thenReturn(EstadoCita.CANCELADA);

                assertThrows(
                                ExcepcionConflicto.class,
                                () -> recordatorioIaServicio.generar(
                                                SALON_ID, CITA_ID));

                verifyNoInteractions(
                                creditoIaServicio,
                                asistenteIa);
        }

        @Test
        void noInvocaProveedorCuandoNoHayCreditos() {

                when(citaRepositorio.buscarPorIdYSalon(CITA_ID, SALON_ID))
                                .thenReturn(Optional.of(cita));

                when(cita.getEstado())
                                .thenReturn(EstadoCita.CONFIRMADA);

                doThrow(
                                new ExcepcionPagoRequerido("Sin creditos de IA"))
                                .when(creditoIaServicio)
                                .verificarSaldo(SALON_ID);

                assertThrows(
                                ExcepcionPagoRequerido.class,
                                () -> recordatorioIaServicio.generar(
                                                SALON_ID, CITA_ID));

                verifyNoInteractions(asistenteIa);

                verify(creditoIaServicio, never())
                                .consumirCredito(anyLong());
        }

        @Test
        void generaRecordatorioYDescuentaExactamenteUnCredito()
                        throws Exception {

                prepararCitaCompleta(true);

                FacturaIa factura = new FacturaIa(
                                "Gemini",
                                "gemini-2.5-flash",
                                100,
                                35,
                                BigDecimal.ZERO,
                                150);

                String respuestaJson = "{\"mensaje\":\"Hola Ana, recuerda tu cita mañana.\"}";

                when(asistenteIa.generar(anyString(), anyString()))
                                .thenReturn(
                                                new ResultadoProveedorIa(
                                                                respuestaJson,
                                                                factura));

                when(validadorJsonIa.interpretar(respuestaJson))
                                .thenReturn(
                                                new ObjectMapper().readTree(respuestaJson));

                when(validadorJsonIa.obtenerTexto(
                                any(), eq("mensaje")))
                                .thenReturn(
                                                "Hola Ana, recuerda tu cita mañana.");

                when(creditoIaServicio.consumirCredito(SALON_ID))
                                .thenReturn(4);

                RespuestaRecordatorioIa respuesta = recordatorioIaServicio.generar(
                                SALON_ID,
                                CITA_ID);

                assertEquals(CITA_ID, respuesta.citaId());

                assertEquals(
                                "3001234567",
                                respuesta.telefonoCliente());

                assertEquals(
                                "Hola Ana, recuerda tu cita mañana.",
                                respuesta.mensaje());

                assertEquals(
                                4,
                                respuesta.creditosRestantes());

                assertEquals(
                                factura,
                                respuesta.factura());

                InOrder orden = inOrder(
                                creditoIaServicio,
                                asistenteIa,
                                validadorJsonIa);

                orden.verify(creditoIaServicio)
                                .verificarSaldo(SALON_ID);

                orden.verify(asistenteIa)
                                .generar(anyString(), anyString());

                orden.verify(creditoIaServicio)
                                .consumirCredito(SALON_ID);

                orden.verify(validadorJsonIa)
                                .interpretar(respuestaJson);

                verify(creditoIaServicio, times(1))
                                .consumirCredito(SALON_ID);

                verify(asistenteIa, times(1))
                                .generar(anyString(), anyString());
        }

        @Test
        void respuestaInvalidaConsumeCreditoYProduceError502() {

                prepararCitaCompleta(false);

                FacturaIa factura = new FacturaIa(
                                "Gemini",
                                "gemini-2.5-flash",
                                20,
                                10,
                                BigDecimal.ZERO,
                                100);

                String contenidoInvalido = "respuesta-no-json";

                when(asistenteIa.generar(anyString(), anyString()))
                                .thenReturn(
                                                new ResultadoProveedorIa(
                                                                contenidoInvalido,
                                                                factura));

                when(validadorJsonIa.interpretar(contenidoInvalido))
                                .thenThrow(
                                                new ExcepcionIntegracion(
                                                                "JSON invalido"));

                assertThrows(
                                ExcepcionIntegracion.class,
                                () -> recordatorioIaServicio.generar(
                                                SALON_ID,
                                                CITA_ID));

                InOrder orden = inOrder(
                                asistenteIa,
                                creditoIaServicio,
                                validadorJsonIa);

                orden.verify(asistenteIa)
                                .generar(anyString(), anyString());

                orden.verify(creditoIaServicio)
                                .consumirCredito(SALON_ID);

                orden.verify(validadorJsonIa)
                                .interpretar(contenidoInvalido);

                verify(creditoIaServicio, times(1))
                                .consumirCredito(SALON_ID);
        }

        @Test
        void proveedorNoDisponibleNoConsumeCreditos() {

                prepararCitaCompleta(false);

                when(asistenteIa.generar(anyString(), anyString()))
                                .thenThrow(
                                                new ExcepcionServicioNoDisponible(
                                                                "Ningun proveedor de IA esta disponible"));

                assertThrows(
                                ExcepcionServicioNoDisponible.class,
                                () -> recordatorioIaServicio.generar(
                                                SALON_ID,
                                                CITA_ID));

                verify(creditoIaServicio)
                                .verificarSaldo(SALON_ID);

                verify(asistenteIa, times(1))
                                .generar(anyString(), anyString());

                verify(creditoIaServicio, never())
                                .consumirCredito(anyLong());

                verifyNoInteractions(validadorJsonIa);
        }

        private void prepararCitaCompleta(
                        boolean incluirDatosDeRespuesta) {

                Salon salon = mock(Salon.class);
                Cliente cliente = mock(Cliente.class);
                Usuario estilista = mock(Usuario.class);
                Servicio servicio = mock(Servicio.class);

                when(citaRepositorio.buscarPorIdYSalon(CITA_ID, SALON_ID))
                                .thenReturn(Optional.of(cita));

                when(cita.getEstado())
                                .thenReturn(EstadoCita.PENDIENTE);

                when(cita.getSalon())
                                .thenReturn(salon);

                when(cita.getCliente())
                                .thenReturn(cliente);

                when(cita.getEstilista())
                                .thenReturn(estilista);

                when(cita.getServicio())
                                .thenReturn(servicio);

                when(cita.getInicio())
                                .thenReturn(
                                                LocalDateTime.of(
                                                                2026, 11, 26, 15, 30));

                when(cita.getPrecio())
                                .thenReturn(35000);

                when(salon.getNombre())
                                .thenReturn("Salon Central");

                when(salon.getDireccion())
                                .thenReturn("Calle 10 # 20-30");

                when(cliente.getNombre())
                                .thenReturn("Ana");

                when(estilista.getNombre())
                                .thenReturn("Carlos");

                when(servicio.getNombre())
                                .thenReturn("Corte de cabello");

                if (incluirDatosDeRespuesta) {

                        when(cita.getId())
                                        .thenReturn(CITA_ID);

                        when(cliente.getTelefono())
                                        .thenReturn("3001234567");
                }
        }
}
