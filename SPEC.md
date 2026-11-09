# SPEC.md — Sillón

> **Proyecto integrador · Módulos 4 y 5 · Java AI Engineer · Dev Senior Code**  
> **Estado:** especificación funcional y contractual de referencia para la implementación.  
> **Fuente de verdad:** enunciado oficial «Sillón · Proyecto integrador · Módulos 4 y 5» (40 páginas), especialmente las secciones 2, 3 y 4. El README del repositorio complementa los requisitos de entrega y calificación.  
> **Regla de precedencia:** no alterar los requisitos del enunciado. Si un detalle no está definido, no inventar un contrato público; documentar la decisión antes de implementarla.

## 1. Visión del producto

**Sillón** es una API REST SaaS multi-tenant para administrar agendas de barberías y salones de belleza. Permite que cada establecimiento configure horarios, servicios y estilistas; que sus clientes consulten disponibilidad y reserven sin crear una cuenta; y que el dueño use herramientas de IA para redactar recordatorios y atender consultas frecuentes.

### 1.1 Problema

Las reservas por WhatsApp, cuaderno o memoria producen pérdida de mensajes, doble asignación de estilistas, citas olvidadas y dificultad para responder consultas de precios y horarios fuera del horario de atención.

### 1.2 Objetivos

- Registrar salones y sus dueños en una misma plataforma, con **aislamiento estricto de datos**.
- Gestionar servicios, estilistas y clientes de cada salón.
- Crear y consultar citas con validación de duración, disponibilidad, horario y estado.
- Permitir consultas y reservas públicas sin autenticación.
- Comercializar el plan PRO y paquetes de créditos de IA mediante Stripe en modo de prueba.
- Generar borradores de recordatorios y respuestas de chatbot con IA, siempre sujetos a reglas verificadas en Java.
- Documentar la API con Swagger, probarla con JUnit/Mockito y Postman, y desplegarla con Docker en Render y PostgreSQL de Supabase.

### 1.3 Entregable técnico

Un **backend exclusivamente**, construido en Java con Spring Boot, API REST, sin interfaz gráfica. Se consume desde Postman y Swagger UI. El proyecto compilable y su `pom.xml` deben estar en la **raíz del fork oficial**.

### 1.4 Fuera de alcance

- Envío real de WhatsApp o correos.
- Recordatorios automáticos programados y reportes.
- Cobros de los clientes finales a los salones / Stripe Connect.
- Frontend o aplicación móvil.
- Permitir que la IA reserve, modifique o cancele citas.

## 2. Plataforma y restricciones tecnológicas

| Área | Tecnología / regla |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 4.1.x, Maven |
| Persistencia | Spring Data JPA; MySQL local, PostgreSQL (Supabase) en producción |
| Seguridad | Spring Security, JWT, BCrypt y autorización por roles |
| Documentación | Swagger/OpenAPI mediante `springdoc` 3.x |
| Pagos | Stripe Java en **modo prueba**, Checkout, suscripciones, pagos únicos, webhooks |
| IA | Gemini por HTTP con `RestClient`, proveedor de respaldo y fallback |
| Pruebas | JUnit 5 y Mockito; colección de Postman con aserciones |
| Despliegue | Docker multi-stage, Render y Supabase |
| Flujo asistido | Cursor/Codex, `SPEC.md`, `AGENTS.md`, `BITACORA-IA.md` |

**Restricciones:** no usar Lombok, Spring AI ni SDKs de IA. Getters y setters explícitos. Las llamadas a modelos se implementan por HTTP. En Spring Boot 4, para el parser JSON se debe tener presente Jackson 3 (`tools.jackson.databind.ObjectMapper`), manteniendo las anotaciones JSON que correspondan. El agente no puede sustituir estas decisiones tecnológicas por alternativas sin autorización.

### 2.1 Organización por dominios

Paquete base: `com.devsenior.sillon`.

| Paquete | Responsabilidad |
|---|---|
| `compartido` | Error HTTP uniforme, excepciones, manejador global, `Clock`, Swagger |
| `seguridad` | JWT, filtro, usuario autenticado y reglas de acceso |
| `auth` | Registro del salón con dueño y login |
| `salon` | Tenant, plan, créditos y configuración |
| `usuario` | Usuarios, roles, estilistas |
| `servicio` | Catálogo del salón |
| `cliente` | Clientes finales |
| `cita` | Agenda, estados, reglas, disponibilidad y recordatorio |
| `publico` | Catálogo, disponibilidad, reservas y chat sin token |
| `pago` | Stripe Checkout, suscripciones, webhooks |
| `chat` | Chatbot, solicitudes de contacto |
| `ia` | Proveedores, fallback, prompts, validación JSON y factura de uso |

## 3. Tenancy, roles y planes

### 3.1 Aislamiento multi-tenant

- Cada `Salon` representa un tenant de la plataforma; todos comparten aplicación y base de datos.
- Los datos de servicios, usuarios/estilistas, clientes, citas y solicitudes pertenecen a un salón.
- Los endpoints privados deben usar el **`salonId` del usuario autenticado**, nunca un identificador proporcionado por el cliente para elegir el tenant.
- Todo acceso a un recurso de otro salón debe responder **404**, sin confirmar su existencia.
- Listados de un salón no deben incluir registros de otros salones; para un salón sin datos, devolver listas vacías.
- La administración global (`ADMIN`) puede consultar todos los salones únicamente por la ruta prevista.

### 3.2 Roles

- `DUENO`: administra su salón, servicios, estilistas, clientes, citas, pagos y solicitudes.
- `ESTILISTA`: consulta servicios y solo su propia agenda; puede marcar sus propias citas como completadas o no asistidas.
- `ADMIN`: consulta todos los salones de la plataforma.
- **Público**: se registra, inicia sesión, consulta catálogo/disponibilidad y crea reservas o usa chat sin token.

### 3.3 Matriz de permisos

| Operación | Público | DUENO | ESTILISTA | ADMIN |
|---|:---:|:---:|:---:|:---:|
| Registro y login | Sí | Sí | Sí | Sí |
| Catálogo, disponibilidad, reserva y chat públicos | Sí | Sí | Sí | Sí |
| Ver/configurar su salón | No | Sí | No | No |
| Crear/editar servicios | No | Sí | No | No |
| Listar servicios privados | No | Sí | Sí | No |
| Crear/listar estilistas y listar clientes | No | Sí | No | No |
| Crear cita desde salón o pedir recordatorio | No | Sí | No | No |
| Consultar agenda | No | Todas las del salón | Solo las propias | No |
| Cambiar estado de cita | No | Transición válida en su salón | Solo propia, hacia `COMPLETADA` o `NO_ASISTIO` | No |
| Comprar PRO/créditos y consultar solicitudes | No | Sí | No | No |
| Ver todos los salones | No | No | No | Sí |

### 3.4 Planes y créditos

| Plan/producto | Modalidad | Beneficio |
|---|---|---|
| `GRATIS` | Sin pago al registrarse | Hasta **2 estilistas** y **10 créditos IA** iniciales |
| `PRO` | Suscripción mensual Stripe | Estilistas sin límite y **300 créditos IA adicionales** al activarse |
| Paquete IA | Pago único Stripe | **200 créditos IA adicionales** |

Un crédito equivale a **una respuesta de IA**; se verifica antes de llamar al proveedor y se descuenta cuando éste responde. La cancelación de PRO devuelve el salón a GRATIS, pero **conserva el saldo de créditos**.

## 4. Modelo de datos

> Las entidades, atributos y relaciones son los expresamente indicados por el enunciado. No se agregan campos a la API pública por conveniencia.

| Entidad / tabla | Atributos | Relaciones / restricciones |
|---|---|---|
| `Salon` / `salones` | `id`, `nombre`, `slug`, `ciudad`, `direccion`, `telefono`, `plan`, `creditosIa`, `stripeSuscripcionId`, `configuracion`, `fechaCreacion` | `slug` único; tiene usuarios, servicios, clientes, citas y solicitudes |
| `ConfiguracionSalon` (embebida) | `horaApertura`, `horaCierre`, `abreDomingos`, `intervaloMinutos` | `@Embeddable` dentro de salón |
| `Usuario` / `usuarios` | `id`, `nombre`, `email`, `password`, `rol`, `activo` | Muchos-a-uno a `Salon`; muchos-a-muchos a `Servicio` mediante `estilista_servicios`; `email` único global |
| `Servicio` / `servicios` | `id`, `nombre`, `descripcion`, `duracionMinutos`, `precio`, `activo` | Muchos-a-uno a `Salon` |
| `Cliente` / `clientes` | `id`, `nombre`, `telefono` | Muchos-a-uno a `Salon`; teléfono único **por salón** |
| `Cita` / `citas` | `id`, `inicio`, `fin`, `precio`, `estado`, `origen`, `nota`, `fechaCreacion`, `fechaActualizacion` | Muchos-a-uno con `Salon`, `Cliente`, `Usuario` (estilista), `Servicio` |
| `SolicitudContacto` / `solicitudes_contacto` | `id`, `clienteNombre`, `clienteContacto`, `mensajes`, `motivo`, `estado`, `fechaCreacion` | Muchos-a-uno a `Salon` |
| `EventoStripe` / `eventos_stripe` | `id` (ID del evento Stripe), `tipo`, `fechaProcesado` | Evento único para idempotencia |

### 4.1 Enumeraciones exactas

| Enum | Valores |
|---|---|
| `Plan` | `GRATIS`, `PRO` |
| `Rol` | `DUENO`, `ESTILISTA`, `ADMIN` |
| `EstadoCita` | `PENDIENTE`, `CONFIRMADA`, `COMPLETADA`, `CANCELADA`, `NO_ASISTIO` |
| `OrigenCita` | `SALON`, `EN_LINEA` |
| `TipoCompra` | `SUSCRIPCION_PRO`, `PAQUETE_CREDITOS` |
| `MotivoSolicitud` | `PEDIDO_POR_IA`, `REGLA_DE_SEGURIDAD` |
| `EstadoSolicitud` | `PENDIENTE`, `ATENDIDA` |

Guardar enumeraciones como cadenas con `EnumType.STRING`. No usar tildes en sus identificadores.

## 5. Reglas funcionales

### 5.1 Salones y registro

1. Al registrar un salón se crean **en una misma transacción** el salón y su usuario `DUENO`; el plan inicial es `GRATIS`, con **10 créditos**.
2. Configuración por defecto: apertura `08:00`, cierre `19:00`, `abreDomingos=false`, `intervaloMinutos=30`.
3. El correo del usuario es único en la plataforma, también para estilistas: duplicado → **409**. Guardar correo en minúsculas, sin espacios externos.
4. `slug` público: derivar del nombre, convertir a minúsculas, quitar tildes, cambiar caracteres no alfanuméricos por guiones y quitar los extremos; si existe, añadir `-2`, `-3`, etc. Ejemplo: «Barbería La Navaja» → `barberia-la-navaja`.
5. Apertura debe ser anterior al cierre; un horario contrario → **409**.
6. `intervaloMinutos` debe estar entre **15 y 60**; fuera del rango → **400**.

### 5.2 Servicios, estilistas y clientes

1. Un servicio dura entre **15 y 480 minutos**, tiene precio **entero positivo en COP**, y puede estar activo o inactivo; validación inválida → **400**.
2. Un estilista debe ofrecer **uno o más servicios del mismo salón**; referencia a servicio de otro salón → **404**.
3. El tercer estilista del plan `GRATIS` → **402**; en `PRO` no hay límite.
4. Para crear un estilista se comprueba primero el límite del plan, luego las demás reglas de correo y servicios según la implementación documentada. Correo repetido → **409**.
5. El cliente se identifica por teléfono **dentro del salón**. Al agendar, buscar o crear; normalizar teléfono conservando únicamente dígitos y `+`.
6. El listado de clientes es de solo lectura; los clientes se originan al crear reservas.

### 5.3 Agenda y disponibilidad

1. Una cita dura exactamente `duracionMinutos` de su servicio; el backend calcula `fin = inicio + duración`.
2. Al crear cita, copiar `precio` del servicio; cambios posteriores del precio no alteran la cita existente.
3. No reservar en el pasado, fuera del horario del salón, en domingo cerrado o con un estilista que no ofrezca el servicio → **409**.
4. No permitir citas superpuestas del mismo estilista cuando una cita ocupa agenda (`PENDIENTE` o `CONFIRMADA`) → **409**.
5. Dos intervalos se superponen cuando `inicioA < finB` **y** `finA > inicioB`. Si una termina exactamente cuando comienza la otra, **no** se cruzan.
6. Las citas creadas por el salón nacen `CONFIRMADA` y origen `SALON`; las reservas públicas nacen `PENDIENTE` y origen `EN_LINEA`.
7. Validar que inicio y fin estén en el mismo día, que inicio no sea anterior a apertura y fin no supere cierre.
8. La disponibilidad se calcula **en Java, nunca con IA**: generar inicios desde la apertura avanzando por `intervaloMinutos`, admitir únicamente donde el servicio termine dentro del horario, no haya cruce y no estén en el pasado.
9. Para determinar «hoy» y «ahora», usar `Clock` inyectado con zona `America/Bogota`, no la hora local del servidor.
10. La agenda del dueño muestra todas las citas de su salón; la del estilista muestra exclusivamente las propias, ordenadas por hora.

### 5.4 Máquina de estados de citas

| Estado actual | Destino permitido |
|---|---|
| `PENDIENTE` | `CONFIRMADA`, `CANCELADA` |
| `CONFIRMADA` | `COMPLETADA`, `CANCELADA`, `NO_ASISTIO` |
| `COMPLETADA` | Ninguno (final) |
| `CANCELADA` | Ninguno (final) |
| `NO_ASISTIO` | Ninguno (final) |

- Transición no incluida → **409**.
- Un estilista solo puede cambiar **sus** citas; ajenas → **404**.
- Un estilista solo puede solicitar destino `COMPLETADA` o `NO_ASISTIO`; otro destino → **403**. La transición además debe ser válida según la tabla.
- Al cambiar estado debe actualizarse `fechaActualizacion` y devolverse la cita actualizada.

### 5.5 Pagos Stripe

1. El frontend/consumidor **no** elige montos ni IDs de precios: el backend decide el precio según el endpoint/tipo de compra.
2. Salón ya `PRO` que intenta crear otra suscripción → **409**.
3. El webhook debe verificar la firma `Stripe-Signature` sobre el cuerpo original; ausente o inválida → **400**.
4. Guardar el ID de cada evento Stripe para procesarlo como máximo una vez. Repetido: responder sin reaplicar efectos.
5. Evento `checkout.session.completed` con pago `paid`: aplicar la compra según la metadata del evento.
6. Activación `PRO`: guardar `stripeSuscripcionId`, pasar a `PRO` y añadir **300 créditos**.
7. Paquete pagado: añadir **200 créditos**, sin alterar el plan.
8. Evento `customer.subscription.deleted`: buscar por ID de suscripción, devolver a `GRATIS` y conservar créditos.
9. Aplicación de efectos y registro del evento en una **misma transacción**; si falla, no debe quedar ningún cambio parcial.
10. Stripe en **modo prueba**; productos de referencia: `Sillón PRO` a **USD 15 mensuales** y paquete de **200 créditos** a **USD 5 pago único**.

### 5.6 Motor de IA y créditos

1. Comprobar créditos antes de invocar IA: sin créditos → **402** y **ninguna llamada** al proveedor.
2. Secuencia exigida: validar recurso y reglas previas → verificar créditos → cargar prompt e invocar IA → consumir 1 crédito cuando el proveedor responde → leer/validar JSON → aplicar reglas de negocio → responder.
3. Descontar créditos mediante una **actualización atómica condicional** en DB que solo reste si hay saldo, evitando valores negativos ante concurrencia.
4. Nunca mantener una transacción de DB abierta durante llamadas a proveedores de IA.
5. Solicitar siempre **JSON**, validar sus campos obligatorios: JSON incorrecto/incompleto → **502**; sin proveedores disponibles → **503**.
6. Usar Gemini mediante endpoint HTTP de formato OpenAI con `RestClient`, y un proveedor de respaldo con fallback. Dirección configurable en `sillon.ia.openai.url` / `SILLON_IA_OPENAI_URL`.
7. Las credenciales se almacenan fuera del código: `secrets.properties` local y variables de entorno en Render. Nunca en repositorio, pruebas, prompts o logs.
8. Registrar la factura del uso de IA: `proveedor`, `modelo`, `tokensEntrada`, `tokensSalida`, `costoEstimadoUsd`, `milisegundos`.

### 5.7 Recordatorios con IA

1. Solo pueden generarse para citas `PENDIENTE` o `CONFIRMADA`; otras → **409**, sin gastar créditos.
2. **Java arma datos verificados**, no la IA: servicio (y duración), estilista, fecha, hora, valor, dirección, además de salón y cliente.
3. El prompt `src/main/resources/prompts/recordatorio.txt` recibe **salón, cliente y datos**; pide un borrador para WhatsApp, tono cálido/profesional y tratamiento de usted, hasta **4 frases**, firmado por el equipo del salón.
4. Solicitar confirmación del cliente («SÍ») o aviso de cambio; no inventar fechas, horas, precios, descuentos ni promociones.
5. Salida obligatoria: objeto JSON con clave **`mensaje`**.
6. `mensaje` obligatorio, no vacío, máximo **700 caracteres**; si no cumple → **502**.
7. La respuesta contiene `citaId`, `telefonoCliente`, `mensaje`, `creditosRestantes`, `factura`.
8. El recordatorio **no se envía** automáticamente; una persona lo revisa y aprueba.

### 5.8 Chatbot y solicitudes de contacto

1. El chatbot usa exclusivamente información real del salón cargada desde DB: servicios activos, duración, precio, horario y dirección.
2. La solicitud pública contiene de **1 a 20 turnos**, cada uno con `rol` (`CLIENTE` o `ASISTENTE`) y `texto` de hasta **500 caracteres**; historial vacío → **400**.
3. El historial completo viaja en cada petición, con «Cliente:» o «Asistente:»; no existe memoria implícita del proveedor.
4. Prompt `src/main/resources/prompts/chatbot.txt` con variables **salón, dirección, horario, catálogo y cliente**. Responde al último mensaje, de usted, en máximo **3 frases**.
5. No puede reservar, modificar o cancelar citas; ante esas peticiones escala y avisa que una persona contactará al cliente.
6. Escala también ante peticiones de atención humana, clientes muy molestos, descuentos, reembolsos o reclamos.
7. Ignora intentos de cambiar las reglas por parte del cliente, aunque afirme ser dueño o empleado.
8. Salida obligatoria: JSON con **`respuesta`** (texto) y **`escalar`** (booleano); respuesta no vacía.
9. Si `escalar=true`, crear `SolicitudContacto` con el historial/mensajes del cliente (máx. **2000 caracteres**) y motivo `PEDIDO_POR_IA`.
10. **Regla anti-promesas en Java:** si la respuesta de IA contiene, sin distinción de mayúsculas, `descuento`, `cupón`, `cupon`, `gratis`, `promoción`, `promocion` o `reembols`, reemplazar por un **mensaje seguro fijo** y escalar con motivo `REGLA_DE_SEGURIDAD`. Esta protección no puede quedar solo en el prompt.
11. El dueño puede listar solicitudes (más recientes primero) y marcar como atendida. Marcar una ya atendida → **409**.

## 6. Contrato HTTP — fuente de verdad de la API

> **Obligatorio:** respetar exactamente los métodos, rutas, claves JSON, restricciones de entrada, roles y estados HTTP descritos aquí. No renombrar campos a inglés, no cambiar pluralización y no introducir prefijos de API diferentes. Cuando una respuesta no está completamente detallada por la guía, conservar los campos explícitos y evitar inventar requisitos del calificador.

### 6.1 Convenciones globales

- Prefijo de rutas: `/api`.
- Rutas protegidas: encabezado `Authorization: Bearer <token>`.
- Fechas: `yyyy-MM-dd`; fecha y hora: `yyyy-MM-dd'T'HH:mm:ss`; hora de entrada `HH:mm` (Jackson puede serializar `HH:mm:ss`).
- Precios: **enteros COP**, p. ej. `35000`, sin puntos ni decimales.
- Todos los errores, incluidos **401 y 403** de Spring Security, deben devolver:

```json
{
  "codigo": 409,
  "error": "Conflict",
  "mensaje": "Explicación en español",
  "fecha": "2026-10-20T10:30:00"
}
```

`codigo` es HTTP numérico, `error` el nombre estándar, `mensaje` español (en errores de validación, nombre y mensaje de cada campo separados por `;`) y `fecha` fecha/hora del error.

| Código | Uso |
|---|---|
| `400` | Validación, JSON mal formado, parámetro faltante, firma inválida |
| `401` | Sin token, token inválido, credenciales inválidas |
| `402` | Límite del plan o falta de créditos IA |
| `403` | Rol sin permiso o transición no autorizada del estilista |
| `404` | Recurso inexistente o de otro salón |
| `409` | Conflictos de negocio: cruces, horarios, estados, correo duplicado, PRO ya activo |
| `502` | Respuesta inutilizable de Stripe o IA |
| `503` | No responde ningún proveedor de IA |

### 6.2 Autenticación — `/api/auth` (público)

**`POST /api/auth/registro`**

Entrada (propiedades exactas):

| Campo | Regla |
|---|---|
| `salonNombre` | Hasta 100 caracteres |
| `ciudad` | Hasta 60 |
| `direccion` | Hasta 150 |
| `telefono` | Hasta 30 |
| `nombre` | Nombre del dueño, hasta 100 |
| `email` | Formato válido; único global |
| `password` | 8–72 caracteres |

Salida **201**: `token`, `tipo` (valor `Bearer`), `expiraEnMinutos`, `usuarioId`, `nombre`, `rol`, `salonId`, `salonSlug`. Errores **400**, **409**.

**`POST /api/auth/login`** — entrada: `email`, `password`. Salida **200** con los mismos campos del registro. Credenciales incorrectas → **401** con mensaje genérico.

### 6.3 Mi salón — `/api/salon` (`DUENO`)

- **`GET /api/salon`** → **200**: `id`, `nombre`, `slug`, `ciudad`, `direccion`, `telefono`, `plan`, `creditosIa`, `configuracion` (`horaApertura`, `horaCierre`, `abreDomingos`, `intervaloMinutos`), `fechaCreacion`.
- **`PUT /api/salon/configuracion`** → entrada obligatoria: `horaApertura`, `horaCierre`, `abreDomingos`, `intervaloMinutos` (15–60). Salida **200** con salón actualizado; errores **400** o **409** si apertura no precede cierre.

### 6.4 Servicios — `/api/servicios`

- **`POST /api/servicios`** (`DUENO`) → entrada: `nombre` (máx. 80), `descripcion` (opcional, máx. 300), `duracionMinutos` (15–480), `precio` (entero >0), `activo` (opcional, defecto `true`). Salida **201** con `id`, `nombre`, `descripcion`, `duracionMinutos`, `precio`, `activo`; error **400**.
- **`GET /api/servicios`** (`DUENO`, `ESTILISTA`) → **200**, lista de servicios del salón.
- **`PUT /api/servicios/{id}`** (`DUENO`) → mismos campos del POST; **200**, **400**, **404**.

### 6.5 Estilistas y clientes (`DUENO`)

- **`POST /api/estilistas`** → entrada: `nombre`, `email`, `password` (8–72), `servicioIds` (al menos un ID). Salida **201**: `id`, `nombre`, `email`, `activo`, `servicios` (lista de nombres). Errores **400**, **402**, **404**, **409**.
- **`GET /api/estilistas`** → **200**, lista de estilistas del salón.
- **`GET /api/clientes`** → **200**, lista con `id`, `nombre`, `telefono` por cliente del salón.

### 6.6 Agenda — `/api/citas`

- **`POST /api/citas`** (`DUENO`) → entrada: `clienteNombre`, `clienteTelefono` (7–30 caracteres), `servicioId`, `estilistaId`, `inicio`, `nota` (opcional, máx. 300). Salida **201** con la cita `CONFIRMADA`/`SALON`; errores **400**, **404**, **409**.
- **`GET /api/citas?fecha=yyyy-MM-dd`** (`DUENO`, `ESTILISTA`) → fecha opcional (por defecto hoy en Colombia); **200**, lista ordenada por hora y filtrada por tenant/usuario.
- **`PATCH /api/citas/{id}/estado`** (`DUENO`, `ESTILISTA`) → entrada: `estado`; salida **200** con cita; errores **400**, **403**, **404**, **409**.
- **`POST /api/citas/{id}/recordatorio`** (`DUENO`) → sin cuerpo; **200**: `citaId`, `telefonoCliente`, `mensaje`, `creditosRestantes`, `factura`; errores **402**, **404**, **409**, **502**, **503**.

**DTO de respuesta de cita** (en creación, cambio de estado y reserva): `id`, `inicio`, `fin`, `estado`, `origen`, `precio`, `servicioId`, `servicioNombre`, `estilistaId`, `estilistaNombre`, `clienteNombre`, `clienteTelefono`, `nota`, `fechaCreacion`, `fechaActualizacion`.

**Factura IA**: `proveedor`, `modelo`, `tokensEntrada`, `tokensSalida`, `costoEstimadoUsd`, `milisegundos`.

### 6.7 Público — `/api/publico/salones/{slug}` (sin token)

- **`GET /api/publico/salones/{slug}`** → **200**: `nombre`, `slug`, `ciudad`, `direccion`, `telefono`, `horario` (texto), `servicios` (solo activos) y `estilistas` (`id`, `nombre`, `servicioIds`; solo activos). **Nunca** exponer emails ni passwords. **404** si no existe.
- **`GET /api/publico/salones/{slug}/disponibilidad?servicioId=&estilistaId=&fecha=`** → los tres parámetros obligatorios; **200**: `fecha`, `servicio`, `estilista`, `duracionMinutos`, `horariosLibres` (lista de horas). Errores **400**, **404**, **409**.
- **`POST /api/publico/salones/{slug}/reservas`** → mismos campos de entrada que `POST /api/citas`; **201** con cita `PENDIENTE`/`EN_LINEA`. Errores **400**, **404**, **409**.
- **`POST /api/publico/salones/{slug}/chat`** → entrada: `clienteNombre`, `clienteContacto` (máx. 120), `historial` de 1 a 20 turnos; cada turno `rol` (`CLIENTE` o `ASISTENTE`) y `texto` (máx. 500). **200**: `respuesta`, `escalado` (booleano), `solicitudId` (o `null`), `factura`. Errores **400**, **402**, **404**, **502**, **503**.

> Distinguir el JSON interno del proveedor IA (`escalar`) del JSON HTTP de chat (`escalado`): **no son el mismo nombre**.

### 6.8 Solicitudes — `/api/solicitudes` (`DUENO`)

- **`GET /api/solicitudes`** → **200** con solicitudes del propio salón, más recientes primero, con `id`, `clienteNombre`, `clienteContacto`, `mensajes`, `motivo`, `estado`, `fechaCreacion`.
- **`PATCH /api/solicitudes/{id}/atendida`** → sin cuerpo; **200**, **404** si no existe/no pertenece al salón; **409** si ya estaba atendida.

### 6.9 Pagos — `/api/pagos`

- **`POST /api/pagos/suscripcion`** (`DUENO`) → sin cuerpo; **200**: `tipo` (`SUSCRIPCION_PRO`), `sesionId`, `urlPago`. **409** si PRO; **502** por respuesta de Stripe inutilizable.
- **`POST /api/pagos/creditos`** (`DUENO`) → sin cuerpo; **200**: `tipo` (`PAQUETE_CREDITOS`), `sesionId`, `urlPago`. **502** por respuesta de Stripe inutilizable.
- **`POST /api/pagos/webhook`** (Stripe, sin token) → cuerpo bruto de Stripe y cabecera `Stripe-Signature`; **200**: `recibido`, `procesado`; **400** firma ausente/inválida. Evento repetido: `procesado=false`.
- **`GET /api/pagos/resultado?estado=`** (público) → `estado` = `exito` o `cancelado`; **200**, mensaje de resultado.

**Checkout:** incluir correo del dueño, URL pública de éxito y cancelación (`/api/pagos/resultado`), referencia al ID del salón, y metadata con ID de salón y tipo de compra. Para PRO, añadir metadata también a la suscripción. El backend es quien elige los IDs de precio.

### 6.10 Administración — `/api/admin`

- **`GET /api/admin/salones`** (`ADMIN`) → **200**, lista de todos los salones; para otros roles **403**.
- El primer `ADMIN` se crea registrando una cuenta normal y cambiando su rol directamente en la base de datos, según el procedimiento descrito por el enunciado; **no** hay endpoint adicional de creación de administradores.

## 7. Configuración y seguridad de secretos

### 7.1 `application.properties`

- Archivo de configuración **`.properties`**, no `.yml`, sin perfiles adicionales.
- Puerto `PORT`, defecto `8080`.
- MySQL local (`sillon_db`) con opción de creación de DB; conexión de producción por variables `SPRING_DATASOURCE_*`.
- `spring.jpa.hibernate.ddl-auto=update` y `spring.jpa.open-in-view=false`.
- Importación **opcional** de `secrets.properties` desde raíz.
- Zona del negocio `America/Bogota`, URL pública y constantes de negocio: 10, 300, 200 créditos; 2 estilistas gratis.
- Valores de relleno para secretos para permitir arranque sin credenciales reales.
- Stripe URL configurable con `STRIPE_URL_API`, por defecto `https://api.stripe.com`.
- IA URL configurable mediante propiedad `sillon.ia.openai.url` y variable `SILLON_IA_OPENAI_URL`.

### 7.2 Variables de entorno esperadas

`PORT`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRETO`, `URL_PUBLICA`, `STRIPE_CLAVE_SECRETA`, `STRIPE_SECRETO_WEBHOOK`, `STRIPE_PRECIO_PRO`, `STRIPE_PRECIO_CREDITOS`, `STRIPE_URL_API`, `GEMINI_LLAVE`, `SILLON_IA_OPENAI_URL`.

- `secrets.example.properties` con valores ficticios se versiona.
- `secrets.properties` con valores reales **no** se versiona y debe estar en `.gitignore`.
- `.cursorignore` debe excluir secretos, llaves y archivos `.env`.
- No exponer secretos en prompts ni registros de errores.

## 8. Validación y pruebas

### 8.1 Pruebas unitarias mínimas (JUnit 5 / Mockito)

- **Estados:** 5 transiciones válidas, al menos 6 inválidas (parametrizadas).
- **Agenda:** horario válido, apertura/cierre, domingo cerrado, cruces, día cerrado y horas pasadas.
- **Crear/cambiar cita:** precio congelado, pasado sin guardar, cruce sin guardar, servicio no ofrecido, cita ajena 404, cancelación por estilista 403, transición ilegal sin persistencia.
- **Planes:** tercer estilista GRATIS → 402, PRO permite tercero.
- **Stripe webhook:** alta PRO, créditos, idempotencia, cancelación y firma inválida.
- **Chatbot:** sin créditos no invoca IA, pregunta normal, escalamiento y bloqueo anti-promesas.
- **Recordatorio:** respuesta válida con saldo, JSON sin `mensaje` → 502.
- **IA:** fallback, clasificación de errores, lectura de prompts y parser JSON.
- **Contexto:** test de arranque Spring con MySQL configurado.

Los tests de servicios usan mocks, sin acceder a proveedores reales. Los escenarios de fechas deben usar un `Clock` fijo. No modificar tests solo para que pasen.

### 8.2 Colección Postman y evidencia

La colección `postman/sillon.postman_collection.json` y entornos `local` / `produccion` deben usar únicamente `baseUrl` por entorno. Carpetas **en este orden**:

1. `1. Registro, login y configuración`
2. `2. Catálogo y equipo`
3. `3. Agenda: reservas, cruces y estados`
4. `4. Aislamiento entre salones y roles`
5. `5. IA: recordatorio y chatbot`
6. `6. Pagos con Stripe (modo prueba)`
7. `7. Después de pagar (manual)`

Reglas para ejecución repetible: sufijo único por corrida para nombres/correos; fechas futuras calculadas dinámicamente, incluyendo próximo domingo; guardar tokens e IDs automáticamente; Runner ejecuta las carpetas 1–6 sin intervención manual.

La tabla de escenarios de las páginas 31–32 del PDF oficial fija **52 escenarios de referencia** para la colección, incluidos los pasos manuales posteriores al pago. El README oficial además exige **50 o más peticiones y al menos 40 con pruebas**, **BITACORA-IA.md con 5 o más entradas reales**, y la guía pide pruebas JUnit. No sustituir esta evidencia por capturas de pantalla.

### 8.3 Criterios de aceptación críticos

- Registro válido → **201** con token, rol `DUENO`, `GRATIS` y 10 créditos; correo repetido → **409**.
- Cita pública de corte de 45 minutos a las 10:00 → `PENDIENTE`/`EN_LINEA`, fin a las 10:45 y precio congelado.
- Tras esa cita, los horarios de 9:30, 10:00 y 10:30 para el mismo estilista y servicio quedan no disponibles; 11:00 sí puede estar libre.
- Cita superpuesta → **409**; tercera alta de estilista gratis → **402**.
- Acceso cruzado de salones → **404**; servicios y agendas de otro salón no aparecen.
- Recordatorio de cita válida consume **un** crédito y contiene datos verificados; cita completada → **409** sin consumir.
- Chatbot no agenda, escala solicitudes, bloquea promociones indebidas desde Java y no inventa precios.
- Webhook falso → **400**; repetido → **200**, `procesado=false`; compra real de prueba aplica beneficio una sola vez.
- Swagger accesible en `/swagger-ui.html` y OpenAPI en `/v3/api-docs`; endpoints protegidos documentados con Bearer JWT.
- Docker y Render deben poder ejecutar la API con PostgreSQL Supabase; README con URL completa `https://...onrender.com`.

## 9. Plan de ejecución (fases oficiales)

Cada fase termina con un commit después de verificar sus comprobaciones; **no saltar fases**:

| Fase | Resultado esperado |
|---|---|
| 0 | Fork y proyecto inicial; configuración, `SPEC.md`, `AGENTS.md`, `BITACORA-IA.md` |
| 1 | Formato de errores, `Clock`, `Salon`, configuración embebida, slug y `Usuario` |
| 2 | JWT, roles, registro/login y consulta/configuración del salón |
| 3 | Servicios, estilistas, clientes y límite GRATIS |
| 4 | Máquina de estados, agenda, cruces y disponibilidad |
| 5 | Catálogo y reservas públicas |
| 6 | Stripe: suscripción PRO, créditos y webhook idempotente |
| 7 | Proveedores IA, fallback y créditos atómicos |
| 8 | Recordatorio con datos verificables |
| 9 | Chatbot, escalamiento y anti-promesas |
| 10 | Tests JUnit 5 / Mockito |
| 11 | Swagger / OpenAPI |
| 12 | Docker + Supabase + Render + webhook de producción |
| 13 | Colección Postman, README y entrega con evidencia ejecutable |

**Regla del Pull Request:** después del primer push de Fase 0, abrir **un solo PR** hacia el repositorio oficial, usando el nombre completo del estudiante como título. Cada push posterior actualiza ese mismo PR y dispara el calificador. No modificar `.github/`.

## 10. Archivos entregables

- `pom.xml` y código Java en raíz/`src` del fork.
- `SPEC.md`, `AGENTS.md`, `BITACORA-IA.md`.
- `secrets.example.properties` (versionado); `secrets.properties` (no versionado).
- `src/main/resources/prompts/recordatorio.txt`.
- `src/main/resources/prompts/chatbot.txt`.
- `src/test/java` con pruebas JUnit/Mockito.
- `Dockerfile`, `.dockerignore`.
- Carpeta `postman/` con colección y dos entornos.
- `README.md` con instrucciones y URL pública real de Render.
- Webhook Stripe en modo prueba apuntando al servicio de Render.
- Historial incremental de commits y **un único Pull Request** contra el repositorio original.

## 11. Decisiones abiertas y límites de esta SPEC

Esta SPEC conserva **los requisitos explícitos** del PDF. El documento no define de manera completa todos los esquemas internos, por ejemplo: el JSON detallado de los servicios y estilistas dentro del catálogo público, las propiedades de las entidades listadas en el endpoint global de administración, los textos literales de error y de respuesta segura, y las claves internas exactas de metadata del Checkout. Antes de implementar estos detalles, contrastarlos con el contrato y las pruebas oficiales sin alterar campos expresamente definidos.

**Esta especificación no reemplaza las comprobaciones paso a paso, los escenarios de Postman ni el README de entrega originales.** Ambos documentos deben seguir disponibles como referencia durante el desarrollo.
