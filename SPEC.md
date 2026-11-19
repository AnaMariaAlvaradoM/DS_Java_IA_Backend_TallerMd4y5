# SPEC · Sillón

## Problema
Las barberías agendan por WhatsApp o cuaderno: citas cruzadas, clientes que olvidan, nadie responde preguntas. Sillón es un SaaS multi-inquilino (cada salón es un inquilino) con agenda, pagos, recordatorios y chatbot con IA.

## Roles
- DUENO: configura el salón, servicios, estilistas, crea citas, paga, usa IA, ve solicitudes.
- ESTILISTA: ve solo su agenda; marca sus citas COMPLETADA o NO_ASISTIO.
- ADMIN: ve todos los salones.
- Público (sin token): catálogo, disponibilidad, reservar, chatear.

## Planes
- GRATIS: máx. 2 estilistas, 10 créditos IA al registrarse.
- PRO: estilistas ilimitados, +300 créditos al activarse.
- Paquete: pago único, +200 créditos. Cancelar PRO vuelve a GRATIS (conserva créditos).

## Reglas de negocio
**Salones y cuentas**
1. Todo dato pertenece a un salón. Dato de otro salón = 404.
2. Registro crea salón + dueño: plan GRATIS, 10 créditos, horario 8:00-19:00, sin domingos, intervalo 30.
3. Correo único en toda la plataforma: 409.
4. Slug desde el nombre (minúsculas, sin tildes, guiones). Si existe, "-2", "-3".
5. Apertura < cierre (409). Intervalo 15 a 60 (400).

**Catálogo y equipo**
6. Servicio: 15 a 480 min, precio entero > 0 (400).
7. Estilista ofrece 1+ servicios de su salón. Servicio ajeno: 404.
8. Crear estilista valida en este orden: límite del plan (402) → correo único (409) → servicios del salón (404). El límite va primero porque no cuesta consultar nada más si el plan ya no lo permite.

**Agenda**
9. Fin = inicio + duración del servicio.
10. La cita guarda el precio del servicio al agendar.
11. 409 si: pasado, fuera de horario, domingo cerrado, estilista sin ese servicio.
12. Sin cruces entre citas PENDIENTE/CONFIRMADA del mismo estilista (409). Se cruzan si inicioA < finB y finA > inicioB. Terminar a las 10:45 y empezar a las 10:45 no cruza.
13. Cita del salón: CONFIRMADA/SALON. Reserva en línea: PENDIENTE/EN_LINEA.
14. Cliente se busca por teléfono dentro del salón o se crea. Teléfono solo dígitos y "+".
15. Disponibilidad la calcula Java: desde apertura, cada intervalo, el servicio cabe antes del cierre, sin cruces, nunca en el pasado.
16. "Hoy" y "ahora" con zona America/Bogota (bean Clock).

**Transiciones de estado**
| Desde | Hacia | Caso real |
|---|---|---|
| PENDIENTE | CONFIRMADA | el salón acepta la reserva en línea |
| PENDIENTE | CANCELADA | el cliente se arrepiente antes de confirmar |
| CONFIRMADA | COMPLETADA | el servicio se realizó |
| CONFIRMADA | CANCELADA | cancelan con aviso |
| CONFIRMADA | NO_ASISTIO | el cliente nunca llegó |

COMPLETADA, CANCELADA y NO_ASISTIO son finales. Cualquier otra: 409.
Estilista: solo sus citas (ajena = 404) y solo a COMPLETADA/NO_ASISTIO (otro destino = 403).
Orden al cambiar estado: buscar por id y salón (404) → permisos del estilista → transición válida (409).

**Orden al agendar**
servicio existe y activo → estilista existe, activo y ofrece servicio → calcular fin → no pasado → horario → sin cruce → buscar/crear cliente, copiar precio, guardar.

**Pagos**
17. El backend decide el precio; el cliente solo pide "PRO" o "paquete".
18. Ya PRO pidiendo suscripción: 409 (antes de llamar a Stripe).
19. Webhook verifica firma (400 si falta o es falsa).
20. Cada evento se procesa una vez (se guarda su id; repetido = se ignora).
21. checkout.session.completed paid: PRO = plan PRO + id suscripción + 300 créditos; paquete = +200. customer.subscription.deleted: vuelve a GRATIS.
22. Todo el webhook en una transacción.

**IA**
23. 1 crédito por respuesta. Sin créditos: 402 y no se llama a la IA. Se descuenta al responder, con un UPDATE atómico (resta solo si quedan).
24. Orden: validar lo no-IA → verificar créditos → llamar IA → consumir crédito → validar JSON → responder. Nunca IA dentro de una transacción.
25. Recordatorio: solo cita PENDIENTE/CONFIRMADA (409). Java arma los datos (servicio, estilista, fecha, hora, valor, dirección); la IA solo redacta. No se envía: lo aprueba una persona. Mensaje no vacío, máx. 700 caracteres (502).
26. Chatbot: usa el catálogo real del salón; no agenda ni cancela (escala). Escalar crea solicitud de contacto.
27. Anti-promesas en Java: si la respuesta contiene descuento, cupón/cupon, gratis, promoción/promocion o reembols (sin importar mayúsculas), el cliente recibe un mensaje seguro y se escala con motivo REGLA_DE_SEGURIDAD.
28. Respuestas de IA en JSON validado: inválido o incompleto = 502; ningún proveedor = 503.
29. Llaves solo en secrets.properties o variables de entorno.

## Contrato de la API (resumen)
Formato de error único: `codigo`, `error`, `mensaje`, `fecha`. Fechas `AAAA-MM-DDTHH:mm:ss`. Precios enteros en COP.

| Ruta | Rol |
|---|---|
| POST /api/auth/registro, /api/auth/login | público |
| GET /api/salon, PUT /api/salon/configuracion | DUENO |
| POST/PUT /api/servicios | DUENO |
| GET /api/servicios | DUENO, ESTILISTA |
| POST/GET /api/estilistas, GET /api/clientes | DUENO |
| POST /api/citas | DUENO |
| GET /api/citas?fecha= | DUENO, ESTILISTA |
| PATCH /api/citas/{id}/estado | DUENO, ESTILISTA |
| POST /api/citas/{id}/recordatorio | DUENO |
| GET /api/publico/salones/{slug} (+ /disponibilidad, POST /reservas, POST /chat) | público |
| GET /api/solicitudes, PATCH /api/solicitudes/{id}/atendida | DUENO |
| POST /api/pagos/suscripcion, /api/pagos/creditos | DUENO |
| POST /api/pagos/webhook, GET /api/pagos/resultado?estado= | público |
| GET /api/admin/salones | ADMIN |

Códigos: 400 datos inválidos/firma, 401 sin token o credenciales, 402 plan o créditos, 403 rol, 404 no existe o es de otro salón, 409 regla de negocio, 502 Stripe/IA inutilizable, 503 sin proveedor de IA.

El detalle de campos de cada ruta está en la sección 4 del PDF (`@Sillon-Proyecto-Integrador...pdf`).

## Fuera de alcance
Envío real de WhatsApp/correo, recordatorios automáticos, reportes, pagos del cliente final al salón, interfaz gráfica.