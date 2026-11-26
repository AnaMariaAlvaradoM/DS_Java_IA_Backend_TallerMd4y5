# 🤖 Bitácora de Uso de Inteligencia Artificial — Sillón API

Registro de consultas, diagnósticos y validaciones realizadas con apoyo de Inteligencia Artificial durante el desarrollo y aseguramiento de calidad de **Sillón API**.

---

## Entrada 1: Alineación de rutas de autenticación con el contrato OpenAPI
* **Contexto / Problema:** El calificador automático y Swagger esperaban el endpoint `POST /api/auth/registro`, pero el controlador tenía mapeado únicamente `/register`, provocando errores `500` y fallos en cascada (`401`) en el resto de pruebas.
* **Prompt / Consulta:** Diagnóstico de fallo en registro de salón dueño (`Se esperaba HTTP 201 y respondió 500` / `Swagger no incluye /api/auth/registro`).
* **Solución aplicada:** Se actualizó `@PostMapping` en `AuthController` para soportar tanto `/registro` como `/register`, y se ajustó el manejo de conflictos para devolver `409 Conflict` cuando el correo ya existe.
* **Verificación:** Ejecución exitosa de `POST /api/auth/registro` devolviendo `201 Created` con el token JWT y rol `DUENO`, y visualización de la ruta en Swagger UI.

---

## Entrada 2: Manejo de excepciones de seguridad (`@PreAuthorize`) y respuesta `403 Forbidden`
* **Contexto / Problema:** Al probar que un usuario con rol `ESTILISTA` intentara crear un servicio protegido con `@PreAuthorize("hasRole('DUENO')")`, la API devolvía `500 Internal Server Error` en lugar de `403 Forbidden`.
* **Prompt / Consulta:** Análisis de por qué `@PreAuthorize` devuelve error 500 teniendo `UsuarioPrincipal` configurado con `ROLE_DUENO`.
* **Solución aplicada:** Se verificó `@EnableMethodSecurity` en `SecurityConfig` y se agregó un `@ExceptionHandler(AccessDeniedException.class)` explícito en `GlobalExceptionHandler` para evitar que el manejador genérico `Exception.class` convirtiera el rechazo de permisos en un `500`.
* **Verificación:** Prueba en Postman con token de estilista sobre `POST /api/servicios` respondiendo `403 Forbidden`.

---

## Entrada 3: Renderizado dinámico de variables en plantillas del Chatbot IA
* **Contexto / Problema:** El asistente virtual devolvía literalmente las etiquetas `{salon}` y `{catalogo}` en sus respuestas sin sustituirlas por los datos reales del comercio.
* **Prompt / Consulta:** Revisión del método `cargar` en `BibliotecaPrompts.java` frente a la llamada en el servicio del chatbot.
* **Solución aplicada:** Se detectó que `BibliotecaPrompts` buscaba marcadores con doble llave (`{{variable}}`), mientras que la plantilla de texto tenía llaves simples (`{variable}`). Se unificó la sintaxis de las plantillas a doble llave `{{salon}}`, `{{direccion}}`, `{{horario}}`, `{{catalogo}}` y `{{cliente}}`.
* **Verificación:** Consulta al endpoint del chatbot confirmando que responde con el nombre del salón y los servicios reales de la base de datos.

---

## Entrada 4: Validación de cabeceras y firma en el Webhook de Stripe (`400 Bad Request`)
* **Contexto / Problema:** Al enviar una petición `POST /api/pagos/webhook` sin la cabecera `Stripe-Signature`, Spring lanzaba `MissingRequestHeaderException` y el manejador global respondía `500` en lugar de `400`.
* **Prompt / Consulta:** Manejo de `MissingRequestHeaderException` sin generar conflicto de beans ambiguos con `HttpMessageNotReadableException` en `GlobalExceptionHandler`.
* **Solución aplicada:** Se añadió un `@ExceptionHandler(MissingRequestHeaderException.class)` específico devolviendo `HttpStatus.BAD_REQUEST` (`400`) con el detalle de la cabecera faltante.
* **Verificación:** Prueba negativa en Postman sin `Stripe-Signature` respondiendo `400 Bad Request` y pruebas unitarias en `PagoWebhookServiceTest` pasando `8/8`.

---

## Entrada 5: Automatización de colección de Postman (52 peticiones) y ejecución con Newman
* **Contexto / Problema:** Se requería evidencia ejecutable con al menos 50 peticiones, 40 aserciones automáticas (`pm.test`) y manejo dinámico de tokens JWT entre roles (`DUENO` y `ESTILISTA`).
* **Prompt / Consulta:** Estructuración de casos de prueba positivos y negativos (`200`, `201`, `400`, `401`, `402`, `403`, `404`, `409`) y scripts *After response* en Postman v2.1.
* **Solución aplicada:** Se organizó la colección en 6 carpetas funcionales usando variables de colección (`{{baseUrl}}`, `{{token}}`), scripts de captura automática del JWT tras el login y validaciones de código HTTP en cada petición, exportándola en `postman/sillon.postman_collection.json`.
* **Verificación:** Ejecución completa mediante *Collection Runner* en Postman y verificación de estructura para CI/CD.