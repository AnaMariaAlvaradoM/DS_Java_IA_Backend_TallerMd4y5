# Sillón: especificación de la entrega académica

Estado: fases 0–5 verificadas. Existen proyecto base, errores, reloj, autenticación, configuración del salón, servicios, estilistas, clientes, agenda interna y catálogo/disponibilidad/reservas públicos. Pagos, IA y las fases siguientes todavía no están implementados. Desde fase 4, VERIFICACION.md documenta el contraste de cada cierre con el PDF y sus evidencias.
Fuentes: enunciado PDF de 40 páginas y README de entrega del repositorio.
Prioridad: reglas de la sección 3 y contrato de la sección 4 del PDF. Las observaciones de diseño no sustituyen esas reglas.

## Objetivo y alcance

API REST multiinquilino para barberías y salones. Cada salón administra catálogo, equipo, clientes y agenda; los visitantes consultan disponibilidad y reservan sin cuenta. Stripe vende suscripción PRO y créditos de IA. La IA redacta recordatorios y atiende consultas con escalamiento humano.

Fuera de alcance: interfaz gráfica, envío real de WhatsApp o correo, recordatorios programados, reportes y pagos de clientes finales al salón.

## Tecnología y estructura

Java 17, Spring Boot 4.1.x (el enunciado propone 4.1.1), Maven, Spring Data JPA, Spring Security, JWT, BCrypt, Validation, RestClient, springdoc 3.x, Stripe Java, JUnit 5 y Mockito. Verificar versiones publicadas y compatibilidad antes de generar el proyecto.

MySQL local, PostgreSQL en Supabase para producción, Docker y Render. Paquete base `com.devsenior.sillon`. Dominios: compartido, seguridad, auth, salon, usuario, servicio, cliente, cita, publico, pago, chat e ia.

DTOs con records, mappers propios, getters/setters explícitos, inyección por constructor. Sin Lombok, Spring AI ni SDKs de IA. Configuración en application.properties; sin YAML ni perfiles. Prompts en archivos. No modificar .github/.

## Modelo

| Entidad | Atributos y relaciones |
|---|---|
| Salon | id, nombre, slug único, ciudad, direccion, telefono, plan, creditosIa, stripeSuscripcionId, configuracion embebida, fechaCreacion |
| ConfiguracionSalon | horaApertura, horaCierre, abreDomingos, intervaloMinutos |
| Usuario | id, nombre, email único global, password BCrypt, rol, activo; pertenece a Salon; relación muchos a muchos con Servicio |
| Servicio | id, nombre, descripcion, duracionMinutos, precio, activo; pertenece a Salon |
| Cliente | id, nombre, telefono; pertenece a Salon; unicidad por salon y telefono |
| Cita | id, inicio, fin, precio, estado, origen, nota, fechaCreacion, fechaActualizacion; relaciones con Salon, Cliente, Usuario estilista y Servicio |
| SolicitudContacto | id, clienteNombre, clienteContacto, mensajes, motivo, estado, fechaCreacion; pertenece a Salon |
| EventoStripe | id del evento, tipo, fechaProcesado; evita procesamiento duplicado |

Enums persistidos como texto: Plan GRATIS/PRO; Rol DUENO/ESTILISTA/ADMIN; EstadoCita PENDIENTE/CONFIRMADA/COMPLETADA/CANCELADA/NO_ASISTIO; OrigenCita SALON/EN_LINEA; TipoCompra SUSCRIPCION_PRO/PAQUETE_CREDITOS; MotivoSolicitud PEDIDO_POR_IA/REGLA_DE_SEGURIDAD; EstadoSolicitud PENDIENTE/ATENDIDA.

## Reglas y aceptación

| ID | Regla | Evidencia esperada |
|---|---|---|
| TEN-01 | Consultas y cambios filtrados por salón del principal autenticado; recursos ajenos no existen para el usuario | 404; listados sin datos de otros salones |
| AUTH-01 | Registro transaccional de salón y dueño; GRATIS, 10 créditos, apertura 08:00, cierre 19:00, sin domingos, intervalo 30 | 201 con JWT; GET salón confirma valores |
| AUTH-02 | Email global único, normalizado a minúsculas y sin espacios; contraseña BCrypt | Duplicado 409; credenciales incorrectas 401 genérico |
| SAL-01 | Slug sin tildes, minúsculas, separadores con guiones; colisiones -2, -3… | Nombres repetidos producen slugs distintos |
| SAL-02 | Apertura anterior al cierre; intervalo 15 a 60 | Horario inválido 409; intervalo inválido 400 |
| CAT-01 | Servicio de 15 a 480 minutos; precio entero COP positivo | Datos inválidos 400 |
| EQU-01 | Estilista con al menos un servicio del mismo salón | Lista vacía 400; servicio ajeno 404 |
| EQU-02 | GRATIS admite 2 estilistas; PRO sin límite | Tercer estilista GRATIS 402; PRO 201 |
| CLI-01 | Buscar o crear cliente por teléfono normalizado, conservando solo dígitos y +, dentro del salón | No duplicar teléfono normalizado en el salón |
| AGE-01 | Fin calculado por duración; precio copiado al reservar | Cambio posterior del precio no altera la cita |
| AGE-02 | Servicio activo; estilista activo del salón que ofrece ese servicio; no pasado; mismo día; dentro del horario; domingo solo si abre | Incumplimientos de negocio 409; recursos inexistentes 404 |
| AGE-03 | Solo PENDIENTE y CONFIRMADA ocupan agenda; cruce si inicioA < finB y finA > inicioB | Cruce 409; intervalos contiguos permitidos |
| AGE-04 | Creación interna CONFIRMADA/SALON; reserva pública PENDIENTE/EN_LINEA | 201 con estado y origen exactos |
| AGE-05 | Agenda diaria ordenada; dueño ve salón y estilista solo sus citas | Filtrado por fecha y propietario |
| AGE-06 | Disponibilidad desde apertura en pasos del intervalo; servicio cabe completo; sin cruces ni pasado | Corte 45 min reservado 10:00 elimina 09:30, 10:00 y 10:30 de la cuadrícula de 30 min |
| AGE-07 | Clock inyectado con America/Bogota | Tests con reloj fijo independientes del servidor |
| PAG-01 | Backend selecciona price ID; ya PRO no puede comprar otra suscripción | Cliente no determina precios; PRO repetido 409 |
| PAG-02 | Webhook valida firma sobre cuerpo original; sin firma o falsa 400 | Pruebas de firmas válidas e inválidas |
| PAG-03 | checkout.session.completed con pago paid aplica metadata; evento y cambios en una transacción | PRO suma 300 y guarda suscripción; paquete suma 200 |
| PAG-04 | Evento repetido no modifica estado ni saldo | 200 con procesado false |
| PAG-05 | customer.subscription.deleted regresa a GRATIS y conserva créditos | Cancelación verificada |
| IA-01 | Validar negocio, verificar saldo, llamar IA, consumir crédito, validar JSON, aplicar reglas | Sin saldo 402 y cero llamadas; respuesta malformada consume y produce 502 |
| IA-02 | Consumo atómico condicionado a saldo positivo; ninguna llamada externa dentro de transacción DB | Saldo nunca negativo |
| IA-03 | Proveedor OpenAI compatible con Gemini y respaldo formato Claude; URL configurable; salida validada | Fallo total 503; JSON inválido/incompleto 502 |
| REC-01 | Solo PENDIENTE/CONFIRMADA; Java prepara servicio, estilista, fecha, hora, valor y dirección | Otro estado 409 sin llamada IA |
| REC-02 | JSON mensaje no vacío, máximo 700 caracteres; borrador humano, máximo 4 frases, trato de usted | Mensaje, teléfono, créditos y factura; sin envío real |
| CHAT-01 | Catálogo y horario reales; historial enviado en cada petición, separado de instrucciones; sin memoria propia | Respuesta contextual por salón |
| CHAT-02 | No reservar/cambiar/cancelar; escalar petición humana, molestia y reclamos | Solicitud PEDIDO_POR_IA; mensajes de cliente máximo 2000 caracteres |
| CHAT-03 | Bloqueo Java sin distinguir mayúsculas de descuento, cupón, cupon, gratis, promoción, promocion o reembols | Mensaje fijo seguro; solicitud REGLA_DE_SEGURIDAD; no filtrar SILLA50 |
| SOL-01 | Dueño lista solicitudes recientes primero y marca atendida | Segunda atención 409; ajena 404 |

Orden al crear estilista confirmado por el usuario en fase 3: límite del plan, email único y pertenencia de servicios. El límite se valida primero para devolver 402 antes de comprobar correo y servicios cuando GRATIS ya tiene dos estilistas; después se valida la unicidad global del correo y finalmente los servicios del salón. La validación de estructura/formato del DTO ocurre antes de entrar al servicio.

Orden al agendar: servicio existente/activo, estilista existente/activo y servicio ofrecido, calcular fin, pasado, horario, cruce, buscar/crear cliente y guardar.

## Estados y permisos

PENDIENTE permite CONFIRMADA o CANCELADA. CONFIRMADA permite COMPLETADA, CANCELADA o NO_ASISTIO. Los otros estados son finales. Transición inválida: 409.

Tabla de transiciones para fase 4, antes de implementar el enum (PDF, paso 4.1):

| Estado actual | Destino permitido | Caso del negocio |
|---|---|---|
| PENDIENTE | CONFIRMADA | El dueño confirma una reserva en línea |
| PENDIENTE | CANCELADA | Se cancela una reserva aún sin confirmar |
| CONFIRMADA | COMPLETADA | El cliente recibió el servicio |
| CONFIRMADA | CANCELADA | Se cancela una cita confirmada |
| CONFIRMADA | NO_ASISTIO | El cliente no llegó |
| COMPLETADA, CANCELADA, NO_ASISTIO | Ninguno | Estados finales |

Solo PENDIENTE y CONFIRMADA ocupan agenda. Toda transición no enumerada, incluido volver al mismo estado, es inválida. El estilista debe además ser dueño de la cita y solo puede usar COMPLETADA o NO_ASISTIO.

DUENO administra su salón, catálogo, equipo, agenda, pagos, recordatorios y solicitudes. ESTILISTA consulta servicios y sus citas; solo puede pasar sus citas a COMPLETADA o NO_ASISTIO. Cita ajena para estilista: 404; destino no permitido: 403 antes de validar transición. ADMIN solo tiene acceso administrativo a todos los salones, sin heredar funciones del dueño. Visitante consulta catálogo/disponibilidad, reserva y chatea.

## Aclaraciones confirmadas por el usuario para fase 2

- JWT: duración de 60 minutos, configurable mediante sillon.jwt.expira-en-minutos. El PDF no establece un número.
- Si el nombre de salón produce un slug vacío (por ejemplo, solo signos), devolver HTTP 400; no inventar un nombre alternativo.
- Registro: salonNombre, ciudad, direccion, telefono, nombre, email y password son obligatorios. Textos vacíos o solo espacios producen HTTP 400. Se mantienen los máximos y formatos de la sección 4.

Aclaración confirmada para fase 4: si el teléfono de la cita no contiene dígitos después de normalizar, HTTP 400; se mantienen los 7–30 caracteres del contrato de entrada.

Aclaraciones confirmadas para fase 5: servicio y estilista de la respuesta de disponibilidad son sus nombres como texto; servicioIds de cada estilista público contiene únicamente servicios activos asignados. No se presentan como precisiones textuales del PDF, sino como decisiones del usuario para sus puntos abiertos.

## Contrato HTTP: inventario

| Método | Ruta | Acceso | Éxito |
|---|---|---|---|
| POST | /api/auth/registro | Público | 201 |
| POST | /api/auth/login | Público | 200 |
| GET | /api/salon | DUENO | 200 |
| PUT | /api/salon/configuracion | DUENO | 200 |
| POST | /api/servicios | DUENO | 201 |
| GET | /api/servicios | DUENO, ESTILISTA | 200 |
| PUT | /api/servicios/{id} | DUENO | 200 |
| POST | /api/estilistas | DUENO | 201 |
| GET | /api/estilistas | DUENO | 200 |
| GET | /api/clientes | DUENO | 200 |
| POST | /api/citas | DUENO | 201 |
| GET | /api/citas?fecha= | DUENO, ESTILISTA | 200 |
| PATCH | /api/citas/{id}/estado | DUENO, ESTILISTA | 200 |
| POST | /api/citas/{id}/recordatorio | DUENO | 200 |
| GET | /api/publico/salones/{slug} | Público | 200 |
| GET | /api/publico/salones/{slug}/disponibilidad?servicioId=&estilistaId=&fecha= | Público | 200 |
| POST | /api/publico/salones/{slug}/reservas | Público | 201 |
| POST | /api/publico/salones/{slug}/chat | Público | 200 |
| GET | /api/solicitudes | DUENO | 200 |
| PATCH | /api/solicitudes/{id}/atendida | DUENO | 200 |
| POST | /api/pagos/suscripcion | DUENO | 200 |
| POST | /api/pagos/creditos | DUENO | 200 |
| POST | /api/pagos/webhook | Firma Stripe | 200 |
| GET | /api/pagos/resultado?estado= | Público | 200 |
| GET | /api/admin/salones | ADMIN | 200 |

El inventario contiene 25 operaciones HTTP. La mención de 22 rutas de la fase Swagger no debe usarse para omitir operaciones.

### Campos y respuestas de la sección 4 (páginas 9–12)

Se registran las restricciones expresas del documento. Donde no fija tipo, longitud, obligatoriedad o estructura, no se añade una restricción por suposición. Los códigos por operación se complementan con las reglas generales de seguridad y error.

| Operación | Entrada y restricciones expresas | Respuesta y errores expresos |
|---|---|---|
| Registro | salonNombre máx. 100; ciudad máx. 60; direccion máx. 150; telefono máx. 30; nombre máx. 100; email con formato válido; password 8–72 | 201: token, tipo con valor Bearer, expiraEnMinutos, usuarioId, nombre, rol, salonId, salonSlug. 400, 409 |
| Login | email, password | 200: mismo objeto del registro. 401 |
| Consultar salón | Sin cuerpo | 200: id, nombre, slug, ciudad, direccion, telefono, plan, creditosIa, configuracion, fechaCreacion |
| Configurar salón | horaApertura, horaCierre, abreDomingos, intervaloMinutos; todos obligatorios; intervalo 15–60 | 200: salón actualizado. 400, 409 por horario inválido |
| Crear servicio | nombre máx. 80; descripcion opcional máx. 300; duracionMinutos 15–480; precio entero positivo; activo opcional con true si se omite | 201: id, nombre, descripcion, duracionMinutos, precio, activo. 400 |
| Listar servicios | Sin cuerpo | 200: lista de servicios del salón |
| Editar servicio | id en ruta; mismo cuerpo que crear servicio | 200. 400, 404. La tabla no vuelve a enumerar los campos de respuesta |
| Crear estilista | nombre, email, password 8–72, servicioIds con al menos un id | 201: id, nombre, email, activo, servicios como lista de nombres. 400, 402, 404, 409 |
| Listar estilistas | Sin cuerpo | 200: lista de estilistas del salón |
| Listar clientes | Sin cuerpo | 200: lista con id, nombre, telefono |
| Crear cita | clienteNombre; clienteTelefono 7–30 caracteres; servicioId; estilistaId; inicio; nota opcional máx. 300 | 201: objeto cita descrito abajo. 400, 404, 409 |
| Consultar agenda | fecha opcional en query; por defecto hoy del negocio | 200: lista de citas ordenadas por hora; estilista solo sus citas |
| Cambiar estado | id en ruta; estado en cuerpo | 200: cita. 400, 403, 404, 409 |
| Generar recordatorio | id en ruta; sin cuerpo | 200: citaId, telefonoCliente, mensaje, creditosRestantes, factura. 402, 404, 409, 502, 503 |
| Catálogo público | slug en ruta; sin cuerpo | 200: nombre, slug, ciudad, direccion, telefono, horario como texto, servicios solo activos, estilistas con id/nombre/servicioIds. Nunca correos. 404 |
| Disponibilidad | slug en ruta; servicioId, estilistaId y fecha obligatorios en query | 200: fecha, servicio, estilista, duracionMinutos, horariosLibres como lista de horas. 400, 404, 409 |
| Reserva pública | slug en ruta; mismo cuerpo que crear cita | 201: cita PENDIENTE con origen EN_LINEA. 400, 404, 409 |
| Chat público | slug en ruta; clienteNombre; clienteContacto máx. 120; historial de 1–20 turnos, cada uno con rol CLIENTE/ASISTENTE y texto máx. 500 | 200: respuesta, escalado booleano, solicitudId o null, factura. 400, 402, 404, 502, 503 |
| Listar solicitudes | Sin cuerpo | 200: lista con id, clienteNombre, clienteContacto, mensajes, motivo, estado, fechaCreacion; recientes primero |
| Atender solicitud | id en ruta; sin cuerpo | 200. 404, 409 si ya atendida. Cuerpo de éxito no especificado |
| Comprar PRO | Sin cuerpo | 200: tipo SUSCRIPCION_PRO, sesionId, urlPago. 409 si ya PRO; 502 |
| Comprar créditos | Sin cuerpo | 200: tipo PAQUETE_CREDITOS, sesionId, urlPago. 502 |
| Webhook | Cuerpo original del evento Stripe y cabecera Stripe-Signature | 200: recibido, procesado. 400 por firma inválida |
| Resultado de pago | estado en query: exito o cancelado | 200 con un mensaje; no especifica nombre del campo ni estructura |
| Administración de salones | Sin cuerpo | 200: todos los salones. 403 para otro rol; no enumera campos del listado |

Objeto configuracion: horaApertura, horaCierre, abreDomingos, intervaloMinutos.

Objeto cita: id, inicio, fin, estado, origen, precio, servicioId, servicioNombre, estilistaId, estilistaNombre, clienteNombre, clienteTelefono, nota, fechaCreacion, fechaActualizacion.

Objeto factura: proveedor, modelo, tokensEntrada, tokensSalida, costoEstimadoUsd, milisegundos. La sección 4 enumera los campos sin fijar su representación numérica exacta.

El primer ADMIN se obtiene registrando una cuenta normal y cambiando su rol directamente en base de datos; no se solicita endpoint de creación de administradores.

### Códigos de error comunes

| Código | Condición definida |
|---|---|
| 400 | Datos inválidos, JSON mal formado, parámetro faltante o firma de webhook inválida |
| 401 | Sin token, token inválido o credenciales incorrectas |
| 402 | Límite de GRATIS o falta de créditos IA |
| 403 | Rol sin permiso o destino de estado no permitido para estilista |
| 404 | Recurso inexistente o perteneciente a otro salón |
| 409 | Regla de negocio: cruce, horario, transición, correo repetido o plan activo |
| 502 | Respuesta no utilizable de Stripe o IA |
| 503 | Ningún proveedor IA disponible |

Errores uniformes, incluso seguridad: codigo, error (nombre HTTP estándar), mensaje español y fecha. Validaciones separadas por punto y coma. Fechas AAAA-MM-DD; fecha-hora local AAAA-MM-DDTHH:mm:ss; horas de entrada HH:mm y salida HH:mm:ss. Precios enteros COP. JSON de IA chatbot usa respuesta/escalar; JSON público usa respuesta/escalado/solicitudId/factura.

Seguridad stateless con Bearer JWT y CSRF deshabilitado. Públicas también /error, Swagger y /v3/api-docs. Nunca devolver entidades ni contraseñas/correos en catálogo público.

## Configuración evaluable

PORT; SPRING_DATASOURCE_URL; SPRING_DATASOURCE_USERNAME; SPRING_DATASOURCE_PASSWORD; JWT_SECRETO; URL_PUBLICA; STRIPE_URL_API; STRIPE_CLAVE_SECRETA; STRIPE_SECRETO_WEBHOOK; STRIPE_PRECIO_PRO; STRIPE_PRECIO_CREDITOS; GEMINI_LLAVE; SILLON_IA_OPENAI_URL (propiedad sillon.ia.openai.url).

Valores de relleno sin secretos reales; importación opcional de secrets.properties; ddl-auto=update; open-in-view=false. URLs externas reemplazables por simuladores. La conexión a base de datos sigue siendo necesaria para levantar el contexto JPA.

## Entrega y verificación

pom.xml y Maven Wrapper en raíz del fork. SPEC.md, AGENTS.md, BITACORA-IA.md, secrets.example.properties, .gitignore, .cursorignore, prompts/recordatorio.txt y prompts/chatbot.txt bajo resources, Dockerfile de dos etapas y .dockerignore. Nunca versionar secrets.properties ni llaves.

JUnit 5/Mockito: cubrir mínimos de páginas 26–27, superar 30 ejecuciones de test, sin llamadas reales Stripe/Gemini. Postman: 52 escenarios de páginas 31–32, mínimo 50 peticiones y 40 con scripts; 7 carpetas en orden oficial; dos entornos con solo baseUrl; sufijos únicos, fechas dinámicas y tokens encadenados. Carpetas 1–6 automáticas y 7 después de pago manual.

Render con Supabase; Swagger /swagger-ui.html y /v3/api-docs públicos; URL real en README; webhook de producción en modo prueba. Un PR al repositorio oficial titulado con nombre completo. Commits por fase/paso verificado; nunca fabricar evidencias o entradas de bitácora.

## Secuencia de construcción

0. Repositorio, proyecto base, especificación, reglas y secretos.
1. Salones, usuarios, errores y reloj.
2. JWT, registro, login, roles y configuración.
3. Servicios, estilistas y clientes.
4. Agenda y estados.
5. Catálogo público, disponibilidad y reservas.
6. Stripe y webhook.
7. Motor IA, fallback y créditos.
8. Recordatorios.
9. Chatbot y solicitudes.
10. Consolidar tests (verificar reglas desde las fases que las introducen).
11. Swagger.
12. Docker, Render y Supabase.
13. Postman completo, README y entrega verificable.

## Pendientes y decisiones a validar

- PR de entrega #5 abierto el 2026-10-27, autorizado por el estudiante, con título Giovanny Hernandez Arbelaez: https://github.com/AnaMariaAlvaradoM/DS_Java_IA_Backend_TallerMd4y5/pull/5. Se comprobó padre y ausencia de duplicados antes de crearlo. Primera ejecución oficial 40535698862 publicó 0.0/5.0 y 0/145 al abortar con ERR_INVALID_PROTOCOL: el calificador usa node:http para HTTPS (líneas 93/416 del archivo oficial confirmado idéntico). Sin cambios en .github/. Pendiente corrección por su responsable y evaluación que termine correctamente; no declarar aprobación académica.
- Fase 13: colección y README verificados. Colección de 55 peticiones/52 escenarios, siete carpetas y dos entornos con solo baseUrl; fechas/datos únicos y captura automática. Carpetas 1–6 locales y Render: 53 peticiones/107 aserciones sin fallos en cada entorno. Carpeta 7 en Render tras pago manual: cuatro aserciones sin fallos; salón 5 confirmado PRO/306 y tres estilistas. No se ejecutó carpeta 7 en local. Fallo inicial de Gemini HTTP 400 resuelto tras ajuste del estudiante en Render y repetición completa; variable exacta corregida no confirmada. Expectativa incorrecta falla en copia temporal. Tests Java: 70 aprobados. README propio y reflexión asistida aprobada completados. El PR ya está abierto; evaluación oficial bloqueada como se detalla arriba y en VERIFICACION.md.
- Fase 12 verificada el 2026-10-27: Dockerfile de dos etapas y .dockerignore; despliegue real https://ds-java-ia-backend-tallermd4y5.onrender.com con Supabase Session pooler 5432. Swagger/OpenAPI 200, registro 201, login 200. Checkout de modo prueba 200 tras corregir clave Stripe en Render; pago manual del estudiante y webhook dejaron salón 2 en PRO/310, confirmado por API y consulta SQL del estudiante en Supabase. URL en README; evidencia y límites en VERIFICACION.md. No se abrió PR por instrucción expresa del estudiante. Fase 13 y calificador oficial pendientes.
- Fase 11 verificada el 2026-10-21: Swagger UI en /swagger-ui.html, título Sillón API, Bearer JWT y diez etiquetas numeradas. Seguridad declarada solo en operaciones privadas. Las 22 rutas del PDF equivalen a 22 paths y 25 operaciones HTTP. GET /api/salon ejecutado desde Swagger con Authorize y token de dueño ficticio: 200. Suite de 70 pruebas y empaquetado aprobados; detalle en VERIFICACION.md. Sin nuevas dependencias.
- Fase 10 verificada el 2026-10-21: por solicitud expresa del estudiante se conservan únicamente los casos mínimos de las páginas 26–27. Suite actual: 70 pruebas aprobadas; matriz en TESTS-ENTREGA.md. Servicios sin Spring y con mocks, relojes fijos, sin llamadas reales a IA/Stripe y test de contexto con MySQL. Las pruebas adicionales anteriores fueron retiradas; su evidencia histórica no equivale a cobertura presente. Sin cambios productivos ni nuevas dependencias.
- Auditoría transversal 2026-10-21, antes de fase 10: A01–A03 corregidos tras la revisión inicial. GET /api/admin/salones implementado para ADMIN; contraseña de registro/estilista conserva mínimo 8 caracteres y rechaza más de 72 bytes UTF-8 con 400 y mensaje claro, por confirmación expresa del usuario. Cliente.nombre ampliado a TEXT, sin añadir máximo contractual; migración MySQL existente aplicada preservando registros. Suite final de aquella revisión: 276 pruebas aprobadas. Arranque completo sin secrets.properties comprobado con base/usuario temporales eliminados al terminar. Detalle histórico, cierre y límites en AUDITORIA-FASES-0-9.md.

- Fase 9 verificada: contexto desde servicios activos/horario reales, prompt chatbot aprobado, historial como mensaje separado, chat público con respuesta/escalado/solicitudId/factura y orden de créditos de fase 7. Regla Java de siete términos, mensaje seguro aprobado y solicitud única con prioridad REGLA_DE_SEGURIDAD; escalamiento normal PEDIDO_POR_IA. Solicitudes del dueño filtradas por salón, recientes primero y atención con 404/409; usuario confirmó devolver DTO actualizado. Nombre/contacto/textos obligatorios por aclaración del usuario. Suite: 257 pruebas aprobadas. Manual con Gemini: precio sin escalar, reserva con solicitud, inyección sin términos prohibidos y saldo final 7; listado/atención, 400/404/409 comprobados. Evidencias y alcance exacto de cada comprobación en VERIFICACION.md.

- Fase 8 verificada: datos del recordatorio en CitaService de solo lectura, filtrados por salón y estados PENDIENTE/CONFIRMADA; seis líneas formateadas con es-CO y precio histórico. Prompt sugerido y aprobado por el usuario en prompts/recordatorio.txt. POST /api/citas/{id}/recordatorio solo DUENO, con orden de créditos contractual, JSON mensaje válido máximo 700 y respuesta/factura exactas. Suite: 207 pruebas aprobadas; manual con Gemini: Sara/10:30, saldo 9; completada 409 sin consumo y llave ficticia inválida 503 con logs de ambos proveedores y saldo conservado. Detalle y límites de evidencia en VERIFICACION.md. El resultado es un borrador y no se envía.

- Fase 7 verificada: paquete ia reutilizado de Atiende Modulo5/Clase3_19, adaptado a com.devsenior.sillon.ia y sillon.ia; RestClient con timeout, URLs configurables, Gemini gemini-3.5-flash-lite principal y API Claude como respaldo sin llave por aclaración del usuario. Errores 502/503 integrados sin exponer respuestas ni llaves. Créditos: verificar y consumir mediante update condicionado a saldo positivo; concurrencia con un crédito comprobada. Suite: 182 pruebas aprobadas; empaquetado aprobado tras liberar el JAR anterior. Conexión real con Gemini y JSON válido comprobados mediante ejecutable temporal independiente de la suite, sin consumir créditos de salón. Detalle y límites de evidencia en VERIFICACION.md. La integración del orden de consumo con recordatorios/chat y sus prompts se realizará en fases 8–9.

- Fase 6 verificada: StripeClient con configuración tipada y base STRIPE_URL_API; Checkout PRO/paquete con precios del backend y metadata salonId/tipoCompra (SUSCRIPCION_PRO o PAQUETE_CREDITOS), misma metadata en suscripción; PRO repetido 409. Webhook firmado, idempotente y transaccional, con fallback de deserialización del SDK. Usuario confirmó resultado JSON mensaje y estado inválido 400; procesado=true para evento nuevo registrado y false para repetido. Suite: 148 pruebas aprobadas. Pagos manuales de prueba comprobaron PRO/310, tercer estilista, PRO repetido 409 y paquete hasta 510; reenvío real no duplicó saldo; cancelación dejó GRATIS/510. Matriz y límites de evidencia en VERIFICACION.md.

- Fase 5 verificada: catálogo sin correos/contraseñas, solo servicios y estilistas activos; disponibilidad con tres parámetros obligatorios, consulta por salón y cálculo Java; reservas públicas PENDIENTE/EN_LINEA con el mismo método privado de agenda. Suite: 130 pruebas aprobadas (11 nuevas). Contraste con páginas 11 y 20–21 en VERIFICACION.md. Chat público corresponde a fase 9.

- Fase 4 verificada: enum de estados, Cita con cuatro relaciones, auditoría con Clock, cálculo de fin y copia de precio, reglas sin repositorios y cálculo de disponibilidad, método privado agendar compartido, creación/listado/cambio de estado internos. Suite final: 119 pruebas aprobadas; contraste con pasos 4.1–4.5 y páginas 7–8/11/19–20 en VERIFICACION.md. Reservas y disponibilidad públicas pertenecen a fase 5.

- Fase 3 verificada: POST/GET/PUT de servicios, POST/GET de estilistas y GET de clientes según contrato. Servicios filtrados por salón, precios enteros sin truncar decimales, relación estilista-servicios, límite GRATIS con bloqueo de la fila del salón durante el alta y PRO sin límite. Clientes: buscar o crear por teléfono normalizado y unicidad por salón/teléfono; sin endpoint de alta manual. Suite: 52 pruebas aprobadas (15 nuevas), evidencia local target/fase3-tests.log. Sin colección Postman ni calificador remoto ejecutados todavía.

- Fase 2 verificada: registro transaccional y login con BCrypt; JWT de 60 minutos firmado y validado con Clock; usuario autenticado cargado desde base de datos; permisos por rutas y errores 401/403 uniformes; GET /api/salon y PUT /api/salon/configuracion con salonId del principal. Suite: 37 pruebas, 0 fallos, 0 errores; evidencia local target/fase2-final.log. Las pruebas HTTP usan MockMvc con filtros reales y transacciones de prueba revertidas; no se ha ejecutado todavía la colección de Postman ni el calificador remoto.

- Fase 1 implementada: ErrorApi, excepciones y manejador global; Clock America/Bogota y auditoría JPA con ese reloj; Salon, ConfiguracionSalon, Plan, Usuario, Rol y repositorios; generador de slug con sufijos desde -2. Verificación de aquella fase: 16 pruebas aprobadas y contexto MySQL iniciado. La relación estilista-servicios y errores de filtros se incorporaron posteriormente en fases 3 y 2, respectivamente.

- GitHub verificado el 2026-10-21: origin es fork de AnaMariaAlvaradoM/DS_Java_IA_Backend_TallerMd4y5. La consulta con estado abierto/cerrado y head giovanni104:main devolvió cero PR. Pendiente crear el único PR de entrega cuando corresponda; esta revisión no hizo push ni creó PR.
- Antes de implementar DTOs de futuras fases, aclarar lo que la sección 4 deje sin precisar: obligatoriedad de textos sin indicación expresa y cuerpos de éxito no enumerados (por ejemplo, atender solicitud o resultado de pago). La representación de servicio/estilista en disponibilidad quedó confirmada por el usuario en fase 5. Consultar primero el material oficial y preguntar si persiste la duda.
- Fase 0 verificada: proyecto generado con Initializr y dependencias aprobadas, Maven Wrapper 3.10.0, Java 17, configuración inicial y documentación. Compilación y test de contexto con MySQL verificados (1 test, sin errores). Por instrucción expresa del usuario se retiró la fijación de JUnit 5.14.4: spring-boot-starter-webmvc-test utiliza JUnit 6.0.3 gestionado por Boot. La mención de JUnit 5 en el stack anterior refleja el documento original; esta excepción autorizada se detalla en DEPENDENCIAS.md. Secretos excluidos de Git y .github/ sin modificaciones. La entrega remota y el PR requieren identificar el repositorio padre.
- En fase 4 se implementó bloqueo del estilista y consulta de cruces con bloqueo en la misma transacción para proteger altas simultáneas. Revisión de código realizada; no se ejecutó prueba de carga concurrente.
- Analizar carrera de dos peticiones IA con un solo crédito: el descuento atómico evita negativos, pero no impide por sí solo ambas llamadas externas. No alterar el orden contractual sin revisión.
- No se define desactivar estilistas existentes al perder PRO ni recargar créditos por renovación: no agregar esas conductas sin aclaración.
- La cuadrícula se exige para disponibilidad; no se exige explícitamente rechazar reservas directas fuera de sus pasos.
- Nombre completo confirmado por el estudiante: Giovanny Hernandez Arbelaez; usado en el título del PR #5. Render/Supabase ya desplegados. No registrar credenciales en esta especificación.
- Compatibilidad de Stripe aprobada el 2026-10-27 tras la evaluación oficial 4.4/5.0 (129/145): aceptar metadata tipo y tipoCompra, ambas coincidentes también válidas; conflicto rechazado como metadata inválida con 502 sin efectos. Checkout conserva tipoCompra. Tests mínimos PRO/paquete parametrizados y caso de conflicto: suite actual 75 aprobados. ChatServiceTest renombrado ChatbotServiceTest sin cambiar aserciones. Misma rama/PR #5, sin modificar calificador; nueva evaluación pendiente.
