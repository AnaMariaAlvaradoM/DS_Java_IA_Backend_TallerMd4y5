
# SPEC — Proyecto Sillón

## 1. Descripción del proyecto
Sillón es una API REST para administrar salones de belleza y barberías mediante un modelo SaaS multi-inquilino.

Su propósito es organizar servicios, estilistas y citas, evitar reservas cruzadas y ofrecer funciones de inteligencia artificial y pagos.

## 2. Problema que resuelve
Los salones pueden perder reservas por una gestión manual de horarios, citas duplicadas y ausencia de recordatorios.

## 3. Roles
- DUENO: administra su salón, servicios, personal y citas.
- ESTILISTA: consulta su agenda y actualiza los estados permitidos de sus citas.
- ADMIN: consulta los salones registrados en la plataforma.
- Cliente final: accede a las funciones públicas sin autenticarse.

## 4. Planes
- GRATIS: máximo 2 estilistas y 10 créditos iniciales de IA.
- PRO: estilistas ilimitados y 300 créditos adicionales al activarse.
- Paquete de créditos: añade 200 créditos mediante un pago único.

## 5. Modelo de datos

### 5.1 Entidades

**Salon**
Representa cada barbería o salón registrado en la plataforma.

Atributos:
- id
- nombre
- slug (único)
- ciudad
- direccion
- telefono
- plan
- creditosIa
- stripeSuscripcionId
- configuracion
- fechaCreacion

**ConfiguracionSalon**
Configuración embebida dentro de Salon mediante @Embeddable.

Atributos:
- horaApertura
- horaCierre
- abreDomingos
- intervaloMinutos

**Usuario**
Representa a los dueños, estilistas y administradores.

Atributos:
- id
- nombre
- email (único)
- password (cifrada con BCrypt)
- rol
- activo

Relaciones:
- Muchos usuarios pertenecen a un Salon.
- Los estilistas pueden ofrecer varios servicios mediante la tabla estilista_servicios.

**Servicio**
Representa los servicios ofrecidos por un salón.

Atributos:
- id
- nombre
- descripcion
- duracionMinutos
- precio
- activo

Relación:
- Cada servicio pertenece a un Salon.

**Cliente**
Representa a las personas que reservan citas.

Atributos:
- id
- nombre
- telefono

Relaciones:
- Cada cliente pertenece a un Salon.
- El teléfono es único dentro de cada salón.

**Cita**
Representa las reservas realizadas por clientes.

Atributos:
- id
- inicio
- fin
- precio
- estado
- origen
- nota
- fechaCreacion
- fechaActualizacion

Relaciones:
- Pertenece a un Salon.
- Pertenece a un Cliente.
- Tiene un Usuario asignado como estilista.
- Está asociada a un Servicio.

**SolicitudContacto**
Almacena solicitudes creadas por el chatbot.

Atributos:
- id
- clienteNombre
- clienteContacto
- mensajes
- motivo
- estado
- fechaCreacion

Relación:
- Pertenece a un Salon.

**EventoStripe**
Registra eventos procesados para evitar duplicaciones.

Atributos:
- id (identificador del evento de Stripe)
- tipo
- fechaProcesado

### 5.2 Enumeraciones

Plan:
- GRATIS
- PRO

Rol:
- DUENO
- ESTILISTA
- ADMIN

EstadoCita:
- PENDIENTE
- CONFIRMADA
- COMPLETADA
- CANCELADA
- NO_ASISTIO

OrigenCita:
- SALON
- EN_LINEA

TipoCompra:
- SUSCRIPCION_PRO
- PAQUETE_CREDITOS

MotivoSolicitud:
- PEDIDO_POR_IA
- REGLA_DE_SEGURIDAD

EstadoSolicitud:
- PENDIENTE
- ATENDIDA

Todas las enumeraciones se almacenarán como texto mediante EnumType.STRING.

## 6. Reglas de negocio

### 6.1 Salones y cuentas
- Todos los datos pertenecen a un salón. Si alguien intenta acceder a datos de otro salón, la API responde 404.
- El registro crea simultáneamente el salón y su dueño, con plan GRATIS, 10 créditos y horario predeterminado de 08:00 a 19:00, sin domingos e intervalos de 30 minutos.
- El correo electrónico es único en toda la plataforma. Si está repetido, responde 409.
- El slug se genera a partir del nombre del salón, sin tildes, en minúsculas y con guiones. Si existe, se agrega un número.
- La apertura debe ser anterior al cierre (409). El intervalo permitido es de 15 a 60 minutos (400).

### 6.2 Catálogo y equipo
- Cada servicio dura entre 15 y 480 minutos y tiene un precio entero positivo en COP. Los valores inválidos responden 400.
- Cada estilista ofrece uno o más servicios de su propio salón. Un servicio de otro salón responde 404.
- El plan GRATIS admite máximo dos estilistas. Intentar crear un tercero responde 402. PRO no tiene ese límite.

### 6.3 Agenda y reservas
- La duración de la cita es igual a la del servicio y el sistema calcula automáticamente la hora de finalización.
- La cita conserva el precio del servicio al momento de reservar.
- No se permiten citas en el pasado, fuera del horario, domingos cerrados ni con estilistas que no ofrezcan el servicio (409).
- No se permiten citas cruzadas para un mismo estilista. PENDIENTE y CONFIRMADA ocupan agenda; un cruce responde 409.
- Las citas creadas por el dueño nacen CONFIRMADA, con origen SALON.
- Las reservas públicas nacen PENDIENTE, con origen EN_LINEA.
- Los clientes se buscan por teléfono dentro del salón antes de crearlos. El teléfono se normaliza conservando únicamente dígitos y el signo +.
- Transiciones permitidas:
  - PENDIENTE → CONFIRMADA o CANCELADA.
  - CONFIRMADA → COMPLETADA, CANCELADA o NO_ASISTIO.
  - COMPLETADA, CANCELADA y NO_ASISTIO son estados finales.
- Las transiciones no permitidas responden 409.
- Un estilista únicamente modifica sus propias citas (404 si son ajenas) y solo puede marcarlas COMPLETADA o NO_ASISTIO (403 para otros estados).
- Java calcula la disponibilidad en intervalos desde la apertura, excluyendo horarios pasados, cruces y servicios que terminen después del cierre.
- Las fechas y horas actuales se calculan con la zona horaria America/Bogota.

### 6.4 Pagos
- El backend selecciona el precio de Stripe; nunca acepta precios o identificadores de precio enviados por el cliente.
- Un salón que ya tiene PRO no puede volver a solicitar la suscripción (409).
- El webhook valida la firma de Stripe; una firma ausente o inválida responde 400.
- Cada evento se procesa una sola vez, registrando su identificador para evitar duplicados.
- Activar PRO guarda el identificador de suscripción y suma 300 créditos.
- Comprar un paquete suma 200 créditos.
- Cancelar la suscripción devuelve el salón a GRATIS sin eliminar los créditos existentes.

### 6.5 Inteligencia artificial
- Cada respuesta de la IA consume un crédito.
- Si no hay créditos, la API responde 402 sin llamar al proveedor.
- El crédito se descuenta cuando la IA responde.
- Los recordatorios solo se generan para citas PENDIENTE o CONFIRMADA; cualquier otro estado responde 409. Java proporciona los datos verificados y la IA redacta el mensaje.
- Los recordatorios necesitan aprobación humana: Sillón no los envía automáticamente.
- El chatbot utiliza el catálogo real del salón y no puede crear, modificar ni cancelar citas.
- Cuando no puede resolver una solicitud, escala el caso y crea una SolicitudContacto.
- Java bloquea respuestas que prometan descuentos, cupones, promociones, productos gratis o reembolsos. En estos casos, responde con un mensaje seguro y escala con motivo REGLA_DE_SEGURIDAD.
- Las respuestas de IA deben ser JSON válido con los campos exigidos. Si son inválidas, responde 502.
- Si ningún proveedor responde, devuelve 503.
- Las credenciales privadas se mantienen fuera de Git y nunca se incluyen en prompts.


## 7. Contrato de la API

El contrato es el acuerdo entre la API y quien la usa: rutas, métodos, nombres de campos y códigos de respuesta. Se respeta al pie de la letra: la API se califica con una colección oficial de pruebas y, si un campo se llama distinto, esa prueba falla.

### 7.1 Convenciones de formato

- Fechas: `AAAA-MM-DD`.
- Fecha y hora: `AAAA-MM-DDTHH:mm:ss` (por ejemplo, el 20 de octubre a las 10:30 se escribe `2026-10-20T10:30:00`).
- Horas: `HH:mm` al enviar; Jackson las devuelve como `HH:mm:ss`.
- Los precios son números enteros en pesos colombianos, sin puntos ni decimales (35000).

### 7.2 Autenticación de las rutas protegidas

Todas las rutas que no son públicas reciben el token en la cabecera `Authorization` con el formato `Bearer` más el token.

### 7.3 Formato único de error

Todos los errores, incluidos los de seguridad (401 y 403), responden con un objeto con estos cuatro campos:

| Campo | Contenido |
|---|---|
| codigo | El número del código HTTP (por ejemplo 409) |
| error | El nombre estándar del código (por ejemplo Conflict) |
| mensaje | Explicación en español. En errores de validación, los campos que fallaron con su mensaje, separados por punto y coma |
| fecha | Fecha y hora del error |

### 7.4 Códigos de respuesta

| Código | Cuándo se usa en Sillón |
|---|---|
| 400 | Datos inválidos, JSON mal formado, parámetro faltante, firma de webhook inválida |
| 401 | Sin token, token inválido o credenciales incorrectas |
| 402 | Límite del plan GRATIS o salón sin créditos de IA |
| 403 | El rol no tiene permiso, o un estilista intenta una transición que no le corresponde |
| 404 | No existe, o pertenece a otro salón |
| 409 | Choca con una regla de negocio: cruce, horario, transición, correo repetido, plan ya activo |
| 502 | Stripe o la IA respondieron algo que no se pudo usar |
| 503 | Ningún proveedor de IA está disponible |

### 7.5 Autenticación · /api/auth (público)

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST /api/auth/registro | salonNombre (máx. 100), ciudad (máx. 60), direccion (máx. 150), telefono (máx. 30), nombre (máx. 100), email (formato válido), password (8 a 72) | 201 con token, tipo ("Bearer"), expiraEnMinutos, usuarioId, nombre, rol, salonId, salonSlug · 400 · 409 |
| POST /api/auth/login | email, password | 200 con el mismo objeto del registro · 401 |

### 7.6 Mi salón · /api/salon (DUENO)

| Método y ruta | Recibe | Responde |
|---|---|---|
| GET /api/salon | — | 200 con id, nombre, slug, ciudad, direccion, telefono, plan, creditosIa, configuracion (horaApertura, horaCierre, abreDomingos, intervaloMinutos), fechaCreacion |
| PUT /api/salon/configuracion | horaApertura, horaCierre, abreDomingos, intervaloMinutos (15 a 60), todos obligatorios | 200 con el salón actualizado · 400 · 409 si abre después de cerrar |

### 7.7 Servicios · /api/servicios

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST /api/servicios (DUENO) | nombre (máx. 80), descripcion (opcional, máx. 300), duracionMinutos (15 a 480), precio (mayor que 0), activo (opcional; si no llega, true) | 201 con id, nombre, descripcion, duracionMinutos, precio, activo · 400 |
| GET /api/servicios (DUENO y ESTILISTA) | — | 200 con la lista de servicios del salón |
| PUT /api/servicios/{id} (DUENO) | Lo mismo que el POST | 200 · 400 · 404 |

### 7.8 Estilistas y clientes (DUENO)

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST /api/estilistas | nombre, email, password (8 a 72), servicioIds (lista con al menos un id) | 201 con id, nombre, email, activo, servicios (lista de nombres) · 400 · 402 · 404 · 409 |
| GET /api/estilistas | — | 200 con la lista de estilistas del salón |
| GET /api/clientes | — | 200 con id, nombre y telefono de cada cliente del salón |

### 7.9 Agenda · /api/citas

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST /api/citas (DUENO) | clienteNombre, clienteTelefono (7 a 30 caracteres), servicioId, estilistaId, inicio, nota (opcional, máx. 300) | 201 con la cita · 400 · 404 · 409 |
| GET /api/citas?fecha= (DUENO y ESTILISTA) | fecha opcional; si no llega, hoy | 200 con las citas del día, ordenadas por hora. El estilista solo ve las suyas |
| PATCH /api/citas/{id}/estado (DUENO y ESTILISTA) | estado | 200 con la cita · 400 · 403 · 404 · 409 |
| POST /api/citas/{id}/recordatorio (DUENO) | — | 200 con citaId, telefonoCliente, mensaje, creditosRestantes, factura · 402 · 404 · 409 · 502 · 503 |

**La cita** se devuelve con: id, inicio, fin, estado, origen, precio, servicioId, servicioNombre, estilistaId, estilistaNombre, clienteNombre, clienteTelefono, nota, fechaCreacion, fechaActualizacion.

**La factura** de la IA tiene: proveedor, modelo, tokensEntrada, tokensSalida, costoEstimadoUsd, milisegundos.

### 7.10 Público · /api/publico/salones/{slug} (sin token)

| Método y ruta | Recibe | Responde |
|---|---|---|
| GET /api/publico/salones/{slug} | — | 200 con nombre, slug, ciudad, direccion, telefono, horario (texto), servicios (solo activos) y estilistas (id, nombre, servicioIds). Nunca correos · 404 |
| GET …/disponibilidad?servicioId=&estilistaId=&fecha= | Los tres parámetros son obligatorios | 200 con fecha, servicio, estilista, duracionMinutos, horariosLibres (lista de horas) · 400 · 404 · 409 |
| POST …/reservas | Lo mismo que POST /api/citas | 201 con la cita PENDIENTE y origen EN_LINEA · 400 · 404 · 409 |
| POST …/chat | clienteNombre, clienteContacto (máx. 120), historial: de 1 a 20 turnos, cada uno con rol (CLIENTE o ASISTENTE) y texto (máx. 500) | 200 con respuesta, escalado (true o false), solicitudId (o null), factura · 400 · 402 · 404 · 502 · 503 |

### 7.11 Solicitudes del chatbot · /api/solicitudes (DUENO)

| Método y ruta | Recibe | Responde |
|---|---|---|
| GET /api/solicitudes | — | 200 con id, clienteNombre, clienteContacto, mensajes, motivo, estado, fechaCreacion; las más recientes primero |
| PATCH /api/solicitudes/{id}/atendida | — | 200 · 404 · 409 si ya estaba atendida |

### 7.12 Pagos · /api/pagos

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST /api/pagos/suscripcion (DUENO) | — | 200 con tipo (SUSCRIPCION_PRO), sesionId, urlPago · 409 si ya es PRO · 502 |
| POST /api/pagos/creditos (DUENO) | — | 200 con tipo (PAQUETE_CREDITOS), sesionId, urlPago · 502 |
| POST /api/pagos/webhook (Stripe) | El evento de Stripe y la cabecera Stripe-Signature | 200 con recibido y procesado · 400 si la firma no es válida |
| GET /api/pagos/resultado?estado= (público) | exito o cancelado | 200 con un mensaje. Es la página a la que vuelve Stripe después del pago |

### 7.13 Administración · /api/admin (ADMIN)

| Método y ruta | Recibe | Responde |
|---|---|---|
| GET /api/admin/salones | — | 200 con todos los salones de la plataforma · 403 para cualquier otro rol |

El primer ADMIN se crea registrando una cuenta normal y cambiando su rol a ADMIN directamente en la base de datos.

## 8. Seguridad y aislamiento
Todos los datos de negocio pertenecen a un salón. Un usuario no puede consultar ni modificar información de otro salón.

## 9. Integraciones
- Stripe: pagos en modo de prueba y webhooks.
- Inteligencia artificial: recordatorios y chatbot con verificación de créditos.
- Supabase y Render: persistencia y despliegue en la nube.

## 10. Fuera de alcance
- Interfaz gráfica.
- Envío real de mensajes por WhatsApp o correo.
- Recordatorios automáticos programados y reportes.
- Pagos de clientes finales directamente a los salones.
