package com.devsenior.sillon.cita;

import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.ia.FlujoIa;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RecordatorioServicio {
    private final CitaServicio citas;
    private final FlujoIa flujo;

    public RecordatorioServicio(CitaServicio citas, FlujoIa flujo) {
        this.citas = citas;
        this.flujo = flujo;
    }

    public RecordatorioRespuesta generar(Long citaId, Long salonId) {
        DatosRecordatorio datos = citas.datosRecordatorio(citaId, salonId);
        FlujoIa.Resultado resultado = flujo.ejecutar(() -> { }, salonId, "recordatorio.txt",
                Map.of("salon", datos.salon(), "cliente", datos.cliente(), "datos", datos.datos()),
                Map.of("mensaje", String.class));
        String mensaje = resultado.contenido().path("mensaje").asText();
        if (mensaje.isBlank() || mensaje.length() > 700) {
            throw new PasarelaException("Mensaje de IA inválido");
        }
        return new RecordatorioRespuesta(datos.citaId(), datos.telefonoCliente(), mensaje,
                resultado.creditosRestantes(), resultado.factura());
    }
}
