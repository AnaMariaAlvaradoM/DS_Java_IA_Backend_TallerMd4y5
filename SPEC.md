# SPEC · Sillón

Agenda para barberías y salones de belleza. API REST en Java con Spring Boot, pagos con Stripe e IA.

## 1. El problema

Una barbería agenda por WhatsApp, en un cuaderno o de memoria. Eso causa tres pérdidas:

- Citas cruzadas o fuera de horario, porque nadie controla la agenda.
- Sillas vacías, porque el cliente olvida la cita y nadie se la recordó.
- Clientes que no vuelven, porque preguntan precios y horarios y nadie responde a tiempo.

Sillón es un SaaS multi-inquilino: una sola aplicación y una sola base de datos atienden a muchos salones, y cada salón solo ve sus propios datos.

## 2. Roles y planes

| Rol | Qué puede hacer |
|-----|-----------------|
| Público (sin cuenta) | Ver catálogo y disponibilidad, reservar en línea y chatear con el asistente. |
| DUENO | Configurar su salón, crear servicios y estilistas, ver toda la agenda, crear citas y cambiar su estado, pedir recordatorios con IA, pagar el plan o créditos, y ver solicitudes del chatbot. |
| ESTILISTA | Ver solo su agenda y marcar sus citas como COMPLETADA o NO_ASISTIO. |
| ADMIN | Ver todos los salones de la plataforma. |

| Plan | Estilistas | Créditos de IA |
|------|-----------|----------------|
| GRATIS | Máximo 2 | 10 al registrarse |
| PRO (suscripción mensual) | Sin límite | +300 al activarse |
| Paquete de créditos (pago único) | — | +200 |

Un crédito equivale a una respuesta de la IA.

## 3. Reglas de negocio

### Salones y cuentas

1. Aislamiento: todo dato pertenece a un salón. Si alguien pide un dato de otro salón, responde 404, como si no existiera.
2. El registro crea el salón y su dueño a la vez, en plan GRATIS, con 10 créditos y el horario por defecto (8:00 a 19:00, sin domingos, intervalo de 30 minutos).
3. El correo es único en toda la plataforma. Si se repite: 409.
4. El slug se genera desde el nombre: minúsculas, sin tildes, con guiones. Si ya existe, se agrega -2, -3 y así.
5. La hora de apertura debe ser anterior a la de cierre (409). El intervalo va de 15 a 60 minutos (400).

### Catálogo y equipo

1. Un servicio dura de 15 a 480 minutos y su precio es un entero mayor que cero (400 si no).
2. Un estilista ofrece uno o más servicios de su mismo salón. Si envían un servicio de otro salón: 404.
3. En plan GRATIS, crear el tercer estilista responde 402. En PRO no hay límite.

### Agenda

1. La cita dura lo que dura su servicio: el sistema calcula la hora de fin.
2. La cita guarda el precio del servicio al momento de agendar. Si el precio cambia después, la cita conserva el suyo.
3. Responde 409 si la cita es en el pasado, fuera del horario, en domingo con el salón cerrado, o con un estilista que no ofrece ese servicio.
4. Sin cruces: un estilista no puede tener dos citas PENDIENTE o CONFIRMADA que se solapen (409). Dos citas se cruzan si el inicio de una es anterior al fin de la otra y el fin de la primera es posterior al inicio de la segunda. Una cita que termina a las 10:45 no se cruza con otra que empieza a las 10:45.
5. La cita creada por el salón nace CONFIRMADA con origen SALON. La reservada en línea nace PENDIENTE con origen EN_LINEA.
6. El cliente se busca por teléfono dentro del salón; si no existe, se crea. El teléfono se guarda solo con dígitos y el signo +.
7. Transiciones de estado permitidas:
    - PENDIENTE -> CONFIRMADA o CANCELADA
    - CONFIRMADA -> COMPLETADA, CANCELADA o NO_ASISTIO
    - COMPLETADA, CANCELADA y NO_ASISTIO son finales.
    - Cualquier otra: 409.
8. El estilista solo cambia sus propias citas (la de otro: 404) y solo a COMPLETADA o NO_ASISTIO (otro estado: 403).
9. La disponibilidad la calcula Java, nunca la IA. Son las horas de inicio, cada intervalo desde la apertura, en las que el servicio cabe completo antes del cierre, sin cruzarse con otra cita del estilista y nunca en el pasado.
10. "Hoy" y "ahora" se calculan con la zona horaria America/Bogota, aunque el servidor esté en UTC.

### Pagos

1. El backend decide el precio. El cliente solo pide "plan PRO" o "paquete de créditos".
2. Si el salón ya es PRO, pedir de nuevo la suscripción responde 409.
3. El webhook verifica la firma de Stripe. Sin firma o con firma falsa: 400.
4. Cada evento se procesa una sola vez: se guarda su id y, si llega repetido, se ignora.
5. Pago PRO confirmado: el salón pasa a PRO, guarda el id de la suscripción y suma 300 créditos. Paquete pagado: suma 200. Suscripción cancelada: el salón vuelve a GRATIS y conserva sus créditos.

### Inteligencia artificial

1. Antes de llamar a la IA se verifica que haya créditos. Si hay 0: 402 y la IA no se llama.
2. El crédito se descuenta cuando la IA responde, antes de validar el JSON. Si el JSON resulta inválido (502), el crédito ya se gastó, porque los tokens ya se consumieron.
3. Recordatorio: solo para citas PENDIENTE o CONFIRMADA (otra: 409). Java arma los datos verificados (servicio, estilista, fecha, hora, valor y dirección) y la IA solo redacta el mensaje. Una persona lo aprueba: Sillón no lo envía.
4. Chatbot: responde con el catálogo real del salón, tomado de la base de datos. No agenda, no cambia ni cancela citas: en esos casos escala. Al escalar crea una solicitud de contacto con los mensajes del cliente y motivo PEDIDO_POR_IA.
5. Regla anti-promesas en Java: si la respuesta del chatbot menciona descuento, cupón, gratis, promoción o reembolso, el cliente recibe un mensaje seguro fijo y se crea la solicitud con motivo REGLA_DE_SEGURIDAD. Se acepta bloquear de más antes que prometer algo indebido.
6. Toda respuesta de la IA se pide en JSON y se valida. Si no es JSON válido o le faltan datos: 502. Si ningún proveedor responde: 503. El proveedor principal es Gemini y el de respaldo tiene formato Claude.
7. Las llaves viven en secrets.properties (local) o en variables de entorno (Render). Nunca en el código, en Git ni en un prompt.

## 4. Contrato de la API

### Convenciones

- Fechas: `AAAA-MM-DD`. Fecha y hora: `AAAA-MM-DDTHH:mm:ss`. Horas al enviar: `HH:mm`.
- Precios: enteros en pesos colombianos, sin puntos ni decimales (35000).
- Las rutas no públicas reciben el token en la cabecera `Authorization: Bearer <token>`.

### Formato único de error (incluye 401 y 403)

```json
{
  "codigo": 409,
  "error": "Conflict",
  "mensaje": "Explicación en español",
  "fecha": "2026-10-27T10:30:00"
}
```

En errores de validación, `mensaje` trae los campos que fallaron, separados por punto y coma.

| Código | Cuándo |
|--------|--------|
| 400 | Datos inválidos, JSON mal formado, parámetro faltante, firma de webhook inválida |
| 401 | Sin token, token inválido o credenciales incorrectas |
| 402 | Límite del plan GRATIS o salón sin créditos de IA |
| 403 | El rol no tiene permiso, o el estilista intenta una transición que no le corresponde |
| 404 | No existe, o pertenece a otro salón |
| 409 | Choca con una regla de negocio |
| 502 | Stripe o la IA respondieron algo inutilizable |
| 503 | Ningún proveedor de IA disponible |

### Rutas

**Autenticación (público)**

- `POST /api/auth/registro`: recibe `salonNombre`, `ciudad`, `direccion`, `telefono`, `nombre`, `email`, `password` (8 a 72). Responde 201 con `token`, `tipo` ("Bearer"), `expiraEnMinutos`, `usuarioId`, `nombre`, `rol`, `salonId`, `salonSlug`. Errores: 400, 409.
- `POST /api/auth/login`: recibe `email`, `password`. Responde 200 con el mismo objeto. Error: 401 (mensaje genérico).

**Mi salón (DUENO)**

- `GET /api/salon`: 200 con `id`, `nombre`, `slug`, `ciudad`, `direccion`, `telefono`, `plan`, `creditosIa`, `configuracion` (`horaApertura`, `horaCierre`, `abreDomingos`, `intervaloMinutos`), `fechaCreacion`.
- `PUT /api/salon/configuracion`: recibe los cuatro campos de configuración, todos obligatorios. Responde 200 con el salón. Errores: 400, 409.

**Servicios**

- `POST /api/servicios` (DUENO): recibe `nombre` (máx. 80), `descripcion` (opcional, máx. 300), `duracionMinutos`, `precio`, `activo` (opcional, por defecto true). Responde 201. Error: 400.
- `GET /api/servicios` (DUENO, ESTILISTA): 200 con la lista del salón.
- `PUT /api/servicios/{id}` (DUENO): igual que el POST. Responde 200. Errores: 400, 404.

**Estilistas y clientes (DUENO)**

- `POST /api/estilistas`: recibe `nombre`, `email`, `password` (8 a 72), `servicioIds` (al menos uno). Responde 201 con `id`, `nombre`, `email`, `activo`, `servicios` (lista de nombres). Errores: 400, 402, 404, 409.
- `GET /api/estilistas`: 200 con la lista.
- `GET /api/clientes`: 200 con `id`, `nombre`, `telefono` de cada cliente.

**Agenda**

- `POST /api/citas` (DUENO): recibe `clienteNombre`, `clienteTelefono` (7 a 30 caracteres), `servicioId`, `estilistaId`, `inicio`, `nota` (opcional, máx. 300). Responde 201 con la cita. Errores: 400, 404, 409.
- `GET /api/citas?fecha=` (DUENO, ESTILISTA): la fecha es opcional (por defecto hoy). Responde 200 ordenado por hora. El estilista solo ve las suyas.
- `PATCH /api/citas/{id}/estado` (DUENO, ESTILISTA): recibe `estado`. Responde 200. Errores: 400, 403, 404, 409.
- `POST /api/citas/{id}/recordatorio` (DUENO): responde 200 con `citaId`, `telefonoCliente`, `mensaje`, `creditosRestantes`, `factura`. Errores: 402, 404, 409, 502, 503.

La cita se devuelve con: `id`, `inicio`, `fin`, `estado`, `origen`, `precio`, `servicioId`, `servicioNombre`, `estilistaId`, `estilistaNombre`, `clienteNombre`, `clienteTelefono`, `nota`, `fechaCreacion`, `fechaActualizacion`.

La factura de la IA tiene: `proveedor`, `modelo`, `tokensEntrada`, `tokensSalida`, `costoEstimadoUsd`, `milisegundos`.

**Público (sin token), base `/api/publico/salones/{slug}`**

- `GET /api/publico/salones/{slug}`: 200 con `nombre`, `slug`, `ciudad`, `direccion`, `telefono`, `horario` (texto), `servicios` (solo activos), `estilistas` (`id`, `nombre`, `servicioIds`). Nunca correos. Error: 404.
- `GET .../disponibilidad?servicioId=&estilistaId=&fecha=`: los tres parámetros son obligatorios. Responde 200 con `fecha`, `servicio`, `estilista`, `duracionMinutos`, `horariosLibres`. Errores: 400, 404, 409.
- `POST .../reservas`: igual que `POST /api/citas`. Responde 201 con la cita PENDIENTE y origen EN_LINEA. Errores: 400, 404, 409.
- `POST .../chat`: recibe `clienteNombre`, `clienteContacto` (máx. 120), `historial` (de 1 a 20 turnos, cada uno con `rol` CLIENTE o ASISTENTE y `texto` máx. 500). Responde 200 con `respuesta`, `escalado`, `solicitudId` (o null), `factura`. Errores: 400, 402, 404, 502, 503.

**Solicitudes del chatbot (DUENO)**

- `GET /api/solicitudes`: 200 con `id`, `clienteNombre`, `clienteContacto`, `mensajes`, `motivo`, `estado`, `fechaCreacion`, las más recientes primero.
- `PATCH /api/solicitudes/{id}/atendida`: 200. Errores: 404, 409 si ya estaba atendida.

**Pagos**

- `POST /api/pagos/suscripcion` (DUENO): 200 con `tipo` (SUSCRIPCION_PRO), `sesionId`, `urlPago`. Errores: 409 si ya es PRO, 502.
- `POST /api/pagos/creditos` (DUENO): 200 con `tipo` (PAQUETE_CREDITOS), `sesionId`, `urlPago`. Error: 502.
- `POST /api/pagos/webhook` (Stripe): recibe el evento y la cabecera `Stripe-Signature`. Responde 200 con `recibido` y `procesado`. Error: 400 si la firma no es válida.
- `GET /api/pagos/resultado?estado=` (público): `exito` o `cancelado`. Responde 200 con un mensaje.

**Administración (ADMIN)**

- `GET /api/admin/salones`: 200 con todos los salones. Cualquier otro rol: 403.

## 5. Fuera de alcance

- Enviar mensajes reales por WhatsApp o correo: una persona aprueba cada recordatorio y lo envía por su cuenta.
- Recordatorios automáticos programados y reportes.
- Pagos del cliente final al salón, que exigirían Stripe Connect.
- Interfaz gráfica: el cliente de la API es Postman.