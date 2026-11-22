
package com.devsenior.sillon.compartido;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentacionRespuestasOpenApi {

    @Bean
    public OpenApiCustomizer documentarRespuestasPendientes() {
        return api -> {

            // CITAS
            documentar(api, "/api/citas", HttpMethod.POST,
                "Crear cita",
                "201", "Cita creada",
                "400", "Datos inválidos",
                "401", "Sin autenticación",
                "403", "Sin permisos",
                "404", "Recurso no encontrado",
                "409", "Conflicto de agenda");

            documentar(api, "/api/citas", HttpMethod.GET,
                "Consultar agenda",
                "200", "Citas del salón",
                "401", "Sin autenticación",
                "403", "Sin permisos");

            documentar(api, "/api/citas/{id}/estado",
                HttpMethod.PATCH,
                "Cambiar estado de cita",
                "200", "Estado actualizado",
                "400", "Datos inválidos",
                "401", "Sin autenticación",
                "403", "Transición no permitida",
                "404", "Cita no encontrada",
                "409", "Transición inválida");

            documentar(api, "/api/citas/{id}/recordatorio",
                HttpMethod.POST,
                "Generar recordatorio con IA",
                "200", "Recordatorio generado",
                "401", "Sin autenticación",
                "402", "Créditos insuficientes",
                "403", "Sin permisos",
                "404", "Cita no encontrada",
                "409", "Estado no permitido",
                "502", "Respuesta de IA inválida",
                "503", "Proveedor de IA no disponible");

            // CATÁLOGO PÚBLICO
            documentar(api, "/api/publico/salones/{slug}",
                HttpMethod.GET,
                "Consultar catálogo público",
                "200", "Catálogo del salón",
                "404", "Salón no encontrado");

            // DISPONIBILIDAD
            documentar(api,
                "/api/publico/salones/{slug}/disponibilidad",
                HttpMethod.GET,
                "Consultar horarios disponibles",
                "200", "Horarios disponibles",
                "400", "Parámetros inválidos",
                "404", "Recurso no encontrado",
                "409", "Conflicto de disponibilidad");

            // RESERVAS
            documentar(api,
                "/api/publico/salones/{slug}/reservas",
                HttpMethod.POST,
                "Crear reserva pública",
                "201", "Reserva creada",
                "400", "Datos inválidos",
                "404", "Recurso no encontrado",
                "409", "Horario no disponible");

            // CHATBOT
            documentar(api,
                "/api/publico/salones/{slug}/chat",
                HttpMethod.POST,
                "Consultar chatbot del salón",
                "200", "Respuesta del chatbot",
                "400", "Solicitud inválida",
                "402", "Créditos insuficientes",
                "404", "Salón no encontrado",
                "502", "Respuesta de IA inválida",
                "503", "Proveedor no disponible");

            // PAGOS
            documentar(api, "/api/pagos/suscripcion",
                HttpMethod.POST,
                "Crear suscripción PRO",
                "200", "Sesión de pago creada",
                "401", "Sin autenticación",
                "403", "Sin permisos",
                "409", "El salón ya tiene plan PRO",
                "502", "Error de Stripe");

            documentar(api, "/api/pagos/creditos",
                HttpMethod.POST,
                "Comprar créditos de IA",
                "200", "Sesión de pago creada",
                "401", "Sin autenticación",
                "403", "Sin permisos",
                "502", "Error de Stripe");

            documentar(api, "/api/pagos/webhook",
                HttpMethod.POST,
                "Recibir webhook de Stripe",
                "200", "Evento recibido",
                "400", "Firma inválida");

            documentar(api, "/api/pagos/resultado",
                HttpMethod.GET,
                "Consultar resultado del pago",
                "200", "Resultado del pago");

            // SOLICITUDES DE CONTACTO
            documentar(api, "/api/solicitudes",
                HttpMethod.GET,
                "Listar solicitudes de contacto",
                "200", "Solicitudes obtenidas",
                "401", "Sin autenticación",
                "403", "Sin permisos");

            documentar(api, "/api/solicitudes/{id}/atendida",
                HttpMethod.PATCH,
                "Marcar solicitud como atendida",
                "200", "Solicitud atendida",
                "401", "Sin autenticación",
                "403", "Sin permisos",
                "404", "Solicitud no encontrada",
                "409", "Solicitud ya atendida");

            // ADMINISTRACIÓN (si la ruta está implementada)
            documentar(api, "/api/admin/salones",
                HttpMethod.GET,
                "Listar todos los salones",
                "200", "Salones registrados",
                "401", "Sin autenticación",
                "403", "Se requiere rol ADMIN");
        };
    }

    private void documentar(
            OpenAPI api,
            String ruta,
            HttpMethod metodo,
            String resumen,
            String... respuestas) {

        if (api.getPaths() == null) {
            return;
        }

        PathItem rutaApi = api.getPaths().get(ruta);

        if (rutaApi == null) {
            return;
        }

        Operation operacion =
                rutaApi.readOperationsMap().get(metodo);

        if (operacion == null) {
            return;
        }

        operacion.setSummary(resumen);

        ApiResponses codigos = operacion.getResponses();

        if (codigos == null) {
            codigos = new ApiResponses();
            operacion.setResponses(codigos);
        }

        for (int i = 0; i < respuestas.length; i += 2) {

            String codigo = respuestas[i];
            String descripcion = respuestas[i + 1];

            ApiResponse respuesta = codigos.get(codigo);

            if (respuesta == null) {
                respuesta = new ApiResponse()
                        .description(descripcion);

                codigos.addApiResponse(codigo, respuesta);
            } else if (respuesta.getDescription() == null
                    || respuesta.getDescription().isBlank()
                    || "OK".equals(respuesta.getDescription())) {

                respuesta.setDescription(descripcion);
            }

            if (!codigo.startsWith("2")
                    && (respuesta.getContent() == null
                    || respuesta.getContent().isEmpty())) {

                respuesta.setContent(
                    new Content().addMediaType(
                        "application/json",
                        new MediaType().schema(
                            new Schema<>()
                                .$ref(
                                    "#/components/schemas/RespuestaError"
                                )
                        )
                    )
                );
            }
        }
    }
}
