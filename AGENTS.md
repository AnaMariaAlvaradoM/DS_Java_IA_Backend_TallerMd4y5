# AGENTS.md · Reglas para el agente · Sillón

Sillón es un SaaS multi-inquilino de agenda inteligente para barberías y salones de belleza. Backend en Java con Spring Boot, API REST documentada con Swagger y desplegada en Render. No hay interfaz gráfica.

Fuente de verdad: `SPEC.md`. Si algo de este archivo o de una petición contradice la SPEC, gana la SPEC. Ante la duda, pregunta antes de construir.

## Stack obligatorio

- Java 21, Spring Boot 4.1.x, Maven.
- Spring Data JPA: MySQL en local, PostgreSQL (Supabase) en producción. Las entidades no cambian entre uno y otro.
- Spring Security con JWT (jjwt) y contraseñas con BCrypt.
- Swagger/OpenAPI con springdoc 3.x (`springdoc-openapi-starter-webmvc-ui`).
- Stripe (`stripe-java`) en modo prueba.
- IA: Gemini por HTTP con `RestClient`, con fallback entre proveedores.
- Tests: JUnit 5 + Mockito.
- Despliegue: Docker (dos etapas) + Render + Supabase.

## Prohibido

- Lombok, Spring AI y SDKs de IA. Getters y setters explícitos; llamadas a IA por HTTP con `RestClient`.
- Agregar dependencias sin preguntarme antes. Las únicas fuera de Initializr son springdoc, `stripe-java` y las tres de jjwt.
- Archivos `.yml` y perfiles de Spring: la configuración va solo en `application.properties`.
- Escribir llaves, secretos o contraseñas reales en código, prompts, tests, logs o commits.
- Modificar un test para que pase. Un test rojo es una pregunta de negocio: indica si la causa está en el test o en el código y decídelo contra la SPEC.
- Modificar la carpeta `.github` (ahí vive el calificador).
- Mover el proyecto a una subcarpeta: el `pom.xml` queda en la raíz del repositorio.
- Hacer llamadas reales a Gemini o a Stripe en los tests.

## Paquete y dominios

Paquete base: `com.devsenior.sillon`. Organización por dominios de negocio:

| Paquete | Responsabilidad |
|---|---|
| `compartido` | Objeto de error de cuatro campos, excepciones propias, `@RestControllerAdvice`, `Clock`, configuración de Swagger |
| `seguridad` | JWT, filtro de autenticación, reglas de acceso por rol, `UsuarioPrincipal` |
| `auth` | Registro del salón con su dueño y login |
| `salon` | Salón (inquilino), plan, créditos, configuración embebida |
| `usuario` | Usuarios, roles y estilistas |
| `servicio` | Catálogo de servicios del salón |
| `cliente` | Clientes finales del salón |
| `cita` | Citas, estados, reglas de agenda, disponibilidad y recordatorio con IA |
| `publico` | Catálogo, disponibilidad, reservas y chat sin iniciar sesión |
| `pago` | Sesiones de pago y webhook de Stripe |
| `chat` | Chatbot del salón y solicitudes de contacto |
| `ia` | Proveedores, fallback, prompts, lector de JSON y factura |

## Multi-inquilino (regla más importante)

- Todo dato pertenece a un salón. Un usuario nunca ve ni modifica datos de otro salón.
- El `salonId` sale siempre del usuario autenticado (`@AuthenticationPrincipal` con `UsuarioPrincipal`) o del slug de la ruta pública. Nunca de un id en la URL o en el cuerpo.
- Toda búsqueda por id se hace por **id y salón a la vez**. Si el dato es de otro salón, responde **404**, no 403.
- Revisa en cada diff que ninguna consulta de repositorio olvide filtrar por salón.

## Convenciones de código

- Capas: controlador, servicio, repositorio. El controlador no tiene lógica de negocio.
- DTOs como `record` y mapper propio. Nunca se devuelve una entidad JPA.
- Las entidades se convierten a DTO dentro del método transaccional (evita `could not initialize proxy`).
- Enums guardados con `EnumType.STRING`, valores sin tildes (`DUENO`, no `DUEÑO`).
- Jackson 3: el `ObjectMapper` está en `tools.jackson.databind`. Solo las anotaciones siguen en `com.fasterxml.jackson.annotation`.
- Precios como enteros en pesos colombianos, sin decimales.
- Fechas `AAAA-MM-DD`, fecha y hora `AAAA-MM-DDTHH:mm:ss`, horas `HH:mm` al enviar.
- Si devuelves una cita después de cambiarle el estado, guárdala con `saveAndFlush` para que `fechaActualizacion` salga correcta.
- Reglas de agenda en un componente propio sin dependencias de base de datos (`ReglasAgenda`).
- Un solo método privado de agendar, usado por la cita del salón y por la reserva en línea.
- Si traes código de otro proyecto (por ejemplo `ia` de Atiende), revisa paquetes, prefijos e imports: no debe quedar nada de `com.devsenior.atiende`.

## Fechas y hora

- Zona del negocio: `America/Bogota`. Render corre en UTC.
- "Hoy" y "ahora" se calculan **solo** con un bean `Clock` recibido por constructor. Nunca `LocalDate.now()` ni `LocalDateTime.now()` sin reloj.
- Los tests usan un reloj fijo.
- Al formatear hora en español de Colombia, reemplaza el espacio especial de "a. m." / "p. m." por un espacio normal.

## Errores y seguridad

- Todo error (incluidos 401 y 403) responde con el objeto de cuatro campos: `codigo`, `error`, `mensaje`, `fecha`.
- Códigos: 400 datos inválidos o firma de webhook inválida · 401 sin token o credenciales incorrectas · 402 límite del plan o sin créditos · 403 rol sin permiso · 404 no existe o es de otro salón · 409 regla de negocio · 502 respuesta inutilizable de Stripe o IA · 503 sin proveedor de IA.
- Sesión stateless, CSRF deshabilitado, autoridad `ROLE_` + rol, reglas con `hasRole` sin prefijo.
- Rutas públicas: `/api/auth/**`, `/api/publico/**`, `/api/pagos/webhook`, `/api/pagos/resultado`, Swagger y `/error`. Si `/error` no es pública, los fallos del servidor se ven como 401 engañosos.
- Login con 401 y mensaje genérico, sin decir si falló el correo o la contraseña.
- Correo guardado en minúsculas y sin espacios; único en toda la plataforma.

## Reglas de negocio clave (detalle en SPEC.md)

- Registro crea salón y dueño en una transacción: plan GRATIS, 10 créditos, horario 8:00 a 19:00, sin domingos, intervalo 30 minutos.
- Plan GRATIS: máximo 2 estilistas (el tercero responde 402). PRO: sin límite. Orden de validación al crear estilista: límite del plan, correo, servicios.
- La cita dura lo que dura su servicio y guarda el precio del momento.
- No se agenda en el pasado, fuera de horario, en domingo cerrado ni con estilista que no ofrece el servicio (409).
- Cruce: inicio de uno anterior al fin del otro **y** fin del primero posterior al inicio del segundo. Solo ocupan agenda `PENDIENTE` y `CONFIRMADA`.
- Transiciones: `PENDIENTE → CONFIRMADA|CANCELADA`; `CONFIRMADA → COMPLETADA|CANCELADA|NO_ASISTIO`; los demás estados son finales. Cualquier otra: 409.
- Estilista: solo sus citas (ajena: 404) y solo a `COMPLETADA` o `NO_ASISTIO` (otro destino: 403).
- La disponibilidad la calcula Java, nunca la IA.

## Pagos (Stripe)

- Solo modo prueba.
- El backend decide el precio. El cliente de la API nunca envía un precio ni un id de precio.
- Salón ya PRO pidiendo la suscripción: 409 antes de llamar a Stripe.
- Webhook: cuerpo como texto plano, verificar `Stripe-Signature` (400 si falta o es falsa), procesar cada evento una sola vez (guardar su id), todo en una sola transacción.
- La dirección de la API de Stripe se lee de `STRIPE_URL_API` (por defecto `https://api.stripe.com`). El calificador la cambia por un simulador.

## Inteligencia artificial

- Java calcula y redacta los datos verificados; la IA solo redacta el mensaje. Una persona aprueba: Sillón no envía nada.
- Un crédito por respuesta de la IA. Orden obligatorio en toda funcionalidad con IA:
  1. Validar lo que no depende de la IA.
  2. Verificar créditos (0 créditos: 402 y no se llama a la IA).
  3. Cargar el prompt y llamar a la IA.
  4. Consumir el crédito (una sola actualización atómica que resta 1 solo si quedan).
  5. Leer y validar el JSON.
  6. Aplicar reglas de negocio y responder.
- Ninguna llamada a la IA va dentro de una transacción de base de datos.
- Prompts en archivos de `src/main/resources/prompts`, nunca escritos dentro del código Java.
  - `recordatorio.txt`: variables salón, cliente y datos; pide JSON con la clave exacta `mensaje`.
  - `chatbot.txt`: variables salón, dirección, horario, catálogo y cliente; pide JSON con las claves exactas `respuesta` y `escalar`.
- Toda respuesta de la IA se pide en JSON y se valida. JSON inválido o incompleto: 502. Ningún proveedor responde: 503.
- El texto del cliente nunca va dentro de las instrucciones del prompt; el historial se envía como mensaje, una línea por turno (`Cliente:` / `Asistente:`).
- Regla anti-promesas **en Java**: si la respuesta del chatbot contiene `descuento`, `cupón`, `cupon`, `gratis`, `promoción`, `promocion` o `reembols` (sin importar mayúsculas), se envía un mensaje seguro fijo y se escala con motivo `REGLA_DE_SEGURIDAD`. Un prompt pide; solo el código garantiza.
- El chatbot usa el catálogo real del salón desde la base de datos. No agenda, no cambia ni cancela citas: escala.
- La URL del proveedor con formato OpenAI se lee de `sillon.ia.openai.url`. Nada fijo en el código.
- El proveedor de respaldo queda configurado aunque no haya llave, para que el fallback se vea en los logs.

## Configuración y secretos

- Todo en `application.properties`: puerto desde `PORT` (8080 por defecto), MySQL local `sillon_db` con creación automática, `ddl-auto=update`, `open-in-view=false`, zona `America/Bogota`, URL pública, números del negocio (10 créditos de registro, 300 de PRO, 200 del paquete, 2 estilistas en GRATIS).
- Cada valor secreto lee una variable de entorno con un valor de relleno, para que la aplicación **siempre arranque**: clave de Stripe, secreto del webhook, ids de precios, llave de Gemini y secreto de JWT.
- Importación opcional de `secrets.properties` desde la raíz. `secrets.properties` está en `.gitignore`; `secrets.example.properties` sí se sube.
- `.cursorignore` bloquea secretos, llaves y archivos `.env`. Nunca leas ni pegues el contenido de esos archivos.
- Si una llave se filtra, hay que revocarla en el panel del proveedor; borrarla en otro commit no basta.

## Tests

- Los tests de servicios no levantan Spring: Mockito para repositorios y para la IA.
- Casos mínimos: transiciones de estado (parametrizado: 5 permitidas y al menos 6 bloqueadas), reglas de agenda, agendar y cambiar estado, plan, webhook, chatbot, recordatorio, motor de IA (fallback, clasificación de errores, biblioteca de prompts, lector de JSON) y un test que levanta el contexto completo.
- Los casos salen de las reglas de negocio de la SPEC, no del código existente.
- Comando: tests de Maven terminando en `BUILD SUCCESS`.

## Flujo de trabajo

1. Lee `SPEC.md` y este archivo antes de planear.
2. Trabaja por fases, en modo Plan y luego Agent. Presenta el plan y espera mi aprobación antes de escribir código.
3. Cada fase deja la aplicación funcionando y termina en un commit con el mensaje definido en la SPEC. Un commit por paso en verde.
4. No avances a la siguiente fase sin comprobar la actual.
5. Nada se acepta sin que yo entienda el diff y lo pruebe en Postman.
6. Para depurar, parte de mi hipótesis y de la regla de negocio involucrada. No cambies la regla para que una prueba pase.
7. Registra los usos importantes de IA en `BITACORA-IA.md` (número, fase, herramienta y modo, para qué, qué se revisó, qué se rechazó o corrigió).

## Entrega

- Fork del repositorio oficial y un solo Pull Request hacia el repositorio oficial, con mi nombre completo como título.
- En la raíz: `pom.xml`, `SPEC.md`, `AGENTS.md`, `BITACORA-IA.md`, `secrets.example.properties`, `Dockerfile`, `.dockerignore`, `README.md` (con la URL completa de Render y Swagger).
- `postman/` con la colección `sillon.postman_collection.json` y los entornos `local` y `produccion` (solo la variable `baseUrl`).
- Swagger en `/swagger-ui.html` con título "Sillón API", esquema Bearer JWT y controladores etiquetados 0 a 9.
- El README no incluye valores de variables de entorno, solo sus nombres.
