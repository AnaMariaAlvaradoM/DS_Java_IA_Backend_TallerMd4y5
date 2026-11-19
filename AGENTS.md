# Sillón · instrucciones para el agente

## Contexto

Backend SaaS multi-inquilino de agenda para barberías y salones de belleza.
Antes de cambiar una regla de negocio, lee `SPEC.md`: es la fuente de verdad.
Herramientas del equipo: IntelliJ IDEA, Antigravity y Claude Code.

## Stack

- Java 17, Spring Boot 4.1, Maven.
- MySQL en local; PostgreSQL (Supabase) en producción. Las entidades no cambian entre bases.
- Configuración solo en `src/main/resources/application.properties`. Nunca `application.yml` ni perfiles.
- Spring Boot 4 trae Jackson 3: el `ObjectMapper` vive en `tools.jackson.databind`. Solo las anotaciones (`@JsonProperty`, `@JsonInclude`) siguen en `com.fasterxml.jackson.annotation`.

## Arquitectura

- Paquete base `com.devsenior.sillon`, organizado por dominio: `compartido/`, `seguridad/`, `auth/`, `salon/`, `usuario/`, `servicio/`, `cliente/`, `cita/`, `publico/`, `pago/`, `chat/`, `ia/`.
- Capas: Controller → Service → Repository. El Controller no tiene lógica de negocio.
- DTOs como `record` en `<dominio>/dto`. Nunca expongas entidades JPA en la API.
- Un mapper propio por dominio (`@Component`), sin librerías de mapeo.
- Inyección por constructor. Nunca `@Autowired` sobre atributos.
- Enums persistidos con `@Enumerated(EnumType.STRING)`.
- Errores con excepciones propias y un único `@RestControllerAdvice` en `compartido/`, con el formato de 4 campos: `codigo`, `error`, `mensaje`, `fecha`.

## Multi-inquilino (obligatorio)

- Todo dato pertenece a un salón. El `salonId` sale siempre de `UsuarioPrincipal` (recibido con `@AuthenticationPrincipal`), nunca de la URL ni del cuerpo.
- Toda búsqueda por id filtra por id y salón a la vez (`findByIdAndSalonId`). Si no aparece: 404.
- Ninguna consulta de listado puede olvidar el filtro por salón.

## Tiempo

- En servicios y componentes, "hoy" y "ahora" salen del bean `Clock` (zona `America/Bogota`), recibido por constructor: `LocalDateTime.now(clock)`.
- Nunca uses `LocalDate.now()` ni `LocalDateTime.now()` sin argumento. La única excepción son los callbacks `@PrePersist`/`@PreUpdate` de las entidades, que usan `LocalDateTime.now(ZoneId.of("America/Bogota"))`.
- El servidor del calificador y Render corren en UTC: nada puede depender de la zona del sistema operativo.

## Código

- Clases, métodos y variables en español.
- Sin Lombok: getters y setters explícitos.
- Validación de entrada con Jakarta Validation en los DTO de request.
- Si devuelves una entidad después de modificarla y la respuesta incluye `fechaActualizacion`, guarda con `saveAndFlush`.
- Convierte entidades a DTO dentro del método transaccional del servicio (`open-in-view=false`).

## Tests

- JUnit 5 + Mockito. Los tests de servicios no levantan Spring; los repositorios y la IA son mocks.
- Las fechas en tests usan un `Clock` fijo.
- Ningún test llama a Gemini ni a Stripe.
- Nunca modifiques un test para que pase. Si un test falla, explica si la causa está en el test o en el código y espera mi decisión.
- Nombres de método: `metodo_escenario_resultado`.

## IA

- Todo proveedor implementa la interfaz `ProveedorIA` en `ia/`.
- Llamadas HTTP con `RestClient`. Sin SDKs de IA ni Spring AI.
- La URL del proveedor con formato OpenAI se lee de `sillon.ia.openai.url`; nunca la escribas fija en el código.
- Nunca escribas llaves en el código ni en `application.properties`: van en `secrets.properties`, que no puedes leer.
- Los prompts viven en `src/main/resources/prompts/*.txt` y se cargan con `BibliotecaPrompts`. Nunca escribas un prompt dentro de una clase Java.
- El texto del cliente va siempre como mensaje, nunca dentro de las instrucciones.
- Toda respuesta JSON de la IA se lee con `LectorJsonIA` y se valida antes de usarla.
- Los cálculos (fechas, horas, disponibilidad, precios, créditos) los hace Java, nunca la IA.
- Ninguna llamada a la IA va dentro de una transacción de base de datos.
- Nunca registres en logs el mensaje del cliente, sus datos personales ni las llaves.

## Pagos

- El cliente de Stripe se construye con la URL base leída de `sillon.stripe.url-api` (variable `STRIPE_URL_API`).
- El webhook recibe el cuerpo como `String` crudo; nunca lo conviertas a DTO antes de verificar la firma.
- La firma se verifica con `Webhook.Signature.verifyHeader` de stripe-java y el evento se lee con el `ObjectMapper` de Jackson 3, no con los modelos de Stripe: el `api_version` de los eventos puede no coincidir con el de la librería y el objeto llegaría vacío.
- Lo que el webhook necesita del evento: `id`, `type`, `data.object.payment_status`, `data.object.subscription`, `data.object.metadata.salonId`, `data.object.metadata.tipo` y, en la cancelación, `data.object.id`.

## Calificador

- La colección oficial vive en `.github/calificador/coleccion/` (solo lectura). Es la referencia exacta de rutas, campos y códigos.
- La IA simulada responde como chatbot si el prompt menciona las claves `respuesta` y `escalar`, y como recordatorio si menciona `mensaje`. El prompt del recordatorio nunca debe contener la palabra "escalar".
- En el recordatorio, la IA simulada copia las líneas de las instrucciones que empiezan por Servicio, Estilista, Fecha, Hora, Valor o Dirección: los datos verificados van una por línea con esas etiquetas.
- En el chatbot, el historial va solo en el mensaje del usuario; el nombre del salón, el catálogo y las reglas van en las instrucciones.

## Límites

- No agregues dependencias al `pom.xml` sin preguntarme.
- No modifiques `application.properties` salvo que te lo pida.
- No toques la carpeta `.github`.
- Si algo de la SPEC es ambiguo, pregunta antes de suponer.
