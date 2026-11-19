package com.devsenior.sillon.cita;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cita.dto.RecordatorioResponse;
import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.LectorJsonIa;
import com.devsenior.sillon.ia.RespuestaIa;
import com.devsenior.sillon.ia.RespuestaIaInvalidaException;
import com.devsenior.sillon.salon.CreditosIaService;

@Service
public class RecordatorioService {

    private final CitaService citas;
    private final CreditosIaService creditos;
    private final BibliotecaPrompts prompts;
    private final AsistenteIa asistente;
    private final LectorJsonIa json;

    public RecordatorioService(CitaService citas, CreditosIaService creditos, BibliotecaPrompts prompts,
            AsistenteIa asistente, LectorJsonIa json) {
        this.citas = citas;
        this.creditos = creditos;
        this.prompts = prompts;
        this.asistente = asistente;
        this.json = json;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RecordatorioResponse generar(Long salonId, Long citaId) {
        var datos = citas.datosParaRecordatorio(salonId, citaId);
        creditos.verificar(salonId);
        String prompt = prompts.recordatorio(datos.salon(), datos.cliente(), datos.datos());
        RespuestaIa respuesta;
        try {
            respuesta = asistente.responder(prompt);
        } catch (RespuestaIaInvalidaException ex) {
            // Hubo respuesta HTTP, aunque no pudiera interpretarse; los tokens ya se gastaron.
            creditos.consumir(salonId);
            throw ex;
        }
        int restantes = creditos.consumir(salonId);
        String mensaje = json.mensaje(respuesta.texto());
        if (!mensaje.toLowerCase(Locale.ROOT).contains(datos.cliente().toLowerCase(Locale.ROOT))
                || !mensaje.contains(datos.hora())) {
            throw new RespuestaIaInvalidaException();
        }
        return new RecordatorioResponse(datos.citaId(), datos.telefonoCliente(), mensaje, restantes, respuesta.factura());
    }
}
