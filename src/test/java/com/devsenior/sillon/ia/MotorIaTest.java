package com.devsenior.sillon.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.IaNoDisponibleException;
import com.devsenior.sillon.compartido.PasarelaException;

/** Fallback entre proveedores, clasificación de errores, biblioteca de prompts y lector de JSON. */
class MotorIaTest {

    private static ProveedorIa proveedorQueFalla(String nombre, boolean deConfiguracion) {
        return new ProveedorIa() {
            public String nombre() {
                return nombre;
            }

            public RespuestaIa generar(String instrucciones, String mensaje) {
                throw new ErrorProveedorIa("falla", deConfiguracion);
            }
        };
    }

    private static ProveedorIa proveedorQueResponde(String nombre) {
        return new ProveedorIa() {
            public String nombre() {
                return nombre;
            }

            public RespuestaIa generar(String instrucciones, String mensaje) {
                return new RespuestaIa("{\"mensaje\":\"ok\"}", nombre, "modelo", 10, 5, 50, 0);
            }
        };
    }

    @Test
    void siElPrimerProveedorFallaRespondeElDeRespaldo() {
        AsistenteIa asistente = new AsistenteIa(List.of(proveedorQueFalla("gemini", true), proveedorQueResponde("claude")));
        assertThat(asistente.generar("a", "b").proveedor()).isEqualTo("claude");
    }

    @Test
    void siNingunProveedorRespondeDa503() {
        AsistenteIa asistente = new AsistenteIa(
                List.of(proveedorQueFalla("gemini", false), proveedorQueFalla("claude", true)));
        assertThatThrownBy(() -> asistente.generar("a", "b")).isInstanceOf(IaNoDisponibleException.class);
    }

    @Test
    void losErroresDeLlaveSonDeConfiguracionYLosDeLimiteSonTemporales() {
        assertThat(ErrorProveedorIa.esErrorDeConfiguracion(401)).isTrue();
        assertThat(ErrorProveedorIa.esErrorDeConfiguracion(404)).isTrue();
        assertThat(ErrorProveedorIa.esErrorDeConfiguracion(429)).isFalse();
        assertThat(ErrorProveedorIa.esErrorDeConfiguracion(503)).isFalse();
    }

    @Test
    void laBibliotecaReemplazaLasVariablesDelPrompt() {
        String prompt = new BibliotecaPrompts().cargar("recordatorio",
                Map.of("salon", "Barbería La Navaja", "cliente", "Andrés", "datos", "Hora: 10:30 a. m."));
        assertThat(prompt).contains("Barbería La Navaja", "Andrés", "Hora: 10:30 a. m.", "\"mensaje\"");
        assertThat(prompt).doesNotContain("{{");
    }

    @Test
    void laBibliotecaFallaSiFaltaUnaVariable() {
        assertThatThrownBy(() -> new BibliotecaPrompts().cargar("recordatorio", Map.of("salon", "X")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void elLectorAceptaJsonEnvueltoEnBloqueDeCodigo() {
        Map<String, Object> json = new LectorJson().leer("```json\n{\"respuesta\":\"hola\",\"escalar\":true}\n```");
        assertThat(json).containsEntry("respuesta", "hola").containsEntry("escalar", true);
    }

    @Test
    void elLectorRechazaTextoQueNoEsJson() {
        assertThatThrownBy(() -> new LectorJson().leer("esto no es json")).isInstanceOf(PasarelaException.class);
    }

    @Test
    void elLectorRechazaUnCampoFaltante() {
        LectorJson lector = new LectorJson();
        assertThatThrownBy(() -> lector.textoObligatorio(lector.leer("{\"otro\":1}"), "mensaje", 700))
                .isInstanceOf(PasarelaException.class);
    }
}
