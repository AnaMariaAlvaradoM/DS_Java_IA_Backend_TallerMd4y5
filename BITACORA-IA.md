# Bitácora de uso de Inteligencia Artificial

**Proyecto:** Sillón  
**Programa:** Java AI Engineer — Dev Senior Code

## Registro de actividades

| # | Fase | Herramienta y modo | Para qué se utilizó | Qué se revisó | Qué se rechazó o corrigió |
|---|---|---|---|---|---|
| 1 | 0.4 | Codex, agente de programación | Preparar las instrucciones operativas del proyecto y organizar la documentación inicial. | README oficial, estructura del repositorio y estado de Git. | Se identificó que la especificación debía basarse en el documento oficial, evitando inventar reglas de negocio. |
| 2 | 0.4 | ChatGPT, asistente de documentación | Elaborar la especificación funcional y técnica inicial del proyecto (`SPEC.md`). | Requisitos del documento oficial: arquitectura, roles, entidades, reglas de negocio, contrato de API e integraciones. | Se estableció que los nombres de campos, rutas y códigos HTTP deben conservarse exactamente como en el enunciado. |
| 3 | 1 | Codex, agente de programación | Implementar y revisar la base SaaS: salones, usuarios, configuración y manejo de errores. | Entidades JPA, relaciones, enums, generador de slugs, manejador global, configuración del reloj y compilación Maven. | Se retiró una contraseña de `application.properties`. Se identificó que `contextLoads` requiere credenciales válidas de MySQL y que los errores 401/403 se integrarán en la Fase 2. |
| 4 | 2 | Codex, agente de programación | Implementar JWT, registro/login, permisos y consulta/configuración del salón. | `SPEC.md`, `AGENTS.md`, código de Fase 1, compilación y pruebas de integración de la Fase 2. | Se corrigió el acceso a la relación diferida del salón en el login con una transacción de lectura; el Maven Wrapper no inició dentro del sandbox y se ejecutó con autorización fuera de él. |
| 5 | 3 | Codex, agente de programación | Implementar servicios, estilistas y clientes con aislamiento por salón. | Contrato de `SPEC.md`, permisos de Fase 2, entidades y repositorios existentes, compilación y pruebas de integración de Fase 3. | Se documentó el orden límite GRATIS → correo → servicios; se ajustó la columna de descripción a 300 caracteres tras revisar el contrato. |
| 6 | 4 | Codex, agente de programación | Implementar estados, reglas de agenda, citas, listado y cambio de estado. | `SPEC.md`, `AGENTS.md`, código de Fases 1 a 3, compilación Maven y pruebas de reglas e integración HTTP. | Se corrigió el recorrido de disponibilidad para comparar fechas completas y evitar un desbordamiento al pasar de medianoche; se añadió la verificación de que una cita finalizada libera su franja. |
| 7 | 5 | Codex, agente de programación | Implementar catálogo, disponibilidad y reservas públicas del salón. | Contrato de `SPEC.md`, reglas de Fase 4, permisos públicos, compilación Maven y suite de 21 pruebas (0 fallas), incluida reserva simultánea en MySQL. | Se mantuvo un único flujo de agendamiento para citas del salón y reservas en línea; se filtraron servicios y estilistas inactivos antes de construir el catálogo público. |
| 8 | 6 | Codex, agente de programación | Implementar sesiones Checkout, webhooks firmados e idempotencia de pagos. | `SPEC.md`, `AGENTS.md`, código de Fases 1 a 5, fuentes locales de stripe-java 34.1.0, compilación y pruebas con Stripe simulado. | La primera prueba reveló que el deserializador seguro de Stripe falla si el evento firmado no trae `api_version`; se corrigió usando la deserialización alternativa de la misma librería tras verificar la firma. |
| 9 | 6 | Codex, agente de programación | Investigar el 502 del webhook del calificador y corregir la metadata de pagos. | `SPEC.md`, colección oficial de pagos en `.github/` (solo lectura), verificador, servicio transaccional, fuentes locales del SDK y pruebas de integración firmadas. | Se reprodujo el 502: el evento oficial usa `metadata.tipo` y el código leía `tipoCompra`. Se alinearon Checkout y el verificador con `tipo`; se añadieron casos de idempotencia, cancelación, firma alterada, evento sin `api_version` y pago incompleto. |
| 10 | 7 | Codex, agente de programación | Implementar el motor HTTP de IA reutilizable, fallback, lectura de JSON y créditos atómicos. | `SPEC.md`, texto oficial de la Fase 7 aportado en la conversación, configuración Spring, `Salon` y repositorio, pruebas unitarias e integración MySQL. `./mvnw.cmd clean test`: 42 pruebas, 0 fallas, 0 errores. | Se corrigió el rechazo de la clave de relleno de Gemini porque el calificador usa un simulador; se añadió validación de negocio explícita antes de consultar créditos. No se fijaron tarifas: el costo solo se calcula si están configuradas. |
| 11 | 7 | Codex, agente de programación | Validar el motor con una llamada manual real a Gemini y revisar créditos y fallback antes del commit. | `SPEC.md`, `AGENTS.md`, paquete `ia`, documentación oficial de Gemini y pruebas Maven. Pruebas dirigidas: 14/14; llamada real: JSON válido, modelo `gemini-3.5-flash-lite`, 19 tokens de entrada y 11 de salida; suite normal: 42/42. | La primera llamada devolvió HTTP 404 con el modelo por defecto `gemini-2.5-flash`; se actualizó el modelo configurable por defecto a `gemini-3.5-flash-lite` y la segunda llamada pasó. La prueba manual quedó fuera de `clean test` y el error clasificado conserva el código HTTP sin registrar la clave. |
## Criterios de registro

- Registrar únicamente actividades de IA realizadas.
- Documentar qué se revisó manualmente en cada actividad.
- Describir las correcciones o decisiones reales, sin inventar resultados.
- Agregar al menos una entrada por cada fase en la que se utilice IA.
- No registrar pruebas como exitosas sin haberlas ejecutado.

## Observaciones

La inteligencia artificial se utiliza como herramienta de apoyo para documentación, planificación, implementación y revisión.

La responsabilidad de validar el cumplimiento de las reglas de negocio y del contrato oficial de la API corresponde al desarrollador.
