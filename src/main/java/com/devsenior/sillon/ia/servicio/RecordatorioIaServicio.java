
package com.devsenior.sillon.ia.servicio;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.cita.modelo.EstadoCita;
import com.devsenior.sillon.cita.repositorio.CitaRepositorio;
import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.dto.RespuestaRecordatorioIa;
import com.devsenior.sillon.ia.dto.ResultadoProveedorIa;
import com.devsenior.sillon.ia.proveedor.AsistenteIa;

@Service
public class RecordatorioIaServicio {

        private static final Locale IDIOMA_COLOMBIA = Locale.forLanguageTag("es-CO");

        private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern(
                        "EEEE d 'de' MMMM",
                        IDIOMA_COLOMBIA);

        private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("h:mm");

        private static final int MAXIMO_CARACTERES = 700;

        private final CitaRepositorio citaRepositorio;
        private final CreditoIaServicio creditoIaServicio;
        private final AsistenteIa asistenteIa;
        private final ValidadorJsonIa validadorJsonIa;
        private final BibliotecaPrompts bibliotecaPrompts;

        public RecordatorioIaServicio(
                        CitaRepositorio citaRepositorio,
                        CreditoIaServicio creditoIaServicio,
                        AsistenteIa asistenteIa,
                        ValidadorJsonIa validadorJsonIa,
                        BibliotecaPrompts bibliotecaPrompts) {

                this.citaRepositorio = citaRepositorio;
                this.creditoIaServicio = creditoIaServicio;
                this.asistenteIa = asistenteIa;
                this.validadorJsonIa = validadorJsonIa;
                this.bibliotecaPrompts = bibliotecaPrompts;
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

                String datos = construirDatosCita(cita);

                String instrucciones = bibliotecaPrompts.procesar(
                                "recordatorio.txt",
                                Map.of(
                                                "salon", cita.getSalon().getNombre(),
                                                "cliente", cita.getCliente().getNombre(),
                                                "datos", datos));

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

        private String construirDatosCita(Cita cita) {

                String fecha = cita.getInicio()
                                .format(FORMATO_FECHA);

                String hora = cita.getInicio()
                                .format(FORMATO_HORA);

                String periodo = cita.getInicio().getHour() < 12
                                ? "a. m."
                                : "p. m.";

                String horaCompleta = hora + " " + periodo;

                String valor = NumberFormat
                                .getIntegerInstance(IDIOMA_COLOMBIA)
                                .format(cita.getPrecio())
                                .replace('\u00A0', ' ')
                                .replace('\u202F', ' ');

                return """
                                Servicio: %s (%d minutos)
                                Estilista: %s
                                Fecha: %s
                                Hora: %s
                                Valor: $%s
                                Dirección: %s
                                """
                                .formatted(
                                                cita.getServicio().getNombre(),
                                                cita.getServicio().getDuracionMinutos(),
                                                cita.getEstilista().getNombre(),
                                                fecha,
                                                horaCompleta,
                                                valor,
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
