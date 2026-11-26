package com.devsenior.sillon.cita;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditoService;
import com.devsenior.sillon.ia.LectorJson;
import com.devsenior.sillon.ia.RespuestaIa;

/**
 * Recordatorio con IA. Sin @Transactional a propósito: la llamada a la IA puede tardar y no debe
 * tener ocupada una conexión de la base de datos (Clase 19).
 */
@Service
public class RecordatorioService {

    private static final int MAXIMO_CARACTERES = 700;

    private final CitaService citaService;
    private final CreditoService creditoService;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final AsistenteIa asistenteIa;
    private final LectorJson lectorJson;

    public RecordatorioService(CitaService citaService, CreditoService creditoService,
            BibliotecaPrompts bibliotecaPrompts, AsistenteIa asistenteIa, LectorJson lectorJson) {
        this.citaService = citaService;
        this.creditoService = creditoService;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.asistenteIa = asistenteIa;
        this.lectorJson = lectorJson;
    }

    public RecordatorioRespuesta generar(Long salonId, Long citaId) {
        // 1. Lo que no depende de la IA: la cita existe (404) y está PENDIENTE o CONFIRMADA (409)
        DatosRecordatorio datos = citaService.datosRecordatorio(salonId, citaId);
        // 2. Créditos (402 sin llamar a la IA)
        creditoService.verificar(salonId);
        // 3. Prompt y llamada a la IA
        String instrucciones = bibliotecaPrompts.cargar("recordatorio", Map.of(
                "salon", datos.salonNombre(),
                "cliente", datos.clienteNombre(),
                "datos", datos.datos()));
        RespuestaIa respuesta = asistenteIa.generar(instrucciones,
                "Redacta ahora el borrador del recordatorio para " + datos.clienteNombre() + ".");
        // 4. La IA ya respondió: los tokens ya se gastaron
        int creditosRestantes = creditoService.consumir(salonId);
        // 5. Leer y validar el JSON (502 si no sirve)
        String mensaje = lectorJson.textoObligatorio(lectorJson.leer(respuesta.texto()), "mensaje",
                MAXIMO_CARACTERES);
        // 6. Responder. Sillón no envía el mensaje: una persona lo aprueba.
        return new RecordatorioRespuesta(datos.citaId(), datos.clienteTelefono(), mensaje, creditosRestantes,
                respuesta.factura());
    }
}
