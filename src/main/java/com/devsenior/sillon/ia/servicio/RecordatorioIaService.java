package com.devsenior.sillon.ia.servicio;

import com.devsenior.sillon.cita.dto.RecordatorioResponse;
import com.devsenior.sillon.cita.servicio.CitaService;
import com.devsenior.sillon.compartido.excepcion.ErrorPasarelaException;
import com.devsenior.sillon.ia.dto.RespuestaIa;
import com.devsenior.sillon.ia.util.BibliotecaPrompts;
import com.devsenior.sillon.ia.util.LectorJsonIa;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Service
public class RecordatorioIaService {

    private final CitaService citaService;
    private final CreditosIaService creditosIaService;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final AsistenteIaService asistenteIaService;
    private final LectorJsonIa lectorJsonIa;

    public RecordatorioIaService(
            CitaService citaService,
            CreditosIaService creditosIaService,
            BibliotecaPrompts bibliotecaPrompts,
            AsistenteIaService asistenteIaService,
            LectorJsonIa lectorJsonIa) {
        this.citaService = citaService;
        this.creditosIaService = creditosIaService;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.asistenteIaService = asistenteIaService;
        this.lectorJsonIa = lectorJsonIa;
    }

    public RecordatorioResponse generar(Long salonId, Long citaId) {
        // 1. Validar cita y armar datos verificados
        CitaService.DatosRecordatorio datos = citaService.obtenerDatosRecordatorio(salonId, citaId);

        // 2. Verificar créditos
        creditosIaService.verificar(salonId);

        // 3. Cargar prompt y llamar a la IA (fuera de transacción)
        String prompt = bibliotecaPrompts.cargar("recordatorio.txt", Map.of(
                "salon", datos.nombreSalon(),
                "cliente", datos.nombreCliente(),
                "datos", datos.datosTexto()));
        RespuestaIa respuestaIa = asistenteIaService.generar(prompt, "Genera el recordatorio para la cita.");

        // 4. Consumir el crédito
        int creditosRestantes = creditosIaService.consumir(salonId);

        // 5. Leer y validar el JSON
        JsonNode json = lectorJsonIa.leerJson(respuestaIa.contenido());
        String mensaje = json.path("mensaje").asText("").trim();
        if (mensaje.isEmpty() || mensaje.length() > 700) {
            throw new ErrorPasarelaException("La IA generó un recordatorio inválido");
        }

        // 6. Responder
        return new RecordatorioResponse(
                datos.citaId(),
                datos.telefonoCliente(),
                mensaje,
                creditosRestantes,
                respuestaIa.factura());
    }
}