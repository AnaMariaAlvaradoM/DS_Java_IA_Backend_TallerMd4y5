# SPEC · Sillón · Fase 1: La base SaaS: salones, registro y errores

## Problema
Sillón es un sistema SaaS (Software as a Service). Antes de añadir seguridad o lógica de negocio, necesitamos crear la estructura base de la base de datos, establecer el manejo global de errores y permitir que un dueño registre su salón inicial.

## Objetivo de la fase 1
1. Crear las entidades y repositorios iniciales: `Salon` y `Usuario`.
2. Implementar el endpoint de registro que crea el salón y su dueño al mismo tiempo.
3. Crear el manejador global de excepciones (`@RestControllerAdvice`) para controlar respuestas HTTP precisas (ej. 409 Conflicto).

## Fuera de alcance (PROHIBIDO IMPLEMENTAR AÚN)
La IA tiene estrictamente prohibido generar código para los siguientes módulos, ya que pertenecen a fases futuras:
- Seguridad: JWT, roles y respuestas 401/403 (Fase 2)[cite: 5].
- Catálogo y equipo: servicios y estilistas (Fase 3)[cite: 5].
- La agenda: citas, reglas, estados y disponibilidad (Fase 4)[cite: 5].
- La cara pública: catálogo, disponibilidad y reservas (Fase 5)[cite: 5].
- Pagos con Stripe: plan PRO, créditos y webhooks (Fase 6)[cite: 5].
- El motor de IA, Recordatorios y Chatbot (Fases 7, 8 y 9)[cite: 5].
- Tests automáticos y Swagger (Fases 10 y 11)[cite: 5].

## Reglas de Negocio (Fase 1)
1. **Registro de salón:** Crea el salón y su dueño (DUEÑO) al mismo tiempo. El salón nace por defecto en plan GRATIS, con 10 créditos de IA y el horario base: 8:00 a.m. a 7:00 p.m., sin domingos, y con un intervalo de 30 minutos.
2. **Correo único:** El correo debe ser único en toda la plataforma. Si se intenta registrar un correo que ya existe, la API debe responder `409 Conflict`.
3. **Slug:** Cada salón tiene un identificador único para su URL pública, generado a partir del nombre (en minúsculas, sin tildes, sin espacios). Si el slug generado ya existe, se le debe concatenar un número para hacerlo único.
4. **DTOs:** Toda entrada y salida de la API se hace con `record`. Nunca se exponen las entidades de la base de datos.

## Endpoints de la Fase 1
| Método | Ruta | Respuestas |
|---|---|---|
| POST | /api/v1/auth/register | 201 · 400 · 409 |

# SPEC · Sillón · Fase 2: Seguridad: JWT, roles y respuestas 401/403

## Problema
Los endpoints actuales están desprotegidos. Es necesario asegurar la API para que solo los dueños y estilistas autenticados puedan acceder a sus recursos y garantizar que el aislamiento por salón (multitenancy) funcione mediante el token.

## Objetivo
1. Implementar Spring Security y generación de tokens JWT.
2. Crear filtros para validar el token en cada petición.
3. Extraer el `salonId` del JWT e inyectarlo en el contexto de seguridad para uso de los repositorios.
4. Configurar el manejo global de errores de seguridad (401 No Autorizado y 403 Prohibido).

## Fuera de alcance
- Servicios, estilistas, citas, IA y pagos (Fases 3 a 9).

## Reglas de Negocio
1. **Multitenancy Estricto:** Toda petición protegida debe extraer obligatoriamente el `salonId` del token para garantizar el aislamiento.
2. **Roles:** Manejar roles básicos (DUEÑO, ESTILISTA) según los permisos de la sección 2.
3. El secreto del JWT se lee desde la configuración (nunca quemado en código).
## Criterios de aceptación
- [ ] Peticiones sin token a rutas protegidas devuelven `401 Unauthorized`.
- [ ] Peticiones con rol incorrecto (ej. un estilista intentando ver configuración del salón) devuelven `403 Forbidden`.
- [ ] El login exitoso devuelve HTTP 200 y un JWT firmado que contiene el `salonId` y el `rol` en el payload.
- [ ] El filtro de seguridad intercepta el JWT, extrae el `salonId` y lo inyecta en el contexto de Spring Security para su uso en toda la petición.

# SPEC · Sillón · Fase 3: Catálogo y equipo: servicios y estilistas

## Problema
El salón existe y es seguro, pero no tiene nada que vender ni quién lo venda.

## Objetivo
1. Implementar el CRUD completo para la entidad `Servicio`.
2. Implementar el CRUD para la entidad `Estilista`.

## Fuera de alcance
- Citas, IA, Stripe, y vista pública (Fases 4 a 9).

## Reglas de Negocio
1. **Aislamiento:** Todo servicio y estilista creado se asocia automáticamente al `salonId` del usuario autenticado. Al consultar, solo se devuelven los del salón actual.
2. **Servicio:** Dura entre 15 y 480 minutos. Su precio es un número entero mayor que cero (400 Bad Request si no cumple).
3. **Estilista:** Un estilista ofrece uno o más servicios **de su mismo salón**. Si se envía el ID de un servicio de otro salón, la API responde `404 Not Found`.
4. **Límite del Plan GRATIS:** Al intentar crear el tercer estilista en un salón con plan GRATIS, la API responde `402 Payment Required`. En plan PRO no hay límite.
## Criterios de aceptación
- [ ] El CRUD de Servicios filtra y asocia correctamente por `salonId`.
- [ ] Intentar crear un servicio con precio 0 o duración fuera de 15-480 minutos devuelve `400 Bad Request`.
- [ ] Intentar asignar a un estilista un servicio perteneciente a un salón diferente devuelve `404 Not Found` (como si no existiera para no revelar datos).
- [ ] Registrar un tercer estilista en un salón con plan GRATIS devuelve `402 Payment Required`. Un salón PRO permite N estilistas.

# SPEC · Sillón · Fase 4: La agenda: citas, reglas, estados y disponibilidad

## Problema
El salón necesita registrar y gestionar los turnos internos respetando los horarios comerciales y evitando cruces entre las agendas de los estilistas.

## Objetivo
1. Implementar la entidad `Cita` y su lógica de agendamiento interno.
2. Construir la lógica de validación de disponibilidad y horarios.

## Fuera de alcance
- Endpoints públicos sin autenticación, IA y pagos (Fases 5 a 9).

## Reglas de Negocio
1. **Duración y Precio:** La cita dura exactamente lo que dura su servicio. Guarda el precio histórico en el momento de agendar.
2. **Validación de Horarios:** No se puede agendar en el pasado, fuera del horario del salón, un domingo si no abre, ni con un estilista que no ofrece el servicio (409 Conflict).
3. **Manejo del Tiempo:** "Hoy" y "ahora" se calculan con el reloj en `America/Bogota`.
4. **Prevención de Cruces:** Un estilista no puede tener dos citas superpuestas. Solo ocupan agenda las citas PENDIENTE y CONFIRMADA (409 Conflict).
5. **Transiciones de Estado:** PENDIENTE → CONFIRMADA o CANCELADA; CONFIRMADA → COMPLETADA, CANCELADA o NO_ASISTIO. (Estados finales: COMPLETADA, CANCELADA, NO_ASISTIO). Otras transiciones: 409 Conflict.
6. **Restricción de Estilista:** Un estilista solo puede cambiar sus propias citas, y solo a COMPLETADA o NO_ASISTIO (403 Forbidden).
## Criterios de aceptación
- [ ] Agendar en el pasado, fuera de horario, o en domingo (si el salón cierra) devuelve `409 Conflict`.
- [ ] Agendar en un bloque de tiempo que se cruza con una cita PENDIENTE o CONFIRMADA del mismo estilista devuelve `409 Conflict`.
- [ ] La cita guarda el valor histórico del servicio al momento de crearse.
- [ ] Transición inválida de estado (ej. COMPLETADA a CONFIRMADA) devuelve `409 Conflict`.
- [ ] Estilista intentando cambiar una cita de otro estilista recibe `404 Not Found`.
- [ ] Estilista intentando cambiar una cita a un estado distinto de COMPLETADA o NO_ASISTIO recibe `403 Forbidden`.

# SPEC · Sillón · Fase 5: La cara pública: catálogo, disponibilidad y reservas

## Problema
Los clientes necesitan ver qué ofrece un salón específico desde una URL pública (sin token JWT) y agendar citas por sí mismos.

## Objetivo
1. Endpoints públicos para listar servicios y disponibilidad usando el `slug` del salón.
2. Endpoint público para recibir reservas en línea.

## Fuera de alcance
- IA y pagos con Stripe (Fases 6 a 9).

## Reglas de Negocio
1. **Cálculo de Disponibilidad:** La disponibilidad la calcula **Java**, nunca la IA. Se retorna en intervalos calculados desde la apertura, donde el servicio quepa completo antes del cierre y no se cruce con otra cita.
2. **Origen de la Cita:** La cita agendada públicamente en línea nace con estado `PENDIENTE` y origen `EN_LINEA`.
3. **Creación de Cliente:** Al agendar, el cliente se busca por teléfono exacto en ese salón. Si no existe, se crea la entidad `Cliente`. El teléfono se guarda solo con dígitos y el signo `+`.
## Criterios de aceptación
- [ ] Consultar el catálogo por el `slug` del salón funciona sin enviar token JWT.
- [ ] La disponibilidad devuelve solo los intervalos válidos según el reloj del proyecto, la apertura/cierre, cruces y duración del servicio.
- [ ] La reserva pública se crea correctamente con origen `EN_LINEA` y estado `PENDIENTE`.
- [ ] Si el cliente no existe, se crea con el formato estricto de teléfono (+ y dígitos). Si ya existe, la cita se asocia a él.

# SPEC · Sillón · Fase 6: Pagos con Stripe: plan PRO, créditos y webhooks

## Problema
El dueño requiere monetizar su negocio escalando a PRO o comprando créditos adicionales para el uso de inteligencia artificial.

## Objetivo
1. Integración con Stripe Checkout (URLs configuradas desde variables).
2. Endpoint para procesar eventos Webhook desde Stripe.

## Fuera de alcance
- Lógica del chatbot y recordatorios IA (Fases 7 a 9)[cite: 5].

## Reglas de Negocio
1. **Control de Precios:** El backend define el precio. El cliente solo pide "plan PRO" o "paquete".
2. **Validación PRO:** Si el salón ya es PRO y pide suscripción, responde `409 Conflict`.
3. **Webhook Seguro:** Obligatorio verificar la firma de Stripe. Sin firma o firma falsa: `400 Bad Request`.
4. **Idempotencia:** Cada evento de Stripe se guarda por su ID. Si llega repetido, se ignora.
5. **Beneficios:** Pago de PRO suma 300 créditos y quita límite de estilistas. Pago de paquete suma 200 créditos. Si se cancela PRO, vuelve a GRATIS pero conserva créditos.
## Criterios de aceptación
- [ ] El cliente recibe la URL de checkout de Stripe generada desde el backend.
- [ ] Pedir el plan PRO cuando el salón ya es PRO devuelve `409 Conflict`.
- [ ] Un evento en el Webhook con firma inválida o ausente devuelve `400 Bad Request`.
- [ ] Recibir el mismo ID de evento de Stripe dos veces procesa los datos solo la primera vez (idempotencia).
- [ ] Un evento de pago de PRO exitoso actualiza el salón a plan PRO, suma 300 créditos y elimina la restricción de estilistas.
- [ ] Un evento de pago de paquete exitoso suma 200 créditos al saldo actual.

# SPEC · Sillón · Fase 7 y 8: Motor IA y Recordatorios

## Problema
Generar recordatorios personalizados para los clientes usando IA, garantizando que el sistema cobre por el servicio y no alucine datos.

## Objetivo
1. Crear cliente `RestClient` para invocar la API de Gemini (url dinámica).
2. Endpoint para solicitar recordatorio de una cita específica.

## Reglas de Negocio
1. **Créditos:** Todo llamado a IA cuesta 1 crédito. Si el salón tiene 0 créditos, responde `402 Payment Required` y la IA no se llama. El crédito se descuenta tras la respuesta.
2. **Datos exactos:** El recordatorio es solo para citas PENDIENTE o CONFIRMADA (409). Java recopila servicio, estilista, fecha, hora, valor y dirección; la IA solo redacta.
3. **JSON Estricto:** Se exige respuesta JSON validada. Si falla el parseo o faltan campos: `502 Bad Gateway`.
## Criterios de aceptación
- [ ] Solicitar la IA cuando el salón tiene 0 créditos devuelve `402 Payment Required` sin ejecutar la petición HTTP al proveedor.
- [ ] Se descuenta exactamente 1 crédito tras una respuesta exitosa de la IA.
- [ ] Pedir recordatorio para una cita que no está PENDIENTE o CONFIRMADA devuelve `409 Conflict`.
- [ ] Si la IA responde con un formato que no es JSON válido o le faltan las llaves esperadas, devuelve `502 Bad Gateway`.
- [ ] Si la API del proveedor de IA cae o no responde, devuelve `503 Service Unavailable`.

# SPEC · Sillón · Fase 9: Chatbot del salón con escalamiento

## Problema
Automatizar preguntas frecuentes (catálogo, horarios) mediante un chat, con mecanismo de seguridad para evitar estafas.

## Objetivo
1. Endpoint para interacción de chat cliente-salón.
2. Escalamiento de conversaciones a tickets/solicitudes.

## Reglas de Negocio
1. **Prohibido alterar negocio:** El chatbot solo lee el catálogo real de BD. No puede agendar ni cancelar citas directamente; para ello debe escalar.
2. **Regla Anti-Promesas (Java):** Si la respuesta generada por IA contiene menciones de descuentos, cupones, promociones o reembolsos gratis, se bloquea la respuesta. El cliente recibe un texto fijo de seguridad y el caso se escala con el motivo `REGLA_DE_SEGURIDAD`.
## Criterios de aceptación
- [ ] El chatbot responde preguntas de catálogo leyendo la base de datos a través de Java (no alucina servicios).
- [ ] Si la respuesta de la IA menciona "descuento", "gratis", "promoción" o "reembolso", la regla en Java bloquea el mensaje, retorna un mensaje seguro predefinido y escala la conversación creando un ticket (`REGLA_DE_SEGURIDAD`).
- [ ] El chatbot no puede agendar ni alterar citas directamente.