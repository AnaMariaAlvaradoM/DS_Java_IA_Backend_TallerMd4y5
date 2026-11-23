# Especificación del Proyecto — Sillón

## 1. Problema de negocio y solución
Muchas barberías y salones de belleza agendan por WhatsApp, cuaderno o memoria, lo que provoca citas cruzadas, ausencias sin aviso ("no-shows"), horas muertas difíciles de recuperar y demoras para responder precios, servicios y horarios.

**Sillón** es una plataforma SaaS multi-inquilino en Java con Spring Boot donde cada salón opera como un inquilino aislado con su propia configuración, equipo, catálogo, agenda, pagos con Stripe y asistente/recordatorios apoyados en IA.

## 2. Roles, planes y fuera de alcance

### Roles
- **Público:** Puede registrarse, iniciar sesión, consultar el catálogo y disponibilidad por `slug`, reservar en línea y usar el chatbot del salón.
- **DUENO:** Administra su propio salón, configuración, servicios, estilistas, clientes, agenda completa del salón, recordatorios con IA, pagos/créditos y solicitudes de contacto de su salón.
- **ESTILISTA:** Consulta los servicios del salón, ve solo sus citas del día y cambia el estado únicamente de sus citas y solo hacia `COMPLETADA` o `NO_ASISTIO`.
- **ADMIN:** Consulta todos los salones registrados en la plataforma.

### Planes y créditos de IA
- **Plan `GRATIS` (al registrarse):** Máximo 2 estilistas activos/creados y 10 créditos de IA de cortesía.
- **Plan `PRO` (suscripción mensual):** Estilistas sin límite y +300 créditos de IA al activarse el pago.
- **Paquete de créditos de IA (pago único):** +200 créditos de IA al confirmarse el pago.
- Cada respuesta válida generada por la IA consume **1 crédito**.

### Fuera de alcance
- Envío real de mensajes por WhatsApp o correo electrónico.
- Recordatorios automáticos programados en segundo plano o reportes analíticos.
- Cobros del cliente final al salón mediante Stripe Connect.
- Interfaz gráfica web o móvil (el cliente es API REST / Postman).

## 3. Reglas de negocio

### Salones y cuentas
1. **Aislamiento multi-inquilino:** Todo recurso pertenece a un salón. Acceder o modificar datos de otro salón responde `404`.
2. **Registro atómico:** En una sola transacción crea el `Salon` (plan `GRATIS`, 10 créditos, horario por defecto 08:00 a 19:00, sin domingos, intervalo de 30 minutos) y su dueño (`DUENO`) con contraseña encriptada con BCrypt y correo normalizado (minúsculas y sin espacios), devolviendo el JWT.
3. **Correo único global:** El email es único en toda la plataforma (incluyendo dueños y estilistas); si ya existe, responde `409`.
4. **Slug único:** Se genera a partir del nombre del salón (sin tildes, minúsculas, caracteres no alfanuméricos convertidos a `-` y sin guiones en los extremos). Si ya existe, se añade sufijo incremental (`-2`, `-3`, etc.).
5. **Horario y configuración:** La hora de apertura debe ser estrictamente anterior a la hora de cierre (`409` si no). El intervalo de agenda debe estar entre 15 y 60 minutos (`400` si no).

### Catálogo y equipo
6. **Servicios:** La duración va de 15 a 480 minutos y el precio es un número entero en pesos mayor que cero (`400` si no cumple).
7. **Servicios del estilista:** Un estilista solo puede asociarse a servicios del mismo salón. Si algún `servicioId` no existe o pertenece a otro salón, responde `404`.
8. **Orden de validación al crear estilista:**
   - **Primero — Límite del plan (`402`):** En plan `GRATIS` no se permite crear un tercer estilista. Validar primero evita gastar trabajo o mezclar errores de plan con otros cuando el salón ya alcanzó su tope gratuito y necesita mejorar de plan.
   - **Segundo — Correo único (`409`):** Verificar que el correo no esté ya registrado en la plataforma.
   - **Tercero — Servicios del salón (`404`):** Verificar que la lista tenga al menos un servicio válido y que todos pertenezcan al salón del dueño.

### Agenda y estados
9. **Duración y precio snapshot:** La cita calcula `fin` sumando `duracionMinutos` del servicio a `inicio`, y copia el `precio` actual del servicio al momento de crear la cita.
10. **Restricciones al agendar (`409`):** No se puede agendar en el pasado (según `Clock` en `America/Bogota`), fuera del horario del salón, en domingo si `abreDomingos` es falso, ni con un estilista que no ofrezca el servicio.
11. **Sin cruces de agenda:** Solo las citas en estado `PENDIENTE` o `CONFIRMADA` ocupan agenda. Dos intervalos `[inicio1, fin1)` y `[inicio2, fin2)` se cruzan si `inicio1 < fin2` y `fin1 > inicio2`. Si hay cruce, responde `409`.
12. **Origen y estado inicial:** Las citas creadas por el salón inician `CONFIRMADA` con origen `SALON`; las reservas en línea inician `PENDIENTE` con origen `EN_LINEA`.
13. **Cliente por teléfono:** Al agendar se normaliza el teléfono (solo dígitos y `+`) y se busca o crea dentro del salón.
14. **Orden de validaciones al agendar:**
    1. Servicio existe en el salón y está activo.
    2. Estilista existe en el salón, está activo y ofrece el servicio.
    3. Calcular `fin` con la duración del servicio.
    4. Validar que `inicio` no esté en el pasado usando el `Clock` del proyecto.
    5. Validar horario del salón (día hábil, mismo día, dentro de apertura y cierre).
    6. Validar que no haya cruce con citas ocupadas (`PENDIENTE` o `CONFIRMADA`) del estilista.
    7. Buscar o crear cliente, copiar precio y guardar cita.
15. **Transiciones de `EstadoCita` y permisos:**
    - Estados finales (sin transiciones salientes): `COMPLETADA`, `CANCELADA`, `NO_ASISTIO`.
    - Cualquier transición no permitida responde `409`.
    - Si actúa un `ESTILISTA`: si la cita no es suya responde `404`; si intenta un estado distinto a `COMPLETADA` o `NO_ASISTIO` responde `403`.

| Estado actual | Nuevo estado permitido | Caso real de negocio |
| :--- | :--- | :--- |
| `PENDIENTE` | `CONFIRMADA` | El salón o el cliente confirma una reserva hecha en línea. |
| `PENDIENTE` | `CANCELADA` | El cliente cancela o no se confirma la reserva pendiente. |
| `CONFIRMADA` | `COMPLETADA` | El estilista realiza el servicio y cierra la cita. |
| `CONFIRMADA` | `CANCELADA` | Se cancela una cita ya confirmada antes de iniciar. |
| `CONFIRMADA` | `NO_ASISTIO` | Llegó la hora y el cliente nunca se presentó. |
| `COMPLETADA` | *(Ninguno — final)* | Una cita completada no se reabre ni cambia de estado. |
| `CANCELADA` | *(Ninguno — final)* | Una cita cancelada permanece cancelada. |
| `NO_ASISTIO` | *(Ninguno — final)* | La inasistencia queda registrada como estado final. |

16. **Disponibilidad:** Calculada en Java a partir de `Clock` (`America/Bogota`), configuración del salón, citas ocupadas del estilista y duración del servicio.

### Pagos con Stripe
17. El backend decide el precio y el modo (`subscription` para `SUSCRIPCION_PRO`, `payment` para `PAQUETE_CREDITOS`). Si el salón ya está en `PRO` y pide suscripción, responde `409` antes de llamar a Stripe.
18. El webhook valida `Stripe-Signature` (`400` si falta o es inválida), garantiza idempotencia mediante `eventos_stripe` (si el evento ya existe, no vuelve a aplicarlo) y procesa `checkout.session.completed` (con `payment_status` `"paid"`) y `customer.subscription.deleted` (regresa el salón a `GRATIS` conservando créditos).

### Inteligencia artificial
19. **Orden de ejecución:** (1) Validar datos/estado no dependientes de IA -> (2) Verificar créditos (`402` si no hay) -> (3) Cargar prompt y llamar a la IA fuera de transacción -> (4) Consumir crédito de forma atómica si la IA respondió -> (5) Validar JSON (`502` si inválido o incompleto; `503` si ningún proveedor responde) -> (6) Aplicar reglas y responder.
20. **Recordatorio:** Solo para citas `PENDIENTE` o `CONFIRMADA` (`409` si no). Java arma los datos verificados (servicio, estilista, fecha en español de Colombia, hora con espacio normal, valor y dirección) y la IA redacta un borrador en JSON `{"mensaje": "..."}` (máx. 700 caracteres) sin enviarlo automáticamente.
21. **Chatbot y regla anti-promesas:** Usa el catálogo real y horario del salón. Responde en JSON `{"respuesta": "...", "escalar": boolean}`. Escala (`PEDIDO_POR_IA`) si el usuario quiere agendar/modificar/cancelar citas, pide persona, reclama o está molesto. Si la respuesta contiene palabras como `descuento`, `cupón`, `cupon`, `gratis`, `promoción`, `promocion` o `reembols` (sin importar mayúsculas), Java bloquea el texto, responde un mensaje seguro fijo y registra una `SolicitudContacto` con motivo `REGLA_DE_SEGURIDAD`.

## 4. Contrato de la API

### Formato único de error
Todos los errores (incluidos 401 y 403) devuelven:
- `codigo` (entero HTTP)
- `error` (nombre estándar del estado HTTP)
- `mensaje` (texto en español; en validaciones, campos y mensajes separados por `; `)
- `fecha` (fecha y hora del error)

### Endpoints principales
- `POST /api/auth/registro` (público) -> `201` con token, `tipo` (`"Bearer"`), `expiraEnMinutos`, `usuarioId`, `nombre`, `rol`, `salonId`, `salonSlug`.
- `POST /api/auth/login` (público) -> `200` con el mismo payload de autenticación.
- `GET /api/salon` y `PUT /api/salon/configuracion` (`DUENO`).
- `POST /api/estilistas`, `GET /api/estilistas`, `GET /api/clientes` (`DUENO`).
- `POST /api/citas` (`DUENO`), `GET /api/citas?fecha=` (`DUENO`, `ESTILISTA`), `PATCH /api/citas/{id}/estado` (`DUENO`, `ESTILISTA`), `POST /api/citas/{id}/recordatorio` (`DUENO`).
- `POST /api/pagos/suscripcion` (`DUENO`), `POST /api/pagos/creditos` (`DUENO`), `POST /api/pagos/webhook` (Stripe), `GET /api/pagos/resultado?estado=` (público).
- `GET /api/solicitudes` y atención de solicitudes (`DUENO`).
- `GET /api/admin/salones` (`ADMIN`).
- Rutas públicas adicionales: `/api/publico/**`, Swagger (`/swagger-ui.html`, etc.) y `/error`.