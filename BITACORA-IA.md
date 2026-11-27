# Bitácora de uso de Inteligencia Artificial — Sillón API

Registro de diagnósticos, revisiones y correcciones aplicadas durante el desarrollo y aseguramiento de calidad del backend.

| # | Fase | Tarea | Prompt o enfoque usado | Qué revisé | Qué corregí |
|---|---|---|---|---|---|
| 1 | Seguridad JWT | Garantizar el principal correcto y aislamiento por salón en endpoints autenticados. | Seguir el flujo JWT hasta los controladores y verificar el tipo de `UserDetails`. | `JwtAuthFilter`, `UsuarioDetailsService`, `UsuarioPrincipal` y consumidores del contexto. | Se entrega el principal de dominio con IDs, rol y estado activo, y se evita registrar dos veces el filtro JWT. |
| 2 | Catálogo y límites de plan | Mantener servicios y estilistas asociados a su salón y respetar límites del plan. | Revisar consultas de aislamiento y casos del plan GRATIS/PRO. | Repositorios, servicios, asociaciones JPA y pruebas unitarias. | Se conserva el filtrado por `salonId`, validación de pertenencia de servicios y límite del plan GRATIS. |
| 3 | Agenda y zonas horarias | Calcular disponibilidad y evitar reservas fuera de horario o con cruces. | Verificar reglas de agenda usando el reloj del proyecto y la zona de Bogotá. | `ConfiguracionReloj`, `ReglasAgenda` y tests con reloj fijo. | Se centraliza el horario `America/Bogota` y se validan apertura, cierre, días y citas ocupadas. |
| 4 | Stripe webhooks e idempotencia | Procesar eventos firmados una sola vez y actualizar beneficios correctamente. | Revisar firmas, eventos repetidos y deserialización de Stripe. | `PagoWebhookService`, persistencia de IDs de evento y pruebas de webhook. | Se conserva la validación de firma y el control idempotente; los fallos de deserialización producen errores explícitos. |
| 5 | IA recordatorios y chatbot anti-promesas | Evitar datos inventados en recordatorios y promesas comerciales del chatbot. | Proporcionar a Java los datos reales y mantener las reglas de seguridad fuera de la IA. | `RecordatorioService`, `ChatbotService`, prompts, lector JSON y tests. | Java asegura nombre del estilista y hora en recordatorios; el chatbot bloquea respuestas sensibles y devuelve texto neutral al escalar. |
| 6 | Pruebas JUnit/Postman | Validar contratos JSON, autenticación, DTOs y respuestas del backend. | Reproducir payloads de Postman y comprobar status y campos JSON exactos. | Tests JUnit/Mockito, prueba MVC con JWT y colección `postman/Sillon.postman_collection.json`. | Se agregaron verificaciones para los formatos horarios `HH:mm:ss`, aliases de DTO y respuestas esperadas de los endpoints. |

## Entrada 1

**Fase:** Seguridad JWT. Se rastreó la autenticación desde el filtro hasta los controladores. Se verificó que el principal contuviera usuario, salón y rol, y se corrigió el tipo devuelto por `UsuarioDetailsService` para evitar errores de casteo y fallos de seguridad.

## Entrada 2

**Fase:** Catálogo y límites de plan. Se revisó el aislamiento multitenant de servicios y estilistas, además del límite del plan GRATIS. La lógica valida que los servicios pertenezcan al salón autenticado y evita crear estilistas por encima del límite.

## Entrada 3

**Fase:** Agenda y zonas horarias. Se evaluaron horarios comerciales, domingos, citas superpuestas y solicitudes en el pasado. El reloj centralizado en `America/Bogota` mantiene los cálculos deterministas y las pruebas usan un reloj controlado.

## Entrada 4

**Fase:** Stripe webhooks e idempotencia. Se revisó que los eventos verifiquen la firma y que un ID repetido no vuelva a procesarse. Los errores de deserialización y validación se mantienen explícitos y cubiertos por pruebas.

## Entrada 5

**Fase:** IA recordatorios y chatbot anti-promesas. Se verificó que Java prepare los datos reales de la cita y que el mensaje de recordatorio incluya cliente, estilista, servicio, fecha y hora. Para el chatbot, una respuesta sensible se bloquea, se escala y se reemplaza por un mensaje fijo neutral.

## Entrada 6

**Fase:** Pruebas JUnit/Postman. Se contrastaron los contratos JSON con las peticiones y verificaciones de la colección Postman. Las pruebas cubren respuestas HTTP, serialización de disponibilidad, compatibilidad de DTOs, reglas de servicio y autenticación real con JWT.
