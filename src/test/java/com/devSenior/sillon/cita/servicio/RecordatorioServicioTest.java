package com.devSenior.sillon.cita.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.cita.dto.RecordatorioRespuesta;
import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.IntegracionInvalidaException;
import com.devSenior.sillon.ia.dto.FacturaIa;
import com.devSenior.sillon.ia.dto.ResultadoIa;
import com.devSenior.sillon.ia.json.LectorJson;
import com.devSenior.sillon.ia.prompt.BibliotecaPrompts;
import com.devSenior.sillon.ia.servicio.AsistenteIa;
import com.devSenior.sillon.ia.servicio.CreditoIaServicio;

@ExtendWith(MockitoExtension.class)
class RecordatorioServicioTest {

	@Mock
	private LecturaCitaServicio lecturaCitaServicio;
	@Mock
	private CreditoIaServicio creditoIaServicio;
	@Mock
	private BibliotecaPrompts bibliotecaPrompts;
	@Mock
	private AsistenteIa asistenteIa;

	private RecordatorioServicio recordatorioServicio;

	@BeforeEach
	void preparar() {
		recordatorioServicio = new RecordatorioServicio(lecturaCitaServicio, creditoIaServicio, bibliotecaPrompts,
				asistenteIa, new LectorJson());
	}

	@Test
	void redactaConDatosRealesYDejaNueveCreditos() {
		when(lecturaCitaServicio.paraRecordatorio(1L, 8L)).thenReturn(new DatosRecordatorio(8L, 1L, "Sillon", "Sara",
				"3001112233", "Estilista: Sara. Hora: 10:30 a. m. Valor: $35.000.", LocalDateTime.of(2026, 12, 1, 10, 30)));
		when(bibliotecaPrompts.cargar(eq("recordatorio.txt"), any())).thenReturn("prompt");
		when(asistenteIa.redactar(eq("prompt"), any())).thenReturn(new ResultadoIa(
				"{\"mensaje\":\"Sara, te esperamos a las 10:30 a. m.\"}",
				new FacturaIa("openai", "gemini", 10, 20, BigDecimal.ZERO, 30)));
		when(creditoIaServicio.consumir(1L)).thenReturn(9);

		RecordatorioRespuesta respuesta = recordatorioServicio.generar(1L, 8L);

		assertEquals("Sara, te esperamos a las 10:30 a. m.", respuesta.mensaje());
		assertEquals(9, respuesta.creditosRestantes());
		assertEquals("openai", respuesta.factura().proveedor());
		assertEquals("3001112233", respuesta.telefonoCliente());
	}

	@Test
	void citaCompletadaNoLlamaNiGasta() {
		when(lecturaCitaServicio.paraRecordatorio(1L, 8L)).thenThrow(new ConflictoException("final"));
		assertThrows(ConflictoException.class, () -> recordatorioServicio.generar(1L, 8L));
		verify(asistenteIa, never()).redactar(any(), any());
		verify(creditoIaServicio, never()).verificar(anyLong());
		verify(creditoIaServicio, never()).consumir(anyLong());
	}

	@Test
	void jsonSinMensajeVacioOLargoNoDescuenta() {
		when(lecturaCitaServicio.paraRecordatorio(1L, 8L)).thenReturn(new DatosRecordatorio(8L, 1L, "Sillon", "Sara",
				"300", "datos", LocalDateTime.of(2026, 12, 1, 10, 30)));
		when(bibliotecaPrompts.cargar(any(), any())).thenReturn("prompt");
		when(asistenteIa.redactar(any(), any())).thenReturn(new ResultadoIa("{\"otro\":\"x\"}",
				new FacturaIa("openai", "m", 1, 1, BigDecimal.ZERO, 1)));
		assertThrows(IntegracionInvalidaException.class, () -> recordatorioServicio.generar(1L, 8L));

		when(asistenteIa.redactar(any(), any())).thenReturn(new ResultadoIa("{\"mensaje\":\"\"}",
				new FacturaIa("openai", "m", 1, 1, BigDecimal.ZERO, 1)));
		assertThrows(IntegracionInvalidaException.class, () -> recordatorioServicio.generar(1L, 8L));

		when(asistenteIa.redactar(any(), any())).thenReturn(new ResultadoIa(
				"{\"mensaje\":\"" + "a".repeat(701) + "\"}",
				new FacturaIa("openai", "m", 1, 1, BigDecimal.ZERO, 1)));
		assertThrows(IntegracionInvalidaException.class, () -> recordatorioServicio.generar(1L, 8L));
		verify(creditoIaServicio, never()).consumir(anyLong());
	}

}
