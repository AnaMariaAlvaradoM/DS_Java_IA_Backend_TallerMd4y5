package com.devsenior.sillon.cita.service;

import com.devsenior.sillon.cita.dto.DatosRecordatorio;
import com.devsenior.sillon.cita.dto.RecordatorioIA;
import com.devsenior.sillon.cita.dto.RecordatorioResponse;
import com.devsenior.sillon.ia.dto.FacturaIA;
import com.devsenior.sillon.ia.excepcion.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.model.RespuestaIA;
import com.devsenior.sillon.ia.service.AsistenteIA;
import com.devsenior.sillon.ia.service.BibliotecaPrompts;
import com.devsenior.sillon.ia.service.LectorJsonIA;
import com.devsenior.sillon.salon.service.CreditosService;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Recordatorio con IA. Sin @Transactional a propósito: la IA puede tardar y no debe ocupar una conexión.
 * Orden: 1. datos (404/409) → 2. créditos (402) → 3. prompt e IA → 4. consumir crédito → 5. validar JSON (502).
 */
@Service
public class RecordatorioService {

    private static final int MAXIMO_CARACTERES = 700;
    private static final String PETICION = "Redacta el recordatorio de la cita con los datos verificados.";

    private final CitaService citaService;
    private final CreditosService creditosService;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final AsistenteIA asistenteIA;
    private final LectorJsonIA lectorJsonIA;

    public RecordatorioService(CitaService citaService, CreditosService creditosService,
                               BibliotecaPrompts bibliotecaPrompts, AsistenteIA asistenteIA, LectorJsonIA lectorJsonIA) {
        this.citaService = citaService;
        this.creditosService = creditosService;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.asistenteIA = asistenteIA;
        this.lectorJsonIA = lectorJsonIA;
    }

    public RecordatorioResponse generar(Long salonId, Long citaId) {
        DatosRecordatorio datos = citaService.datosRecordatorio(salonId, citaId);

        creditosService.verificar(salonId);

        String instrucciones = bibliotecaPrompts.cargar("recordatorio", Map.of(
                "salon", datos.salonNombre(),
                "cliente", datos.clienteNombre(),
                "datos", datos.datos()));
        RespuestaIA respuesta = asistenteIA.completar(instrucciones, PETICION);

        int creditosRestantes = creditosService.consumir(salonId);

        RecordatorioIA recordatorio = lectorJsonIA.leer(respuesta, RecordatorioIA.class);
        String mensaje = recordatorio.mensaje() == null ? "" : recordatorio.mensaje().trim();
        if (mensaje.isEmpty()) {
            throw new RespuestaIAInvalidaException("el recordatorio no trae la clave mensaje");
        }
        if (mensaje.length() > MAXIMO_CARACTERES) {
            throw new RespuestaIAInvalidaException("el recordatorio supera los " + MAXIMO_CARACTERES + " caracteres");
        }
        return new RecordatorioResponse(datos.citaId(), datos.clienteTelefono(), mensaje, creditosRestantes,
                FacturaIA.de(respuesta));
    }
}
