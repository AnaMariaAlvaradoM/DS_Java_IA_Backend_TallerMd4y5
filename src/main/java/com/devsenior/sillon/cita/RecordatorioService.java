package com.devsenior.sillon.cita;

import com.devsenior.sillon.cita.dto.DatosRecordatorioCita;
import com.devsenior.sillon.cita.dto.MensajeIaDto;
import com.devsenior.sillon.cita.dto.RecordatorioRespuestaDto;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosService;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.dto.FacturaIA;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RecordatorioService {

    private final CitaService citaService;
    private final CreditosService creditosService;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final AsistenteIA asistenteIA;
    private final LectorJsonIA lectorJsonIA;

    public RecordatorioService(
            CitaService citaService,
            CreditosService creditosService,
            BibliotecaPrompts bibliotecaPrompts,
            AsistenteIA asistenteIA,
            LectorJsonIA lectorJsonIA) {
        this.citaService = citaService;
        this.creditosService = creditosService;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.asistenteIA = asistenteIA;
        this.lectorJsonIA = lectorJsonIA;
    }

    public RecordatorioRespuestaDto generarRecordatorio(Long citaId, Long salonId) {
        DatosRecordatorioCita datos = citaService.obtenerDatosVerificadosRecordatorio(citaId, salonId);

        creditosService.verificar(salonId);

        String instrucciones = bibliotecaPrompts.cargar("recordatorio", Map.of(
                "salon", datos.nombreSalon(),
                "cliente", datos.nombreCliente(),
                "datos", datos.datosTexto()
        ));

        RespuestaIA respuestaIA = asistenteIA.completar(instrucciones, "Genera el recordatorio de cita en formato JSON.");

        int creditosRestantes = creditosService.consumir(salonId);


        MensajeIaDto mensajeDto = lectorJsonIA.leer(respuestaIA, MensajeIaDto.class);

        if (mensajeDto == null || mensajeDto.mensaje() == null || mensajeDto.mensaje().isBlank() || mensajeDto.mensaje().length() > 700) {
            throw new RespuestaIAInvalidaException(respuestaIA.proveedor(), respuestaIA.texto());
        }

        return new RecordatorioRespuestaDto(
                citaId,
                datos.telefonoCliente(),
                mensajeDto.mensaje(),
                creditosRestantes,
                FacturaIA.de(respuestaIA)
        );
    }
}
