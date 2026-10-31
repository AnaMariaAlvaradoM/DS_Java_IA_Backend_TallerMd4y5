package com.devsenior.sillon.cita;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosService;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.dto.FacturaIA;

@Service
public class RecordatorioService {

    private static final int LONGITUD_MAXIMA_MENSAJE = 700;

    private final CitaService citaService;
    private final CreditosService creditosService;
    private final AsistenteIA asistenteIA;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final LectorJsonIA lectorJsonIA;

    public RecordatorioService(CitaService citaService, CreditosService creditosService, AsistenteIA asistenteIA,
            BibliotecaPrompts bibliotecaPrompts, LectorJsonIA lectorJsonIA) {
        this.citaService = citaService;
        this.creditosService = creditosService;
        this.asistenteIA = asistenteIA;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.lectorJsonIA = lectorJsonIA;
    }

    public RecordatorioRespuesta generar(Long salonId, Long citaId) {
        DatosRecordatorio datos = citaService.obtenerDatosRecordatorio(salonId, citaId);

        creditosService.verificar(salonId);

        String instrucciones = bibliotecaPrompts.cargar("recordatorio", Map.of(
                "salon", datos.nombreSalon(),
                "cliente", datos.nombreCliente(),
                "datos", datos.datos()));
        RespuestaIA respuestaIA = asistenteIA.completar(instrucciones, "Genera el recordatorio para esta cita.");

        int creditosRestantes = creditosService.consumir(salonId);

        RecordatorioIaRespuesta json = lectorJsonIA.leer(respuestaIA, RecordatorioIaRespuesta.class);
        String mensaje = json.mensaje();
        if (mensaje == null || mensaje.isBlank() || mensaje.length() > LONGITUD_MAXIMA_MENSAJE) {
            throw new RespuestaIAInvalidaException(respuestaIA.proveedor(), respuestaIA.texto());
        }

        return new RecordatorioRespuesta(datos.citaId(), datos.telefonoCliente(), mensaje, creditosRestantes,
                FacturaIA.de(respuestaIA));
    }
}
