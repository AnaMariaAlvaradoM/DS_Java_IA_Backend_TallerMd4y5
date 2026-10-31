# SPEC - Sillon

## Problema
Las barberias y salones agendan por WhatsApp o cuaderno: citas cruzadas, sin recordatorios, sin respuesta fuera de horario. Sillon es un SaaS multi-inquilino: cada salon es un inquilino y nunca ve datos de otro.

## Roles
- DUENO: configura el salon, servicios, estilistas, agenda, pagos, recordatorios IA y solicitudes.
- ESTILISTA: ve solo su agenda y marca sus citas COMPLETADA o NO_ASISTIO.
- ADMIN: ve todos los salones.
- Publico (sin cuenta): ve catalogo, disponibilidad, reserva y chatea.

## Planes
- GRATIS: maximo 2 estilistas, 10 creditos de IA al registrarse.
- PRO (suscripcion mensual): estilistas sin limite, +300 creditos.
- Paquete de creditos (pago unico): +200 creditos.
- Un credito = una respuesta de la IA.

## Reglas de negocio
### Salones y cuentas
1. Aislamiento: todo dato pertenece a un salon; si se intenta acceder a datos de otro, responde 404.
2. Registro crea salon y dueno juntos: plan GRATIS, 10 creditos, horario 8:00-19:00, sin domingos, intervalo 30 min.
3. Correo unico en toda la plataforma (409).
4. Slug: del nombre, minusculas, sin tildes ni espacios; si existe se agrega -2, -3...
5. Apertura debe ser antes del cierre (409). Intervalo entre 15 y 60 (400).

### Catalogo y equipo
1. Servicio: 15 a 480 minutos, precio entero mayor que 0 (400).
2. Un estilista ofrece servicios de su mismo salon; si son de otro, 404.
3. Orden de validacion al crear estilista: limite del plan (402), correo unico (409), servicios del salon (404). El limite va primero porque no tiene sentido validar datos de una operacion que el plan no permite.

### Agenda
1. Fin de la cita = inicio + duracion del servicio.
2. La cita guarda el precio del servicio al momento de agendar.
3. 409 si: esta en el pasado, fuera del horario, domingo cerrado, o el estilista no ofrece el servicio.
4. Sin cruces para el mismo estilista entre citas PENDIENTE y CONFIRMADA (409). Una cita que termina a las 10:45 no se cruza con otra que empieza a las 10:45.
5. Cita del salon nace CONFIRMADA (origen SALON); reserva en linea nace PENDIENTE (origen EN_LINEA).
6. El cliente se busca por telefono dentro del salon; si no existe, se crea. Telefono solo con digitos y +.
7. Transiciones: PENDIENTE a CONFIRMADA o CANCELADA; CONFIRMADA a COMPLETADA, CANCELADA o NO_ASISTIO. Los demas estados son finales. Otra transicion: 409.
8. Estilista: solo sus citas (otra: 404) y solo a COMPLETADA o NO_ASISTIO (otra cosa: 403).
9. La disponibilidad la calcula Java, nunca la IA.
10. Hoy y ahora se calculan con la zona America/Bogota (Clock), no con la del servidor.

### Pagos
1. El backend decide el precio; el cliente solo pide PRO o paquete.
2. Si ya es PRO, pedir la suscripcion responde 409.
3. El webhook verifica la firma de Stripe (400 si falta o es falsa).
4. Cada evento se procesa una sola vez (se guarda su id).
5. Pago PRO: plan PRO, guarda id de suscripcion, +300 creditos. Paquete: +200. Suscripcion cancelada: vuelve a GRATIS y conserva creditos.

### Inteligencia artificial
1. Se verifica credito antes de llamar a la IA (402 si no hay) y se descuenta cuando la IA responde.
2. Recordatorio: solo citas PENDIENTE o CONFIRMADA (409). Java arma los datos; la IA solo redacta; una persona lo aprueba.
3. Chatbot: responde con el catalogo real; no agenda ni cancela, escala y crea solicitud de contacto.
4. Regla anti-promesas en Java: si menciona descuento, cupon, gratis, promocion o reembolso, se envia mensaje seguro y se escala con motivo REGLA_DE_SEGURIDAD.
5. Respuestas de la IA en JSON validado: JSON invalido 502; ningun proveedor responde 503.
6. Las llaves viven en secrets.properties o variables de entorno; nunca en codigo ni en Git.

## Contrato de la API (22 rutas)
- POST /api/auth/registro, POST /api/auth/login
- GET /api/salon, PUT /api/salon/configuracion
- POST /api/servicios, GET /api/servicios, PUT /api/servicios/{id}
- POST /api/estilistas, GET /api/estilistas, GET /api/clientes
- POST /api/citas, GET /api/citas?fecha=, PATCH /api/citas/{id}/estado, POST /api/citas/{id}/recordatorio
- GET /api/publico/salones/{slug}, GET .../disponibilidad, POST .../reservas, POST .../chat
- GET /api/solicitudes, PATCH /api/solicitudes/{id}/atendida
- POST /api/pagos/suscripcion, POST /api/pagos/creditos, POST /api/pagos/webhook, GET /api/pagos/resultado?estado=
- GET /api/admin/salones

Formato unico de error: codigo, error, mensaje, fecha.

## Fuera de alcance
Mensajes reales por WhatsApp o correo, recordatorios automaticos, reportes, pagos del cliente final al salon, interfaz grafica.
