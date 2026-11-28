# AGENTS.md — Reglas para agentes de IA en Sillón

Instrucciones técnicas y convenciones para cualquier agente de IA (Cursor, Claude Code u otro) que trabaje en este repositorio.

> **Antes de cualquier cambio, lee `SPEC.md`.** Es la fuente de verdad del modelo, las reglas (R1–R24, SPEC §16) y el contrato de la API. Si algo no está en el SPEC o es ambiguo, **pregunta; no lo inventes.**
> Quien dirige el proyecto decide y responde por el resultado; el agente ejecuta lo ya decidido.

**Cómo usar este archivo:** la **Parte A** (secciones 1–12) son las reglas generales, válidas siempre. La **Parte B** (Fases 0 a 8) tiene un bloque independiente por fase: trabaja **solo** el bloque de la fase indicada y no adelantes fases.

### Mapa de referencias al SPEC

| Tema | Sección del SPEC |
|---|---|
| Stack, configuración y secretos | §2 |
| Planes, créditos y roles/permisos | §3 |
| Modelo de dominio y máquinas de estado | §4 |
| Formato de error, códigos y formatos | §5 |
| Mapa de fases | §6 |
| Detalle de cada fase | §7 (Fase 0) a §15 (Fase 8) |
| Catálogo de reglas R1–R24 | §16 |
| Estrategia de pruebas | §17 |
| Calificación y entrega | §18 |

---

# PARTE A — REGLAS GENERALES

## 1. Contexto del proyecto

**Sillón** es una API REST SaaS multi-inquilino para barberías y salones de belleza: salones, usuarios, estilistas, servicios, clientes, citas, reservas en línea, recordatorios y chatbot con IA, y cobros con Stripe (modo prueba).

- Solo backend. No hay interfaz gráfica.
- Java 17, Spring Boot 4.1.x (verifica la versión exacta en `pom.xml`), Maven. Paquete base: `com.devsenior.sillon` (todo en minúsculas).
- Cada salón es un inquilino; ningún usuario ve datos de otro salón.
- El proyecto vive en la **raíz** del repositorio (`pom.xml` junto al README).

## 2. Stack y prohibiciones

**Permitido** (verificar versiones contra `pom.xml`): Spring Web, Spring Data JPA, Hibernate, Validation, Spring Security, HTTP Client (`RestClient`), MySQL (local), PostgreSQL/Supabase (producción), jjwt, BCrypt, springdoc-openapi 3.x, `stripe-java`, JUnit 5, Mockito, Docker.

**Prohibido:**

- Lombok. Escribe getters, setters y constructores explícitos.
- Spring AI y cualquier SDK de proveedores de IA. La IA se consume por HTTP con `RestClient`.
- Agregar dependencias sin preguntar primero (con justificación técnica).
- Archivos `.yml` o perfiles de Spring: la configuración vive solo en `application.properties`.
- Flyway u otras herramientas de migración: el esquema lo gestiona `ddl-auto=update`.

**Jackson 3:** el `ObjectMapper` está en `tools.jackson.databind`. Solo las anotaciones (`@JsonProperty`…) siguen en `com.fasterxml.jackson.annotation`. Un import de `com.fasterxml.jackson.databind` indica código de la versión anterior.

## 3. Estructura del proyecto

Organización **por dominios**, y dentro de cada uno por capas.

```text
com.devsenior.sillon
├── compartido   formato de error, manejador global, Clock, config de Swagger
├── seguridad    JWT, filtro, reglas de acceso por rol
├── auth         registro del salón con su dueño, login
├── salon        salón (inquilino), plan, créditos, configuración
├── usuario      usuarios, roles, estilistas
├── servicio     catálogo de servicios
├── cliente      clientes del salón
├── cita         citas, estados, reglas de agenda, disponibilidad, recordatorio
├── publico      catálogo, disponibilidad, reservas y chat sin token
├── pago         sesiones de pago y webhook de Stripe
├── chat         chatbot y solicitudes de contacto
└── ia           proveedores, fallback, prompts, lector de JSON, factura
```

Capas por dominio: `entidad`, `repositorio`, `servicio`, `controlador`, `dto`.

- Respeta los paquetes que **existan** en el repositorio. No crees paquetes duplicados ni reorganices sin justificación.
- Controladores **delgados**: sin reglas de negocio. Las reglas viven en servicios y en clases puras como `ReglasAgenda`.
- **Nunca** expongas entidades JPA en respuestas: usa DTO (`record`) con mapper propio.
- Convierte la entidad a DTO **dentro** del método transaccional (evita `could not initialize proxy - no Session`).

## 4. Convenciones de nombres

- Nombres técnicos del dominio en **español**: entidades, tablas, atributos, columnas, enums, DTO, repositorios, servicios, controladores, excepciones y métodos de dominio. Tecnologías y contratos externos pueden ir en inglés.
- Java en `camelCase`; SQL en `snake_case` (`creditosIa` → `creditos_ia`).
- Enums con `EnumType.STRING` y **sin tildes** (`DUENO`, no `DUEÑO`).
- Entidades y tablas: `Salon/salones`, `Usuario/usuarios`, `Servicio/servicios`, `Cliente/clientes`, `Cita/citas`, `SolicitudContacto/solicitudes_contacto`, `EventoStripe/eventos_stripe`. Tabla intermedia: `estilista_servicios`.
- `ConfiguracionSalon` es `@Embeddable` dentro de `Salon`; **no** es entidad ni tiene tabla.
- Rutas, campos JSON y códigos HTTP: **exactamente** los del contrato (endpoints de cada fase en el SPEC; errores y formatos en SPEC §5). Un campo con otro nombre hace fallar al calificador.
- No renombres campos ni agregues entidades, campos o tablas sin una decisión documentada y aprobada en el SPEC.

## 5. Reglas críticas (no negociables)

### 5.1 Multi-inquilino (R1)

- Todo se filtra por el `salonId` del **usuario autenticado** (`@AuthenticationPrincipal` con `UsuarioPrincipal`).
- **Nunca** confíes en un `salonId` que llegue por URL, parámetro o cuerpo.
- Toda búsqueda por id se hace por **id y salón a la vez**. Dato de otro salón → **404** (como si no existiera).
- En cada consulta nueva, verifica que no olvidaste el filtro por salón.

### 5.2 Tiempo (R15)

- "Hoy" y "ahora" salen **siempre** del bean `Clock` (zona `America/Bogota`), inyectado por constructor.
- Prohibido `LocalDateTime.now()`, `LocalDate.now()` o `Instant.now()` sin reloj: Render corre en UTC y los tests dejarían de ser deterministas.

### 5.3 Errores

- Formato único para **todo**, incluidos 401 y 403: `{codigo, error, mensaje, fecha}`.
- Una excepción propia por código: 400 (firma inválida), 401, 402, 403, 404, 409, 502 y 503 (IA no disponible). Manejador global con `@RestControllerAdvice` (incluye validación, JSON mal formado, parámetro faltante y tipo inválido).
- En validación, `mensaje` lista los campos fallidos separados por `;`.
- `/error` debe ser **ruta pública** (si no, cualquier fallo se ve como un 401 engañoso).
- Login: 401 con mensaje **genérico** (no revelar si falló el correo, la contraseña o si la cuenta está inactiva).
- Nunca devuelvas contraseñas ni correos de otros usuarios en respuestas públicas.

### 5.4 Seguridad

- Sesión stateless, CSRF deshabilitado, JWT con correo como sujeto y rol y salón como claims.
- Autoridad = `ROLE_` + rol; las reglas usan `hasRole` **sin** el prefijo.
- Contraseñas con BCrypt. Correo normalizado (minúsculas, sin espacios).
- Permisos por rol: exactamente la tabla del SPEC §3.

### 5.5 Secretos

- Nunca en código, Git, logs ni prompts. Viven en `secrets.properties` (ignorado por Git) o en variables de entorno.
- Cada secreto en `application.properties` con variable de entorno y **valor de relleno**, para que la app siempre arranque.
- **No leas, imprimas ni modifiques** `secrets.properties`, `.env` ni archivos de llaves.
- Si una llave se filtra: revocarla en el proveedor y crear otra. Borrarla en otro commit **no basta** (Git guarda la historia).
- `STRIPE_URL_API` y `sillon.ia.openai.url` son configurables y **nunca van fijas** en el código: el calificador las reemplaza por simuladores.

## 6. Reglas por dominio (resumen transversal)

### 6.1 Agenda (R8–R14)

- Cruce: `inicioA < finB && finA > inicioB`. Una cita que termina a las 10:45 **no** se cruza con otra que empieza a las 10:45.
- Solo `PENDIENTE` y `CONFIRMADA` ocupan agenda.
- `EstadoCita` es una máquina de estados con `puedeCambiarA` y `ocupaAgenda`. Transiciones: `PENDIENTE→CONFIRMADA|CANCELADA`; `CONFIRMADA→COMPLETADA|CANCELADA|NO_ASISTIO`; el resto son finales.
- `ReglasAgenda` es un componente **sin dependencias de base de datos**.
- **Un único método privado de agendar** (cita del salón y reserva en línea), con este orden: servicio existe y activo → estilista existe, activo y ofrece el servicio → calcular `fin` → no pasado → anticipación máxima 90 días → horario del salón, domingo e intervalo → sin cruce → buscar/crear cliente, copiar precio y guardar.
- Cita del personal: `PENDIENTE`/`SALON`. Reserva en línea: `PENDIENTE`/`EN_LINEA`.
- Pasado, domingo cerrado y fuera de horario → 409 (R9). Inicio no alineado y más de 90 días → 400. Solapamiento y transición inválida → 409.
- Cambiar estado: buscar por id y salón (404) → si es estilista, cita suya (404) y destino `COMPLETADA`/`NO_ASISTIO` (403) → transición válida (409).
- Si devuelves la cita tras cambiar el estado, guarda con `saveAndFlush`.
- La disponibilidad la calcula **Java**, nunca la IA.
- Teléfono normalizado a dígitos y `+`; el cliente se busca por teléfono dentro del salón y, si no existe, se crea.

### 6.2 Catálogo y equipo (R6–R7)

- Orden al crear estilista: **(1) límite del plan (402) → (2) correo único (409) → (3) servicios del salón (404)**.
- Servicio: 15–480 min, precio entero > 0. Servicios inactivos no se ofrecen en nuevas reservas.

### 6.3 Pagos (R16–R18)

- El backend elige el precio; el cliente **nunca** envía importes ni ids de precio.
- `StripeClient` se construye con la clave y la base de `STRIPE_URL_API`.
- Salón ya `PRO` pidiendo suscripción → 409 **antes** de llamar a Stripe.
- Webhook: cuerpo como **texto plano**, verificar `Stripe-Signature` (400 si falta o falla), idempotencia por id de evento, y aplicar cambio + guardar evento en **una sola transacción**.
- PRO: +300 créditos y guarda la suscripción; paquete: +200; `customer.subscription.deleted` → `GRATIS` conservando créditos.
- Solo modo prueba. Si el objeto del evento viene vacío por diferencia de versión de API, usa la deserialización sin verificación de versión y **regístralo en la bitácora**.

### 6.4 Inteligencia artificial (R19–R22, R24)

- **Java calcula y verifica; la IA solo redacta.** Precios, horarios, disponibilidad y datos de la cita nunca los decide la IA.
- Prompts en `src/main/resources/prompts/` (`recordatorio.txt`, `chatbot.txt`); jamás incrustados en el código.
- Claves JSON exactas: `mensaje` (recordatorio); `respuesta` y `escalar` (chatbot). Valida siempre la salida: inválida → 502; sin proveedor → 503.
- Orden obligatorio: **validar lo no-IA → verificar créditos (402) → cargar prompt y llamar → leer y validar JSON → reglas de negocio → consumir crédito → responder**. Un 502 o un 503 no descuentan.
- **Nunca** una llamada a la IA dentro de una transacción de base de datos.
- El crédito se consume con **una sola actualización atómica** (resta 1 solo si quedan).
- El texto del cliente **nunca** va dentro de las instrucciones del prompt; el historial se envía como mensaje, una línea por turno (`Cliente:` / `Asistente:`).
- Recordatorio: solo citas `PENDIENTE`/`CONFIRMADA` (409 si no); `mensaje` ≤ 700 caracteres; "a. m." con espacio normal (reemplaza el espacio especial). Sillón **no envía** mensajes.
- Chatbot: responde con el catálogo real de la base de datos; no agenda, cambia ni cancela (escala). Al escalar: `SolicitudContacto` con `PEDIDO_POR_IA` (mensajes ≤ 2000).
- **Regla anti-promesas en Java:** si la respuesta contiene `descuento`, `cupón`/`cupon`, `gratis`, `promoción`/`promocion` o `reembols` (sin distinguir mayúsculas) → mensaje seguro fijo y solicitud `REGLA_DE_SEGURIDAD`. El falso positivo se acepta a propósito. Un prompt pide; solo el código garantiza.
- Revisa cada archivo traído de Atiende: paquetes, prefijos y nombres. Ninguna referencia a `com.devsenior.atiende`.

## 7. Pruebas

- Tests de servicios **sin levantar Spring**: mocks (Mockito) para repositorios y para la IA; reloj fijo.
- Ningún test llama a Gemini ni a Stripe.
- Casos mínimos: SPEC §17.
- **Nunca modifiques un test para que pase.** Un test rojo es una pregunta de negocio: determina si la causa está en el test o en el código y decide contra el SPEC.
- **No afirmes que algo pasó sin haberlo ejecutado.** Reporta el resultado real (comando y salida), incluidos los fallos.
- El test de contexto completo requiere MySQL encendido.

## 8. Comandos útiles

```bash
./mvnw clean compile          # compilar
./mvnw test                   # tests (Windows PowerShell: .\mvnw.cmd test)
./mvnw spring-boot:run        # ejecutar (puerto PORT, 8080 por defecto)
stripe listen --forward-to localhost:8080/api/pagos/webhook   # webhooks en local
```

Swagger local: `http://localhost:8080/swagger-ui.html`.

## 9. Flujo de trabajo del agente

1. **Lee** `SPEC.md` y este archivo. Trabaja **una fase a la vez** (bloque correspondiente de la Parte B; detalle en SPEC §7–§15); no adelantes fases.
2. **Modo Plan primero.** Presenta un plan antes de codificar y espera aprobación. El plan debe citar las reglas del SPEC que aplica.
3. **Cambios pequeños y revisables.** Un commit por paso que quede en verde; mensajes descriptivos.
4. **No toques** la carpeta `.github` (calificador automático) ni `.mvn` salvo necesidad justificada.
5. **No ejecutes** migraciones destructivas ni borres datos de producción sin autorización expresa.
6. **No agregues** dependencias, entidades, campos, rutas ni paquetes fuera del SPEC sin preguntar.
7. Si el SPEC es ambiguo o dos reglas chocan, **detente y pregunta**.
8. Al terminar, resume qué cambió, qué verificaste (y cómo) y qué queda pendiente.
9. Registra cada uso importante en `BITACORA-IA.md` (qué se revisó y qué se rechazó o corrigió).

### Plantilla general de petición

```text
Contexto: @SPEC.md @AGENTS.md
Fase: <número y nombre>
Tarea: <qué construir, citando reglas R# y secciones del SPEC>
Restricciones: sin Lombok, sin dependencias nuevas, filtrar por salón, Clock por constructor.
Entrega primero un PLAN (archivos a crear/modificar, orden, validaciones). No escribas código hasta que lo apruebe.
```

## 10. Qué NO se delega al agente

- Decidir reglas de negocio, estados, permisos y planes (ya están en el SPEC).
- Redactar los casos de prueba (salen de las reglas); el agente solo escribe su código.
- La lista y el orden de validaciones de cada operación.
- Revisar el diff y verificar el comportamiento (Postman/tests).
- Manejar claves y configuración de Stripe, Render o Supabase.
- Cambiar una regla para que una prueba pase.

## 11. Lista de revisión del diff (antes de aceptar un cambio)

- [ ] ¿Cada consulta filtra por salón? ¿Algún `salonId` viene de la URL o del cuerpo?
- [ ] ¿Algún `now()` sin `Clock`?
- [ ] ¿Alguna entidad JPA sale en una respuesta?
- [ ] ¿Hay reglas de negocio en un controlador?
- [ ] ¿Los nombres de rutas, campos y enums coinciden con el SPEC?
- [ ] ¿Los códigos HTTP son los del contrato (400/401/402/403/404/409/502/503)?
- [ ] ¿La IA se llama dentro de una transacción? ¿Se verifica el crédito antes y se consume después?
- [ ] ¿Hay secretos, URLs de Stripe/IA o prompts incrustados en el código?
- [ ] ¿Se agregó alguna dependencia, import de Lombok o de `com.fasterxml.jackson.databind`?
- [ ] ¿Se modificó algún test para que pase?
- [ ] ¿Se ejecutaron los tests y el resultado es real?
- [ ] ¿Se actualizó la documentación y `BITACORA-IA.md`?

## 12. Ante un fallo

1. Lee el mensaje completo y ubica la **primera línea con la causa**.
2. Identifica la regla de negocio involucrada (SPEC §16).
3. Escribe tu hipótesis **antes** de pedir una corrección.
4. Corrige el código, no la regla ni el test.
5. Verifica en local y luego en el calificador.

Causas frecuentes: `/error` no pública (401 engañoso); autoridad sin `ROLE_`; "ahora" sin `Clock` (falla solo en Render); `STRIPE_URL_API` o `sillon.ia.openai.url` fijas en el código (502/503 en el calificador); prompt sin las claves JSON exactas (502); proyecto fuera de la raíz del fork.

---

# PARTE B — INSTRUCCIONES POR FASE

Cada bloque es independiente. Cuando se te asigne una fase, lee **solo** su bloque (más la Parte A) y la sección correspondiente del SPEC. Cada fase termina con la aplicación funcionando, criterios verificados y un commit.

---

## FASE 0 — Preparación y documentación

**SPEC:** §7 · **Reglas:** R23

**Alcance**
- Hace: fork y estructura del repositorio, proyecto Maven, `pom.xml`, `application.properties`, archivos de secretos y de ignorado, documentos base.
- No hace: ningún código de negocio (sin entidades, endpoints ni seguridad).

**Pasos para el agente**
1. Confirmar que `pom.xml` está en la raíz y que el padre de Spring Boot **no** lleva sufijo `.RELEASE`.
2. Verificar dependencias: Web, Data JPA, MySQL, PostgreSQL, Validation, Security, HTTP Client. Agregar a mano `springdoc-openapi-starter-webmvc-ui` (3.x), `stripe-java` y las tres de `jjwt` (api, impl, jackson), con la última versión estable (consultar y pedir confirmación).
3. Crear `application.properties` con: `PORT` (8080 por defecto), MySQL `sillon_db` con creación automática, `ddl-auto=update`, `open-in-view=false`, importación **opcional** de `secrets.properties`, zona `America/Bogota`, URL pública, números del negocio (10, 300, 200, 2), secretos con variable y **valor de relleno**, `STRIPE_URL_API` y `sillon.ia.openai.url`.
4. Crear `secrets.example.properties` (valores de ejemplo), `.gitignore` con `secrets.properties`, `.cursorignore` (secretos, llaves, `.env`).
5. Crear la estructura de paquetes por dominios (§3 de este archivo) con paquetes vacíos solo si el repositorio los necesita.
6. Verificar que `SPEC.md`, `AGENTS.md`, `BITACORA-IA.md` y `README.md` existen en la raíz.

**Prohibido en esta fase**
- Crear `secrets.properties` con valores reales o leerlo.
- Escribir URLs de Stripe o de la IA fijas.
- Agregar dependencias fuera de la lista.

**Verificación**
- `./mvnw clean verify` en verde.
- La app arranca con MySQL y **también sin** `secrets.properties`.
- `git status` no muestra `secrets.properties`.

**Cierre:** `STRIPE_URL_API` y `sillon.ia.openai.url` configurables; ninguna llave en el repositorio.
**Commit:** "Proyecto base de Sillón, SPEC y reglas del agente".

**Prompt de arranque**
```text
Contexto: @SPEC.md @AGENTS.md
Fase 0 — Preparación (SPEC §7, bloque FASE 0 de AGENTS.md).
Entrega un PLAN para revisar pom.xml, application.properties, secrets.example.properties, .gitignore y .cursorignore.
No escribas lógica de negocio, no leas secrets.properties, no agregues dependencias sin mi confirmación.
```

---

## FASE 1 — Persistencia y modelo base

**SPEC:** §8 · **Reglas:** R1, R4, R15

**Alcance**
- Hace: formato de error y manejador global, `Clock`, `Salon` con `ConfiguracionSalon` embebida, generación de slug, `Usuario` y `Rol`.
- No hace: seguridad, JWT ni endpoints de negocio.

**Pasos para el agente**
1. `compartido`: objeto de error (`codigo`, `error`, `mensaje`, `fecha`); una excepción por código (400 firma, 401, 402, 403, 404, 409, 502, 503); `@RestControllerAdvice` que además maneja validación (campos y mensajes separados por `;`), JSON mal formado, parámetro faltante y tipo inválido.
2. Bean `Clock` con `America/Bogota`, recibido por constructor.
3. Enum `Plan`; entidad `Salon` con todos sus atributos; `ConfiguracionSalon` `@Embeddable` con método de configuración por defecto (08:00–19:00, sin domingos, 30 min).
4. Repositorio de salón: por `slug`, existencia de `slug`, por id de suscripción de Stripe.
5. Generador de slug: sin tildes → minúsculas → no alfanumérico a `-` → sin guiones en los extremos → `-2`, `-3`… si existe.
6. Enum `Rol` y entidad `Usuario` con N–1 hacia `Salon` (todavía sin servicios).

**Prohibido en esta fase**
- Spring Security, JWT, controladores de negocio.
- Tabla propia para `ConfiguracionSalon`.
- `now()` sin `Clock`; enums con tildes.

**Verificación**
- Hibernate crea `salones` (con la configuración embebida como columnas) y `usuarios`.
- Tests: slug (tildes, símbolos, repetido), `slug` único, `email` único.
- Un error forzado devuelve el formato de 4 campos.

**Cierre:** nombres de tablas, campos y enums idénticos al SPEC §4.
**Commit:** "Base SaaS: salones, usuarios y errores".

**Prompt de arranque**
```text
Contexto: @SPEC.md @AGENTS.md
Fase 1 — Persistencia y modelo base (SPEC §8; reglas R1, R4, R15).
Entrega primero un PLAN: excepciones y manejador, Clock, Salon + ConfiguracionSalon embebida, slug y Usuario.
Sin seguridad ni endpoints. Revisaré el plan contra el modelo de datos antes de que escribas código.
```

---

## FASE 2 — Usuarios, acceso y permisos

**SPEC:** §9 · **Reglas:** R1, R2, R3, R5, R23

**Alcance**
- Hace: `UsuarioPrincipal`, JWT y filtro, registro, login, reglas de acceso por rol, `GET /api/salon` y `PUT /api/salon/configuracion`.
- No hace: recuperar contraseña, OAuth, estilistas, servicios.

**Pasos para el agente**
1. `UsuarioPrincipal` (implementa `UserDetails`) con `id`, `salonId`, `email`, contraseña, `rol`, `activo`; autoridad `ROLE_` + rol.
2. Servicio JWT: secreto de configuración, sujeto = correo, claims = rol y salón, expiración 8 horas (`expiraEnMinutos` = 480).
3. Filtro que lee `Authorization: Bearer`, valida y autentica; token inválido → no autentica.
4. `POST /api/auth/registro` en **una sola transacción**: normalizar correo (409 si existe) → crear salón (slug, `GRATIS`, 10 créditos, configuración por defecto) → crear dueño `DUENO` con BCrypt → 201 con `token`, `tipo`, `expiraEnMinutos`, `usuarioId`, `nombre`, `rol`, `salonId`, `salonSlug`. Validar longitudes (SPEC §9).
5. `POST /api/auth/login`: 200 o 401 con mensaje **genérico**.
6. Reglas de acceso: públicas `/api/auth/**`, `/api/publico/**`, webhook y resultado de pago, Swagger y **`/error`**; por rol según SPEC §3; stateless; CSRF off; 401 y 403 con el formato único (entry point y access denied handler propios).
7. `GET /api/salon` y `PUT /api/salon/configuracion` (DUENO) usando siempre el `salonId` del principal. Validar intervalo 15–60 (400) y apertura < cierre (409).

**Prohibido en esta fase**
- Tomar el `salonId` de la URL o del cuerpo.
- Devolver `password` o un hash.
- Revelar si falló el correo o la contraseña.
- Dejar `/error` protegida.

**Verificación**
- Registro 201; correo repetido 409; datos inválidos 400 nombrando `email` y `password`.
- Login incorrecto 401; `GET /api/salon` sin token 401 **con formato único**.
- Plan `GRATIS`, 10 créditos; apertura > cierre 409; hash en base de datos empieza por `$2`.

**Cierre:** matriz de acceso por rol aplicada; ningún endpoint privado sin autenticación.
**Commit:** "Seguridad con JWT, roles y mi salón".

**Prompt de arranque**
```text
Contexto: @SPEC.md @AGENTS.md
Fase 2 — Usuarios, acceso y permisos (SPEC §9; reglas R1, R2, R3, R5).
Entrega un PLAN: UsuarioPrincipal, JWT + filtro, registro transaccional, login genérico, reglas de acceso (incluye /error pública) y mi salón.
El salonId sale siempre del usuario autenticado. Sin código hasta que apruebe el plan.
```

---


## FASE 3 — Salones, servicios y estilistas

**SPEC:** §10 · **Reglas:** R1, R3, R6, R7, R12

**Alcance**
- Hace: servicios (CRUD sin borrado), estilistas con relación N–N, clientes (solo lectura), límite del plan.
- No hace: citas, disponibilidad, IA.

**Pasos para el agente**
1. Servicios: entidad, DTOs `record`, mapper propio, servicio y controlador. Validar `nombre` ≤ 80, `descripcion` ≤ 300, `duracionMinutos` 15–480, `precio` entero > 0, `activo` por defecto `true` (400). Búsquedas por **id y salón** (404). No se borra: se desactiva.
2. `Usuario` ↔ `Servicio` N–N con `estilista_servicios` y método "¿ofrece este servicio?".
3. `POST /api/estilistas` con el orden **límite del plan (402) → correo único (409) → servicios del salón (404)**. Respuesta: `id`, `nombre`, `email`, `activo`, `servicios` (nombres). Nunca la contraseña.
4. `GET /api/estilistas` (DUENO).
5. Clientes: entidad con restricción única (`salon_id`, `telefono`); método **buscar o crear** con teléfono normalizado (dígitos y `+`); `GET /api/clientes` (DUENO) de solo lectura.
6. Permisos: `POST/PUT /api/servicios` solo DUENO; `GET /api/servicios` DUENO y ESTILISTA.

**Prohibido en esta fase**
- Crear clientes por endpoint (nacen al agendar).
- Cambiar el orden de validación de estilistas.
- Aceptar servicios de otro salón.

**Verificación**
- Corte clásico (45 min, 35000) y Arreglo de barba (30 min, 20000) → 201; 5 min o precio negativo → 400.
- Mateo (corte y barba) y Sara (solo corte) → 201; tercer estilista en `GRATIS` → 402; correo repetido 409; servicio ajeno 404.
- Tests del plan: 3.er estilista `GRATIS` 402; en `PRO` se crea.

**Cierre:** aislamiento por salón verificado en cada consulta nueva.
**Commit:** "Catálogo de servicios y equipo de estilistas".

**Prompt de arranque**
```text
Contexto: @SPEC.md @AGENTS.md
Fase 3 — Salones, servicios y estilistas (SPEC §10; reglas R1, R3, R6, R7, R12).
Entrega un PLAN con el orden de validación de estilistas (402 → 409 → 404) y cómo se filtra cada consulta por salón.
No construyas citas ni disponibilidad.
---

## FASE 4 — Clientes, agenda, disponibilidad y reservas

**SPEC:** §11 · **Reglas:** R1, R8–R15

**Alcance**
- Hace: `EstadoCita`, `Cita`, `ReglasAgenda`, agendar, listar, cambiar estado, catálogo público, disponibilidad y reserva en línea.
- No hace: recordatorio, chatbot ni pagos.

**Pasos para el agente**
1. `EstadoCita` (`puedeCambiarA`, `ocupaAgenda`) y `OrigenCita`. Tabla de transiciones antes del enum.
2. Entidad `Cita` con cuatro N–1, precio copiado del servicio, fechas automáticas.
3. `ReglasAgenda` **sin dependencias de base de datos**: horario, domingo, alineación al `intervaloMinutos`, pasado y anticipación máxima de 90 días responden 400. Calcula horarios libres con `Clock`, sin pasado y sin cruces; día cerrado → lista vacía. Cruce: `inicioA < finB && finA > inicioB` (409).
4. **Método privado único de agendar**: servicio activo (404) → estilista activo que lo ofrece (404/409) → `fin` → reglas de agenda (400) → sin cruce (409) → buscar/crear cliente, copiar precio y guardar. Personal: `PENDIENTE`/`SALON`; en línea: `PENDIENTE`/`EN_LINEA`.
5. `POST /api/citas` (DUENO), `GET /api/citas?desde=&hasta=&estado=&estilistaId=` (orden por inicio; el estilista solo las suyas), `GET /api/citas/{id}`, `PATCH /api/citas/{id}/estado` (id y salón 404 → estilista: cita suya 404 y destino `COMPLETADA`/`NO_ASISTIO` 403 → transición válida 409). Usar `saveAndFlush` al devolver.
6. Público: `GET /api/publico/salones/{slug}` (servicios y estilistas activos, `horario` en texto, **sin correos**), `…/disponibilidad` (`servicioId`, `estilistaId`, `fecha` obligatorios) y `POST …/reservas` (mismo método de agendar).

**Prohibido en esta fase**
- Calcular disponibilidad fuera de Java o con IA.
- Aceptar `fin` o `precio` del cliente.
- Dos métodos de agendar distintos.

**Verificación**
- Cita del personal → 201 `CONFIRMADA`/`SALON` (R11). Reserva en línea → `PENDIENTE`/`EN_LINEA`. Domingo, pasado y fuera de horario → 409. Desalineado y más de 90 días → 400.
- Reserva pública → 201 `PENDIENTE`/`EN_LINEA`, con `fin` y precio copiados del servicio.
- Confirmar `PENDIENTE` → `fechaActualizacion` posterior; `CONFIRMADA`→`PENDIENTE` 409.
- Mateo: solo sus citas, ajena 404, cancelar 403, completar 200.
- Tests de estados (5 permitidas y ≥ 6 bloqueadas) y de `ReglasAgenda`.

**Cierre:** toda consulta filtra por salón; el catálogo público no contiene `@`.
**Commits:** "Agenda: citas, reglas de horario, cruces y estados" y "Catálogo público, disponibilidad y reservas en línea".

**Prompt de arranque**
```text
Contexto: @SPEC.md @AGENTS.md
Fase 4 — Agenda, disponibilidad y reservas (SPEC §11; reglas R8–R15).
Entrega un PLAN con: tabla de transiciones, ReglasAgenda (sin BD), el método único de agendar en el orden del SPEC y las consultas derivadas.
Confirma que cada consulta filtra por salón y que el cruce usa solo PENDIENTE y CONFIRMADA.
```

---

## FASE 5 — Asistente IA, recordatorio y solicitudes

**SPEC:** §12 · **Reglas:** R14, R19–R22, R24

**Alcance**
- Hace: motor de IA (`ia`), créditos, recordatorio, chatbot, solicitudes de contacto.
- No hace: envío real de mensajes, guardar conversaciones, voz, pagos.

**Pasos para el agente**
1. Traer el paquete `ia` de Atiende a `com.devsenior.sillon.ia` (proveedor OpenAI/Gemini, respaldo formato Claude, fallback, configuración tipada, fábrica HTTP con tiempo máximo, biblioteca de prompts, lector de JSON, factura). Prefijo `sillon.ia`; URL de `sillon.ia.openai.url`. Eliminar todo import de `com.devsenior.atiende`.
2. Servicio de créditos: **verificar** (0 → 402) y **consumir** con una sola actualización atómica que resta 1 solo si quedan.
3. Orden de toda función con IA: validar no-IA → verificar créditos → cargar prompt y llamar → leer y validar JSON → reglas → consumir crédito → responder. Un 502 o un 503 no descuentan. **Sin transacción alrededor de la llamada.**
4. Recordatorio `POST /api/citas/{id}/recordatorio` (DUENO): datos verificados por Java (servicio y duración, estilista, fecha en español, hora `10:30 a. m.` con espacio normal, valor `$35.000`, dirección); solo `PENDIENTE`/`CONFIRMADA` (409 sin gastar créditos); prompt `recordatorio.txt` con variables `salón`, `cliente`, `datos` que pida JSON con la clave **`mensaje`**; validar existencia, no vacío y ≤ 700 caracteres (502). Responder `citaId`, `telefonoCliente`, `mensaje`, `creditosRestantes`, `factura`.
5. Chatbot `POST /api/publico/salones/{slug}/chat`: catálogo real desde la BD; prompt `chatbot.txt` con variables `salón`, `dirección`, `horario`, `catálogo`, `cliente` que pida JSON con **`respuesta`** y **`escalar`**; historial (1–20 turnos, ≤ 500) convertido en un solo mensaje por turnos; escalar → `SolicitudContacto` (≤ 2000) con `PEDIDO_POR_IA`; **anti-promesas en Java** → mensaje seguro y `REGLA_DE_SEGURIDAD`. Respuesta: `respuesta`, `escalado`, `solicitudId`, `factura`.
6. Solicitudes (DUENO): `GET /api/solicitudes` (recientes primero) y `PATCH /api/solicitudes/{id}/atendida` (409 si ya estaba).

**Prohibido en esta fase**
- Spring AI o SDKs de IA; URL del proveedor fija.
- Prompts incrustados en código; texto del cliente dentro de las instrucciones.
- Llamar a la IA dentro de una transacción.
- Dejar la regla anti-promesas solo en el prompt.
- Que la IA calcule disponibilidad, precios o fechas.
- Registrar mensajes del cliente o llaves en logs.

**Verificación**
- Recordatorio de Sara: menciona "Sara" y "10:30", trae factura y deja **9 créditos**; de cita `COMPLETADA` → 409 sin gastar créditos; JSON sin `mensaje` → 502.
- Con llave dañada: 503 y en el log un `ERROR` del primer proveedor y otro del respaldo.
- Chatbot: precio → no escala y consume 1; "quiero reservar" → escala con `solicitudId`; cupón SILLA50 → respuesta sin cupón, descuento, gratis ni SILLA50 y solicitud `REGLA_DE_SEGURIDAD`; historial vacío 400; salón inexistente 404; 0 créditos 402 sin llamar a la IA.
- Tests: fallback, lector de JSON, prompts, sin créditos, escalamiento, anti-promesas, recordatorio válido y 502.

**Cierre:** ninguna referencia a Atiende; la IA fuera de transacciones; claves JSON exactas.
**Commits:** "Motor de IA reutilizado y créditos por respuesta" · "Recordatorio con IA y datos verificados" · "Chatbot del salón con escalamiento y regla anti-promesas".

**Prompt de arranque**
```text
Contexto: @SPEC.md @AGENTS.md
Fase 5 — Asistente IA (SPEC §12; reglas R14, R19–R22, R24). Trabaja por subpasos: 5.1 motor, 5.2 créditos, 5.4 recordatorio, 5.5 chatbot, 5.6 solicitudes.
Entrega un PLAN del subpaso que te indique, con el orden: validar no-IA → créditos → llamar → consumir → validar JSON → reglas.
El prompt lo escribo yo; tú solo cargas el archivo. Sin llamadas a la IA dentro de transacciones.
```

---

## FASE 6 — Stripe, plan y créditos

**SPEC:** §13 · **Reglas:** R16, R17, R18, R23

**Alcance**
- Hace: sesiones de pago (suscripción y paquete), webhook idempotente, página de resultado.
- No hace: facturación, impuestos, reembolsos, cobros reales.

**Pasos para el agente**
1. `StripeClient` con la clave y la base de `STRIPE_URL_API`; configuración de Stripe tipada (clave, secreto del webhook, ids de precio PRO y paquete).
2. `POST /api/pagos/suscripcion` y `POST /api/pagos/creditos` (DUENO): el backend recibe el `TipoCompra` y **elige el precio** (suscripción para PRO, pago para el paquete). Sesión con correo del dueño, URLs de éxito y cancelación (`/api/pagos/resultado` sobre la URL pública), id del salón como referencia y metadata (salón y tipo; en suscripción, también en los datos de la suscripción). Salón ya `PRO` → 409 **antes** de llamar a Stripe. Error de Stripe → 502.
3. `POST /api/pagos/webhook`: cuerpo texto plano + `Stripe-Signature` (falta o inválida → 400) → si el id del evento ya existe, `procesado: false` → `checkout.session.completed` con pago `paid` (PRO: plan `PRO`, guarda suscripción, +300; paquete: +200) o `customer.subscription.deleted` (busca por id de suscripción y vuelve a `GRATIS`) → guardar `EventoStripe`. **Todo en una sola transacción.** Otro tipo: 200 y se ignora.
4. `GET /api/pagos/resultado?estado=exito|cancelado` (público).

**Prohibido en esta fase**
- Aceptar precios, importes o ids de precio del cliente.
- Dejar `STRIPE_URL_API` fija o ignorarla al construir el cliente.
- Procesar un evento dos veces o dejar el evento guardado si el cambio falló.
- Modo producción de Stripe; llaves en código o logs.

**Verificación (local con Stripe CLI)**
- `stripe listen --forward-to localhost:8080/api/pagos/webhook`; `whsec_` en `secrets.properties`.
- Pago con `4242 4242 4242 4242`: salón `PRO` +300 y tercer estilista permitido; paquete +200.
- Segunda suscripción 409; reenvío del mismo evento → `procesado: false` y créditos sin cambio.
- Webhook sin firma 400; estilista que paga 403; cancelación → `GRATIS` conservando créditos.
- Tests del webhook: PRO activa y suma, paquete suma, evento repetido, cancelación, firma falsa.

**Cierre:** idempotencia y transacción verificadas; si la deserialización del evento vino vacía, registrado en `BITACORA-IA.md`.
**Commit:** "Pagos con Stripe: plan PRO, paquete de créditos y webhook idempotente".

**Prompt de arranque**
```text
Contexto: @SPEC.md @AGENTS.md
Fase 6 — Stripe (SPEC §13; reglas R16–R18).
Entrega un PLAN: StripeClient con STRIPE_URL_API, selección de precio en backend, orden del webhook (firma → idempotencia → efecto → guardar evento) en una sola transacción.
No pidas ni uses llaves reales; usa los valores de relleno de application.properties.
```

---


## FASE 7 — Consultas y administración

**SPEC:** §14 · **Reglas:** R1 y la matriz de roles (SPEC §3)

**Alcance**
- Hace: `GET /api/admin/salones`, auditoría de roles y de aislamiento entre salones.
- No hace: reportes e indicadores (fuera de alcance de la guía; ver extensión opcional).

**Pasos para el agente**
1. `GET /api/admin/salones` (ADMIN): lista todos los salones; cualquier otro rol → 403. El primer ADMIN se crea cambiando el rol en la base de datos (no por endpoint).
2. Auditoría de roles: una prueba por rol para **cada** endpoint contra la tabla del SPEC §3.
3. Auditoría de aislamiento: con dos salones A y B, cada endpoint privado devuelve 404 o lista vacía al usuario del otro salón (B cambia cita de A → 404; B lista servicios y agenda → vacío; dueño A en administración → 403).
4. Corregir cualquier consulta que no filtre por salón (se reporta como hallazgo, no se oculta).

**Extensión opcional (no calificada; solo con aprobación expresa y después de la Fase 8 en verde)**
- Indicadores (citas por estado, ingresos de `COMPLETADA`, tasa de no asistencia, top servicios, solicitudes pendientes, créditos) sin entidades nuevas. **No implementar sin que se pida.**

**Prohibido en esta fase**
- Crear un endpoint para asignar `ADMIN`.
- Tablas o entidades nuevas.
- Exponer contraseñas, correos de otros salones o datos internos.

**Verificación**
- ADMIN 200; DUENO y ESTILISTA 403.
- Matriz de roles sin excepciones; aislamiento A/B en todos los endpoints.

**Cierre:** lista de hallazgos de la auditoría y su estado en `BITACORA-IA.md`.
**Commit:** "Administración y auditoría de roles y aislamiento".

**Prompt de arranque**
```text
Contexto: @SPEC.md @AGENTS.md
Fase 7 — Administración y auditoría (SPEC §14).
Implementa solo GET /api/admin/salones. Luego entrégame un INFORME (no código) con los endpoints que no cumplen la matriz de roles o que no filtran por salón.
No implementes indicadores ni reportes.
```


---

## FASE 8 — Calidad, Swagger, despliegue y evidencia

**SPEC:** §15 · **Reglas:** R23 y el catálogo completo

**Alcance**
- Hace: tests automáticos, Swagger, Dockerfile, preparación de Render/Supabase, colección Postman, README final.
- No hace: funcionalidades nuevas ni cambios de contrato.

**Pasos para el agente**

*A. Tests (SPEC §17)*
1. Tests de servicios con Mockito y reloj fijo, sin Spring; ninguno llama a Gemini ni a Stripe. Casos mínimos: estados, `ReglasAgenda`, agendar y cambiar estado, plan, webhook, chatbot, recordatorio, motor de IA y contexto completo (requiere MySQL).
2. Si un test falla: decidir contra el SPEC si el error está en el test o en el código. **No modificar el test para que pase.**

*B. Swagger*
3. `/swagger-ui.html` con título "Sillón API", esquema Bearer JWT (Authorize), controladores protegidos marcados y etiquetas numeradas: 0 Público, 1 Autenticación, 2 Mi salón, 3 Servicios, 4 Estilistas, 5 Clientes, 6 Agenda, 7 Pagos, 8 Solicitudes, 9 Administración.

*C. Despliegue*
4. `Dockerfile` de dos etapas (Maven + Java 17 sin tests → JRE 17) y `.dockerignore` (`target`, `.git`, `secrets.properties`).
5. Documentar (sin valores) las variables de Render: `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `JWT_SECRETO`, `URL_PUBLICA`, `STRIPE_CLAVE_SECRETA`, `STRIPE_PRECIO_PRO`, `STRIPE_PRECIO_CREDITOS`, `STRIPE_SECRETO_WEBHOOK`, `GEMINI_LLAVE`. **No** configurar `STRIPE_URL_API` ni `SILLON_IA_OPENAI_URL` en Render. Supabase por **Session Pooler** (puerto 5432).
6. Crear el destino de eventos de Stripe y los secretos de producción lo hace **la persona**, no el agente.

*D. Evidencia y entrega*
7. Colección `postman/sillon.postman_collection.json` y entornos `local` y `produccion` (solo `baseUrl`), con 7 carpetas en el orden del SPEC §15 y los 52 escenarios. Reglas: datos únicos por ejecución (sufijo con la hora), fechas calculadas por script, nada a mano entre peticiones en las carpetas 1–6. Verificar que cada prueba falla si se cambia el código esperado.
8. README final con la **URL de Render completa**, Swagger, stack, ejecución local, cómo verificar, tabla de endpoints y nombres de variables (nunca valores).

**Prohibido en esta fase**
- Modificar la carpeta `.github`.
- Subir llaves, `secrets.properties` o URLs de simuladores.
- Agregar funcionalidad o cambiar rutas/campos del contrato.
- Escribir fechas fijas en la colección.
- Afirmar que algo pasó sin haberlo ejecutado.

**Verificación**
- `./mvnw test` → `BUILD SUCCESS` con los casos mínimos presentes.
- Swagger abre local y en Render; Authorize funciona (`GET /api/salon` 200).
- Runner de Postman, carpetas 1–6, en verde en local y contra Render.
- Comentario del calificador en el PR revisado; cada fallo se lee, se ubica su regla (SPEC §16) y se corrige el código.
- `git log` muestra un commit por paso; ninguna llave en el repositorio ni en su historial.

**Cierre:** entregables del SPEC §18 completos y PR hacia el repositorio oficial.
**Commits:** "Tests de agenda, pagos, IA y chatbot" · "Documentación con Swagger" · "Dockerfile y despliegue en Render" · "Evidencia ejecutable y README".

**Prompt de arranque**
```text
Contexto: @SPEC.md @AGENTS.md
Fase 8 — Calidad y entrega (SPEC §15). Trabaja por bloques: 8.A tests, 8.B Swagger, 8.C Docker, 8.D Postman/README.
Para el bloque que te indique, entrega un PLAN. No cambies el contrato ni toques .github.
Los casos de prueba son los del SPEC §17; tú solo escribes el código. No modifiques un test para que pase.
```

# AGENTS.md — Reglas para Cursor

## FASES 9 A 12 · SILLÓN

Este documento complementa SPEC.md y establece las reglas obligatorias para implementar las Fases 9, 10, 11 y 12.

Las instrucciones de las fases anteriores siguen vigentes.

**Orden obligatorio de trabajo:**

1. Fase 8 — Calidad, seguridad y despliegue (según el alcance establecido).
2. Fase 9 — Chatbot del salón.
3. Fase 10 — Pruebas automáticas.
4. Fase 11 — Documentación Swagger.
5. Fase 12 — Docker, Supabase y Render.

No implementar varias fases simultáneamente.

---

# 1. REGLAS GENERALES

## 1.1. Antes de modificar código

Antes de iniciar cada fase:

1. Leer `SPEC.md` completo.
2. Leer `AGENTS.md` completo.
3. Leer `BITACORA-IA.md`.
4. Leer `README.md`.
5. Inspeccionar `pom.xml`.
6. Inspeccionar la estructura completa de `src`.
7. Revisar los controladores, servicios, entidades, DTO, repositorios y configuraciones existentes.
8. Revisar el historial Git.
9. Ejecutar las pruebas existentes cuando sea posible.
10. Identificar las funcionalidades que ya están implementadas.

No asumir que un componente no existe sin buscarlo.

## 1.2. Arquitectura obligatoria

Respetar:

- Java 17.
- Spring Boot 4.1.
- Maven.
- Spring Security.
- JWT.
- BCrypt.
- JPA/Hibernate.
- MySQL local.
- PostgreSQL/Supabase en producción.
- JUnit 5.
- Mockito.
- Springdoc OpenAPI.
- Docker.
- Render.

No introducir:

- Lombok.
- Spring AI.
- SDK adicionales de IA.
- Dependencias innecesarias.
- Nuevas tecnologías sin autorización.

Utilizar las clases y paquetes existentes.

## 1.3. Modelo oficial

No modificar los nombres del modelo oficial definido en SPEC.md.

Entidades existentes:

- Salon
- ConfiguracionSalon
- Usuario
- Servicio
- Cliente
- Cita
- SolicitudContacto
- EventoStripe

Respetar:

- `ConfiguracionSalon` como `@Embeddable`.
- La relación `estilista_servicios`.
- La unicidad del teléfono del cliente por salón.
- `EventoStripe` para idempotencia.
- Los enums existentes.
- Las relaciones y restricciones de base de datos.

No crear entidades o tablas nuevas sin autorización expresa.

## 1.4. Reglas de negocio

- El salón debe obtenerse del contexto autenticado cuando el endpoint sea privado.
- Los endpoints públicos deben resolver el salón mediante el `slug`.
- Nunca confiar en un `salonId` enviado por el cliente para autorizar acceso.
- No exponer datos de otro salón.
- Mantener los estados y transiciones definidos en SPEC.md.
- No eliminar validaciones existentes.
- No reducir la seguridad para hacer pasar una prueba.

## 1.5. Gestión de cambios

Antes de implementar:

1. Explicar qué archivos deben modificarse.
2. Explicar qué clases nuevas son necesarias.
3. Identificar riesgos de regresión.
4. Identificar pruebas que se deben ejecutar.

Después:

1. Revisar el diff.
2. Ejecutar las pruebas correspondientes.
3. Corregir problemas sin alterar las reglas de negocio.
4. Actualizar BITACORA-IA.md.
5. Presentar un resumen.
6. Esperar autorización antes de avanzar a la fase siguiente.

No hacer commits automáticamente sin autorización del usuario.

---

# 2. FASE 9 · CHATBOT DEL SALÓN

## 2.1. Inspección previa

Antes de implementar, revisar:

- Controlador del chatbot.
- Servicio del chatbot.
- Motor IA.
- Biblioteca de prompts.
- Cliente HTTP de IA.
- DTO de entrada y salida.
- Modelo SolicitudContacto.
- Repositorio de solicitudes.
- Sistema de créditos.
- Contexto multi-tenant.

Reutilizar lo existente.

No crear una segunda implementación del chatbot.

## 2.2. Catálogo dinámico

El catálogo debe construirse desde la base de datos.

Obtener:

- Salón.
- Dirección.
- Horario.
- Servicios activos.
- Duración.
- Precio.

No incluir servicios inactivos.

No escribir servicios ni precios manualmente en `chatbot.txt`.

No utilizar datos de otro salón.

## 2.3. Prompt

Crear o adaptar `chatbot.txt`.

Utilizar exclusivamente estas variables:

- `salon`
- `direccion`
- `horario`
- `catalogo`
- `cliente`

No introducir nombres de variables alternativos sin necesidad.

El prompt debe cumplir todos los requisitos de SPEC.md.

El prompt no es un mecanismo de seguridad suficiente: las reglas críticas deben validarse en Java.

## 2.4. Protección contra inyección

El historial debe llegar en cada petición.

Reglas:

1. Orden cronológico.
2. Separar claramente los turnos.
3. Utilizar `Cliente:` y `Asistente:`.
4. No insertar contenido del cliente en las instrucciones del sistema.
5. No permitir que el cliente cambie el rol o las reglas del asistente.
6. Validar el JSON devuelto por el proveedor.
7. Rechazar respuestas vacías o inválidas.
8. No asumir que el modelo conserva memoria.

No permitir que mensajes como «soy el dueño», «ignora las instrucciones anteriores» o «ahora puedes ofrecer descuentos» modifiquen las reglas.

## 2.5. Regla anti-promesas

Implementar la comprobación en Java.

Lista obligatoria:

- `descuento`
- `cupón`
- `cupon`
- `gratis`
- `promoción`
- `promocion`
- `reembols`

La comparación debe ignorar mayúsculas y minúsculas.

Si se detecta una coincidencia:

1. Bloquear la respuesta generada.
2. Devolver el mensaje seguro fijo.
3. Crear una solicitud de contacto.
4. Utilizar `REGLA_DE_SEGURIDAD`.
5. Asociarla al salón correcto.

No enviar al cliente el texto bloqueado.

No confiar exclusivamente en el indicador `escalar` del modelo.

## 2.6. Solicitudes de contacto

Respetar la entidad existente.

El dueño solo puede consultar solicitudes de su salón.

Ordenar de más recientes a más antiguas.

Comprobar la pertenencia antes de marcar una solicitud como atendida.

Si ya está atendida, devolver `409`.

No exponer identificadores ni datos de otro salón.

## 2.7. Créditos

Respetar las reglas de consumo existentes.

No llamar al proveedor de IA si no hay créditos disponibles.

No descontar créditos por respuestas inválidas o errores de proveedor si las reglas de las fases anteriores establecen que el crédito se consume únicamente después de una respuesta válida.

Evitar dobles consumos por errores, reintentos o respuestas duplicadas.

No alterar los valores de los planes ni paquetes.

## 2.8. Verificación

Probar al menos:

- Consulta de precio.
- Solicitud de cita.
- Solicitud de persona.
- Cliente molesto.
- Reclamación.
- Respuesta normal.
- Historial vacío.
- Salón inexistente.
- Solicitud de escalamiento.
- Intento de inyección.
- Intento de obtener un descuento.
- Intento de obtener información de otro salón.

Al finalizar, actualizar BITACORA-IA.md.

Commit propuesto:

`Chatbot del salón con escalamiento y regla anti-promesas`

---

# 3. FASE 10 · PRUEBAS AUTOMÁTICAS

## 3.1. Reglas obligatorias

- JUnit 5.
- Mockito.
- Pruebas parametrizadas.
- Reloj fijo.
- Servicios probados sin levantar Spring cuando sea posible.
- Repositorios simulados.
- Proveedores externos simulados.
- Sin llamadas reales a Gemini.
- Sin llamadas reales a Stripe.

No modificar pruebas existentes únicamente para obtener `BUILD SUCCESS`.

No eliminar pruebas que fallen.

## 3.2. Estructura

Organizar las pruebas de forma coherente con los paquetes del proyecto.

Evitar clases de prueba gigantes.

Utilizar nombres descriptivos.

Separar los escenarios positivos y negativos cuando mejore la claridad.

## 3.3. Cobertura mínima

Cubrir todos los casos de SPEC.md:

- Transiciones de estados.
- ReglasAgenda.
- Agendar y modificar citas.
- Permisos de estilistas.
- Límites de planes.
- Webhooks.
- Idempotencia.
- Créditos.
- Chatbot.
- Escalamiento.
- Anti-promesas.
- Recordatorios.
- Motor IA.
- Clasificación de errores.
- Lectura de JSON.
- Aislamiento multi-tenant.

## 3.4. Aislamiento multi-tenant

Probar que un usuario del salón A no pueda consultar ni modificar recursos del salón B.

Cubrir los endpoints privados relevantes de:

- Salón.
- Servicios.
- Estilistas.
- Clientes.
- Citas.
- Solicitudes.
- Pagos y operaciones asociadas cuando corresponda.

Verificar respuestas y ausencia de modificaciones en datos ajenos.

No limitar las pruebas al controlador: comprobar también las reglas del servicio cuando sea necesario.

## 3.5. Pruebas de fecha y hora

Utilizar `Clock` fijo cuando una operación dependa de la fecha actual.

No utilizar fechas arbitrarias que puedan quedar obsoletas.

No depender de la hora real del equipo.

## 3.6. Pruebas externas

Stripe y Gemini deben simularse.

No utilizar:

- API keys reales.
- Cuentas reales.
- Cobros reales.
- Servicios externos reales.

## 3.7. Verificación final

Ejecutar:

`./mvnw test`

En Windows también se puede utilizar:

`mvnw.cmd test`

Comprobar:

- Compilación correcta.
- Pruebas exitosas.
- Ausencia de errores.
- Ausencia de llamadas externas reales.

No afirmar que todas las pruebas pasan si no se ejecutaron.

Actualizar BITACORA-IA.md.

Commit propuesto:

`Tests de agenda, pagos, IA y chatbot`

---

# 4. FASE 11 · SWAGGER Y OPENAPI

## 4.1. Inspección

Antes de modificar dependencias:

- Revisar `pom.xml`.
- Buscar la configuración actual de OpenAPI.
- Revisar las anotaciones de los controladores.
- Identificar las rutas reales.
- Verificar si Swagger ya está instalado.

No duplicar dependencias.

## 4.2. Configuración

La documentación debe:

- Mostrar el título `Sillón API`.
- Estar disponible en `/swagger-ui.html`.
- Permitir autenticación Bearer JWT.
- Diferenciar operaciones públicas y privadas.
- Documentar parámetros y respuestas.
- Mostrar códigos de error relevantes.

## 4.3. Organización

Utilizar las etiquetas numeradas definidas en SPEC.md:

0. Público.
1. Autenticación.
2. Mi salón.
3. Servicios.
4. Estilistas.
5. Clientes.
6. Agenda.
7. Pagos.
8. Solicitudes.
9. Administración.

No inventar controladores ni rutas.

## 4.4. Seguridad

- No exponer credenciales.
- No incluir tokens reales en ejemplos.
- No documentar endpoints privados como públicos.
- No eliminar las reglas de Spring Security para facilitar el acceso a Swagger.
- Mantener Swagger accesible según la configuración de seguridad definida para el proyecto.

## 4.5. Verificación

Comprobar:

1. Swagger abre.
2. Se visualiza `Sillón API`.
3. Aparece `Authorize`.
4. Se acepta Bearer JWT.
5. Un token válido de dueño permite ejecutar `GET /api/salon`.
6. La respuesta es `200`.
7. Los endpoints públicos funcionan sin token.
8. Las operaciones están agrupadas correctamente.

Actualizar BITACORA-IA.md.

Commit propuesto:

`Documentación con Swagger`

---

# 5. FASE 12 · DOCKER, SUPABASE Y RENDER

## 5.1. Dockerfile

Crear un Dockerfile multietapa.

Etapa de compilación:

- Maven.
- Java 17.
- Construcción del JAR.
- Sin ejecutar pruebas durante la construcción.

Etapa de ejecución:

- JRE 17.
- Copiar el JAR.
- Configurar ejecución.
- Respetar `PORT`.

No introducir archivos secretos en ninguna etapa.

## 5.2. .dockerignore

Excluir como mínimo:

- `target`
- `.git`
- `secrets.properties`

No modificar `.github`.

No incluir archivos personales o de configuración local innecesarios.

## 5.3. Supabase

Utilizar PostgreSQL y Session Pooler en el puerto `5432`.

Configurar:

- URL JDBC.
- Usuario.
- Contraseña.

Mantener la compatibilidad con el entorno MySQL local.

No modificar entidades JPA para corregir errores de conexión.

No alterar nombres de tablas, columnas o relaciones.

## 5.4. Render

Preparar el servicio para ejecutar el JAR dentro del contenedor.

La aplicación debe respetar:

`server.port=${PORT:8080}`

Utilizar variables de entorno.

Variables requeridas:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRETO`
- `URL_PUBLICA`
- `STRIPE_CLAVE_SECRETA`
- `STRIPE_PRECIO_PRO`
- `STRIPE_PRECIO_CREDITOS`
- `STRIPE_SECRETO_WEBHOOK`
- `GEMINI_LLAVE`

No incluir valores reales en el repositorio.

No configurar en Render:

- `STRIPE_URL_API`
- `SILLON_IA_OPENAI_URL`

No inventar valores para ninguna variable.

## 5.5. Stripe

Documentar el procedimiento para:

1. Crear el endpoint de webhook en Stripe.
2. Configurar la URL de producción.
3. Seleccionar los eventos requeridos.
4. Obtener el secreto de firma.
5. Guardarlo en Render.

Eventos:

- `checkout.session.completed`
- `customer.subscription.deleted`

No realizar cambios en cuentas reales del usuario.

No solicitar ni almacenar secretos en el código.

No utilizar Stripe CLI como solución de producción.

## 5.6. README

Actualizar el README con instrucciones verificadas.

Debe incluir:

- Arquitectura.
- Tecnologías.
- Instalación.
- Configuración local.
- Variables de entorno.
- MySQL.
- Supabase.
- Docker.
- Render.
- Stripe.
- Pruebas.
- Swagger.
- URLs reales.

No inventar URLs.

No incluir secretos ni credenciales.

## 5.7. Verificación

Comprobar:

- Docker build.
- Inicio de aplicación.
- Java 17.
- Puerto dinámico.
- Conexión a Supabase.
- Swagger de producción.
- Registro.
- Login.
- JWT.
- Endpoint protegido.
- Webhook firmado.
- Activación de plan PRO mediante pago de prueba.

No afirmar que Render está desplegado si el usuario no ha realizado el despliegue o no existe evidencia verificable.

Actualizar BITACORA-IA.md.

Commit propuesto:

`Dockerfile y despliegue en Render`

---

# 6. GIT Y CONTROL DE VERSIONES

Antes de iniciar una fase:

`git status`

`git log --oneline -5`

Después de completar una fase:

`git diff --check`

`git diff --stat`

Revisar todos los archivos modificados.

No ejecutar:

- `git reset --hard`
- `git clean -fd`
- `git push --force`
- comandos destructivos equivalentes.

No eliminar modificaciones previas del usuario.

No sobrescribir archivos de configuración sin revisar su contenido.

No modificar `.github`.

No subir:

- `secrets.properties`
- `.env`
- API keys
- JWT secrets
- contraseñas
- tokens
- credenciales de Supabase
- credenciales de Stripe.

Antes de cualquier commit, presentar los cambios para revisión del usuario.

Los commits deben seguir el orden de las fases.

---

# 7. BITACORA-IA.md

Actualizar la bitácora al completar cada fase.

Registrar:

- Fecha.
- Fase.
- Objetivo.
- Archivos creados.
- Archivos modificados.
- Decisiones tomadas.
- Pruebas ejecutadas.
- Resultado real.
- Problemas encontrados.
- Soluciones aplicadas.
- Uso de IA.
- Código generado, corregido o rechazado.

Debe existir al menos una entrada de IA por fase.

Registrar también los errores importantes y las correcciones.

No inventar pruebas, resultados ni actividades.

No borrar entradas históricas.

---

# 8. PROTOCOLO OBLIGATORIO DE TRABAJO EN CURSOR

Para cada fase, seguir este procedimiento:

### PASO 1 — Inspección

Leer los documentos y analizar el código existente.

### PASO 2 — Diagnóstico

Explicar:

- Qué existe.
- Qué falta.
- Qué debe modificarse.
- Qué riesgos existen.
- Qué pruebas se necesitan.

### PASO 3 — Plan

Presentar una lista ordenada de implementación.

### PASO 4 — Autorización

Esperar la autorización del usuario antes de modificar código.

### PASO 5 — Implementación

Implementar únicamente la fase autorizada.

No adelantar funcionalidades de fases posteriores.

### PASO 6 — Verificación

Ejecutar las pruebas correspondientes y analizar los resultados.

### PASO 7 — Revisión

Inspeccionar el diff y comprobar que no se hayan alterado contratos o reglas anteriores.

### PASO 8 — Documentación

Actualizar BITACORA-IA.md y README.md cuando corresponda.

### PASO 9 — Entrega

Presentar:

- Archivos creados.
- Archivos modificados.
- Funcionalidades implementadas.
- Pruebas ejecutadas.
- Resultado real.
- Problemas pendientes.
- Commit recomendado.

Esperar autorización antes de continuar con la siguiente fase.

---

# 9. REGLA FINAL

**La prioridad absoluta es mantener la fidelidad a SPEC.md, el modelo oficial, las reglas de negocio y la seguridad multi-tenant.**

No se debe implementar una funcionalidad simplemente porque sea técnicamente conveniente.

No se deben inventar requisitos, tablas, rutas, respuestas ni decisiones comerciales.

Cuando exista una contradicción entre el código actual y SPEC.md:

1. Identificar la contradicción.
2. Explicar su impacto.
3. Proponer una solución.
4. Esperar autorización si la solución modifica el contrato o el modelo.

La implementación debe ser incremental, verificable y segura.

No avanzar automáticamente entre fases.