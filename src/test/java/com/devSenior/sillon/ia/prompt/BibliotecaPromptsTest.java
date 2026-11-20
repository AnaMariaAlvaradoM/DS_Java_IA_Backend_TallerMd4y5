package com.devSenior.sillon.ia.prompt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class BibliotecaPromptsTest {

	private final BibliotecaPrompts biblioteca = new BibliotecaPrompts();

	@Test
	void sustituyeVariablesYPideLasClaves() {
		String recordatorio = biblioteca.cargar("recordatorio.txt", Map.of(
				"salón", "Sillon",
				"cliente", "Sara",
				"datos", "Hora: 10:30 a. m."));
		assertTrue(recordatorio.contains("Sara"));
		assertTrue(recordatorio.contains("10:30 a. m."));
		assertTrue(recordatorio.contains("\"mensaje\""));
		assertFalse(recordatorio.contains("{{"));

		String chat = biblioteca.cargar("chatbot.txt", Map.of(
				"salón", "Sillon",
				"dirección", "Calle 1",
				"horario", "08:00 a 19:00",
				"catálogo", "Corte — 45 min — $35.000",
				"cliente", "Ana"));
		assertTrue(chat.contains("Calle 1"));
		assertTrue(chat.contains("$35.000"));
		assertTrue(chat.contains("\"respuesta\""));
		assertTrue(chat.contains("\"escalar\""));
	}

}
