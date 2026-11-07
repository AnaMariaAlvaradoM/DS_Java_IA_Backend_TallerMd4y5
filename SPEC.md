# SPEC Sillón — Fase 0-8

## Problema
Las barberías y salones administran información de negocios, sucursales, empleados y servicios de forma dispersa. Antes de gestionar reservas, Sillón necesita una base de datos coherente y una API estable sobre la que construir las siguientes fases.

## Objetivo de la fase 1
Crear la base técnica del backend y definir el modelo inicial del negocio: barberías, sucursales, empleados, clientes y servicios. La aplicación debe arrancar, conectarse a la base de datos y exponer una API documentada.
## 1. Propósito y alcance

Muchas barberías y salones agendan por WhatsApp, cuaderno o de memoria: hay citas cruzadas, clientes que olvidan su cita (silla vacía) y nadie responde precios u horarios fuera del horario laboral.

**Sillón** es un SaaS multi-inquilino (cada salón es un inquilino) que ofrece:

- Reservas en línea que solo muestran horarios realmente libres.
- Agenda que impide cruces y citas fuera del horario.
- Recordatorios redactados con IA: borrador que una persona aprueba (Sillón no envía mensajes).
- Chatbot que responde con el catálogo real del salón y escala a una persona lo que no puede resolver.
- Cobro del plan PRO y de paquetes de créditos de IA con Stripe (modo prueba).

Es solo backend: API REST documentada con Swagger, probada con Postman y desplegada en Render. El cliente de la API es Postman.

**Fuera de alcance:** envío real de WhatsApp o correo · recordatorios automáticos programados y reportes/indicadores · pagos del cliente final al salón (Stripe Connect) · interfaz gráfica · cobros reales de Stripe.

---

## 2. Planes y créditos

| Producto | Pago en Stripe | Beneficio |
|---|---|---|
| Plan `GRATIS` | Ninguno (al registrarse) | Máx. 2 estilistas y 10 créditos de IA |
| Plan `PRO` | Suscripción mensual (15 USD) | Estilistas sin límite y +300 créditos al activarse |
| Paquete de créditos | Pago único (5 USD) | +200 créditos |

Un crédito = una respuesta de la IA (cada respuesta cuesta tokens a la plataforma; los créditos evitan pérdidas por uso ilimitado).

---

## 3. Roles y permisos

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
| Cambiar estado de una cita | No | Cualquier transición válida | Solo las suyas, a `COMPLETADA` o `NO_ASISTIO` | No |
| Pagar PRO o créditos, ver solicitudes | No | Sí | No | No |
| Ver todos los salones | No | No | No | Sí |

El primer `ADMIN` se crea registrando una cuenta normal y cambiando su rol a `ADMIN` directamente en la base de datos.

---

## 4. Modelo de dominio

### 4.1 Entidades

| Entidad | Tabla | Atributos | Relaciones |
|---|---|---|---|
| `Salon` | `salones` | `id`, `nombre`, `slug` (único), `ciudad`, `direccion`, `telefono`, `plan`, `creditosIa`, `stripeSuscripcionId`, `configuracion`, `fechaCreacion` | Tiene usuarios, servicios, clientes, citas y solicitudes |
| `ConfiguracionSalon` | embebida en `salones` | `horaApertura`, `horaCierre`, `abreDomingos`, `intervaloMinutos` | `@Embeddable` dentro de `Salon` |
| `Usuario` | `usuarios` | `id`, `nombre`, `email` (único), `password` (BCrypt), `rol`, `activo` | N–1 `Salon`; N–N `Servicio` vía `estilista_servicios` |
| `Servicio` | `servicios` | `id`, `nombre`, `descripcion`, `duracionMinutos`, `precio`, `activo` | N–1 `Salon` |
| `Cliente` | `clientes` | `id`, `nombre`, `telefono` | N–1 `Salon`; teléfono único por salón |
| `Cita` | `citas` | `id`, `inicio`, `fin`, `precio`, `estado`, `origen`, `nota`, `fechaCreacion`, `fechaActualizacion` | N–1 `Salon`, `Cliente`, `Usuario` (estilista), `Servicio` |
| `SolicitudContacto` | `solicitudes_contacto` | `id`, `clienteNombre`, `clienteContacto`, `mensajes`, `motivo`, `estado`, `fechaCreacion` | N–1 `Salon` |
| `EventoStripe` | `eventos_stripe` | `id` (id del evento de Stripe), `tipo`, `fechaProcesado` | Ninguna (idempotencia) |

Solo `Cita` tiene `fechaActualizacion`. `Salon` solo tiene `fechaCreacion`.

### 4.2 Enumeraciones (`EnumType.STRING`, valores sin tildes)

| Enum | Valores |
|---|---|
| `Plan` | `GRATIS`, `PRO` |
| `Rol` | `DUENO`, `ESTILISTA`, `ADMIN` |
| `EstadoCita` | `PENDIENTE`, `CONFIRMADA`, `COMPLETADA`, `CANCELADA`, `NO_ASISTIO` |
| `OrigenCita` | `SALON`, `EN_LINEA` |
| `TipoCompra` | `SUSCRIPCION_PRO`, `PAQUETE_CREDITOS` |
| `MotivoSolicitud` | `PEDIDO_POR_IA`, `REGLA_DE_SEGURIDAD` |
| `EstadoSolicitud` | `PENDIENTE`, `ATENDIDA` |

### 4.3 Restricciones

- `salones.slug` único; `usuarios.email` único en toda la plataforma (incluye estilistas).
- `clientes`: único por (`salon_id`, `telefono`).
- Contraseñas con BCrypt; nunca se devuelven en respuestas.
- `eventos_stripe.id` = id del evento de Stripe.
- Nombres de entidades, tablas, campos y enums exactamente como aquí. No se crean entidades ni tablas adicionales sin decisión documentada.

### 4.4 Máquina de estados de `Cita`

| Desde | Hacia | Caso de negocio |
|---|---|---|
| `PENDIENTE` | `CONFIRMADA` | El salón confirma una reserva en línea |
| `PENDIENTE` | `CANCELADA` | Se anula la reserva antes de confirmarla |
| `CONFIRMADA` | `COMPLETADA` | El servicio se prestó |
| `CONFIRMADA` | `CANCELADA` | Se anula una cita ya confirmada |
| `CONFIRMADA` | `NO_ASISTIO` | El cliente nunca llegó |

`COMPLETADA`, `CANCELADA` y `NO_ASISTIO` son finales. Cualquier otra transición → 409. Ocupan agenda solo `PENDIENTE` y `CONFIRMADA`.

---

## 5. Reglas de negocio (resumen transversal)

El detalle de cada regla vive en la fase donde se implementa. Aquí, el índice:

| # | Regla | Fase |
|---|---|---|
| R1 | Aislamiento por salón: dato ajeno → 404 | 1–5 |
| R2 | Registro crea salón + dueño: `GRATIS`, 10 créditos, 08:00–19:00, sin domingos, 30 min | 2 |
| R3 | Correo único en toda la plataforma → 409; minúsculas y sin espacios | 2–3 |
| R4 | Slug desde el nombre; duplicado → sufijo `-2`, `-3`… | 1 |
| R5 | Horario: apertura < cierre (409); intervalo 15–60 (400) | 2 |
| R6 | Servicio: 15–480 min; precio entero > 0 (400) | 3 |
| R7 | Estilista: servicios del mismo salón (404); límite `GRATIS` = 2 (402) | 3 |
| R8 | Cita: duración = la del servicio; precio copiado al agendar | 4 |
| R9 | Sin pasado, sin fuera de horario, sin domingo cerrado, sin estilista que no ofrece el servicio (409) | 4 |
| R10 | Sin cruces entre citas que ocupan agenda (409) | 4 |
| R11 | Origen/estado: `SALON`→`CONFIRMADA`; `EN_LINEA`→`PENDIENTE` | 4–5 |
| R12 | Cliente buscado/creado por teléfono normalizado dentro del salón | 3–4 |
| R13 | Transiciones válidas (409); estilista solo suyas (404) y solo a `COMPLETADA`/`NO_ASISTIO` (403) | 4 |
| R14 | Disponibilidad calculada por Java, nunca por la IA | 4–5 |
| R15 | Hora del salón con `Clock` en `America/Bogota` | 1 |
| R16 | El backend decide precios de Stripe; salón PRO → 409 al repetir suscripción | 6 |
| R17 | Webhook: firma válida (400), idempotente, transaccional | 6 |
| R18 | PRO: +300 créditos; paquete: +200; cancelación → `GRATIS` | 6 |
| R19 | Créditos: verificar antes (402), consumir después, atómico | 7 |
| R20 | IA devuelve JSON validado: inválido 502, sin proveedor 503 | 7 |
| R21 | Recordatorio: solo `PENDIENTE`/`CONFIRMADA`; Java arma datos, IA redacta | 8 |
| R22 | Chatbot: catálogo real, no agenda, escala; regla anti-promesas en Java | 9 |
| R23 | Secretos fuera del código y de Git | 0 |

---

## 6. Contrato de la API

Se respeta al pie de la letra: el calificador prueba rutas, campos y códigos.

**Formatos:** fecha `AAAA-MM-DD`; fecha y hora `AAAA-MM-DDTHH:mm:ss`; horas `HH:mm` al enviar (Jackson las devuelve `HH:mm:ss`); precios enteros en COP (`35000`).

### 6.1 Formato único de error (incluye 401 y 403)

```json
{ "codigo": 409, "error": "Conflict", "mensaje": "Explicación en español", "fecha": "2026-11-06T10:30:00" }
```

En validación, `mensaje` lista los campos fallidos con su mensaje separados por `;`.

| Código | Uso |
|---|---|
| 400 | Datos inválidos, JSON mal formado, parámetro faltante o de tipo inválido, firma de webhook inválida |
| 401 | Sin token, token inválido o credenciales incorrectas (mensaje genérico en login) |
| 402 | Límite del plan GRATIS o salón sin créditos de IA |
| 403 | Rol sin permiso o transición no permitida a un estilista |
| 404 | No existe o pertenece a otro salón |
| 409 | Choca con regla de negocio: cruce, horario, transición, correo repetido, plan ya activo |
| 502 | Stripe o la IA respondieron algo inutilizable |
| 503 | Ningún proveedor de IA disponible |

### 6.2 Rutas

Rutas privadas: `Authorization: Bearer <token>`.

**Autenticación · `/api/auth` (público)**

| Ruta | Recibe | Responde |
|---|---|---|
| `POST /api/auth/registro` | `salonNombre` (≤100), `ciudad` (≤60), `direccion` (≤150), `telefono` (≤30), `nombre` (≤100), `email` (válido), `password` (8–72) | 201 con `token`, `tipo` ("Bearer"), `expiraEnMinutos`, `usuarioId`, `nombre`, `rol`, `salonId`, `salonSlug` · 400 · 409 |
| `POST /api/auth/login` | `email`, `password` | 200 (mismo objeto) · 401 |

**Mi salón · `/api/salon` (DUENO)**

| Ruta | Recibe | Responde |
|---|---|---|
| `GET /api/salon` | — | 200 con `id`, `nombre`, `slug`, `ciudad`, `direccion`, `telefono`, `plan`, `creditosIa`, `configuracion` (`horaApertura`, `horaCierre`, `abreDomingos`, `intervaloMinutos`), `fechaCreacion` |
| `PUT /api/salon/configuracion` | `horaApertura`, `horaCierre`, `abreDomingos`, `intervaloMinutos` (15–60), todos obligatorios | 200 con el salón · 400 · 409 si abre después de cerrar |

**Servicios · `/api/servicios`**

| Ruta | Recibe | Responde |
|---|---|---|
| `POST /api/servicios` (DUENO) | `nombre` (≤80), `descripcion` (opc., ≤300), `duracionMinutos` (15–480), `precio` (>0), `activo` (opc., por defecto `true`) | 201 con `id`, `nombre`, `descripcion`, `duracionMinutos`, `precio`, `activo` · 400 |
| `GET /api/servicios` (DUENO, ESTILISTA) | — | 200 lista del salón |
| `PUT /api/servicios/{id}` (DUENO) | Igual al POST | 200 · 400 · 404 |

**Estilistas y clientes (DUENO)**

| Ruta | Recibe | Responde |
|---|---|---|
| `POST /api/estilistas` | `nombre`, `email`, `password` (8–72), `servicioIds` (≥1) | 201 con `id`, `nombre`, `email`, `activo`, `servicios` (nombres) · 400 · 402 · 404 · 409 |
| `GET /api/estilistas` | — | 200 lista del salón |
| `GET /api/clientes` | — | 200 con `id`, `nombre`, `telefono` |

**Agenda · `/api/citas`**

| Ruta | Recibe | Responde |
|---|---|---|
| `POST /api/citas` (DUENO) | `clienteNombre`, `clienteTelefono` (7–30), `servicioId`, `estilistaId`, `inicio`, `nota` (opc., ≤300) | 201 con la cita · 400 · 404 · 409 |
| `GET /api/citas?fecha=` (DUENO, ESTILISTA) | `fecha` opcional (por defecto hoy) | 200 ordenadas por hora; el estilista solo ve las suyas |
| `PATCH /api/citas/{id}/estado` (DUENO, ESTILISTA) | `estado` | 200 con la cita · 400 · 403 · 404 · 409 |
| `POST /api/citas/{id}/recordatorio` (DUENO) | — | 200 con `citaId`, `telefonoCliente`, `mensaje`, `creditosRestantes`, `factura` · 402 · 404 · 409 · 502 · 503 |

Cita: `id`, `inicio`, `fin`, `estado`, `origen`, `precio`, `servicioId`, `servicioNombre`, `estilistaId`, `estilistaNombre`, `clienteNombre`, `clienteTelefono`, `nota`, `fechaCreacion`, `fechaActualizacion`.
Factura de IA: `proveedor`, `modelo`, `tokensEntrada`, `tokensSalida`, `costoEstimadoUsd`, `milisegundos`.

**Público · `/api/publico/salones/{slug}` (sin token)**

| Ruta | Recibe | Responde |
|---|---|---|
| `GET /api/publico/salones/{slug}` | — | 200 con `nombre`, `slug`, `ciudad`, `direccion`, `telefono`, `horario` (texto), `servicios` (solo activos), `estilistas` (`id`, `nombre`, `servicioIds`). Nunca correos · 404 |
| `GET …/disponibilidad?servicioId=&estilistaId=&fecha=` | Los tres obligatorios | 200 con `fecha`, `servicio`, `estilista`, `duracionMinutos`, `horariosLibres` · 400 · 404 · 409 |
| `POST …/reservas` | Igual a `POST /api/citas` | 201 con cita `PENDIENTE`/`EN_LINEA` · 400 · 404 · 409 |
| `POST …/chat` | `clienteNombre`, `clienteContacto` (≤120), `historial` (1–20 turnos: `rol` `CLIENTE`/`ASISTENTE`, `texto` ≤500) | 200 con `respuesta`, `escalado`, `solicitudId` (o null), `factura` · 400 · 402 · 404 · 502 · 503 |

**Solicitudes · `/api/solicitudes` (DUENO)**

| Ruta | Responde |
|---|---|
| `GET /api/solicitudes` | 200 con `id`, `clienteNombre`, `clienteContacto`, `mensajes`, `motivo`, `estado`, `fechaCreacion`; más recientes primero |
| `PATCH /api/solicitudes/{id}/atendida` | 200 · 404 · 409 si ya estaba atendida |

**Pagos · `/api/pagos`**

| Ruta | Responde |
|---|---|
| `POST /api/pagos/suscripcion` (DUENO) | 200 con `tipo` (`SUSCRIPCION_PRO`), `sesionId`, `urlPago` · 409 si ya es PRO · 502 |
| `POST /api/pagos/creditos` (DUENO) | 200 con `tipo` (`PAQUETE_CREDITOS`), `sesionId`, `urlPago` · 502 |
| `POST /api/pagos/webhook` (Stripe) | Recibe evento + `Stripe-Signature`. 200 con `recibido` y `procesado` · 400 firma inválida |
| `GET /api/pagos/resultado?estado=` (público) | `exito` o `cancelado` → 200 con mensaje |

**Administración · `/api/admin` (ADMIN)**

| Ruta | Responde |
|---|---|
| `GET /api/admin/salones` | 200 con todos los salones · 403 para otro rol |

---

## 7. Stack y configuración

- Java 17, Spring Boot 4.1.2, Maven. Verificar contra `pom.xml`.
- Spring Web, Data JPA, Validation, Security, HTTP Client (`RestClient`).
- MySQL (local, `sillon_db`) y PostgreSQL/Supabase (producción).
- JWT con `jjwt` (api, impl, jackson); BCrypt.
- springdoc-openapi 3.x; `stripe-java`.
- IA: Gemini (formato OpenAI) por HTTP; respaldo con formato Claude; fallback.
- JUnit 5 + Mockito; Docker + Render + Supabase.
- **Prohibido:** Lombok, Spring AI, SDKs de IA. Getters y setters explícitos. Jackson 3 (`tools.jackson.databind`; solo anotaciones en `com.fasterxml.jackson.annotation`).
- No se agregan dependencias sin justificación aprobada.

---

## 8. Plan de fases (vista general)

Cada fase deja la aplicación funcionando y termina con un commit. No se saltan fases.

| Fase | Qué se construye | Dominios (paquetes) |
|---|---|---|
| 0 | Terreno: fork, proyecto, configuración, secretos, documentos | — |
| 1 | Base SaaS: errores, `Clock`, salón, slug, usuario | `compartido`, `salon`, `usuario` |
| 2 | Seguridad: JWT, registro/login, roles, mi salón | `seguridad`, `auth`, `salon` |
| 3 | Catálogo y equipo: servicios, estilistas, clientes | `servicio`, `usuario`, `cliente` |
| 4 | Agenda: citas, reglas, estados, disponibilidad | `cita` |
| 5 | Cara pública: catálogo, disponibilidad, reservas | `publico` |
| 6 | Pagos con Stripe | `pago` |
| 7 | Motor de IA y créditos | `ia`, `salon` |
| 8 | Recordatorio con IA | `cita`, `ia` |
| 9 | Chatbot y solicitudes | `chat`, `publico` |
| 10 | Tests automáticos | `src/test` |
| 11 | Swagger | `compartido` |
| 12 | Despliegue Render + Supabase | raíz |
| 13 | Evidencia (Postman) y entrega | `postman/`, README |

**Definición de "terminada" para toda fase:** implementación revisada (diff leído y entendido) · comportamiento verificado (Postman o test, ejecutado de verdad) · pendientes anotados · documentación y `BITACORA-IA.md` actualizadas · commit descriptivo.

---

## 9. FASE 0 — Preparar el terreno

**Objetivo:** dejar el proyecto reproducible, configurado, sin secretos expuestos y con la documentación base lista.
**Tema:** contexto para el agente (SPEC, reglas del proyecto) y manejo de secretos.

### 0.1 Fork y repositorio

1. Hacer fork del repositorio oficial y clonar **el fork**; ahí se entrega todo.
2. El proyecto va en la **raíz** del fork: `pom.xml` junto al README. Si queda en una subcarpeta, el calificador no lo encuentra y la nota es 0.0.
3. No modificar la carpeta `.github` (vive el calificador). Modificarla marca el PR con ⚠️ Revisar.
4. Un commit por cada paso que quede en verde; el historial se revisa y un proyecto subido en un solo commit no demuestra proceso.

### 0.2 Crear el proyecto (Spring Initializr)

| Opción | Valor |
|---|---|
| Project / Language / Packaging | Maven · Java · Jar |
| Spring Boot | 4.1.1 (o la 4.1.2 estable vigente) |
| Group / Artifact / Package | `com.devSenior` · `sillon` · `com.devSenior.sillon` |
| Java | 17 |
| Dependencias | Spring Web, Spring Data JPA, MySQL Driver, PostgreSQL Driver, Validation, Spring Security, HTTP Client |

Copiar `pom.xml`, `src`, `mvnw`, `mvnw.cmd` y `.mvn` a la raíz del fork.

Agregar **a mano** en el `pom.xml` (no las ofrece Initializr): `springdoc-openapi-starter-webmvc-ui` (línea 3.x), `stripe-java` y las tres de `jjwt` (`api`, `impl`, `jackson`). Usar la versión estable más reciente de Maven Central y recargar el proyecto.

**Trampas conocidas:**

- Si Initializr escribe la versión del padre con sufijo `.RELEASE`, Maven no la encuentra: quitar el sufijo.
- Sin HTTP Client, no existe el bean `RestClient.Builder` y los proveedores de IA no arrancan.
- Spring Boot 4 trae Jackson 3: `ObjectMapper` en `tools.jackson.databind`.

### 0.3 Configuración y secretos

`application.properties` (nunca `.yml`, nunca perfiles) debe contener:

| Propiedad | Valor |
|---|---|
| Puerto | Variable `PORT`, `8080` por defecto (Render asigna el puerto) |
| Base local | MySQL, base `sillon_db`, creación automática de la base |
| JPA | `ddl-auto=update`, `open-in-view=false` |
| Secretos locales | Importación **opcional** de `secrets.properties` desde la raíz |
| Zona horaria del negocio | `America/Bogota` |
| URL pública de la API | Configurable (variable `URL_PUBLICA`) |
| Números del negocio | Créditos de registro `10`, créditos PRO `300`, créditos del paquete `200`, límite de estilistas `GRATIS` `2` |
| Secretos | Cada uno con variable de entorno y **valor de relleno** (clave de Stripe, secreto del webhook, id del precio PRO, id del precio del paquete, llave de Gemini, secreto JWT), para que la app siempre arranque |
| Dirección de Stripe | Variable `STRIPE_URL_API`, por defecto `https://api.stripe.com` |
| Dirección de IA (formato OpenAI) | Propiedad `sillon.ia.openai.url` (la de Gemini); se sobrescribe con la variable `SILLON_IA_OPENAI_URL` |

**Por qué dos direcciones configurables:** el calificador no usa el Stripe ni el Gemini reales; las reemplaza por simuladores (sin llaves ni costo). Nunca van fijas en el código.

Archivos de secretos:

- `secrets.example.properties`: **se sube**, con valores de ejemplo.
- `secrets.properties`: **nunca se sube**, valores reales; agregar al `.gitignore`.
- `.cursorignore`: bloquea secretos, llaves y archivos `.env`.

### 0.4 Documentos

| Archivo | Contenido |
|---|---|
| `SPEC.md` | Problema, roles, planes, reglas, contrato y fuera de alcance (este documento) |
| `AGENTS.md` | Reglas para el agente: paquete y dominios, filtrar por salón, fechas con el reloj del proyecto, prompts en archivos, Java calcula y la IA redacta, nunca modificar un test para que pase, no agregar dependencias sin preguntar |
| `BITACORA-IA.md` | Tabla de seis columnas (#, fase, herramienta y modo, para qué, qué revisé, qué rechacé/corregí) |

Uso permitido de IA: chat como **entrevistador** para descubrir casos de negocio no pensados. Las reglas ya fijadas no se delegan ni se cambian.

### 0.5 Criterios de aceptación

- [ ] `git status` no muestra `secrets.properties`.
- [ ] La aplicación arranca aunque `secrets.properties` no exista.
- [ ] El proyecto compila (`./mvnw clean compile`).
- [ ] `STRIPE_URL_API` y `sillon.ia.openai.url` existen y son configurables.
- [ ] Ninguna llave real en el repositorio.

**Commit:** "Proyecto base de Sillón, SPEC y reglas del agente". Tras este primer push, abrir **un solo** Pull Request hacia el repositorio oficial (título: nombre completo). Cada push posterior recalifica.