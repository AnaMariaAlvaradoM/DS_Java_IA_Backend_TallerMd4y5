# Bitácora de Uso de IA · Proyecto Sillón

| # | Fase | Herramienta / Modo | Para qué se usó | Qué revisaste | Qué rechazaste o corregiste |
|---|---|---|---|---|---|
| 01 | Fase 0 | Chat / PM | Planificación y configuración base | Coherencia del `pom.xml`, variables de entorno y aislamiento multi-tenant | Se postergó Spring Security a la Fase 2 para evitar bloqueos tempranos en Fase 1 |
| 02 | Fase 1 | Chat / PM | Paquete compartido, excepciones y entidades base | Validación del formato de 4 campos de error, reloj inyectado, modelo multi-tenant y generador de slug | Se postergó la lógica de controladores y JWT a la Fase 2 para respetar el orden incremental de fases |
| 03 | Fase 2 | Chat / PM | Seguridad Spring Security, JWT, AuthService y SalonService | Contrato JSON de errores 401/403, desacoplamiento con Clock, validación horaria de apertura/cierre y aislamiento multi-tenant | Se rechazó el uso de sesiones HTTP tradicionales; se mantuvo arquitectura stateless y se agregaron EntryPoint y AccessDeniedHandler propios para evitar respuestas HTML |
| 04 | Fase 3 | Chat / PM | Catálogo de servicios, estilistas ManyToMany y clientes | Validación de límite de estilistas (402) antes que correo (409) y servicios del salón (404), y normalización de teléfonos | Se garantizó que todas las búsquedas por ID filtren por `salonId` para mantener el aislamiento multi-tenant |
| 05 | Fase 4 | Chat / PM | Módulo de agenda, cálculo de disponibilidad y catálogo público | Cálculo de horarios libres con Clock, validación de horario comercial, detección de traslapes (409) y reservas públicas | Se restringieron permisos en SecurityConfig para /api/citas por rol (DUEÑO/ESTILISTA) y se mantuvo /api/publico/** abierto para reservas |
| 06 | Fase 5 | Chat / PM | Pasarela de pagos con Stripe y Webhooks | Verificación criptográfica de Stripe-Signature (400), idempotencia de eventos, recarga de créditos IA y actualización de plan | Se corrigió la firma del endpoint GET /api/pagos/resultado con ResponseEntity<Map<String, String>> y se garantizó persistencia de eventos procesados para evitar cobros/recargas duplicadas |


