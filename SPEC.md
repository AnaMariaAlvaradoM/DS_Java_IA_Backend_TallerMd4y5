# SPEC · Sillón

## 1. El problema
Las barberías y salones de belleza agendan por WhatsApp, en cuaderno o de memoria. Se cruzan citas del mismo estilista, se pierden reservas, los clientes olvidan la cita y la silla queda vacía, y nadie responde preguntas de precios y horarios fuera de hora.

## 2. La solución
Sillón es un SaaS multi-inquilino: una sola API y una sola base de datos atienden a muchos salones, y cada dato pertenece a un salón. Ningún salón ve los datos de otro.

- Los clientes reservan en línea viendo solo horarios realmente libres.
- El sistema no permite citas cruzadas ni fuera del horario.
- El dueño genera recordatorios con IA listos para WhatsApp (una persona los revisa y envía).
- Un asistente virtual responde precios y horarios y escala a una persona lo que no puede resolver.

## 3. Roles
| Rol | Quién es | Qué hace |
|---|---|---|
| DUENO | Dueño del salón (Camilo) | Registra el salón, configura, crea servicios y estilistas, agenda, paga, usa la IA |
| ESTILISTA | Mateo, Sara | Ve sus citas y las marca COMPLETADA o NO_ASISTIO |
| ADMIN | Equipo de Sillón | Ve todos los salones |
| Público | Clientes finales | Ven el catálogo, la disponibilidad, reservan y chatean sin cuenta |

### Permisos
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
| Cambiar estado de una cita | No | Cualquier transición válida | Solo las suyas, a COMPLETADA o NO_ASISTIO | No |
| Pagar PRO o créditos, ver solicitudes | No | Sí | No | No |
| Ver todos los salones | No | No | No | Sí |

## 4. Planes
| Producto | Pago en Stripe | Qué da |
|---|---|---|
| GRATIS | Ninguno (al registrarse) | Máximo 2 estilistas, 10 créditos de IA |
| PRO | Suscripción mensual (15 USD) | Estilistas sin límite, +300 créditos |
| Paquete de créditos | Pago único (5 USD) | +200 créditos |

Un crédito = una respuesta de la IA.

## 5. Modelo de datos
| Entidad (tabla) | Atributos | Relaciones |
|---|---|---|
| Salon (salones) | id, nombre, slug (único), ciudad, direccion, telefono, plan, creditosIa, stripeSuscripcionId, configuracion, fechaCreacion | Tiene usuarios, servicios, clientes, citas y solicitudes |
| ConfiguracionSalon (@Embeddable) | horaApertura, horaCierre, abreDomingos, intervaloMinutos | Parte del salón |
| Usuario (usuarios) | id, nombre, email (único), password (BCrypt), rol, activo | ManyToOne Salon; ManyToMany Servicio (estilista_servicios) |
| Servicio (servicios) | id, nombre, descripcion, duracionMinutos, precio, activo | ManyToOne Salon |
| Cliente (clientes) | id, nombre, telefono | ManyToOne Salon; teléfono único por salón |
| Cita (citas) | id, inicio, fin, precio, estado, origen, nota, fechaCreacion, fechaActualizacion | ManyToOne Salon, Cliente, Usuario (estilista), Servicio |
| SolicitudContacto (solicitudes_contacto) | id, clienteNombre, clienteContacto, mensajes, motivo, estado, fechaCreacion | ManyToOne Salon |
| EventoStripe (eventos_stripe) | id (id del evento de Stripe), tipo, fechaProcesado | Ninguna |

### Enums (EnumType.STRING, sin tildes)
- Plan: GRATIS, PRO
- Rol: DUENO, ESTILISTA, ADMIN
- EstadoCita: PENDIENTE, CONFIRMADA, COMPLETADA, CANCELADA, NO_ASISTIO
- OrigenCita: SALON, EN_LINEA
- TipoCompra: SUSCRIPCION_PRO, PAQUETE_CREDITOS
- MotivoSolicitud: PEDIDO_POR_IA, REGLA_DE_SEGURIDAD
- EstadoSolicitud: PENDIENTE, ATENDIDA

## 6. Reglas de negocio

### Salones y cuentas
1. Aislamiento: todo dato pertenece a un salón. Si un usuario intenta ver o modificar datos de otro salón, recibe 404, como si el dato no existiera.
2. El registro crea el salón y su dueño juntos: plan GRATIS, 10 créditos, horario por defecto de 8:00 a 19:00, sin domingos, intervalo de 30 minutos.
3. El correo es único en toda la plataforma, también para estilistas: 409.
4. Slug: sale del nombre, en minúsculas, sin tildes ni espacios. Si ya existe se agrega -2, -3...
5. Horario: la apertura debe ser antes del cierre (409). Intervalo de 15 a 60 minutos (400).

### Catálogo y equipo
1. Un servicio dura de 15 a 480 minutos y cuesta un entero mayor que cero (400).
2. Un estilista ofrece uno o más servicios de su mismo salón. Servicio de otro salón: 404.
3. En plan GRATIS, el tercer estilista responde 402. En PRO no hay límite.
4. Orden al crear un estilista: primero el límite del plan (402), luego el correo (409), luego los servicios (404). El límite va primero porque si el salón no puede tener más estilistas, no tiene sentido validar lo demás.

### Agenda
1. La cita dura lo que dura su servicio: el sistema calcula el fin.
2. La cita guarda el precio del servicio al momento de agendar.
3. No se agenda en el pasado, fuera del horario, en domingo si el salón no abre, ni con un estilista que no ofrece el servicio: 409.
4. Sin cruces: un estilista no puede tener dos citas que ocupen agenda y se crucen (409). Ocupan agenda PENDIENTE y CONFIRMADA. Dos citas se cruzan si inicioA < finB y finA > inicioB.
5. La cita creada por el salón nace CONFIRMADA (origen SALON). La reserva en línea nace PENDIENTE (origen EN_LINEA).
6. El cliente se busca por teléfono dentro del salón; si no existe se crea. El teléfono se guarda solo con dígitos y +.
7. Transiciones:

| Desde | Hacia | Caso real |
|---|---|---|
| PENDIENTE | CONFIRMADA | El salón acepta la reserva en línea |
| PENDIENTE | CANCELADA | El cliente ya no puede venir |
| CONFIRMADA | COMPLETADA | Se atendió al cliente |
| CONFIRMADA | CANCELADA | Se canceló con anticipación |
| CONFIRMADA | NO_ASISTIO | El cliente nunca llegó |

COMPLETADA, CANCELADA y NO_ASISTIO son finales. Cualquier otra transición: 409.

8. Un estilista solo cambia sus citas (la de otro: 404) y solo a COMPLETADA o NO_ASISTIO (otra: 403).
9. La disponibilidad la calcula Java: horas de inicio cada intervalo desde la apertura, donde el servicio cabe completo antes del cierre, sin cruzarse con otra cita del estilista y nunca en el pasado.
10. "Hoy" y "ahora" se calculan con la zona horaria de Colombia (America/Bogota).

### Pagos
1. El backend decide el precio: el cliente solo pide "plan PRO" o "paquete de créditos".
2. Si el salón ya está en PRO, pedir la suscripción otra vez responde 409.
3. El webhook verifica la firma de Stripe. Sin firma o con firma falsa: 400.
4. Cada evento de Stripe se procesa una sola vez (se guarda su id).
5. PRO pagado: el salón pasa a PRO, guarda el id de la suscripción y suma 300 créditos. Paquete pagado: suma 200. Suscripción cancelada: vuelve a GRATIS y conserva sus créditos.

### Inteligencia artificial
1. Un crédito por respuesta. Sin créditos: 402 y la IA no se llama. El crédito se descuenta cuando la IA responde.
2. Recordatorio solo para citas PENDIENTE o CONFIRMADA (otra: 409). Java arma los datos verificados; la IA solo redacta. Una persona lo aprueba: Sillón no lo envía.
3. El chatbot responde con el catálogo real del salón. No agenda, no cambia ni cancela citas: en esos casos escala y crea una solicitud de contacto.
4. Regla anti-promesas en Java: si la respuesta menciona descuentos, cupones, algo gratis, promociones o reembolsos, el cliente recibe un mensaje seguro fijo y se escala con motivo REGLA_DE_SEGURIDAD.
5. Toda respuesta de la IA se pide en JSON y se valida. JSON inválido o incompleto: 502. Ningún proveedor responde: 503.
6. Las llaves viven en secrets.properties o en variables de entorno. Nunca en el código, en Git ni en un prompt.

## 7. Contrato de la API
Fechas AAAA-MM-DD. Fecha y hora AAAA-MM-DDTHH:mm:ss. Horas HH:mm. Precios enteros en pesos.

### Formato único de error
`{ codigo, error, mensaje, fecha }` para todos los errores, incluidos 401 y 403.

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos, JSON mal formado, parámetro faltante, firma de webhook inválida |
| 401 | Sin token, token inválido o credenciales incorrectas |
| 402 | Límite del plan GRATIS o sin créditos de IA |
| 403 | El rol no tiene permiso, o un estilista intenta una transición que no le corresponde |
| 404 | No existe o pertenece a otro salón |
| 409 | Choca con una regla de negocio |
| 502 | Stripe o la IA respondieron algo inusable |
| 503 | Ningún proveedor de IA disponible |

### Endpoints
| Método y ruta | Rol | Recibe | Responde |
|---|---|---|---|
| POST /api/auth/registro | Público | salonNombre, ciudad, direccion, telefono, nombre, email, password | 201 token, tipo, expiraEnMinutos, usuarioId, nombre, rol, salonId, salonSlug · 400 · 409 |
| POST /api/auth/login | Público | email, password | 200 mismo objeto · 401 |
| GET /api/salon | DUENO | — | 200 id, nombre, slug, ciudad, direccion, telefono, plan, creditosIa, configuracion, fechaCreacion |
| PUT /api/salon/configuracion | DUENO | horaApertura, horaCierre, abreDomingos, intervaloMinutos | 200 · 400 · 409 |
| POST /api/servicios | DUENO | nombre, descripcion, duracionMinutos, precio, activo | 201 · 400 |
| GET /api/servicios | DUENO, ESTILISTA | — | 200 lista |
| PUT /api/servicios/{id} | DUENO | igual al POST | 200 · 400 · 404 |
| POST /api/estilistas | DUENO | nombre, email, password, servicioIds | 201 id, nombre, email, activo, servicios · 400 · 402 · 404 · 409 |
| GET /api/estilistas | DUENO | — | 200 lista |
| GET /api/clientes | DUENO | — | 200 id, nombre, telefono |
| POST /api/citas | DUENO | clienteNombre, clienteTelefono, servicioId, estilistaId, inicio, nota | 201 · 400 · 404 · 409 |
| GET /api/citas?fecha= | DUENO, ESTILISTA | fecha opcional | 200 citas del día ordenadas |
| PATCH /api/citas/{id}/estado | DUENO, ESTILISTA | estado | 200 · 400 · 403 · 404 · 409 |
| POST /api/citas/{id}/recordatorio | DUENO | — | 200 citaId, telefonoCliente, mensaje, creditosRestantes, factura · 402 · 404 · 409 · 502 · 503 |
| GET /api/publico/salones/{slug} | Público | — | 200 nombre, slug, ciudad, direccion, telefono, horario, servicios, estilistas · 404 |
| GET /api/publico/salones/{slug}/disponibilidad | Público | servicioId, estilistaId, fecha | 200 fecha, servicio, estilista, duracionMinutos, horariosLibres · 400 · 404 · 409 |
| POST /api/publico/salones/{slug}/reservas | Público | igual a POST /api/citas | 201 PENDIENTE, EN_LINEA · 400 · 404 · 409 |
| POST /api/publico/salones/{slug}/chat | Público | clienteNombre, clienteContacto, historial (1 a 20 turnos) | 200 respuesta, escalado, solicitudId, factura · 400 · 402 · 404 · 502 · 503 |
| GET /api/solicitudes | DUENO | — | 200 lista, más recientes primero |
| PATCH /api/solicitudes/{id}/atendida | DUENO | — | 200 · 404 · 409 |
| POST /api/pagos/suscripcion | DUENO | — | 200 tipo, sesionId, urlPago · 409 · 502 |
| POST /api/pagos/creditos | DUENO | — | 200 tipo, sesionId, urlPago · 502 |
| POST /api/pagos/webhook | Stripe | evento + Stripe-Signature | 200 recibido, procesado · 400 |
| GET /api/pagos/resultado?estado= | Público | exito o cancelado | 200 mensaje |
| GET /api/admin/salones | ADMIN | — | 200 todos los salones · 403 |

La cita se devuelve con: id, inicio, fin, estado, origen, precio, servicioId, servicioNombre, estilistaId, estilistaNombre, clienteNombre, clienteTelefono, nota, fechaCreacion, fechaActualizacion.

La factura de la IA tiene: proveedor, modelo, tokensEntrada, tokensSalida, costoEstimadoUsd, milisegundos.

## 8. Fuera de alcance
- Enviar mensajes reales por WhatsApp o correo.
- Recordatorios automáticos programados y reportes.
- Pagos del cliente final al salón.
- Interfaz gráfica.
