# SPEC — Sillón

> Especificación de negocio y contrato del backend **Sillón**. Es la fuente de verdad para el agente, junto con `AGENTS.md`.
>
> **Fuente:** derivada de `Contexto_trabajo_final/PROYECTO-SILLON.md` (documento maestro). Donde el maestro dice algo, aquí está; donde el maestro **no** dice nada, está marcado `[SUPUESTO]` o `[PROPUESTO]` y aparece listado en la sección 7 para aprobación.
>
> **Convención:** `[SUPUESTO]` = asunción operativa razonable, pendiente de confirmar. `[PROPUESTO]` = decisión de diseño abierta, pendiente de aprobación. Todo lo demás es regla fija.

---

## 1. El problema y la solución

**Problema:** las barberías y salones agendan por WhatsApp, cuaderno o memoria. Se cruzan citas del mismo estilista, los clientes olvidan sus citas (silla vacía) y nadie responde precios/horarios fuera de horario.

**Solución:** Sillón es un **SaaS multi-inquilino** (Java + Spring Boot, API REST, sin interfaz gráfica). Cada salón es un inquilino: registra su horario, servicios y equipo; sus clientes reservan en línea solo horarios libres; el sistema impide cruces y citas fuera de horario; el dueño genera recordatorios con IA listos para WhatsApp y un chatbot responde precios/horarios escalando a una persona lo que no puede resolver.

**Aislamiento:** un dato pertenece a un salón. Ver o tocar un dato de otro salón responde **404**, como si no existiera (no se revela que existe).

---

## 2. Roles y permisos

| Acción | Público | DUENO | ESTILISTA | ADMIN |
|---|---|---|---|---|
| Registrarse, iniciar sesión | Sí | Sí | Sí | Sí |
| Ver catálogo, disponibilidad, reservar, chatear | Sí | Sí | Sí | Sí |
| Ver y configurar su salón | No | Sí | No | No |
| Crear y editar servicios | No | Sí | No | No |
| Ver servicios del salón | No | Sí | Sí | No |
| Crear y ver estilistas, ver clientes | No | Sí | No | No |
| Crear citas, pedir recordatorios con IA | No | Sí | No | No |
| Ver la agenda | No | Todas las del salón | Solo las suyas | No |
| Cambiar el estado de una cita | No | Cualquier transición válida | Solo las suyas, a COMPLETADA o NO_ASISTIO | No |
| Pagar el plan PRO o créditos, ver solicitudes | No | Sí | No | No |
| Ver todos los salones | No | No | No | Sí |

Rutas públicas (sin token): `/api/auth/**`, `/api/publico/**`, `POST /api/pagos/webhook`, `GET /api/pagos/resultado`, Swagger y `/error`.

El primer ADMIN se crea registrando una cuenta normal y cambiando el rol a `ADMIN` directamente en la base de datos.

---

## 3. Planes, precios y créditos

| Plan / producto | Pago | Qué da |
|---|---|---|
| GRATIS (al registrarse) | Ninguno | Máximo **2 estilistas**, **10 créditos** de IA de cortesía, horario por defecto (8:00–19:00, sin domingos, intervalo 30 min) |
| PRO (Stripe, suscripción mensual, 15 USD) | Recurrente | Estilistas sin límite, **+300 créditos** al activarse |
| Paquete de créditos (Stripe, pago único, 5 USD) | Único | **+200 créditos** |

- **1 crédito = 1 respuesta de la IA** (recordatorio o chatbot).
- El **precio del backend decide**: el cliente de la API solo pide "plan PRO" o "paquete de créditos"; nunca envía precio ni id de precio.
- Solo modo prueba (Stripe no permite cobros reales a negocios en Colombia).

---

## 4. Reglas de negocio

### 4.1 Salones y cuentas

1. **Aislamiento:** todo dato pertenece a un salón. Usuario de otro salón intentando ver/modificar → **404**.
2. **Registro:** crea salón + dueño en la misma transacción, plan GRATIS, 10 créditos y configuración por defecto (8:00–19:00, sin domingos, intervalo 30). Respuesta **201** con token.
3. **Correo único en toda la plataforma** (ni un estilista puede reusar un correo existente) → **409**. El correo se guarda en minúsculas y sin espacios.
4. **Slug:** generado desde el nombre del salón, en minúsculas, sin tildes ni espacios (letras y números unidos con guiones, sin guiones en los extremos; "Barbería La Navaja" → `barberia-la-navaja`). Si ya existe, se le agrega `-2`, `-3`, …
5. **Horario:** apertura anterior a cierre (si no: **409**); intervalo de agenda entre 15 y 60 minutos (si no: **400**). Hora del salón: "hoy"/"ahora" se calculan con la zona **America/Bogota** vía bean `Clock`, aunque el servidor esté en UTC.

### 4.2 Catálogo y equipo

1. Un servicio dura entre **15 y 480 minutos** y cuesta un entero de pesos **> 0** (si no: **400**).
2. Un estilista ofrece uno o más servicios **de su mismo salón**; servicio de otro salón → **404**.
3. Límite del plan: en GRATIS, el **tercer estilista → 402**. En PRO no hay límite.
4. Orden de validaciones al crear estilista: **límite del plan (402) → servicioIds del salón (404) → correo único (409)**. El límite va primero para verificarlo antes de hacer búsquedas masivas. *(Orden definido por el dueño del proyecto el 2026-11-08; reemplaza el orden original 402 → 409 → 404.)*

### 4.3 Agenda

1. La cita dura exactamente lo que dura su servicio: el sistema calcula `fin`.
2. La cita **copia el precio** del servicio al agendar; cambios posteriores del servicio no la afectan.
3. No se agenda: en el pasado, fuera del horario, un domingo si el salón no abre, ni con un estilista que no ofrece el servicio → **409**.
4. **Sin cruces:** un estilista no puede tener dos citas que se crucen. Solo ocupan agenda las citas **PENDIENTE** y **CONFIRMADA** → **409**. Dos intervalos se cruzan si `inicioA < finB && finA > inicioB`; una cita que termina a las 10:45 **no** choca con otra que empieza a las 10:45.
5. Origen/estado inicial: cita del salón → **CONFIRMADA / SALON**; reserva en línea → **PENDIENTE / EN_LINEA**.
6. Al agendar, el cliente se busca por teléfono **dentro del salón**; si no existe, se crea. El teléfono se guarda solo con dígitos y `+`.
7. **Transiciones permitidas:**
   - PENDIENTE → CONFIRMADA | CANCELADA
   - CONFIRMADA → COMPLETADA | CANCELADA | NO_ASISTIO
   - COMPLETADA, CANCELADA, NO_ASISTIO = **finales**
   - Cualquier otra → **409**
8. Un estilista solo cambia **sus** citas (de otro estilista → **404**) y solo a **COMPLETADA** o **NO_ASISTIO** (otro destino → **403**).
9. **Disponibilidad** (la calcula Java, nunca la IA): horas de inicio, cada intervalo desde la apertura, en las que el servicio cabe completo antes del cierre, sin cruzar citas ocupadas del estilista y nunca en el pasado. Si hay cita 10:00–10:45 y el servicio dura 45 min, **tampoco se ofrece 9:30** (terminaría a las 10:15 encima).
10. Zona horaria del negocio: `Clock` con `America/Bogota` (render corre en UTC).

### 4.4 Pagos (Stripe, modo prueba)

1. El backend decide el precio (ver sección 3). El cliente solo pide el tipo de compra.
2. Si el salón ya es PRO, pedir la suscripción otra vez → **409** (antes de llamar a Stripe).
3. El webhook **verifica la firma** (`Stripe-Signature` sobre el cuerpo en texto plano). Sin firma o firma inválida → **400**.
4. **Idempotencia:** cada evento de Stripe se procesa una sola vez; si su id ya está en `eventos_stripe`, se ignora y se responde `procesado: false`.
5. Efectos:
   - `checkout.session.completed` + estado `paid` → aplica la compra según la metadata: PRO (salón a PRO, guarda `stripeSuscripcionId`, **+300 créditos**) o PAQUETE (**+200 créditos**).
   - `customer.subscription.deleted` → el salón vuelve a **GRATIS** (conserva créditos).
6. Todo el manejo del webhook ocurre en **una sola transacción**: si algo falla, no queda ni el cambio ni el evento y Stripe reintenta.
7. El evento se guarda **después** de aplicar el cambio, en la misma transacción; el id del evento es la clave de idempotencia.

### 4.5 Inteligencia artificial

1. **Crédito por respuesta:** verificar créditos **antes** de llamar a la IA; sin créditos → **402** y la IA no se llama. El crédito se consume **después** de que la IA responde.
2. **Recordatorio:** solo para citas PENDIENTE o CONFIRMADA (otra: **409**). Java calcula los datos verificados (salón, cliente, servicio, estilista, fecha en español de Colombia, hora, valor, dirección); la IA **solo redacta** el mensaje. Sillón no lo envía: una persona lo aprueba. Máximo **700 caracteres** (si no: **502**). Prompt en `src/main/resources/prompts/recordatorio.txt` con variables salón/cliente/datos y salida **solo JSON con la clave `mensaje`**.
3. **Chatbot:** responde con el **catálogo real** del salón (de la BD, no escrito a mano). No agenda, no cambia ni cancela citas: en esos casos **escala** y crea una `SolicitudContacto` con los mensajes del cliente (máximo **2000 caracteres**, motivo `PEDIDO_POR_IA`). Prompt en `prompts/chatbot.txt` con variables salón/dirección/horario/catálogo/cliente y salida **solo JSON con las claves `respuesta` (texto) y `escalar` (booleano)**. Historial completo en cada petición (1 a 20 turnos); el texto del cliente va como mensaje, **nunca** dentro de las instrucciones.
4. **Anti-promesas en Java:** si la respuesta contiene `descuento`, `cupón`, `cupon`, `gratis`, `promoción`, `promocion` o `reembols` (sin importar mayúsculas), el cliente recibe un **mensaje seguro fijo** y se crea la solicitud con motivo `REGLA_DE_SEGURIDAD`. El falso positivo se acepta a propósito.
5. **Salida JSON siempre:** si no es JSON válido o le faltan datos → **502**. Si ningún proveedor responde → **503**.
6. **Secretos:** llaves solo en `secrets.properties` (local) o variables de entorno (Render). Nunca en código, Git ni prompts.
7. **Consumo atómico:** el descuento de créditos es una sola actualización que resta 1 **solo si quedan créditos** (dos peticiones simultáneas nunca dejan el saldo en negativo).
8. **Orden de cada funcionalidad con IA:** (1) validar lo que no depende de la IA → (2) verificar créditos → (3) cargar prompt y llamar a la IA → (4) consumir el crédito → (5) leer y validar el JSON → (6) aplicar reglas de negocio y responder.
9. **Ninguna llamada a la IA dentro de una transacción de BD** (la IA tarda y ocuparía la conexión).
10. **Límites:** recordatorio ≤ 700 caracteres; solicitudes ≤ 2000; historial de chat completo por petición; prefijo de configuración `sillon.ia` con `sillon.ia.openai.url` **configurable** (el calificador la cambia por su IA simulada).

### 4.6 Fuera de alcance

- Envío real de mensajes por WhatsApp o correo.
- Recordatorios automáticos programados y reportes.
- Pagos del cliente final al salón (Stripe Connect).
- Interfaz gráfica.
- Paginación, refresh token, recuperación de contraseña, verificación de correo, notificaciones push.

---

## 5. Contrato de la API

**22 rutas.** Se respeta **al pie de la letra**: rutas, métodos, nombres de campos y códigos. La colección oficial del calificador falla si un campo se llama distinto.

### 5.1 Convenciones

- Fechas: `AAAA-MM-DD`. Fecha y hora: `AAAA-MM-DDTHH:mm:ss` (ej. `2026-10-20T10:30:00`). Hora: `HH:mm` al enviar; Jackson la devuelve como `HH:mm:ss`.
- Precios: enteros en pesos colombianos, sin puntos ni decimales (`35000`).
- Autenticación: cabecera `Authorization: Bearer <token>` en todas las rutas no públicas.
- Salvo el webhook (texto plano), el cuerpo es JSON. `[SUPUESTO S-12]`
- Las respuestas incluyen **exactamente** los campos del contrato. `[SUPUESTO S-09]`
- No existen rutas adicionales a las 22 (p. ej. `GET/DELETE /api/servicios/{id}`). `[SUPUESTO S-10]`

### 5.2 Formato único de error

Todos los errores (incluidos 401 y 403) responden:

| Campo | Contenido |
|---|---|
| `codigo` | Número HTTP (ej. 409) |
| `error` | Nombre estándar (ej. Conflict) |
| `mensaje` | En español. En validación (400): campos fallidos con su mensaje, separados por `;` |
| `fecha` | Fecha y hora del error |

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos, JSON mal formado, parámetro faltante/inválido, firma de webhook inválida |
| 401 | Sin token, token inválido, credenciales incorrectas |
| 402 | Límite del plan GRATIS o salón sin créditos de IA |
| 403 | Rol sin permiso, o estilista con transición que no le corresponde |
| 404 | No existe, o pertenece a otro salón |
| 409 | Cruce, horario, transición, correo repetido, plan ya activo, cita en estado final |
| 502 | Stripe o la IA respondieron algo no usable |
| 503 | Ningún proveedor de IA disponible |

### 5.3 Rutas

**Autenticación · `/api/auth` (público)**

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST `/api/auth/registro` | salonNombre (máx. 100), ciudad (máx. 60), direccion (máx. 150), telefono (máx. 30), nombre (máx. 100), email (formato válido), password (8 a 72) | 201 con token, tipo ("Bearer"), expiraEnMinutos, usuarioId, nombre, rol, salonId, salonSlug · 400 · 409 |
| POST `/api/auth/login` | email, password | 200 con el mismo objeto del registro · 401 |

**Mi salón · `/api/salon` (DUENO)**

| Método y ruta | Recibe | Responde |
|---|---|---|
| GET `/api/salon` | — | 200 con id, nombre, slug, ciudad, direccion, telefono, plan, creditosIa, configuracion (horaApertura, horaCierre, abreDomingos, intervaloMinutos), fechaCreacion |
| PUT `/api/salon/configuracion` | horaApertura, horaCierre, abreDomingos, intervaloMinutos (15 a 60), todos obligatorios | 200 con el salón actualizado · 400 · 409 si abre después de cerrar |

Siempre con el `salonId` del usuario autenticado, nunca un id de la URL o el cuerpo.

**Servicios · `/api/servicios`**

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST `/api/servicios` (DUENO) | nombre (máx. 80), descripcion (opcional, máx. 300), duracionMinutos (15 a 480), precio (> 0), activo (opcional; default true) | 201 con id, nombre, descripcion, duracionMinutos, precio, activo · 400 |
| GET `/api/servicios` (DUENO y ESTILISTA) | — | 200 con la lista de servicios del salón |
| PUT `/api/servicios/{id}` (DUENO) | Lo mismo que el POST | 200 · 400 · 404 |

**Estilistas y clientes (DUENO)**

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST `/api/estilistas` | nombre, email, password (8 a 72), servicioIds (lista con al menos un id) | 201 con id, nombre, email, activo, servicios (lista de nombres) · 400 · 402 · 404 · 409 |
| GET `/api/estilistas` | — | 200 con la lista de estilistas del salón |
| GET `/api/clientes` | — | 200 con id, nombre y telefono de cada cliente (solo lectura; nacen al agendar) |

**Agenda · `/api/citas`**

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST `/api/citas` (DUENO) | clienteNombre, clienteTelefono (7 a 30), servicioId, estilistaId, inicio, nota (opcional, máx. 300) | 201 con la cita · 400 · 404 · 409 |
| GET `/api/citas?fecha=` (DUENO y ESTILISTA) | fecha opcional; sin fecha → hoy | 200 con las citas del día, ordenadas por hora. El estilista solo ve las suyas |
| PATCH `/api/citas/{id}/estado` (DUENO y ESTILISTA) | estado | 200 con la cita · 400 · 403 · 404 · 409 |
| POST `/api/citas/{id}/recordatorio` (DUENO) | — | 200 con citaId, telefonoCliente, mensaje, creditosRestantes, factura · 402 · 404 · 409 · 502 · 503 |

La cita se devuelve con: id, inicio, fin, estado, origen, precio, servicioId, servicioNombre, estilistaId, estilistaNombre, clienteNombre, clienteTelefono, nota, fechaCreacion, fechaActualizacion.

La factura de la IA: proveedor, modelo, tokensEntrada, tokensSalida, costoEstimadoUsd, milisegundos.

**Público · `/api/publico/salones/{slug}` (sin token)**

| Método y ruta | Recibe | Responde |
|---|---|---|
| GET `/api/publico/salones/{slug}` | — | 200 con nombre, slug, ciudad, direccion, telefono, horario (texto), servicios (solo activos) y estilistas (id, nombre, servicioIds). **Nunca correos** · 404 |
| GET `…/disponibilidad?servicioId=&estilistaId=&fecha=` | Los tres obligatorios | 200 con fecha, servicio, estilista, duracionMinutos, horariosLibres · 400 · 404 · 409 `[SUPUESTO S-14]` |
| POST `…/reservas` | Lo mismo que POST `/api/citas` | 201 con la cita PENDIENTE y origen EN_LINEA · 400 · 404 · 409 |
| POST `…/chat` | clienteNombre, clienteContacto (máx. 120), historial: 1 a 20 turnos, cada uno con rol (CLIENTE o ASISTENTE) y texto (máx. 500) | 200 con respuesta, escalado (true/false), solicitudId (o null), factura · 400 · 402 · 404 · 502 · 503 |

Texto de horario (ejemplo): `"Lunes a sábado (domingos cerrado), de 8:00 a. m. a 7:00 p. m."`.

**Solicitudes · `/api/solicitudes` (DUENO)**

| Método y ruta | Recibe | Responde |
|---|---|---|
| GET `/api/solicitudes` | — | 200 con id, clienteNombre, clienteContacto, mensajes, motivo, estado, fechaCreacion; más recientes primero |
| PATCH `/api/solicitudes/{id}/atendida` | — | 200 · 404 · 409 si ya estaba atendida |

**Pagos · `/api/pagos`**

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST `/api/pagos/suscripcion` (DUENO) | — | 200 con tipo (SUSCRIPCION_PRO), sesionId, urlPago · 409 si ya es PRO · 502 |
| POST `/api/pagos/creditos` (DUENO) | — | 200 con tipo (PAQUETE_CREDITOS), sesionId, urlPago · 502 |
| POST `/api/pagos/webhook` (Stripe) | Evento + cabecera Stripe-Signature | 200 con recibido y procesado · 400 si la firma no es válida |
| GET `/api/pagos/resultado?estado=` (público) | exito o cancelado | 200 con un mensaje `[SUPUESTO S-06]` |

**Administración · `/api/admin` (ADMIN)**

| Método y ruta | Recibe | Responde |
|---|---|---|
| GET `/api/admin/salones` | — | 200 con todos los salones · 403 para cualquier otro rol |

---

## 6. Decisiones de implementación heredadas del maestro

- Stack: Java 17, Spring Boot 4.1.x, Maven, JPA, MySQL local / PostgreSQL (Supabase) prod, JWT + BCrypt, springdoc 3.x, Stripe modo prueba, Gemini por `RestClient` con fallback, JUnit 5 + Mockito, Docker + Render.
- **Prohibido:** Lombok, Spring AI, SDKs de IA, `application.yml`, perfiles, llaves en código/Git.
- Paquete base `com.devsenior.sillon` con dominios: `compartido`, `seguridad`, `auth`, `salon`, `usuario`, `servicio`, `cliente`, `cita`, `publico`, `pago`, `chat`, `ia`. Dentro de cada uno: `model/repository/service/controller/dto/mapper`.
- Inyección por constructor; enums con `@Enumerated(EnumType.STRING)`; DTOs records; mapper propio `@Component`; entidades jamás expuestas en la API.
- Errores: excepción propia por código + `@RestControllerAdvice` en `compartido`.
- Reloj: bean `Clock` (`America/Bogota`); nunca `LocalDate.now()` directo.
- Prompts en `src/main/resources/prompts/*.txt` (`recordatorio.txt`, `chatbot.txt`).
- Claves de configuración: prefijo `sillon.ia`, `sillon.ia.openai.url` configurable, `STRIPE_URL_API` configurable (por defecto `https://api.stripe.com`).
- Proyecto en la **raíz del fork**; no modificar `.github/`.

---

## 7. Supuestos y propuestas

> Todo lo marcado `[SUPUESTO]`/`[PROPUESTO]` en este SPEC, que **no** está explícito en `PROYECTO-SILLON.md`.
>
> **Estado de revisión (2026-11-06): los 14 ítems fueron aprobados por el usuario.** Los marcadores se conservan para trazabilidad (señalan que la regla no viene del PDF/maestro, aunque ya esté autorizada).
>
> **Condición de S-07:** los nombres concretos de clases de servicio se **revisan al inicio de cada fase, antes de implementar**, y se aprueban en el momento.

| ID | Sección | Qué | Tipo | Detalle / alternativa | Estado |
|---|---|---|---|---|---|
| S-01 | §5.1 | Duración del token JWT | SUPUESTO | El contrato solo dice `expiraEnMinutos`; se propone **60 minutos** | ✅ Aprobado |
| S-02 | §5.1 | No hay refresh token: se pide uno nuevo con login | SUPUESTO | El maestro no menciona refresh token | ✅ Aprobado |
| S-03 | §5.2 | Redacción exacta de los mensajes de error en español | SUPUESTO | Libre y estable, salvo el 400, que debe nombrar los campos fallidos | ✅ Aprobado |
| S-04 | §4.1 / §5.3 | Mensaje genérico del 401 en login | SUPUESTO | Texto fijo tipo "Credenciales inválidas", sin distinguir correo vs contraseña | ✅ Aprobado |
| S-05 | §4.5 regla 4 | Texto fijo del mensaje seguro anti-promesas | PROPUESTO | Redactarlo (p. ej. "Un asistente del equipo te contactará pronto.") y aprobar | ✅ Aprobado |
| S-06 | §5.3 | Cuerpo de `GET /api/pagos/resultado` | SUPUESTO | 200 con un mensaje simple de éxito o cancelación en español | ✅ Aprobado |
| S-07 | §6 | Nombres concretos de clases de servicio | PROPUESTO | El maestro solo da "por ejemplo" (ReglasAgenda, CatalogoService, UsuarioPrincipal); el resto (servicio de créditos, recordatorio, chatbot, pagos) se nombrará al construir cada fase — **revisión por fase antes de implementar** | ✅ Aprobado (con condición) |
| S-08 | §6 | Versiones exactas de `jjwt`, `springdoc`, `stripe-java` | SUPUESTO | Última versión estable en Maven Central al crear el proyecto (Fase 0) — ya en el pom: springdoc 3.2.0, stripe-java 34.0.0, jjwt 0.13.0 | ✅ Aprobado |
| S-09 | §5.1 | Las respuestas traen **exactamente** los campos del contrato | SUPUESTO | Lectura de "respétalo al pie de la letra"; no agregar campos extra "por si acaso" | ✅ Aprobado |
| S-10 | §5.3 | El contrato de 22 rutas es exhaustivo | SUPUESTO | No crear rutas adicionales (ni `GET/PUT/DELETE /api/servicios/{id}` más allá de las listadas) | ✅ Aprobado |
| S-11 | §4.5 regla 4 | Ampliar la lista de palabras anti-promesas | PROPUESTO | La lista del maestro tiene 7 palabras; la prueba 43 exige bloquear también `SILLA50`/`50%` → agregar `silla50` y `50%` a la lista | ✅ Aprobado |
| S-12 | §5.1 | Todas las rutas usan JSON salvo el webhook (texto plano) | SUPUESTO | Convención estándar; el maestro solo lo dice explícitamente del webhook (la firma de Stripe se calcula sobre los bytes originales) | ✅ Aprobado |
| S-13 | §4.5 reglas 1 y 8 | Crédito consumido en un 502 (JSON inválido) | PROPUESTO | El orden del maestro consume el crédito (paso 4) **antes** de validar el JSON (paso 5): en 502 el crédito ya se gastó; en 503 no. Confirmado por el usuario junto con P-12 | ✅ Aprobado |
| S-14 | §5.3 | Cuándo responde **409** `GET …/disponibilidad` | SUPUESTO | Día fuera del horario del salón (p. ej. domingo cerrado) → 409; si el día abre pero no hay cupo → 200 con lista vacía | ✅ Aprobado |
