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
- **Un único método privado de agendar** (cita del salón y reserva en línea), con este orden: servicio existe y activo → estilista existe, activo y ofrece el servicio → calcular `fin` → no pasado → horario del salón → sin cruce → buscar/crear cliente, copiar precio y guardar.
- Cita del salón: `CONFIRMADA`/`SALON`. Reserva en línea: `PENDIENTE`/`EN_LINEA`.
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
- Orden obligatorio: **validar lo no-IA → verificar créditos (402) → cargar prompt y llamar → consumir crédito → leer y validar JSON → reglas de negocio → responder**.
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