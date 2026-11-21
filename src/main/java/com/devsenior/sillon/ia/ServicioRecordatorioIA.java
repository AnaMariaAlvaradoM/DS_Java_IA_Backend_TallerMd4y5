
package com.devsenior.sillon.ia;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.devsenior.sillon.cita.CitaService;
import com.devsenior.sillon.cita.DatosRecordatorio;
import com.devsenior.sillon.compartido.ErrorPasarelaException;

@Service
public class ServicioRecordatorioIA {

    private final CitaService citaService;
    private final ServicioCreditosIA servicioCreditos;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final AsistenteIA asistenteIA;
    private final LectorJsonIA lectorJson;

    public ServicioRecordatorioIA(
            CitaService citaService,
            ServicioCreditosIA servicioCreditos,
            BibliotecaPrompts bibliotecaPrompts,
            AsistenteIA asistenteIA,
            LectorJsonIA lectorJson
    ) {
        this.citaService = citaService;
        this.servicioCreditos = servicioCreditos;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.asistenteIA = asistenteIA;
        this.lectorJson = lectorJson;
    }

    public RespuestaRecordatorioIA generar(
            Long salonId,
            Long citaId
    ) {

        // 1. Validar la cita y obtener datos reales
        DatosRecordatorio datos =
                citaService.obtenerDatosRecordatorio(
                        salonId,
                        citaId
                );

        // 2. Verificar créditos disponibles
        servicioCreditos.verificar(salonId);

        // 3. Cargar el prompt con datos verificados
        String instrucciones = bibliotecaPrompts.cargar(
                "recordatorio",
                Map.of(
                        "salon", datos.nombreSalon(),
                        "cliente", datos.nombreCliente(),
                        "datos", datos.datos()
                )
        );

        // 4. Solicitar el borrador a la IA
        ProveedorIA.Resultado resultado =
                asistenteIA.generar(
                        instrucciones,
                        "Redacta el recordatorio y responde únicamente con el JSON solicitado."
                );

        // 5. Consumir el crédito después de la IA
        int creditosRestantes =
                servicioCreditos.consumir(salonId);

        // 6. Leer y validar la respuesta JSON
        String mensaje;

        try {
            var objeto = lectorJson.leerObjeto(
                    resultado.contenido()
            );

            mensaje = lectorJson.leerTextoObligatorio(
                    objeto,
                    "mensaje"
            );

        } catch (IllegalStateException ex) {
            throw new ErrorPasarelaException(
                    "La IA devolvió un recordatorio inválido"
            );
        }

        if (mensaje.length() > 700) {
            throw new ErrorPasarelaException(
                    "El recordatorio supera los 700 caracteres permitidos"
            );
        }

        // 7. Construir la respuesta final
        return new RespuestaRecordatorioIA(
                citaId,
                datos.telefonoCliente(),
                mensaje,
                creditosRestantes,
                resultado.factura()
        );
    }
}
