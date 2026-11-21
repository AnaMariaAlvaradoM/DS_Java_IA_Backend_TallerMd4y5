# 📋 Especificación Técnica Oficial · Proyecto Sillón

> **Sistema:** Sillón — SaaS Multi-inquilino de Agenda Inteligente para Barberías y Salones de Belleza  
> **Versión:** 1.0.0-PROD  
> **Backend:** Java 17 / Spring Boot 3.4+ / Maven  
> **Persistencia:** Hibernate / JPA con discriminador multi-tenant (`MySQL` local / `PostgreSQL Supabase` producción)  
> **Seguridad:** Spring Security 6 stateless + JWT Bearer  
> **Inteligencia Artificial:** OpenAI API Spec (Google Gemini 2.5 Flash primario, Anthropic Claude fallback)  
> **Pagos:** Stripe Checkout Sessions + Webhooks criptográficos HMAC-SHA256  

---

## 1. Problema, Solución y Arquitectura SaaS Multi-Inquilino

### 1.1 Problema del Sector
Las barberías y centros de estética en Colombia y América Latina enfrentan problemas operativos crónicos:
1. **Pérdida de ingresos por inasistencia (*no-shows*):** Clientes que agendan turnos pero olvidan asistir, dejando estilistas inactivos en horarios pico.
2. **Saturación en canales de mensajería (WhatsApp):** Los dueños o estilistas interrumpen sus cortes y tratamientos para contestar precios, horarios y disponibilidad.
3. **Falta de visibilidad de nómina y citas:** Cruces de horarios (*double-booking*) entre reservas presenciales y telefónicas.
4. **Cobros y suscripciones desordenadas:** Dificultad para monetizar paquetes de turnos o controlar el cobro de comisiones y membresías.

### 1.2 Solución Propuesta
**Sillón** es una plataforma SaaS B2B en la nube que automatiza la operación integral del salón:
* **Catálogo y Autoservicio Público:** Cada salón dispone de un subdominio/slug único (`/api/publico/salones/{slug}`) donde los clientes consultan servicios, estilistas y disponibilidad exacta en tiempo real, agendando turnos sin requerir cuenta previa.
* **Asistente Virtual (Chatbot IA):** Responde preguntas sobre precios, horarios y servicios con base en datos verificados, escalando automáticamente a humanos ante solicitudes complejas o reclamos.
* **Recordatorios Deterministas por WhatsApp:** Genera borradores de confirmación personalizados listos para revisión humana y envío, reduciendo la tasa de inasistencia.
* **Monetización con Stripe:** Modelo Freemium con suscripción PRO y recargas de paquetes de créditos de IA mediante checkout seguro y webhooks idempotentes.

### 1.3 Arquitectura Multi-Inquilino por Discriminador
* **Estrategia:** Base de datos compartida y esquema compartido (*Shared Database, Shared Schema*).
* **Columna discriminadora:** Todas las tablas de dominio (`usuarios`, `servicios`, `estilistas_servicios`, `clientes`, `citas`, `solicitudes_contacto`) incluyen la columna obligatoria `salon_id` indexada.
* **Aislamiento a nivel de datos:**
  * Ninguna operación administrativa confía en parámetros enviados por el cliente (`body`, `path` o `param`).
  * El `salonId` se extrae exclusivamente del token criptográfico del usuario autenticado (`@AuthenticationPrincipal UsuarioPrincipal usuario`).
  * Toda consulta JPA o mutación en la capa de persistencia incluye el filtro explícito `WHERE salon_id = :salonId`.

```
                    ┌──────────────────────────────────────────────┐
                    │               CLIENTE HTTP                   │
                    │       (Postman / Swagger / Frontend)         │
                    └──────────────────────┬───────────────────────┘
                                           │
                        [ Authorization: Bearer <JWT> ]
                                           ▼
                    ┌──────────────────────────────────────────────┐
                    │          SPRING SECURITY 6 FILTER            │
                    │    - Valida token HMAC-SHA256                │
                    │    - Extrae claims: email, rol, salonId      │
                    │    - Inyecta UsuarioPrincipal en contexto    │
                    └──────────────────────┬───────────────────────┘
                                           │
                                           ▼
                    ┌──────────────────────────────────────────────┐
                    │          CONTROLADORES REST (API)            │
                    │    - Valida DTOs con @Valid                  │
                    │    - Extrae salonId seguro desde Principal   │
                    └──────────────────────┬───────────────────────┘
                                           │
                                           ▼
                    ┌──────────────────────────────────────────────┐
                    │             SERVICIOS DE DOMINIO             │
                    │    - Reglas de negocio puras                 │
                    │    - Validaciones temporales con Clock       │
                    │    - Consultas JPA filtradas por salonId     │
                    └──────────────────────┬───────────────────────┘
                                           │
                                           ▼
                    ┌──────────────────────────────────────────────┐
                    │           BASE DE DATOS MULTI-TENANT         │
                    │     (Tablas aisladas con salon_id NOT NULL)   │
                    └──────────────────────────────────────────────┘
```

---

## 2. Roles, Permisos y Matriz de Control de Acceso

El sistema define cuatro niveles jerárquicos de acceso:

| Rol / Nivel | Descripción | Rutas Permitidas | Acciones Clave |
|---|---|---|---|
| **PÚBLICO** | Clientes finales anónimos y pasarelas de pago | `/api/auth/**`<br>`/api/publico/**`<br>`/api/pagos/webhook`<br>`/api/pagos/resultado`<br>`/swagger-ui/**`<br>`/v3/api-docs/**` | Registrar nuevo salón con dueño, autenticarse, consultar catálogo y disponibilidad, crear reservas públicas, interactuar con chatbot y recibir webhooks de Stripe. |
| **DUENO** | Administrador principal del salón (inquilino) | `/api/salon/**`<br>`/api/servicios/**`<br>`/api/estilistas/**`<br>`/api/clientes/**`<br>`/api/citas/**`<br>`/api/solicitudes/**`<br>`/api/pagos/suscripcion`<br>`/api/pagos/creditos` | Configurar horarios del salón, crear/editar servicios, contratar estilistas, gestionar clientes, crear citas manuales, generar recordatorios IA, gestionar solicitudes de contacto y comprar créditos/planes. |
| **ESTILISTA** | Profesional que atiende servicios en el salón | `GET /api/servicios`<br>`GET /api/citas`<br>`PATCH /api/citas/{id}/estado` | Consultar la nómina de servicios activos, ver las citas asignadas a su agenda en una fecha específica y actualizar el estado de una cita (`COMPLETADA`, `CANCELADA`, `NO_ASISTIO`). |
| **ADMIN** | Superadministrador de la plataforma SaaS | `/api/admin/**` | Auditoría global de todos los salones registrados en la plataforma y supervisión del estado de suscripciones. |

---

## 3. Modelo de Negocio, Planes y Créditos IA

El sistema opera bajo un modelo *Freemium* con consumo controlado de créditos para funciones de Inteligencia Artificial:

| Plan | Costo | Límite de Estilistas Activos | Créditos IA Incluidos | Características |
|---|---|---|---|---|
| **GRATIS** | $0 / mes | Máximo **2** estilistas | **10** créditos iniciales al registrar el salón | Acceso a agenda básica, catálogo público, reservas online y recordatorios hasta agotar saldo. |
| **PRO** | Pago mensual recurrente (`sillon.stripe.precio-pro`) | **Ilimitados** | **+300** créditos acumulables por ciclo | Notificaciones extendidas, soporte prioritario, mayor capacidad operativa y chatbot 24/7. |
| **PAQUETE ADICIONAL** | Pago único (`sillon.stripe.precio-creditos`) | N/A | **+200** créditos acumulables | Recarga inmediata de saldo de IA para salones de cualquier plan sin alterar suscripción. |

---

## 4. Algoritmos de Negocio y Orden Estricto de Validaciones

### 4.1 Creación de Estilistas (`EstilistaService.crear`)
Para evitar inconsistencias y respuestas erróneas en el calificador automático, la creación de estilistas aplica un orden secuencial e inmutable de tres validaciones:

1. **Límite de Estilistas según Plan (`402 Payment Required`):**  
   Si el plan del salón es `Plan.GRATIS` y el conteo de estilistas activos en `usuarioRepository.countBySalonIdAndRol(salonId, Rol.ESTILISTA)` es mayor o igual a 2, se arroja inmediatamente `PagoRequeridoException("El plan GRATIS permite máximo 2 estilistas...")`.
2. **Unicidad Global de Correo Electrónico (`409 Conflict`):**  
   Si `usuarioRepository.existsByEmail(request.email())` es verdadero, se arroja `ReglaNegocioException("Ya existe un usuario con este correo electrónico")`.
3. **Pertenencia de Servicios al Salón (`404 Not Found`):**  
   Se consulta `servicioRepository.findAllByIdInAndSalonId(request.servicioIds(), salonId)`. Si la lista de servicios recuperada tiene un tamaño menor a los IDs únicos solicitados, significa que al menos un servicio no existe o pertenece a otro salón inquilino; se arroja `RecursoNoEncontradoException("Uno o más servicios no existen en el salón")`.

---

### 4.2 Agendamiento de Citas (`CitaService.crear` / `ReglasAgenda`)
Tanto en citas manuales de dueño como en reservas públicas online, se ejecuta un algoritmo estricto de 7 pasos:

1. **Servicio Válido y Activo:** Verifica que el servicio exista en el salón (`404`) y que su estado `activo` sea verdadero (`409 ReglaNegocioException` si está inactivo).
2. **Estilista Habilitado:** Verifica que el estilista pertenezca al salón (`404`), esté activo (`409`) y tenga asignado el servicio solicitado (`409 ReglaNegocioException`).
3. **Jornada Comercial:** Calcula `inicio` y `fin = inicio.plusMinutes(duracion)`. Valida que `inicio.toLocalDate().equals(fin.toLocalDate())` y que `horaInicio >= horaApertura` y `horaFin <= horaCierre` (`409 ReglaNegocioException`).
4. **Política Dominical:** Si `inicio.getDayOfWeek() == DayOfWeek.SUNDAY` y `!configuracion.isAbreDomingos()`, rechaza con `409 ReglaNegocioException`.
5. **Turno en el Futuro:** Valida que `inicio.isAfter(LocalDateTime.now(clock))` mediante el reloj inyectado, impidiendo reservas en el pasado (`409 ReglaNegocioException`).
6. **Ausencia de Traslapes (Cero Colisiones):** Verifica mediante `citaRepository.existsByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(...)` que el estilista no tenga citas en estado `PENDIENTE` o `CONFIRMADA` cuyo intervalo colisione con el nuevo turno (`409 ReglaNegocioException`).
7. **Resolución de Cliente y Persistencia:** Normaliza el teléfono con `ClienteService.normalizarTelefono()`, busca o crea la entidad `Cliente` de manera atómica, y persiste la `Cita` con su origen correspondiente (`MANUAL` u `ONLINE`).

---

## 5. Máquina de Estados de Citas (`EstadoCita`)

### 5.1 Ocupación de Agenda
Una cita bloquea la agenda del estilista impidiendo nuevos turnos únicamente si su estado ocupa disponibilidad:
* `PENDIENTE`: **Ocupa agenda** (`true`)
* `CONFIRMADA`: **Ocupa agenda** (`true`)
* `COMPLETADA`: **Libera agenda** (`false`)
* `CANCELADA`: **Libera agenda** (`false`)
* `NO_ASISTIO`: **Libera agenda** (`false`)

### 5.2 Matriz de Transiciones Permitidas (`puedeCambiarA`)

```
   ┌─────────────┐       (Confirmar)       ┌─────────────┐
   │  PENDIENTE  ├────────────────────────►│ CONFIRMADA  │
   └──────┬──────┘                         └──────┬──────┘
          │                                       │
          │ (Cancelar)             ┌──────────────┼──────────────┐
          │                        │              │              │
          ▼                        ▼              ▼              ▼
   ┌─────────────┐          ┌─────────────┐┌─────────────┐┌─────────────┐
   │  CANCELADA  │          │ COMPLETADA  ││  CANCELADA  ││ NO_ASISTIO  │
   │ (Terminal)  │          │ (Terminal)  ││ (Terminal)  ││ (Terminal)  │
   └─────────────┘          └─────────────┘└─────────────┘└─────────────┘
```

* Desde **`PENDIENTE`**: Solo puede transicionar a `CONFIRMADA` o `CANCELADA`.
* Desde **`CONFIRMADA`**: Solo puede transicionar a `COMPLETADA`, `CANCELADA` o `NO_ASISTIO`.
* Desde **`COMPLETADA`**, **`CANCELADA`** o **`NO_ASISTIO`**: Son estados terminales; cualquier cambio posterior retorna `false` y es rechazado con `409 ReglaNegocioException`.
* Transición hacia `null`: Retorna `false`.

---

## 6. Módulo de Inteligencia Artificial y Reglas de Seguridad

### 6.1 Descuento Atómico de Créditos
Para prevenir condiciones de carrera cuando múltiples clientes o dueños interactúan concurrentemente, el consumo de créditos se realiza directamente en la base de datos a nivel de fila:
```sql
UPDATE salones 
SET creditos_ia = creditos_ia - 1 
WHERE id = :id AND creditos_ia > 0;
```
Si la cantidad de filas afectadas es `0`, el saldo era nulo y el servicio arroja de inmediato `PagoRequeridoException` (HTTP 402), impidiendo que el balance caiga en números negativos.

### 6.2 Desacoplamiento Transaccional (Llamadas HTTP fuera de `@Transactional`)
Las llamadas a proveedores externos de IA (`ProveedorOpenAi`, `ProveedorClaude`) conllevan latencias de red impredecibles (1 a 15 segundos). Por diseño arquitectónico:
* **Queda estrictamente prohibido invocar APIs de IA dentro de métodos anotados con `@Transactional`.**
* Esto evita agotar el pool de conexiones JDBC (`HikariCP`), garantizando que la conexión a base de datos solo se abra para leer datos y descontar créditos, cerrándose antes del intercambio HTTP con el modelo de lenguaje.

### 6.3 Datos Verificados en Java
Para evitar alucinaciones en los recordatorios de WhatsApp:
* Java extrae directamente de la base de datos los 6 campos verificados:
  1. Nombre y duración del servicio.
  2. Nombre del estilista.
  3. Fecha completa formateada en español (`EEEE d 'de' MMMM` con `Locale es-CO`).
  4. Hora formateada (`h:mm a` limpiando espacios no separables `\u00A0` y `\u202F`).
  5. Valor exacto en pesos colombianos (`$XX.XXX`).
  6. Dirección comercial y ciudad del salón.
* La IA recibe estos 6 datos en el prompt y tiene prohibido inventar o alterar cualquier valor.

### 6.4 Salida JSON Estricta y Regla Anti-Promesas
* **Contratos JSON:**
  * `recordatorio.txt`: Obliga a responder exclusivamente `{"mensaje": "..."}`.
  * `chatbot.txt`: Obliga a responder exclusivamente `{"respuesta": "...", "escalar": false}`.
* **Filtro de Seguridad en Código (Garantía Java):**
  * Java analiza la respuesta del LLM antes de enviarla al usuario buscando palabras clave restringidas: `["descuento", "cupón", "cupon", "gratis", "promoción", "promocion", "reembols", "silla50"]`.
  * Si el LLM promete descuentos o servicios gratuitos, Java neutraliza la respuesta con el mensaje seguro predeterminado, activa la bandera `escalar = true` y crea automáticamente una entidad `Solicitud` en la base de datos para atención humana con motivo `REGLA_DE_SEGURIDAD`.

---

## 7. Pasarela de Pagos Stripe e Idempotencia

* **Firma Criptográfica:** Todos los webhooks entrantes en `POST /api/pagos/webhook` deben validar la cabecera `Stripe-Signature` mediante HMAC-SHA256 y secreto de webhook (`Webhook.constructEvent`). La ausencia o discrepancia de firma arroja `FirmaInvalidaException` (400 Bad Request).
* **Idempotencia:** Cada evento de Stripe tiene un ID único (`evt_...`). Se persiste en la tabla `eventos_stripe` con su timestamp. Si `eventoStripeRepository.existsById(id)` es verdadero, la petición retorna inmediatamente `WebhookResponse(true, false)` sin aplicar duplicaciones de plan o créditos.
* **Procesamiento de Eventos:**
  * `checkout.session.completed` (`paid`):
    * `SUSCRIPCION_PRO`: Salón asciende a `Plan.PRO`, suma +300 créditos IA y vincula `stripeSubscriptionId`.
    * `PAQUETE_CREDITOS`: Suma +200 créditos IA al saldo existente.
  * `customer.subscription.deleted`: Salón revierte a `Plan.GRATIS` y desvincula `stripeSubscriptionId`.

---

## 8. Catálogo Completo de Endpoints de la API

### Formato Único de Error (4 Campos Obligatorios)
Todas las excepciones capturadas por `ManejadorGlobalErrores` retornan estrictamente este contrato JSON:
```json
{
  "codigo": 404,
  "error": "Not Found",
  "mensaje": "Cita no encontrada",
  "fecha": "2026-11-20T15:30:00"
}
```

### Tabla de Endpoints REST

| Módulo | Método | Endpoint | Rol Mínimo | Descripción |
|---|---|---|---|---|
| **Auth** | `POST` | `/api/auth/registro` | PÚBLICO | Registro atómico de salón y usuario dueño (crea slug y da 10 créditos). |
| **Auth** | `POST` | `/api/auth/login` | PÚBLICO | Autenticación de usuario con emisión de JWT Bearer. |
| **Salón** | `GET` | `/api/salon` | DUENO | Consulta de información general y horarios del propio salón. |
| **Salón** | `PUT` | `/api/salon` | DUENO | Actualización de datos comerciales, dirección y horarios. |
| **Servicios**| `POST` | `/api/servicios` | DUENO | Creación de nuevo servicio en el catálogo. |
| **Servicios**| `GET` | `/api/servicios` | DUENO / ESTILISTA | Listado de servicios del salón. |
| **Servicios**| `PUT` | `/api/servicios/{id}/estado` | DUENO | Activación / desactivación lógica de un servicio. |
| **Estilistas**| `POST`| `/api/estilistas` | DUENO | Registro de nuevo estilista (valida plan 402, correo 409 y servicios 404). |
| **Estilistas**| `GET` | `/api/estilistas` | DUENO | Nómina completa de estilistas y sus servicios ofrecidos. |
| **Clientes** | `GET` | `/api/clientes` | DUENO | Directorio de clientes con teléfonos normalizados. |
| **Citas** | `POST` | `/api/citas` | DUENO | Agendamiento manual de turno administrativo. |
| **Citas** | `GET` | `/api/citas` | DUENO / ESTILISTA | Agenda de citas filtrada opcionalmente por fecha (`?fecha=YYYY-MM-DD`). |
| **Citas** | `PATCH`| `/api/citas/{id}/estado` | DUENO / ESTILISTA | Modificación del estado del turno (`CONFIRMADA`, `CANCELADA`, etc.). |
| **Citas** | `POST` | `/api/citas/{id}/recordatorio` | DUENO | Genera borrador IA de recordatorio WhatsApp para la cita. |
| **Público** | `GET` | `/api/publico/salones/{slug}` | PÚBLICO | Catálogo público para clientes sin autenticación. |
| **Público** | `GET` | `/api/publico/salones/{slug}/disponibilidad` | PÚBLICO | Slots horarios libres calculados por fecha, servicio y estilista. |
| **Público** | `POST` | `/api/publico/salones/{slug}/reservas` | PÚBLICO | Reserva pública directa de clientes. |
| **Público** | `POST` | `/api/publico/salones/{slug}/chat` | PÚBLICO | Consulta conversacional con el chatbot del salón. |
| **Solicitudes**| `GET`| `/api/solicitudes` | DUENO | Bandeja de solicitudes de contacto generadas por escalamiento del chatbot. |
| **Solicitudes**| `PATCH`| `/api/solicitudes/{id}/estado` | DUENO | Actualiza estado de solicitud a `ATENDIDA` o `DESCARTADA`. |
| **Pagos** | `POST` | `/api/pagos/suscripcion` | DUENO | Inicia sesión de Stripe Checkout para suscripción Plan PRO. |
| **Pagos** | `POST` | `/api/pagos/creditos` | DUENO | Inicia sesión de Stripe Checkout para recarga de 200 créditos IA. |
| **Pagos** | `POST` | `/api/pagos/webhook` | PÚBLICO | Receptor de eventos firmados de Stripe. |
| **Pagos** | `GET` | `/api/pagos/resultado` | PÚBLICO | Redirección amigable post-pago (`?estado=exito` o `cancelado`). |
| **Admin** | `GET` | `/api/admin/salones` | ADMIN | Auditoría global de todos los inquilinos y sus suscripciones. |

---

## 9. Alcance Excluido del Proyecto

Para mantener el enfoque en la robustez arquitectónica del backend y cumplir con los criterios de evaluación, se excluyen deliberadamente:
1. **Frontend Web de Gestión:** No se incluye interfaz visual en React/Angular; el backend se consume de manera estándar mediante Swagger UI (`/swagger-ui.html`) y colección de Postman.
2. **Conexión Directa con WhatsApp Cloud API:** No se envían mensajes automáticos directos a teléfonos; la plataforma genera el borrador determinista verificado para que el equipo humano lo revise y envíe.
3. **Almacenamiento de Tarjetas de Crédito:** No se almacenan datos financieros sensibles en la base de datos (cumplimiento PCI-DSS mediante delegación total en Stripe Checkout).
4. **Pasarelas de Pago Locales Directas:** En esta versión no se implementan integraciones nativas con PSE o Wompi, unificando el cobro internacional y suscripciones mediante Stripe.

