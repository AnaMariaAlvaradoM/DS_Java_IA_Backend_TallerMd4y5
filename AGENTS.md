    # AGENTS.md — Instrucciones para agentes de desarrollo

## 1. Contexto del proyecto

Sillón es una API REST SaaS multi-tenant para gestionar agendas de barberías y salones de belleza.

El proyecto forma parte del programa Java AI Engineer de Dev Senior Code.

La fuente de verdad funcional y técnica es `SPEC.md`, elaborado a partir del enunciado oficial.

Estas instrucciones aplican a Codex, Cursor y cualquier agente que trabaje en el repositorio.

## 2. Flujo de trabajo obligatorio

Antes de implementar cualquier funcionalidad:

1. Leer `SPEC.md` y las instrucciones de este archivo.
2. Identificar la fase y los requisitos aplicables.
3. Revisar los archivos existentes relacionados con la tarea.
4. Presentar un plan breve de implementación.
5. Implementar únicamente lo solicitado.
6. Ejecutar las verificaciones pertinentes.
7. Revisar los cambios realizados.
8. Informar qué archivos se modificaron, qué pruebas se ejecutaron y qué problemas permanecen.

No avanzar automáticamente a otra fase.

Si existe una contradicción entre los requisitos o falta información esencial, comunicarla antes de introducir una decisión que modifique el comportamiento esperado.

## 3. Arquitectura y tecnologías

Tecnologías obligatorias:

- Java 17.
- Spring Boot 4.1.x.
- Maven.
- Spring Data JPA.
- Spring Security con JWT.
- MySQL en desarrollo.
- PostgreSQL en producción.
- JUnit 5 y Mockito.
- Stripe Java.
- HTTP RestClient para proveedores de IA.
- Swagger / OpenAPI.

Paquete base: `com.devsenior.sillon`.

Organización por dominios:

- `compartido`
- `seguridad`
- `auth`
- `salon`
- `usuario`
- `servicio`
- `cliente`
- `cita`
- `publico`
- `pago`
- `chat`
- `ia`

Reglas:

- Mantener una arquitectura por dominios de negocio.
- Separar controladores, servicios, repositorios, entidades y DTOs.
- Utilizar inyección de dependencias por constructor.
- Evitar duplicación innecesaria.
- Utilizar DTOs para respuestas públicas.
- No exponer directamente entidades JPA en las respuestas.
- Mantener las reglas de negocio en Java, no en controladores ni prompts.
- No introducir Lombok, Spring AI ni SDKs de proveedores de IA.
- No agregar dependencias sin aprobación.

## 4. Seguridad y aislamiento multi-tenant

Cada salón representa un tenant independiente.

Reglas obligatorias:

- Aplicar autenticación con JWT y contraseñas BCrypt.
- Respetar los roles `DUENO`, `ESTILISTA` y `ADMIN`.
- Obtener el identificador del salón desde el usuario autenticado.
- Filtrar las consultas privadas por el `salonId` correspondiente.
- No confiar en identificadores de tenant enviados por el cliente.
- Responder HTTP 404 cuando un recurso pertenezca a otro salón.
- Evitar filtraciones de información entre tenants.
- No exponer contraseñas, tokens ni secretos en respuestas o logs.

Las rutas públicas y administrativas deben seguir exactamente los permisos del contrato oficial.

## 5. Reglas de negocio

- Cumplir estrictamente las reglas de `SPEC.md`.
- Conservar los nombres exactos de atributos, enums, endpoints y códigos HTTP.
- Utilizar `Clock` inyectable con la zona horaria `America/Bogota`.
- Calcular en Java la disponibilidad, duración, horarios y cruces de citas.
- Respetar las transiciones permitidas entre estados.
- Validar que un estilista pertenezca al salón y ofrezca el servicio solicitado.
- Conservar el precio del servicio al crear una cita.
- Aplicar los límites de los planes GRATIS y PRO.
- Usar transacciones cuando una operación de negocio deba ejecutarse de forma atómica.

No simplificar reglas de negocio para reducir código.

## 6. Inteligencia artificial

- Integrar los proveedores mediante HTTP con `RestClient`.
- Mantener los prompts en `src/main/resources/prompts/`.
- Usar la información real de la base de datos como contexto del chatbot.
- Solicitar y validar respuestas JSON.
- Respetar las claves exactas exigidas por el contrato.
- Implementar fallback entre proveedores según `SPEC.md`.
- Verificar créditos antes de invocar un proveedor.
- Consumir los créditos mediante una operación atómica que impida saldos negativos.
- No ejecutar llamadas externas de IA dentro de transacciones de base de datos.
- No permitir que la IA calcule disponibilidad, modifique reservas o decida reglas de negocio.
- Aplicar en Java la regla anti-promesas del chatbot.
- No registrar claves de API ni información sensible en prompts o logs.

La IA redacta respuestas; Java verifica y aplica las reglas del negocio.

## 7. Integración con Stripe

- Utilizar únicamente el modo de prueba.
- Determinar los precios desde el backend.
- No aceptar precios arbitrarios enviados por el cliente.
- Verificar la firma de los webhooks.
- Procesar cada evento una sola vez.
- Garantizar la idempotencia mediante el identificador del evento.
- Ejecutar los cambios de estado del salón y el registro del evento en una misma transacción.
- Mantener configurable la URL de la API de Stripe para el calificador.

## 8. Pruebas y calidad

Utilizar JUnit 5 y Mockito.

Para cada funcionalidad, considerar:

- Casos exitosos.
- Validaciones de entrada.
- Errores de negocio.
- Permisos por rol.
- Aislamiento multi-tenant.
- Casos límite.

Reglas:

- No eliminar, desactivar ni debilitar pruebas para conseguir que pasen.
- Si una prueba falla, identificar primero si el problema está en el código, la prueba o la configuración.
- No modificar pruebas existentes sin una justificación basada en `SPEC.md`.
- Ejecutar las pruebas relacionadas con los cambios.
- Ejecutar la suite completa cuando corresponda.
- No afirmar que una prueba pasó sin haberla ejecutado.

## 9. Contrato de la API

El contrato de la sección 4 del documento oficial es obligatorio.

No modificar sin autorización:

- Rutas y métodos HTTP.
- Nombres de campos JSON.
- Tipos de datos.
- Enumeraciones.
- Códigos HTTP.
- Estructura de respuestas.
- Formato único de errores.
- Reglas de autorización.

El calificador automático depende de estos detalles.

## 10. Protección del repositorio

- No modificar `.github/`.
- No modificar archivos de configuración del calificador.
- No subir `secrets.properties`, `.env`, llaves privadas ni credenciales.
- No sobrescribir archivos existentes sin revisar su contenido.
- No modificar el README original hasta la fase correspondiente.
- No ejecutar `git commit`, `git push`, `git merge` ni crear Pull Requests sin autorización.
- No realizar cambios ajenos a la tarea solicitada.

## 11. Uso responsable de IA

Cuando el agente participe en una actividad importante:

- Informar qué tarea realizó.
- Indicar las decisiones técnicas relevantes.
- Señalar errores encontrados y correcciones realizadas.
- Proporcionar información verificable para actualizar `BITACORA-IA.md`.

No inventar registros de actividades ni resultados de pruebas.

## 12. Entrega de cada tarea

Al terminar, presentar:

1. Resumen de la implementación.
2. Archivos creados o modificados.
3. Reglas de negocio implementadas.
4. Pruebas ejecutadas y resultados.
5. Problemas, riesgos o pendientes.
6. Siguiente paso recomendado según la guía.

No continuar automáticamente con otra fase.