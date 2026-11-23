package com.devsenior.sillon.cita;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.devsenior.sillon.compartido.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.dto.FacturaIA;
import com.devsenior.sillon.salon.CreditoIaServicio;

@Service
public class RecordatorioServicio {

    private static final String PROMPT = "recordatorio";
    private static final String PETICION = "Redacta el recordatorio de la cita";
    private static final int LIMITE_MENSAJE = 700;

    private final CitaServicio citaServicio;
    private final CreditoIaServicio creditoIaServicio;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final AsistenteIA asistenteIA;
    private final LectorJsonIA lectorJsonIA;

    public RecordatorioServicio(CitaServicio citaServicio, CreditoIaServicio creditoIaServicio,
            BibliotecaPrompts bibliotecaPrompts, AsistenteIA asistenteIA, LectorJsonIA lectorJsonIA) {
        this.citaServicio = citaServicio;
        this.creditoIaServicio = creditoIaServicio;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.asistenteIA = asistenteIA;
        this.lectorJsonIA = lectorJsonIA;
    }

    public RecordatorioRespuesta generar(Long salonId, Long citaId) {
        DatosRecordatorio datos = citaServicio.datosVerificados(salonId, citaId);
        creditoIaServicio.verificar(salonId);
        String instrucciones = bibliotecaPrompts.cargar(PROMPT, Map.of(
                "salon", datos.nombreSalon(),
                "cliente", datos.nombreCliente(),
                "datos", datos.texto()));
        RespuestaIA respuesta = asistenteIA.completar(instrucciones, PETICION);
        int creditosRestantes = creditoIaServicio.consumir(salonId);
        MensajeRecordatorio mensaje = lectorJsonIA.leer(respuesta, MensajeRecordatorio.class);
        validarMensaje(mensaje.mensaje());
        return new RecordatorioRespuesta(citaId, datos.telefonoCliente(), mensaje.mensaje(),
                creditosRestantes, FacturaIA.de(respuesta));
    }

    private void validarMensaje(String texto) {
        if (texto == null || texto.isBlank() || texto.length() > LIMITE_MENSAJE) {
            throw new RespuestaIAInvalidaException(
                    "El mensaje del recordatorio está vacío o supera " + LIMITE_MENSAJE
                            + " caracteres");
        }
    }
}
