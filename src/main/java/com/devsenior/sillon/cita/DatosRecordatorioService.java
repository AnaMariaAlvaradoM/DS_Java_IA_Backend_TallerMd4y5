package com.devsenior.sillon.cita;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatosRecordatorioService {
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private final CitaRepository citas;

    public DatosRecordatorioService(CitaRepository citas) {
        this.citas = citas;
    }

    @Transactional(readOnly = true)
    public DatosRecordatorio obtener(Long salonId, Long citaId) {
        Cita cita = citas.findByIdAndSalonId(citaId, salonId)
                .orElseThrow(() -> new NoEncontradoException("La cita no existe en este salón"));
        if (!cita.getEstado().ocupaAgenda()) {
            throw new ReglaNegocioException("Solo se pueden recordar citas pendientes o confirmadas");
        }
        String fecha = cita.getInicio().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", COLOMBIA));
        String datos = "Servicio: " + cita.getServicio().getNombre() + " ("
                + cita.getServicio().getDuracionMinutos() + " minutos)\n"
                + "Estilista: " + cita.getEstilista().getNombre() + "\n"
                + "Fecha: " + fecha + "\n"
                + "Hora: " + hora(cita.getInicio().toLocalTime()) + "\n"
                + "Valor: $" + String.format(Locale.ROOT, "%,d", cita.getPrecio()).replace(',', '.') + "\n"
                + "Dirección: " + cita.getSalon().getDireccion() + ", " + cita.getSalon().getCiudad();
        return new DatosRecordatorio(salonId, citaId, cita.getSalon().getNombre(),
                cita.getCliente().getNombre(), cita.getCliente().getTelefono(), datos);
    }

    private String hora(LocalTime hora) {
        int h = hora.getHour() % 12 == 0 ? 12 : hora.getHour() % 12;
        return String.format(Locale.ROOT, "%d:%02d %s", h, hora.getMinute(),
                hora.getHour() < 12 ? "a. m." : "p. m.");
    }
}
