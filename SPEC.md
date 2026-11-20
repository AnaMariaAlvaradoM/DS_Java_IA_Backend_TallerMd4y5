# Sillón · Especificación de trabajo

Fuente de requisitos: enunciado «Sillón · Proyecto integrador · Módulos 4 y 5». El contrato de la sección 4 del enunciado prevalece sobre este resumen. No se cambian nombres de rutas, campos ni códigos HTTP sin contrastarlos con él.

## Problema y producto

Las reservas por mensajes o cuaderno producen cruces de horario, espacios vacíos y respuestas tardías a clientes. Sillón ofrece una API SaaS para que cada barbería o salón gestione equipo, servicios, clientes y citas; el público puede consultar horarios libres y reservar sin cuenta.

## Actores y límites

- `DUENO`: administra su salón, catálogo, equipo, citas, créditos y pagos.
- `ESTILISTA`: consulta su agenda y marca sus citas como completadas o no asistidas.
- Público: consulta catálogo y disponibilidad, reserva y conversa con el chatbot.
- `ADMIN`: consulta los salones de la plataforma.
- Cada dato de negocio pertenece a un salón. El acceso de otro salón se responde con 404; las listas se filtran por salón.

## Planes

- Registro: plan `GRATIS`, 10 créditos de IA, máximo 2 estilistas, horario 08:00–19:00, domingos cerrados e intervalo de 30 minutos.
- `PRO`: estilistas sin límite y 300 créditos adicionales al confirmarse el pago de suscripción.
- Paquete de créditos: 200 créditos adicionales tras un pago único confirmado.
- Un crédito corresponde a una respuesta válida de IA. Sin saldo se responde 402 antes de llamarla.

## Reglas centrales

- Correo único en la plataforma; slug único derivado del nombre del salón.
- Apertura anterior al cierre; intervalo entre 15 y 60 minutos. Servicio de 15 a 480 minutos y precio entero positivo en pesos colombianos.
- Un estilista ofrece al menos un servicio activo de su salón. El tercer estilista en `GRATIS` recibe 402.
- La duración de una cita es la del servicio y su precio queda congelado al crearla. No se permiten citas en el pasado, fuera de horario, en domingo cerrado, con un estilista no habilitado o cruzadas con citas pendientes/confirmadas.
- Una cita interna nace `CONFIRMADA`, origen `SALON`; una reserva pública nace `PENDIENTE`, origen `EN_LINEA`.
- Transiciones: `PENDIENTE` a `CONFIRMADA` o `CANCELADA`; `CONFIRMADA` a `COMPLETADA`, `CANCELADA` o `NO_ASISTIO`. Los demás estados son finales. El estilista solo puede marcar sus citas `COMPLETADA` o `NO_ASISTIO`.
- La disponibilidad la calcula Java con la zona `America/Bogota`; el inicio cabe por completo en el horario y no cruza una cita que ocupa agenda.
- El backend decide precios de Stripe. Webhooks con firma válida e idempotencia por ID de evento. Suscripción cancelada devuelve al salón a `GRATIS` sin quitar créditos.
- Java aporta datos verificados para recordatorios; la IA redacta y una persona decide enviarlos. El chatbot usa el catálogo real, escala solicitudes de reserva o cambio y bloquea en Java promesas de descuentos, cupones, promociones, gratuidad y reembolsos.
- Respuestas de IA en JSON validado. JSON inválido: 502; proveedores indisponibles: 503.

## Contrato de API

- `/api/auth`: `POST /registro`, `POST /login`.
- `/api/salon`: `GET /`, `PUT /configuracion`.
- `/api/servicios`: `POST /`, `GET /`, `PUT /{id}`.
- `/api/estilistas`: `POST /`, `GET /`; `/api/clientes`: `GET /`.
- `/api/citas`: `POST /`, `GET /?fecha=`, `PATCH /{id}/estado`, `POST /{id}/recordatorio`.
- `/api/publico/salones/{slug}`: `GET /`, `GET /disponibilidad`, `POST /reservas`, `POST /chat`.
- `/api/solicitudes`: `GET /`, `PATCH /{id}/atendida`.
- `/api/pagos`: `POST /suscripcion`, `POST /creditos`, `POST /webhook`, `GET /resultado`.
- `/api/admin/salones`: `GET /`.
- Todos los errores usan `codigo`, `error`, `mensaje`, `fecha`, incluidos 401 y 403.

## Fuera de alcance

No habrá interfaz gráfica, envío real de WhatsApp o correo, recordatorios programados, reportes ni pagos del cliente final al salón.
