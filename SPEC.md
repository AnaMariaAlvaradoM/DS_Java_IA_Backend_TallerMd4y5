# Especificación del Proyecto Sillón (SPEC.md)

## 1. El Negocio: Por qué existe Sillón
Sillón es un software como servicio (SaaS) multi-inquilino para barberías y salones de belleza en Colombia. Permite gestionar:
- Catálogo de servicios y estilistas.
- Agenda con prevención de cruces y respeto de horarios comerciales.
- Reservas públicas en línea para clientes finales.
- Recordatorios automáticos con IA listos para WhatsApp.
- Asistente virtual (chatbot) con respuestas basadas en el catálogo real y escalamiento ante dudas complejas o peticiones de agendamiento/modificación.
- Suscripción mensual al plan PRO y paquetes de créditos de IA procesados con Stripe.

## 2. Personajes y Roles
- **DUENO**: Dueño del salón. Registra el salón, configura el horario comercial, gestiona servicios y estilistas, consulta agenda global, solicita recordatorios y adquiere planes/créditos.
- **ESTILISTA**: Empleado del salón. Consulta únicamente su agenda asignada y puede marcar sus citas como `COMPLETADA` o `NO_ASISTIO`.
- **ADMIN**: Administrador de la plataforma. Puede listar todos los salones registrados en el sistema.
- **Público (Clientes Finales)**: Sin autenticación. Consultan información básica del salón, catálogo activo, disponibilidad en tiempo real, crean reservas en línea y conversan con el chatbot.

## 3. Planes y Monetización
- **Plan GRATIS**: Plan por defecto al registrarse. Incluye máximo 2 estilistas y 10 créditos de IA de cortesía.
- **Plan PRO**: Suscripción mensual de \$15 USD (`price_1UdggAF0OVCb0DiuE8ZeJBSV`). Sin límite de estilistas y +300 créditos de IA al activarse.
- **Paquete de Créditos**: Pago único de \$5 USD (`price_1UdgiRF0OVCb0DiukjLarNzr`) que añade +200 créditos de IA.

## 4. Reglas de Negocio Esenciales
1. **Aislamiento Multi-inquilino**: Cada dato pertenece exclusivamente a un salón (`salon_id`). Toda consulta o modificación fuera del salón autenticado devuelve `404 Not Found`.
2. **Registro de Salón y Dueño**: Creación atómica del salón (plan GRATIS, 10 créditos, horario por defecto 08:00 a 19:00, domingos cerrado, intervalo de 30 min) y su usuario dueño con contraseña BCrypt. Slug único generado a partir del nombre del salón.
3. **Correo Único Global**: No puede registrarse dos veces el mismo correo electrónico en la plataforma (HTTP 409).
4. **Validación de Servicios**: Duración entre 15 y 480 minutos, precio entero positivo en COP (HTTP 400 si no cumple).
5. **Estilistas y Límite de Plan**: Asignados a servicios de su mismo salón. Si el salón está en plan GRATIS e intenta crear un tercer estilista, se rechaza con HTTP 402.
6. **Agenda y Cruces**:
   - Una cita dura la duración de su servicio (`fin = inicio + duracionMinutos`).
   - Se valida que la cita esté dentro del horario de apertura y cierre del salón, en un día hábil (domingo solo si el salón abre) y no en el pasado (según la zona horaria `America/Bogota`).
   - Dos citas del mismo estilista no pueden cruzarse si sus estados ocupan agenda (`PENDIENTE` o `CONFIRMADA`).
   - Dos intervalos se cruzan si: `inicio1 < fin2 && fin1 > inicio2`.
7. **Máquina de Estados de Citas**:
   - Cita creada por el dueño: estado `CONFIRMADA`, origen `SALON`.
   - Reserva en línea: estado `PENDIENTE`, origen `EN_LINEA`.
   - Transiciones permitidas:
     - `PENDIENTE` -> `CONFIRMADA` o `CANCELADA`.
     - `CONFIRMADA` -> `COMPLETADA`, `CANCELADA` o `NO_ASISTIO`.
   - Estados terminales: `COMPLETADA`, `CANCELADA`, `NO_ASISTIO`.
8. **Pagos y Webhook de Stripe**:
   - La API nunca recibe montos en el request; los precios son determinados por el backend.
   - El webhook valida la firma `Stripe-Signature` (HTTP 400 si es inválida).
   - Idempotencia: Los eventos se registran en `eventos_stripe`. Si un evento ya fue procesado, se ignora y no se duplican créditos ni estados.
9. **Inteligencia Artificial**:
   - 1 crédito consumido por cada respuesta exitosa de IA. Si no hay créditos, responde HTTP 402 sin invocar al proveedor.
   - Los datos de las citas en los recordatorios son calculados y verificados por Java; la IA solo redacta el texto.
   - El chatbot responde según el catálogo real; si detecta promesas de descuentos, promociones, regalos o reembolsos, aplica regla anti-promesas y escala a `REGLA_DE_SEGURIDAD`.
   - Si el cliente desea agendar, cambiar o cancelar, se escala a solicitud de contacto (`PEDIDO_POR_IA`).

## 5. Contrato de Respuestas de Error
Formato JSON estándar de 4 campos:
- `codigo`: Entero HTTP (ej. 400, 401, 402, 403, 404, 409, 502, 503).
- `error`: Nombre estándar del estado HTTP (ej. "Conflict", "Not Found").
- `mensaje`: Descripción clara en español.
- `fecha`: Fecha y hora del error (`LocalDateTime`).

## 6. Fuera de Alcance
- Envío real de mensajes por WhatsApp o correo.
- Interfaz gráfica (frontend).
- Cobros a clientes finales del salón.
- Recordatorios en background programados.
