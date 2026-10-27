package com.devsenior.sillon.cita;

import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosService;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.dto.FacturaIA;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RecordatorioService {
    record BorradorIa(Object mensaje) { }

    private final CitaService citas;
    private final CreditosService creditos;
    private final BibliotecaPrompts prompts;
    private final AsistenteIA asistente;
    private final LectorJsonIA lector;

    public RecordatorioService(CitaService citas, CreditosService creditos, BibliotecaPrompts prompts,
            AsistenteIA asistente, LectorJsonIA lector) {
        this.citas = citas;
        this.creditos = creditos;
        this.prompts = prompts;
        this.asistente = asistente;
        this.lector = lector;
    }

    public RecordatorioResponse generar(Long salonId, Long citaId) {
        DatosRecordatorio datos = citas.datosRecordatorio(salonId, citaId);
        creditos.verificar(salonId);
        String instrucciones = prompts.cargar("recordatorio", Map.of(
                "salon", datos.salonNombre(), "cliente", datos.clienteNombre(), "datos", datos.datos()));
        var respuesta = asistente.completar(instrucciones, datos.datos());
        int restantes = creditos.consumir(salonId);
        BorradorIa borrador = lector.leer(respuesta, BorradorIa.class);
        if (!(borrador.mensaje() instanceof String mensaje) || mensaje.isBlank() || mensaje.length() > 700) {
            throw new RespuestaIAInvalidaException(respuesta.proveedor(), respuesta.texto());
        }
        return new RecordatorioResponse(datos.citaId(), datos.telefonoCliente(), mensaje, restantes, FacturaIA.de(respuesta));
    }
}
