# Sillón · Especificación de trabajo

Fuente de requisitos: enunciado «Sillón · Proyecto integrador · Módulos 4 y 5». El contrato de la sección 4 del enunciado prevalece sobre este resumen. No se cambian nombres de rutas, campos ni códigos HTTP sin contrastarlos con él.

## Problema y producto

Las reservas por mensajes o cuaderno producen cruces de horario, espacios vacíos y respuestas tardías a clientes. Sillón ofrece una API SaaS para que cada barbería o salón gestione equipo, servicios, clientes y citas; el público puede consultar horarios libres y reservar sin cuenta.

## Actores y límites

- `DUENO`: administra su salón, catálogo, equipo, citas, créditos y pagos.
- `ESTILISTA`: consulta su agenda y marca sus citas como completadas o no asistidas.
- Público: consulta catálogo y disponibilidad, reserva y conversa con el chatbot.
- `ADMIN`: consulta los salones de la plataforma.
- Cada dato de negocio pertenece a un salón. El acceso de otro salón se responde con 404; las listas se filtran por salón.

## Planes

- Registro: plan `GRATIS`, 10 créditos de IA, máximo 2 estilistas, horario 08:00–19:00, domingos cerrados e intervalo de 30 minutos.
- `PRO`: estilistas sin límite y 300 créditos adicionales al confirmarse el pago de suscripción.
- Paquete de créditos: 200 créditos adicionales tras un pago único confirmado.
- Un crédito corresponde a una respuesta de IA. Sin saldo se responde 402 antes de llamar al proveedor; el descuento ocurre cuando este responde.

## Reglas centrales

- Correo único en la plataforma; slug único derivado del nombre del salón.
- El registro crea salón y dueño en una transacción. El slug se escribe en minúsculas, sin tildes, con los grupos de caracteres no alfanuméricos convertidos a guiones y sin guiones en los extremos; si está ocupado, se agrega `-2`, `-3`, etc. El correo repetido responde 409.
- Apertura anterior al cierre; intervalo entre 15 y 60 minutos. Servicio de 15 a 480 minutos y precio entero positivo en pesos colombianos.
- Un estilista ofrece uno o más servicios de su salón. Un servicio de otro salón responde 404. El tercer estilista en `GRATIS` recibe 402.
- La duración de una cita es la del servicio y su precio queda congelado al crearla. No se permiten citas en el pasado, fuera de horario, en domingo cerrado, con un estilista no habilitado o cruzadas con citas pendientes/confirmadas.
- Al agendar se busca al cliente por teléfono dentro del salón y se crea si no existe. El teléfono se conserva solo con dígitos y el signo `+`.
- Una cita interna nace `CONFIRMADA`, origen `SALON`; una reserva pública nace `PENDIENTE`, origen `EN_LINEA`.
- Transiciones: `PENDIENTE` a `CONFIRMADA` o `CANCELADA`; `CONFIRMADA` a `COMPLETADA`, `CANCELADA` o `NO_ASISTIO`. Los demás estados son finales. El estilista solo puede marcar sus citas `COMPLETADA` o `NO_ASISTIO`.
- La disponibilidad la calcula Java con un reloj de zona configurable; el valor por defecto exigido por el enunciado es `America/Bogota` y el entorno local del usuario usa `America/Santiago`. El inicio cabe por completo en el horario y no cruza una cita que ocupa agenda.
- La agenda valida que inicio y fin caigan el mismo día. Dos citas se cruzan si el inicio de cada una es anterior al fin de la otra; terminar justo cuando otra empieza no constituye cruce. Los turnos libres se generan desde la apertura cada `intervaloMinutos`, excluyendo horas pasadas.
- El backend decide precios de Stripe; la petición de pago no acepta precio ni ID de precio. Un salón `PRO` que pide otra suscripción recibe 409. Webhooks sin firma válida reciben 400 y cada evento se procesa una sola vez por su ID. Al pagar PRO se guarda el ID de suscripción y se suman 300 créditos; el paquete suma 200. La cancelación de suscripción devuelve al salón a `GRATIS` sin quitar créditos.
- Solo se genera recordatorio para una cita `PENDIENTE` o `CONFIRMADA`; otro estado recibe 409. Java prepara servicio, estilista, fecha, hora, valor y dirección verificados. La IA redacta y una persona decide enviarlo; Sillón no lo envía.
- El chatbot usa el catálogo real. No agenda, cambia ni cancela citas: escala esas solicitudes y guarda los mensajes del cliente en una solicitud de contacto. Si la respuesta menciona descuentos, cupones, gratuidad, promociones o reembolsos, Java entrega un mensaje seguro fijo y escala con motivo `REGLA_DE_SEGURIDAD`.
- Toda salida de IA se pide en JSON y se valida. JSON inválido o incompleto: 502; ningún proveedor disponible: 503. Las llaves solo viven en `secrets.properties` local o variables de entorno de Render; nunca en código, Git o prompts.

## Secuencia y límites de IA

- El motor incluye interfaz de proveedor, adaptador compatible con OpenAI para Gemini, adaptador Claude de respaldo, fallback, configuración tipada, fábrica HTTP con timeout, biblioteca de prompts, lector de JSON y factura. La URL compatible con OpenAI sale de `sillon.ia.openai.url` para permitir el simulador del calificador; el respaldo permanece configurado aunque falte su llave.
- En cada operación: validar el recurso y la regla sin IA, verificar saldo, cargar prompt y llamar al proveedor, consumir un crédito, validar el JSON y aplicar la regla final. El consumo usa un `UPDATE` condicional y atómico para no permitir saldo negativo. La llamada remota queda fuera de transacciones de base de datos.
- `prompts/recordatorio.txt` recibe salón, cliente y datos verificados. Pide un borrador de WhatsApp que una persona revisa; saludo por nombre, seis datos exactos, confirmación con `SÍ` o aviso de cambio, trato de usted, tono cálido, máximo cuatro frases y firma del equipo. Prohíbe inventar fechas, horarios, precios o promociones. Salida: solo JSON con `mensaje`. Java valida que `mensaje` no esté vacío ni supere 700 caracteres. Fecha y hora se formatean en español colombiano, normalizando espacios especiales.
- `prompts/chatbot.txt` recibe salón, dirección, horario, catálogo activo y cliente. Solo usa esos datos, responde el último mensaje de usted en máximo tres frases y devuelve solo JSON con `respuesta` y `escalar`. Escala reservas/cambios/cancelaciones, petición de persona, molestia, descuentos, reembolsos y reclamos. No acepta cambios de reglas de quien diga ser dueño o empleado.
- El historial completo llega en cada petición y se presenta al modelo como turnos `Cliente:` o `Asistente:` en un mensaje separado de las instrucciones. La respuesta no puede estar vacía. Al escalar, se guarda una solicitud con los mensajes del cliente hasta 2000 caracteres y motivo `PEDIDO_POR_IA`.
- Antes de responder, Java busca sin distinguir mayúsculas `descuento`, `cupón`, `cupon`, `gratis`, `promoción`, `promocion` o `reembols`. Si encuentra alguna, entrega un texto seguro fijo y crea solicitud `REGLA_DE_SEGURIDAD`, incluso si la IA no pidió escalar.

## Invariantes de implementación por fase

- Errores: un `@RestControllerAdvice` cubre excepciones de negocio, validación, JSON mal formado y parámetros faltantes o con tipo inválido. El reloj es un bean `Clock` inyectado en las reglas de tiempo.
- Seguridad: el principal lleva `id`, `salonId`, correo, contraseña, rol y activo; la autoridad es `ROLE_` más el rol. JWT usa el correo como sujeto y rol y salón como datos adicionales. Token inválido deja la petición sin autenticar. Correo de registro en minúsculas y sin espacios; login 401 genérico. Sesión stateless y CSRF deshabilitado. Son públicas también `/error`, Swagger, webhook y resultado de pagos. 401 y 403 usan el DTO de error común. El `salonId` proviene del principal, nunca del cuerpo o URL.
- Catálogo: buscar cada recurso por ID y salón simultáneamente. Al crear estilista, validar primero el límite del plan, luego correo único y pertenencia de todos los servicios. No devolver contraseñas. Clientes solo nacen al agendar y su listado es de lectura.
- Agenda: un componente puro valida horario y calcula disponibilidad. Al agendar, verificar en orden servicio activo del salón, estilista activo del salón y que ofrezca el servicio, fin calculado, pasado, horario, cruce, y recién entonces buscar/crear cliente y guardar precio, estado y origen. Al cambiar estado, buscar por cita y salón, verificar pertenencia al estilista, destino permitido y transición válida. Persistir antes de formar la respuesta para actualizar `fechaActualizacion`.
- Catálogo público: solo servicios y estilistas activos, con los IDs de servicios ofrecidos y horario en texto; nunca exponer correos o contraseñas. La reserva pública reutiliza la misma operación de agendar.
- Stripe: construir el cliente con `STRIPE_URL_API`. La sesión lleva correo del dueño, URL pública de éxito/cancelación, referencia al salón y metadata de salón y tipo; para PRO, repetir metadata en la suscripción. El webhook recibe cuerpo plano y firma, omite eventos ya vistos, aplica `checkout.session.completed` solo cuando `payment_status` sea `paid`, maneja `customer.subscription.deleted` por ID de suscripción y guarda cambio y evento en una transacción.

## Tecnología y estructura

- Java 17, Spring Boot 4.1.x y Maven; JPA con MySQL local y PostgreSQL en Supabase; Spring Security, JWT y BCrypt.
- Swagger con springdoc 3.x; Stripe Checkout y webhooks en modo prueba; Gemini por HTTP con `RestClient` y fallback entre proveedores; JUnit 5 y Mockito; Docker y Render.
- Sin Lombok, Spring AI ni SDK de IA. Paquete base `com.devsenior.sillon`; dominios `compartido`, `seguridad`, `auth`, `salon`, `usuario`, `servicio`, `cliente`, `cita`, `publico`, `pago`, `chat`, `ia`.

## Modelo persistente

| Entidad | Tabla y atributos Java | Relaciones y restricciones |
|---|---|---|
| `Salon` | `salones`: `id`, `nombre`, `slug`, `ciudad`, `direccion`, `telefono`, `plan`, `creditosIa`, `stripeSuscripcionId`, `configuracion`, `fechaCreacion` | `slug` único; dueño de los datos del inquilino. |
| `ConfiguracionSalon` | Embebida: `horaApertura`, `horaCierre`, `abreDomingos`, `intervaloMinutos` | `@Embeddable` dentro de `Salon`. |
| `Usuario` | `usuarios`: `id`, `nombre`, `email`, `password`, `rol`, `activo` | Muchos a uno con `Salon`; email globalmente único; contraseña BCrypt; estilista-servicio muchos a muchos mediante `estilista_servicios`. |
| `Servicio` | `servicios`: `id`, `nombre`, `descripcion`, `duracionMinutos`, `precio`, `activo` | Muchos a uno con `Salon`. |
| `Cliente` | `clientes`: `id`, `nombre`, `telefono` | Muchos a uno con `Salon`; teléfono único por salón. |
| `Cita` | `citas`: `id`, `inicio`, `fin`, `precio`, `estado`, `origen`, `nota`, `fechaCreacion`, `fechaActualizacion` | Muchos a uno con `Salon`, `Cliente`, estilista `Usuario` y `Servicio`. |
| `SolicitudContacto` | `solicitudes_contacto`: `id`, `clienteNombre`, `clienteContacto`, `mensajes`, `motivo`, `estado`, `fechaCreacion` | Muchos a uno con `Salon`. |
| `EventoStripe` | `eventos_stripe`: `id`, `tipo`, `fechaProcesado` | `id` es el ID de Stripe para idempotencia. |

Todos los enums se guardan como texto (`EnumType.STRING`): `Plan` = `GRATIS`, `PRO`; `Rol` = `DUENO`, `ESTILISTA`, `ADMIN`; `EstadoCita` = `PENDIENTE`, `CONFIRMADA`, `COMPLETADA`, `CANCELADA`, `NO_ASISTIO`; `OrigenCita` = `SALON`, `EN_LINEA`; `TipoCompra` = `SUSCRIPCION_PRO`, `PAQUETE_CREDITOS`; `MotivoSolicitud` = `PEDIDO_POR_IA`, `REGLA_DE_SEGURIDAD`; `EstadoSolicitud` = `PENDIENTE`, `ATENDIDA`.

## Acceso por rol

Registro, login, catálogo público, disponibilidad, reserva y chat no requieren token. Solo el dueño configura su salón, modifica servicios, administra estilistas y clientes, crea citas, solicita recordatorios, paga y consulta solicitudes. Dueño y estilista leen servicios y agenda; el dueño ve todas las citas y el estilista solo las suyas. El estilista solo marca sus citas como `COMPLETADA` o `NO_ASISTIO`. Solo `ADMIN` lee todos los salones. El primer administrador se crea registrando un usuario y cambiando su rol en la base de datos.

## Convenciones del contrato HTTP

- Fechas: `AAAA-MM-DD`; fecha y hora: `AAAA-MM-DDTHH:mm:ss`; hora enviada: `HH:mm` (Jackson responde `HH:mm:ss`). Precios: enteros en pesos colombianos.
- Las rutas privadas exigen `Authorization: Bearer <token>`.
- Todo error, incluidos 401 y 403, usa `codigo` (número HTTP), `error` (nombre estándar), `mensaje` (español; validaciones de campos separadas por `;`) y `fecha`.
- 400: entrada inválida, JSON roto, parámetro faltante o firma falsa. 401: autenticación ausente, inválida o credenciales erradas. 402: límite de plan o créditos agotados. 403: rol o transición de estilista prohibida. 404: recurso ausente o de otro salón. 409: conflicto de reglas. 502: respuesta inutilizable de Stripe o IA. 503: ningún proveedor de IA disponible.

## Operaciones y DTO

| Método y ruta | Acceso y entrada | Salida y errores específicos |
|---|---|---|
| `POST /api/auth/registro` | Público. `salonNombre` ≤100, `ciudad` ≤60, `direccion` ≤150, `telefono` ≤30, `nombre` ≤100, `email` válido, `password` de 8 a 72. | 201: `token`, `tipo`=`Bearer`, `expiraEnMinutos`, `usuarioId`, `nombre`, `rol`, `salonId`, `salonSlug`. 400/409. |
| `POST /api/auth/login` | Público. `email`, `password`. | 200: mismo DTO de autenticación. 401. |
| `GET /api/salon` | `DUENO`. | 200: `id`, `nombre`, `slug`, `ciudad`, `direccion`, `telefono`, `plan`, `creditosIa`, `configuracion` y `fechaCreacion`. Configuración: `horaApertura`, `horaCierre`, `abreDomingos`, `intervaloMinutos`. |
| `PUT /api/salon/configuracion` | `DUENO`. Los cuatro campos de configuración son obligatorios; intervalo 15–60. | 200: salón actualizado. 400/409. |
| `POST /api/servicios` | `DUENO`. `nombre` ≤80, `descripcion` opcional ≤300, `duracionMinutos` 15–480, `precio` >0, `activo` opcional (por defecto `true`). | 201: `id`, `nombre`, `descripcion`, `duracionMinutos`, `precio`, `activo`. 400. |
| `GET /api/servicios` | `DUENO` o `ESTILISTA`. | 200: servicios del salón. |
| `PUT /api/servicios/{id}` | `DUENO`. Mismos campos del POST. | 200; 400/404. |
| `POST /api/estilistas` | `DUENO`. `nombre`, `email`, `password` de 8 a 72, `servicioIds` con al menos un ID. | 201: `id`, `nombre`, `email`, `activo`, `servicios` como nombres. 400/402/404/409. |
| `GET /api/estilistas` | `DUENO`. | 200: estilistas del salón. |
| `GET /api/clientes` | `DUENO`. | 200: clientes del salón con `id`, `nombre`, `telefono`. |
| `POST /api/citas` | `DUENO`. `clienteNombre`, `clienteTelefono` de 7 a 30 caracteres, `servicioId`, `estilistaId`, `inicio`, `nota` opcional ≤300. | 201: DTO de cita. 400/404/409. |
| `GET /api/citas?fecha=` | `DUENO` o `ESTILISTA`. Fecha opcional; por defecto, hoy en Colombia. | 200: citas del día ordenadas por hora; el estilista solo ve las suyas. |
| `PATCH /api/citas/{id}/estado` | `DUENO` o `ESTILISTA`. `estado`. | 200: DTO de cita. 400/403/404/409. |
| `POST /api/citas/{id}/recordatorio` | `DUENO`. Sin cuerpo. | 200: `citaId`, `telefonoCliente`, `mensaje`, `creditosRestantes`, `factura`. 402/404/409/502/503. |
| `GET /api/publico/salones/{slug}` | Público. | 200: `nombre`, `slug`, `ciudad`, `direccion`, `telefono`, `horario` como texto, servicios activos y estilistas (`id`, `nombre`, `servicioIds`); sin correos. 404. |
| `GET /api/publico/salones/{slug}/disponibilidad` | Público. Parámetros obligatorios `servicioId`, `estilistaId`, `fecha`. | 200: `fecha`, `servicio`, `estilista`, `duracionMinutos`, `horariosLibres`. 400/404/409. |
| `POST /api/publico/salones/{slug}/reservas` | Público. Misma entrada que `POST /api/citas`. | 201: DTO de cita en `PENDIENTE`, origen `EN_LINEA`. 400/404/409. |
| `POST /api/publico/salones/{slug}/chat` | Público. `clienteNombre`, `clienteContacto` ≤120, `historial` de 1 a 20 turnos con `rol`=`CLIENTE` o `ASISTENTE` y `texto` ≤500. | 200: `respuesta`, `escalado`, `solicitudId` (o `null`), `factura`. 400/402/404/502/503. |
| `GET /api/solicitudes` | `DUENO`. | 200: solicitudes recientes primero, con `id`, `clienteNombre`, `clienteContacto`, `mensajes`, `motivo`, `estado`, `fechaCreacion`. |
| `PATCH /api/solicitudes/{id}/atendida` | `DUENO`. Sin cuerpo. | 200; 404/409 si ya estaba atendida. |
| `POST /api/pagos/suscripcion` | `DUENO`. Sin precio ni ID de precio en el cuerpo. | 200: `tipo`=`SUSCRIPCION_PRO`, `sesionId`, `urlPago`. 409/502. |
| `POST /api/pagos/creditos` | `DUENO`. Sin precio ni ID de precio en el cuerpo. | 200: `tipo`=`PAQUETE_CREDITOS`, `sesionId`, `urlPago`. 502. |
| `POST /api/pagos/webhook` | Stripe. Cuerpo del evento y cabecera `Stripe-Signature`. | 200: `recibido`, `procesado`; 400 si la firma no es válida. |
| `GET /api/pagos/resultado?estado=` | Público. Estado `exito` o `cancelado`. | 200: mensaje de retorno de Stripe. |
| `GET /api/admin/salones` | `ADMIN`. | 200: todos los salones; 403 para otro rol. |

El DTO de cita contiene `id`, `inicio`, `fin`, `estado`, `origen`, `precio`, `servicioId`, `servicioNombre`, `estilistaId`, `estilistaNombre`, `clienteNombre`, `clienteTelefono`, `nota`, `fechaCreacion`, `fechaActualizacion`. La factura de IA contiene `proveedor`, `modelo`, `tokensEntrada`, `tokensSalida`, `costoEstimadoUsd`, `milisegundos`.

## Verificación y entrega

- Tests de servicios con JUnit 5, Mockito y reloj fijo, sin levantar Spring ni llamar a proveedores reales. Cubrir las cinco transiciones válidas y al menos seis bloqueadas; horario, domingo, cruces, pasado; agendamiento, permisos y precio congelado; límite GRATIS/PRO; webhook firmado, repetido, cancelación y créditos; chatbot, recordatorio, JSON inválido y fallback. Mantener aparte el test de contexto completo que requiere MySQL.
- Swagger UI en `/swagger-ui.html`, título `Sillón API` y esquema Bearer JWT. La sección de Swagger del PDF dice «22 rutas», pero su contrato de la sección 4 enumera 25 operaciones; implementaremos y documentaremos las 25 enumeradas. Etiquetas: `0 Público`, `1 Autenticación`, `2 Mi salón`, `3 Servicios`, `4 Estilistas`, `5 Clientes`, `6 Agenda`, `7 Pagos`, `8 Solicitudes`, `9 Administración`.
- Un solo fork en `Fhv5`, proyecto en la raíz y un solo PR abierto hacia el repositorio oficial. No modificar `.github/`; commits por fase.
- Entregar `SPEC.md`, `AGENTS.md`, `BITACORA-IA.md`, `secrets.example.properties`, dos prompts en `src/main/resources/prompts`, tests JUnit/Mockito, `Dockerfile`, `.dockerignore`, colección y entornos de Postman, README y API de Render con Swagger. `secrets.properties` y llaves reales jamás se suben.
- Colección con siete carpetas en el orden prescrito, al menos 50 peticiones y 40 con pruebas según README; entornos `local` y `produccion` con `baseUrl`. Las carpetas 1–6 corren de principio a fin sin copiar tokens manualmente. Correos y salones usan sufijo por ejecución; fechas futuras y domingo se calculan dinámicamente. Carpeta 7 verifica manualmente después de pagar.
- README con URL completa de Render, Swagger, arranque local, variables de entorno por nombre, verificación y reflexión final. El calificador recompila, prueba con simuladores y consulta la API desplegada en cada push.
- Producción: Docker de dos etapas con Java 17; Supabase Session Pooler en 5432; Render con variables de base, JWT, URL pública, Stripe y Gemini; webhook de Stripe para `checkout.session.completed` y `customer.subscription.deleted`.

## Fuera de alcance

No habrá interfaz gráfica, envío real de WhatsApp o correo, recordatorios programados, reportes ni pagos del cliente final al salón.
