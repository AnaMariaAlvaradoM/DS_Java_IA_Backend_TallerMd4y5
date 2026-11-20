package com.devsenior.sillon.cita;

import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosIaService;
import com.devsenior.sillon.ia.LectorJsonIa;
import com.devsenior.sillon.ia.RespuestaIa;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RecordatorioService {
    private final DatosRecordatorioService datos;
    private final CreditosIaService creditos;
    private final BibliotecaPrompts prompts;
    private final AsistenteIa ia;
    private final LectorJsonIa lector;

    public RecordatorioService(DatosRecordatorioService datos, CreditosIaService creditos,
                               BibliotecaPrompts prompts, AsistenteIa ia, LectorJsonIa lector) {
        this.datos = datos;
        this.creditos = creditos;
        this.prompts = prompts;
        this.ia = ia;
        this.lector = lector;
    }

    public RecordatorioRespuesta generar(Long salonId, Long citaId) {
        DatosRecordatorio contexto = datos.obtener(salonId, citaId);
        creditos.verificar(salonId);
        String instrucciones = prompts.cargar("recordatorio", Map.of("salon", contexto.salonNombre(),
                "cliente", contexto.clienteNombre(), "datos", contexto.datos()));
        RespuestaIa respuesta = ia.generar(instrucciones, "Redacte el borrador del recordatorio en JSON.");
        int restantes = creditos.consumir(salonId);
        String mensaje = lector.textoObligatorio(lector.leerObjeto(respuesta.texto()), "mensaje", 700);
        return new RecordatorioRespuesta(citaId, contexto.telefonoCliente(), mensaje, restantes,
                respuesta.factura());
    }
}
