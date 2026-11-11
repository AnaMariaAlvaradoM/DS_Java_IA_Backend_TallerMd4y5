package com.devsenior.sillon.cita.service;

import com.devsenior.sillon.cita.dto.DatosRecordatorio;
import com.devsenior.sillon.cita.dto.MensajeIa;
import com.devsenior.sillon.cita.dto.RecordatorioResponse;
import com.devsenior.sillon.compartido.IaNoDisponibleException;
import com.devsenior.sillon.compartido.ServicioExternoException;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.service.CreditoIaService;
import com.devsenior.sillon.ia.util.BibliotecaPrompts;
import com.devsenior.sillon.ia.util.LectorJsonIA;

import org.springframework.stereotype.Service;


import java.util.Map;

    @Service
    public class RecordatorioService {

        private final CitaService citaService;
        private final CreditoIaService creditoIaService;
        private final AsistenteIA asistenteIa;
        private final LectorJsonIA lectorJson;
        private final BibliotecaPrompts bibliotecaPrompts;

        public RecordatorioService(CitaService citaService, CreditoIaService creditoIaService,
                                   AsistenteIA asistenteIa, LectorJsonIA lectorJson,
                                   BibliotecaPrompts bibliotecaPrompts) {
            this.citaService = citaService;
            this.creditoIaService = creditoIaService;
            this.asistenteIa = asistenteIa;
            this.lectorJson = lectorJson;
            this.bibliotecaPrompts = bibliotecaPrompts;
        }

        // SIN @Transactional, aplicando la regla de negocio estricta
        public RecordatorioResponse generarRecordatorio(Long salonId, Long citaId) {

            // 1. Validar lo que no depende de la IA
            DatosRecordatorio datos = citaService.obtenerDatosRecordatorio(citaId);

            // 2. Verificar créditos (Lanza 402 si es <= 0)
            creditoIaService.verificar(salonId);

            // 3. Cargar el prompt y llamar a la IA
            RespuestaIA respuestaIa;
            try {
                String userPrompt = bibliotecaPrompts.cargar("recordatorio", Map.of(
                        "salon", datos.salonNombre(),
                        "cliente", datos.clienteNombre(),
                        "datos", datos.textoDatos()
                ));

                String systemPrompt = "Eres un asistente experto para salones de belleza. Responde estrictamente en formato JSON válido.";

                respuestaIa = asistenteIa.completar(systemPrompt, userPrompt);

            } catch (IaNoDisponibleException e) {
                throw e;
            } catch (Exception e) {
                throw new ServicioExternoException("Error al cargar el prompt o procesar la IA: " + e.getMessage());
            }
            // 4. Consumir el crédito atómicamente
            int creditosRestantes = creditoIaService.consumirCredito(salonId);
            // 5. Leer y validar el JSON
            MensajeIa jsonParseado;
            try {
                jsonParseado = lectorJson.leer(respuestaIa, MensajeIa.class);
            } catch (Exception e) { // Captura tu RespuestaIAInvalidaException
                throw new ServicioExternoException("La IA no devolvió un JSON con el formato esperado: " + e.getMessage());
            }

            String mensaje = jsonParseado.mensaje();
            if (mensaje == null || mensaje.isBlank() || mensaje.length() > 700) {
                throw new ServicioExternoException("El mensaje generado por la IA es inválido o supera los 700 caracteres.");
            }

            // 6. Aplicar las reglas de negocio y responder
            return new RecordatorioResponse(
                    citaId,
                    datos.clienteTelefono(),
                    mensaje,
                    creditosRestantes,
                    respuestaIa
            );
        }
    }
