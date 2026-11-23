# Reglas operativas del agente — Proyecto Sillón

## Stack y restricciones técnicas
- **Lenguaje y framework:** Java 17, Spring Boot 4.1.x, Maven, Spring Data JPA, Spring Security + JWT, springdoc-openapi 3.x (`springdoc-openapi-starter-webmvc-ui`), Stripe (`stripe-java`) y llamadas HTTP a IA mediante `RestClient`.
- **Prohibiciones explícitas:** No uses Lombok, Spring AI ni SDKs de IA. Escribe getters y setters explícitos (o `record` para DTOs).
- **Jackson 3:** En Spring Boot 4 usa `tools.jackson.*` (por ejemplo, `tools.jackson.databind.ObjectMapper`). Solo anotaciones como `@JsonProperty` van en `com.fasterxml.jackson.annotation`.
- **Configuración:** Usa `application.properties` (nunca `.yml` ni perfiles de Spring). Mantén `STRIPE_URL_API` (por defecto `https://api.stripe.com`) y `sillon.ia.openai.url` configurables por variable de entorno.

## Arquitectura y paquete base
- **Paquete base:** `com.devsenior.sillon`.
- **Dominios:** Organiza por dominios de negocio: `compartido`, `seguridad`, `auth`, `salon`, `usuario`, `servicio`, `cliente`, `cita`, `publico`, `pago`, `chat`, `ia`.
- **Enums:** Persiste todos los enums con `@Enumerated(EnumType.STRING)` y valores sin tildes (`DUENO`, `ESTILISTA`, `ADMIN`, `GRATIS`, `PRO`, `PENDIENTE`, `CONFIRMADA`, `COMPLETADA`, `CANCELADA`, `NO_ASISTIO`, `SALON`, `EN_LINEA`, `SUSCRIPCION_PRO`, `PAQUETE_CREDITOS`, `PEDIDO_POR_IA`, `REGLA_DE_SEGURIDAD`, `ATENDIDA`).

## Reglas críticas de negocio y seguridad
- **Multi-inquilino estricto:** Todo dato pertenece a un `Salon`. Filtra siempre por el `salonId` del usuario autenticado (o por `slug` en rutas públicas). Si un recurso existe pero pertenece a otro salón (o una cita pertenece a otro estilista al consultar/cambiar por estilista), responde `404` para no revelar su existencia.
- **Reloj del sistema (`Clock`):** Toda lógica que dependa de "hoy" o "ahora" debe recibir por constructor el bean `Clock` configurado con `America/Bogota`. Nunca uses `LocalDate.now()` o `LocalDateTime.now()` sin `Clock`.
- **IA, prompts y transacciones:** Guarda los prompts en `src/main/resources/prompts/`. Java calcula y valida los datos; la IA solo redacta o responde en el JSON exacto pedido (`mensaje`; o `respuesta` y `escalar`). **Nunca** hagas llamadas HTTP a proveedores de IA dentro de una transacción (`@Transactional`) de base de datos.
- **Secretos y `.github`:** Nunca escribas llaves ni secretos en el código, prompts, logs o archivos subidos a Git. **Nunca** modifiques la carpeta `.github`.

## Flujo de trabajo y disciplina
- **Plan antes de ejecutar:** Cuando pida implementar una fase o cambio relevante, presenta primero el plan y espera mi aprobación antes de modificar o crear archivos.
- **Tests como contrato:** Si un test falla, analiza y dime si el problema está en el test o en el código contrastando con `SPEC.md`. **Nunca** modifiques un test existente para que pase.
- **Dependencias:** No agregues dependencias al `pom.xml` sin consultarme primero.