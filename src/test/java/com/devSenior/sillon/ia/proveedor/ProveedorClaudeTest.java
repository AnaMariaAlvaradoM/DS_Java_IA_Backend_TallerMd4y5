package com.devSenior.sillon.ia.proveedor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devSenior.sillon.ia.dto.MensajeIa;

class ProveedorClaudeTest {

	@Test
	void alternaRolesPorqueMessagesApiNoAdmiteDosSeguidos() {
		List<java.util.Map<String, String>> mensajes = ProveedorClaude.alternar(List.of(
				new MensajeIa("user", "precio"),
				new MensajeIa("user", "horario"),
				new MensajeIa("assistant", "el corte cuesta 35000")));
		assertEquals(2, mensajes.size());
		assertEquals("user", mensajes.get(0).get("role"));
		assertEquals("precio\nhorario", mensajes.get(0).get("content"));
		assertEquals("assistant", mensajes.get(1).get("role"));
	}

}
