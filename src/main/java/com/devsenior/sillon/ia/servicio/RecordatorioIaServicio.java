
package com.devsenior.sillon.ia.servicio;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.cita.modelo.EstadoCita;
import com.devsenior.sillon.cita.repositorio.CitaRepositorio;
import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.ia.dto.RespuestaRecordatorioIa;
import com.devsenior.sillon.ia.dto.ResultadoProveedorIa;
import com.devsenior.sillon.ia.proveedor.AsistenteIa;

@Service
public class RecordatorioIaServicio {

        private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy",
                        Locale.forLanguageTag("es-CO"));

        private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

        private static final int MAXIMO_CARACTERES = 700;

        private final CitaRepositorio citaRepositorio;
        private final CreditoIaServicio creditoIaServicio;
        private final AsistenteIa asistenteIa;
        private final ValidadorJsonIa validadorJsonIa;

        public RecordatorioIaServicio(
                        CitaRepositorio citaRepositorio,
                        CreditoIaServicio creditoIaServicio,
                        AsistenteIa asistenteIa,
                        ValidadorJsonIa validadorJsonIa) {

                this.citaRepositorio = citaRepositorio;
                this.creditoIaServicio = creditoIaServicio;
                this.asistenteIa = asistenteIa;
                this.validadorJsonIa = validadorJsonIa;
        }

        public RespuestaRecordatorioIa generar(
                        Long salonId,
                        Long citaId) {

                Cita cita = citaRepositorio
                                .buscarPorIdYSalon(citaId, salonId)
                                .orElseThrow(
                                                () -> new ExcepcionRecursoNoEncontrado(
                                                                "Cita no encontrada"));

                validarEstado(cita);

                creditoIaServicio.verificarSaldo(salonId);

                String instrucciones = construirInstrucciones();
                String datos = construirDatosCita(cita);

                ResultadoProveedorIa resultado = asistenteIa.generar(instrucciones, datos);

                int creditosRestantes = creditoIaServicio.consumirCredito(salonId);

                JsonNode contenido = validadorJsonIa.interpretar(
                                resultado.contenido());

                String mensaje = validadorJsonIa.obtenerTexto(
                                contenido,
                                "mensaje").trim();

                validarMensaje(mensaje);

                return new RespuestaRecordatorioIa(
                                cita.getId(),
                                cita.getCliente().getTelefono(),
                                mensaje,
                                creditosRestantes,
                                resultado.factura());
        }

        private void validarEstado(Cita cita) {

                EstadoCita estado = cita.getEstado();

                if (estado != EstadoCita.PENDIENTE
                                && estado != EstadoCita.CONFIRMADA) {

                        throw new ExcepcionConflicto(
                                        "Solo se pueden generar recordatorios "
                                                        + "para citas pendientes o confirmadas");
                }
        }

        private String construirInstrucciones() {

                return """
                                Eres un asistente de una barberia o salon de belleza.

                                Tu tarea es redactar un recordatorio breve,
                                amable, profesional y claro para una cita.

                                Utiliza exclusivamente los datos proporcionados.

                                No inventes promociones, descuentos, precios,
                                servicios, fechas, horarios ni condiciones.

                                No cambies la fecha, la hora ni el valor
                                correspondiente a la cita.

                                No confirmes, canceles ni reprogrames la cita.

                                El mensaje sera revisado por una persona
                                antes de enviarse al cliente.

                                Escribe en espanol de Colombia.

                                El mensaje debe tener como maximo 700 caracteres.

                                Responde solamente con un objeto JSON valido
                                que contenga un unico campo de texto llamado
                                "mensaje".

                                Ejemplo de estructura:
                                {"mensaje":"Texto del recordatorio"}
                                """;
        }

        private String construirDatosCita(Cita cita) {

                String fecha = cita.getInicio()
                                .format(FORMATO_FECHA);

                String hora = cita.getInicio()
                                .format(FORMATO_HORA);

                return """
                                Redacta un recordatorio para esta cita.

                                Cliente: %s
                                Salon: %s
                                Servicio: %s
                                Estilista: %s
                                Fecha: %s
                                Hora: %s
                                Valor: %d COP
                                Dirección: %s

                                Conserva los datos exactos de la cita.
                                No agregues informacion que no este indicada.
                                """
                                .formatted(
                                                cita.getCliente().getNombre(),
                                                cita.getSalon().getNombre(),
                                                cita.getServicio().getNombre(),
                                                cita.getEstilista().getNombre(),
                                                fecha,
                                                hora,
                                                cita.getPrecio(),
                                                cita.getSalon().getDireccion());
        }

        private void validarMensaje(String mensaje) {

                if (mensaje.isBlank()) {
                        throw new ExcepcionIntegracion(
                                        "El proveedor de IA devolvio "
                                                        + "un recordatorio vacio");
                }

                if (mensaje.length() > MAXIMO_CARACTERES) {
                        throw new ExcepcionIntegracion(
                                        "El recordatorio generado supera "
                                                        + "el limite de 700 caracteres");
                }
        }
}
