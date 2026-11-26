
package com.devsenior.sillon.ia.proveedor;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.excepcion.ExcepcionServicioNoDisponible;
import com.devsenior.sillon.ia.dto.ResultadoProveedorIa;

@Component
public class AsistenteIa {

    private final ProveedorIa gemini;
    private final ProveedorIa claude;

    public AsistenteIa(
            ClienteOpenAiCompatibleIa gemini,
            ClienteClaudeIa claude) {

        this.gemini = gemini;
        this.claude = claude;
    }

    public ResultadoProveedorIa generar(
            String instrucciones,
            String mensajeUsuario) {

        try {

            return gemini.generar(
                    instrucciones,
                    mensajeUsuario);

        } catch (ExcepcionServicioNoDisponible falloGemini) {

            try {

                return claude.generar(
                        instrucciones,
                        mensajeUsuario);

            } catch (ExcepcionServicioNoDisponible falloClaude) {

                throw new ExcepcionServicioNoDisponible(
                        "Ningun proveedor de IA esta disponible");
            }
        }
    }
}
