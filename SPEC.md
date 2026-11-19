# SPEC · Sillón

Agenda inteligente para barberías y salones de belleza. Backend SaaS multi-inquilino en Java 17 + Spring Boot 4.1.

Esta SPEC es la fuente de verdad del proyecto. Si el código y la SPEC no coinciden, se corrige el código o se discute la regla; nunca se ajusta un test para que pase.

---

## 1. Problema

Las barberías y salones en Colombia agendan por WhatsApp, en cuadernos o de memoria. Eso produce:

- Reservas perdidas mientras el dueño atiende.
- Citas cruzadas del mismo estilista.
- Sillas vacías porque el cliente olvidó la cita (esa hora no se recupera).
- Preguntas de precios y horarios sin respuesta fuera de horario.

## 2. Objetivo

Que cualquier salón se registre, configure horario, servicios y equipo, y desde ese momento:

1. Sus clientes reserven en línea viendo solo horarios realmente libres.
2. El sistema impida citas cruzadas o fuera de horario.
3. El dueño genere recordatorios con IA, con los datos exactos de la cita, listos para enviar por WhatsApp (una persona los aprueba y los envía).
4. Un asistente virtual responda precios y horarios y pase a una persona lo que no puede resolver.

## 3. Fuera de alcance

- Enviar mensajes reales por WhatsApp o correo.
- Recordatorios automáticos programados y reportes.
- Pagos del cliente final al salón (requeriría Stripe Connect).
- Interfaz gráfica: el cliente de la API es Postman.

---

## 4. Conceptos

- **SaaS:** una sola aplicación en la nube que muchos salones usan pagando un plan.
- **Multi-inquilino:** una sola base de datos atiende a muchos salones; cada dato pertenece a uno y ningún salón ve los datos de otro.
- **Inquilino:** cada salón.

## 5. Roles

| Rol | Quién es | Qué hace |
|---|---|---|
| Público (sin token) | Cliente final | Ve el catálogo, la disponibilidad, reserva y chatea |
| DUENO | Dueño del salón | Configura el salón, servicios, estilistas, agenda, IA y pagos |
| ESTILISTA | Empleado | Ve sus citas y las marca COMPLETADA o NO_ASISTIO |
| ADMIN | Equipo de Sillón | Ve todos los salones. Se crea cambiando el rol en la base de datos |

### Matriz de permisos

| Acción | Público | DUENO | ESTILISTA | ADMIN |
|---|---|---|---|---|
| Registrarse, iniciar sesión | Sí | Sí | Sí | Sí |
| Catálogo, disponibilidad, reservar, chatear | Sí | Sí | Sí | Sí |
| Ver y configurar su salón | No | Sí | No | No |
| Crear y editar servicios | No | Sí | No | No |
| Ver servicios del salón | No | Sí | Sí | No |
| Crear y ver estilistas, ver clientes | No | Sí | No | No |
| Crear citas, pedir recordatorios con IA | No | Sí | No | No |
| Ver la agenda | No | Todas las del salón | Solo las suyas | No |
| Cambiar estado de una cita | No | Cualquier transición válida | Solo las suyas, a COMPLETADA o NO_ASISTIO | No |
| Pagar PRO o créditos, ver solicitudes | No | Sí | No | No |
| Ver todos los salones | No | No | No | Sí |

## 6. Planes y créditos

| Producto | Pago en Stripe | Efecto |
|---|---|---|
| GRATIS | Ninguno (al registrarse) | Máximo 2 estilistas y 10 créditos de IA |
| PRO | Suscripción mensual (15 USD) | Estilistas sin límite y +300 créditos al activarse |
| Paquete de créditos | Pago único (5 USD) | +200 créditos |

Un crédito = una respuesta de la IA. Existen porque cada respuesta cuesta tokens a la plataforma.

---

## 7. Modelo de datos

| Entidad (tabla) | Atributos | Relaciones |
|---|---|---|
| Salon (salones) | id, nombre, slug (único), ciudad, direccion, telefono, plan, creditosIa, stripeSuscripcionId, configuracion, fechaCreacion | Tiene usuarios, servicios, clientes, citas y solicitudes |
| ConfiguracionSalon (embebida) | horaApertura, horaCierre, abreDomingos, intervaloMinutos | @Embeddable dentro de Salon |
| Usuario (usuarios) | id, nombre, email (único), password (BCrypt), rol, activo | ManyToOne Salon; ManyToMany Servicio (tabla estilista_servicios) |
| Servicio (servicios) | id, nombre, descripcion, duracionMinutos, precio, activo | ManyToOne Salon |
| Cliente (clientes) | id, nombre, telefono | ManyToOne Salon; teléfono único dentro de cada salón |
| Cita (citas) | id, inicio, fin, precio, estado, origen, nota, fechaCreacion, fechaActualizacion | ManyToOne Salon, Cliente, Usuario (estilista), Servicio |
| SolicitudContacto (solicitudes_contacto) | id, clienteNombre, clienteContacto, mensajes, motivo, estado, fechaCreacion | ManyToOne Salon |
| EventoStripe (eventos_stripe) | id (id del evento de Stripe), tipo, fechaProcesado | Ninguna: evita procesar un evento dos veces |

### Enumeraciones (todas con EnumType.STRING, sin tildes)

| Enum | Valores |
|---|---|
| Plan | GRATIS, PRO |
| Rol | DUENO, ESTILISTA, ADMIN |
| EstadoCita | PENDIENTE, CONFIRMADA, COMPLETADA, CANCELADA, NO_ASISTIO |
| OrigenCita | SALON, EN_LINEA |
| TipoCompra | SUSCRIPCION_PRO, PAQUETE_CREDITOS |
| MotivoSolicitud | PEDIDO_POR_IA, REGLA_DE_SEGURIDAD |
| EstadoSolicitud | PENDIENTE, ATENDIDA |

---

## 8. Reglas de negocio

### 8.1 Salones y cuentas

- **R1 Aislamiento.** Todo dato pertenece a un salón. El salón sale siempre del usuario autenticado, nunca de la URL ni del cuerpo. Si alguien pide un dato de otro salón, responde 404, como si no existiera.
- **R2 Registro.** Crea el salón y su dueño en una sola transacción: plan GRATIS, 10 créditos y horario por defecto (8:00 a 19:00, sin domingos, intervalo de 30 minutos). Devuelve el token.
- **R3 Correo único** en toda la plataforma, también para estilistas. Se guarda en minúsculas y sin espacios. Repetido: 409.
- **R4 Slug.** Se genera desde el nombre: sin tildes, en minúsculas, todo lo que no sea letra o número pasa a guion, sin guiones en los extremos. "Barbería La Navaja" → `barberia-la-navaja`. Si ya existe: `-2`, `-3`…
- **R5 Horario.** La apertura debe ser anterior al cierre (409). El intervalo va de 15 a 60 minutos (400).
- **R6 Login.** Credenciales incorrectas: 401 con un mensaje genérico (no dice si falló el correo o la contraseña).

### 8.2 Catálogo y equipo

- **R7 Servicio.** Dura entre 15 y 480 minutos y cuesta un entero de pesos mayor que cero (400 si no). `activo` es opcional y por defecto true.
- **R8 Estilista.** Ofrece uno o más servicios de su mismo salón. Un servicio de otro salón: 404. La respuesta nunca incluye la contraseña.
- **R9 Límite del plan.** En GRATIS, crear el tercer estilista responde 402. En PRO no hay límite.
- **R10 Orden de validación al crear un estilista** (decisión propia):
  1. Validación del cuerpo (400), antes de entrar al servicio.
  2. Límite del plan (402): va primero porque no depende de los datos enviados; si el salón no puede crear más estilistas, revisar el correo o los servicios no tiene sentido y además evitaría revelar si un correo existe.
  3. Correo único (409).
  4. Que todos los servicioIds sean del salón (404).
- **R11 Clientes.** Nacen al agendar; el listado es solo de lectura. El teléfono se normaliza dejando solo dígitos y `+`.

### 8.3 Agenda

- **R12 Duración.** La cita dura exactamente lo que dura su servicio: el sistema calcula `fin`.
- **R13 Precio congelado.** La cita copia el precio del servicio al agendar; si después cambia el precio, la cita conserva el suyo.
- **R14 Prohibido (409):** agendar en el pasado, fuera del horario del salón, en domingo si el salón no abre, o con un estilista que no ofrece el servicio. La cita debe empezar y terminar el mismo día.
- **R15 Sin cruces.** Un estilista no puede tener dos citas que ocupen agenda y se crucen (409). Ocupan agenda PENDIENTE y CONFIRMADA. Dos intervalos se cruzan si `inicioA < finB` y `finA > inicioB`; una cita que termina a las 10:45 no choca con otra que empieza a las 10:45.
- **R16 Estado inicial.** La cita creada por el salón nace CONFIRMADA (origen SALON). La reservada en línea nace PENDIENTE (origen EN_LINEA).
- **R17 Cliente.** Al agendar se busca por teléfono normalizado dentro del salón; si no existe, se crea.
- **R18 Orden al agendar** (un solo método para salón y reserva en línea):
  1. El servicio existe en el salón y está activo (404).
  2. El estilista existe en el salón, está activo (404) y ofrece el servicio (409).
  3. Se calcula `fin`.
  4. No está en el pasado (409).
  5. Cumple el horario del salón (409, con un mensaje que diga el horario).
  6. No se cruza con otra cita del estilista que ocupe agenda (409).
  7. Se busca o crea el cliente, se copia el precio y se guarda con su estado y origen.

### 8.4 Transiciones de estado

| Desde | Hacia | Caso real |
|---|---|---|
| PENDIENTE | CONFIRMADA | El salón acepta la reserva en línea |
| PENDIENTE | CANCELADA | El cliente o el salón la anulan antes de confirmar |
| CONFIRMADA | COMPLETADA | El servicio se prestó |
| CONFIRMADA | CANCELADA | El cliente avisó que no va |
| CONFIRMADA | NO_ASISTIO | El cliente nunca llegó |

COMPLETADA, CANCELADA y NO_ASISTIO son finales. Cualquier otra transición: 409.

- **R19 Estilista y estados.** Orden de validación al cambiar estado:
  1. La cita existe en el salón (404).
  2. Si es ESTILISTA: la cita debe ser suya (404) y el destino debe ser COMPLETADA o NO_ASISTIO (403).
  3. La transición es válida (409).
  4. Se guarda con `saveAndFlush` para que `fechaActualizacion` salga correcta en la respuesta.

### 8.5 Disponibilidad y hora

- **R20 Disponibilidad.** La calcula Java, nunca la IA. Son las horas de inicio, de intervalo en intervalo desde la apertura, en las que el servicio cabe completo antes del cierre, sin cruzarse con citas que ocupan agenda del estilista y nunca en el pasado. Un día cerrado no tiene horarios.
- **R21 Hora del salón.** "Hoy" y "ahora" se calculan con un `Clock` en la zona `America/Bogota`, aunque el servidor esté en UTC (Render).

### 8.6 Pagos

- **R22 El backend decide el precio.** El cliente solo pide "plan PRO" o "paquete de créditos"; nunca envía un precio ni un id de precio.
- **R23** Si el salón ya es PRO, pedir la suscripción responde 409 antes de llamar a Stripe.
- **R24 Firma.** El webhook recibe el cuerpo crudo y verifica la cabecera `Stripe-Signature`. Sin firma o con firma falsa: 400.
- **R25 Idempotencia.** Cada evento se procesa una sola vez: se guarda su id; si llega repetido se responde `procesado: false` sin cambiar nada.
- **R26 Efectos.** `checkout.session.completed` pagado: PRO → plan PRO, guarda el id de la suscripción y +300 créditos; paquete → +200 créditos. `customer.subscription.deleted`: el salón vuelve a GRATIS y conserva sus créditos. Todo en una sola transacción con el registro del evento.

### 8.7 Inteligencia artificial

- **R27 Créditos.** Un crédito por respuesta. Antes de llamar a la IA se verifica que haya créditos (si no, 402 y no se llama). El crédito se descuenta cuando la IA responde, con una sola actualización atómica que nunca deja créditos negativos.
- **R28 Orden de toda funcionalidad con IA:**
  1. Validar lo que no depende de la IA.
  2. Verificar créditos.
  3. Cargar el prompt y llamar a la IA (fuera de cualquier transacción).
  4. Consumir el crédito.
  5. Leer y validar el JSON.
  6. Aplicar reglas de negocio y responder.
- **R29 Recordatorio.** Solo para citas PENDIENTE o CONFIRMADA (otra: 409, sin gastar crédito). Java arma los datos verificados (servicio, estilista, fecha, hora, valor, dirección); la IA solo redacta. El mensaje debe existir, no estar vacío y tener máximo 700 caracteres (502 si no). Una persona lo aprueba: Sillón no lo envía.
- **R30 Chatbot.** Responde con el catálogo real del salón (de la base de datos). No agenda, no cambia ni cancela citas: escala. Al escalar crea una solicitud de contacto con los mensajes del cliente (máximo 2000 caracteres) y motivo PEDIDO_POR_IA.
- **R31 Anti-promesas (en Java).** Si la respuesta contiene "descuento", "cupón", "cupon", "gratis", "promoción", "promocion" o "reembols" (sin importar mayúsculas), el cliente recibe un mensaje seguro fijo y se crea la solicitud con motivo REGLA_DE_SEGURIDAD. El falso positivo se acepta a propósito.
- **R32 JSON validado.** Toda respuesta de la IA se pide en JSON y se valida. JSON inválido o incompleto: 502. Ningún proveedor responde: 503.
- **R33 Llaves.** Viven en `secrets.properties` (local) o en variables de entorno (Render). Nunca en el código, en Git ni en un prompt.

---

## 9. Contrato de la API

Formatos: fecha `AAAA-MM-DD`; fecha y hora `AAAA-MM-DDTHH:mm:ss`; hora `HH:mm` al enviar (se devuelve `HH:mm:ss`). Precios en pesos enteros (35000).

### Formato único de error (incluye 401 y 403)

```json
{ "codigo": 409, "error": "Conflict", "mensaje": "…", "fecha": "2026-10-20T10:30:00" }
```

En validación, `mensaje` lista los campos que fallaron con su mensaje, separados por punto y coma.

| Código | Uso |
|---|---|
| 400 | Datos inválidos, JSON mal formado, parámetro faltante o de tipo inválido, firma de webhook inválida |
| 401 | Sin token, token inválido, credenciales incorrectas |
| 402 | Límite del plan GRATIS o salón sin créditos |
| 403 | Rol sin permiso, o estilista con transición que no le corresponde |
| 404 | No existe o pertenece a otro salón |
| 409 | Regla de negocio: cruce, horario, transición, correo repetido, plan ya activo |
| 502 | Stripe o la IA respondieron algo inutilizable |
| 503 | Ningún proveedor de IA disponible |

### Autenticación · `/api/auth` (público)

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST /api/auth/registro | salonNombre (máx. 100), ciudad (máx. 60), direccion (máx. 150), telefono (máx. 30), nombre (máx. 100), email (válido), password (8 a 72) | 201: token, tipo ("Bearer"), expiraEnMinutos, usuarioId, nombre, rol, salonId, salonSlug · 400 · 409 |
| POST /api/auth/login | email, password | 200 con el mismo objeto · 401 |

### Mi salón · `/api/salon` (DUENO)

| Método y ruta | Recibe | Responde |
|---|---|---|
| GET /api/salon | — | 200: id, nombre, slug, ciudad, direccion, telefono, plan, creditosIa, configuracion {horaApertura, horaCierre, abreDomingos, intervaloMinutos}, fechaCreacion |
| PUT /api/salon/configuracion | horaApertura, horaCierre, abreDomingos, intervaloMinutos (15 a 60), todos obligatorios | 200 salón actualizado · 400 · 409 |

### Servicios · `/api/servicios`

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST (DUENO) | nombre (máx. 80), descripcion (opcional, máx. 300), duracionMinutos (15 a 480), precio (> 0), activo (opcional, true) | 201: id, nombre, descripcion, duracionMinutos, precio, activo · 400 |
| GET (DUENO, ESTILISTA) | — | 200 lista del salón |
| PUT /{id} (DUENO) | Igual que POST | 200 · 400 · 404 |

### Estilistas y clientes (DUENO)

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST /api/estilistas | nombre, email, password (8 a 72), servicioIds (al menos uno) | 201: id, nombre, email, activo, servicios (nombres) · 400 · 402 · 404 · 409 |
| GET /api/estilistas | — | 200 lista del salón |
| GET /api/clientes | — | 200: id, nombre, telefono |

### Agenda · `/api/citas`

| Método y ruta | Recibe | Responde |
|---|---|---|
| POST (DUENO) | clienteNombre, clienteTelefono (7 a 30), servicioId, estilistaId, inicio, nota (opcional, máx. 300) | 201 cita · 400 · 404 · 409 |
| GET ?fecha= (DUENO, ESTILISTA) | fecha opcional (hoy por defecto) | 200 citas del día ordenadas por hora; el estilista solo ve las suyas |
| PATCH /{id}/estado (DUENO, ESTILISTA) | estado | 200 cita · 400 · 403 · 404 · 409 |
| POST /{id}/recordatorio (DUENO) | — | 200: citaId, telefonoCliente, mensaje, creditosRestantes, factura · 402 · 404 · 409 · 502 · 503 |

**Cita:** id, inicio, fin, estado, origen, precio, servicioId, servicioNombre, estilistaId, estilistaNombre, clienteNombre, clienteTelefono, nota, fechaCreacion, fechaActualizacion.

**Factura:** proveedor, modelo, tokensEntrada, tokensSalida, costoEstimadoUsd, milisegundos.

### Público · `/api/publico/salones/{slug}` (sin token)

| Método y ruta | Recibe | Responde |
|---|---|---|
| GET /{slug} | — | 200: nombre, slug, ciudad, direccion, telefono, horario (texto), servicios (solo activos), estilistas (id, nombre, servicioIds). Nunca correos · 404 |
| GET /{slug}/disponibilidad?servicioId=&estilistaId=&fecha= | Los tres obligatorios | 200: fecha, servicio, estilista, duracionMinutos, horariosLibres · 400 · 404 · 409 |
| POST /{slug}/reservas | Igual que POST /api/citas | 201 cita PENDIENTE, EN_LINEA · 400 · 404 · 409 |
| POST /{slug}/chat | clienteNombre, clienteContacto (máx. 120), historial (1 a 20 turnos: rol CLIENTE o ASISTENTE, texto máx. 500) | 200: respuesta, escalado, solicitudId (o null), factura · 400 · 402 · 404 · 502 · 503 |

### Solicitudes · `/api/solicitudes` (DUENO)

| Método y ruta | Responde |
|---|---|
| GET | 200: id, clienteNombre, clienteContacto, mensajes, motivo, estado, fechaCreacion; más recientes primero |
| PATCH /{id}/atendida | 200 · 404 · 409 si ya estaba atendida |

### Pagos · `/api/pagos`

| Método y ruta | Responde |
|---|---|
| POST /suscripcion (DUENO) | 200: tipo (SUSCRIPCION_PRO), sesionId, urlPago · 409 si ya es PRO · 502 |
| POST /creditos (DUENO) | 200: tipo (PAQUETE_CREDITOS), sesionId, urlPago · 502 |
| POST /webhook (Stripe) | Cuerpo crudo + cabecera Stripe-Signature → 200: recibido, procesado · 400 firma inválida |
| GET /resultado?estado= (público) | exito o cancelado → 200 con un mensaje |

### Administración · `/api/admin` (ADMIN)

| Método y ruta | Responde |
|---|---|
| GET /salones | 200 todos los salones · 403 para otros roles |

---

## 10. Criterios de aceptación (resumen)

- [ ] Registro 201 con token y rol DUENO; correo repetido 409; datos inválidos 400 nombrando email y password.
- [ ] Login incorrecto 401; GET /api/salon sin token 401 con el formato único.
- [ ] Mi salón: GRATIS y 10 créditos; apertura después del cierre 409.
- [ ] Servicio de 5 minutos o precio negativo 400; tercer estilista en GRATIS 402.
- [ ] Reserva de Valentina con Mateo a las 10:00: PENDIENTE, EN_LINEA, fin 10:45, precio 35000.
- [ ] Después de la reserva, la disponibilidad de Mateo no incluye 9:30, 10:00 ni 10:30 y sí 11:00.
- [ ] Cruce, barba con Sara, 6:45 p. m., pasado y domingo: 409.
- [ ] Mateo solo ve su cita; la de Sara 404; cancelar la suya 403; COMPLETADA 200.
- [ ] El salón B recibe 404 sobre datos del salón A y listas vacías; el dueño A en /api/admin 403.
- [ ] Recordatorio de la cita de Sara menciona a Sara y "10:30", trae factura y deja 9 créditos; cita COMPLETADA 409.
- [ ] Chatbot: precio sin escalar; reservar escala con solicitud; el "dueño" con SILLA50 nunca recibe cupón, descuento, gratis ni SILLA50.
- [ ] Pagos: urlPago https; webhook sin firma 400; estilista que paga 403; evento repetido no se aplica.
- [ ] Tests JUnit en verde; Swagger con 22 rutas; API desplegada en Render.
