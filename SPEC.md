# SPEC — Sillón

## 1. Objetivo

Desarrollar una API REST para administrar barberías y salones de
belleza, gestionar sus citas e integrar funciones de inteligencia
artificial y pagos.

## 2. Alcance

El proyecto comprende únicamente el backend e incluye:

- Registro de salones y usuarios.
- Autenticación con JWT y autorización por roles.
- Gestión de servicios y estilistas.
- Consulta de disponibilidad y gestión de citas.
- Catálogo público y reservas.
- Suscripciones y compra de créditos mediante Stripe.
- Funciones de IA con proveedor principal y proveedor alternativo.
- Recordatorios y chatbot con escalamiento a atención humana.
- Documentación de la API, pruebas y despliegue.

## 3. Separación entre salones

Cada salón administra sus propios datos.

Las operaciones privadas deben comprobar que el usuario tenga
permiso para acceder al salón solicitado. Conocer el identificador
de un recurso no concede acceso a él.

## 4. Tecnologías

- Java 17, Spring Boot y Maven.
- Spring Security, JWT y BCrypt.
- Spring Data JPA.
- MySQL para desarrollo local.
- PostgreSQL en Supabase para producción.
- Swagger / OpenAPI para documentar la API.
- Stripe en modo de pruebas.
- Integraciones de IA mediante HTTP con RestClient.
- JUnit y Mockito para pruebas.
- Docker y Render para despliegue.

No se utilizará Lombok ni SDK de IA.

## 5. Planes, pagos y créditos

### Plan GRATIS

- Permite hasta 2 estilistas por salón.
- El salón recibe 10 créditos de IA al registrarse.
- Crear un tercer estilista devuelve HTTP 402.

### Plan PRO y paquetes

- PRO permite estilistas sin límite.
- Un pago confirmado del plan PRO activa el plan, guarda
  el identificador de la suscripción y suma 300 créditos.
- Un paquete pagado suma 200 créditos.
- Solicitar otra suscripción cuando el salón ya es PRO
  devuelve HTTP 409.
- Si Stripe cancela la suscripción, el salón vuelve a GRATIS
  y conserva sus créditos disponibles.

### Procesamiento de pagos

- Stripe se utiliza en modo de pruebas.
- El cliente de la API solicita el tipo de compra:
  suscripción PRO o paquete de créditos.
- El backend determina el precio. El cliente no envía
  importes ni identificadores de precios.
- Volver a la URL de éxito no activa beneficios por sí solo.
- El webhook debe verificar la firma de Stripe.
- Una firma ausente o inválida devuelve HTTP 400.
- Se guarda el identificador de cada evento procesado.
- Los eventos repetidos se ignoran para evitar duplicar
  créditos o beneficios.

## 6. Protección de credenciales

- Las contraseñas de los usuarios se almacenan mediante BCrypt.
- Las claves de servicios externos y de base de datos no se
  incluyen en el repositorio.
- El archivo secrets.properties contiene credenciales locales
  y está excluido de Git.
- secrets.example.properties contiene únicamente ejemplos.
- En producción se utilizan variables de entorno.

## 7. Reglas de citas

- Cada cita pertenece a un salón. Sus servicios y estilistas
  deben pertenecer al mismo salón; un recurso ajeno devuelve 404.
- El estilista debe ofrecer el servicio seleccionado; si no,
  se devuelve 409.
- La hora de finalización se calcula con la duración del servicio.
- La cita conserva el precio del servicio al momento de reservar,
  aunque el precio del catálogo cambie después.
- No se permiten citas en el pasado, fuera del horario del salón
  ni en domingos cuando el salón no atiende. Se devuelve 409.
- Las citas PENDIENTE y CONFIRMADA bloquean la agenda.
  Si se superponen para un mismo estilista, se devuelve 409.
- Una cita creada por el salón nace CONFIRMADA, con origen SALON.
- Una reserva pública nace PENDIENTE, con origen EN_LINEA.
- Al reservar, se busca al cliente por teléfono dentro del salón.
  Si no existe, se crea. El teléfono conserva solo dígitos y +.
- Java calcula la disponibilidad usando el intervalo de agenda
  desde la apertura. El servicio debe caber completo antes
  del cierre, sin cruces ni horarios pasados.
- Las fechas de "hoy" y "ahora" se calculan con la zona horaria
  de Colombia: America/Bogota.
- La disponibilidad se comprueba al guardar, protegiendo
  también contra reservas simultáneas.

## 8. Criterios de aceptación de reservas

- Una reserva con datos válidos y un horario disponible
  se guarda y devuelve su identificador.
- Una reserva que se superpone con otra del mismo estilista
  se rechaza y no se guarda.
- Un servicio o estilista de otro salón se rechaza.
- Una reserva fuera del horario de atención se rechaza.
- Si llegan dos reservas simultáneas para el mismo estilista
  y horario, como máximo una se guarda.
- Un usuario sin acceso al salón no puede consultar ni
  modificar sus citas mediante endpoints privados.
- Consultar disponibilidad no modifica las citas existentes.

## 9. Estados y cancelación de citas

### Transiciones permitidas

- PENDIENTE → CONFIRMADA o CANCELADA.
- CONFIRMADA → COMPLETADA, CANCELADA o NO_ASISTIO.
- COMPLETADA, CANCELADA y NO_ASISTIO son estados finales.
- Una transición no permitida devuelve HTTP 409.

### Permisos

El cambio de estado se realiza mediante:

PATCH /api/citas/{id}/estado

- DUENO puede realizar las transiciones permitidas sobre
  las citas de su salón.
- ESTILISTA solo puede cambiar sus propias citas y únicamente
  a COMPLETADA o NO_ASISTIO.
- Si un estilista intenta cambiar una cita de otro estilista,
  se devuelve HTTP 404.
- Si intenta establecer otro estado, se devuelve HTTP 403.

### Alcance de la cancelación

La actividad no establece un plazo mínimo de anticipación
para cancelar ni un endpoint público de cancelación para clientes.
No se agregan esas restricciones o funcionalidades.

## 10. Registro y configuración del salón

- El registro crea el salón y su dueño en una misma operación.
- El salón comienza en plan GRATIS, con 10 créditos de IA.
- Su horario inicial es de 08:00 a 19:00, sin atención los
  domingos y con intervalos de agenda de 30 minutos.
- El correo de cada usuario debe ser único en toda la plataforma,
  incluidos los estilistas. Un correo repetido devuelve 409.
- El slug público se genera desde el nombre del salón,
  en minúsculas, sin tildes ni espacios.
- Si el slug ya existe, se agrega un número para diferenciarlo.
- La apertura debe ser anterior al cierre; si no, se devuelve 409.
- El intervalo de agenda debe estar entre 15 y 60 minutos;
  un valor fuera de ese rango devuelve 400.
- Intentar acceder a datos de otro salón devuelve 404,
  sin revelar que el recurso existe.

## 11. Servicios y estilistas

- La duración de un servicio debe estar entre 15 y 480 minutos.
- El precio debe ser un número entero de pesos mayor que cero.
- Una duración o un precio inválidos devuelven 400.
- Cada estilista debe ofrecer al menos un servicio de su salón.
- Asignarle un servicio de otro salón devuelve 404.
- Intentar crear un tercer estilista en plan GRATIS devuelve 402.
- El plan PRO no limita la cantidad de estilistas.

## 12. Inteligencia artificial

### Consumo de créditos

- Cada respuesta de IA consume un crédito.
- Antes de llamar al proveedor, se verifica el saldo del salón.
- Si no hay créditos, se devuelve HTTP 402 y no se llama a la IA.
- El crédito se descuenta cuando la IA responde.

### Recordatorios

- Solo se generan para citas PENDIENTE o CONFIRMADA.
- Para otros estados se devuelve HTTP 409.
- Java obtiene y prepara los datos verificados de la cita:
  servicio, estilista, fecha, hora, valor y dirección.
- La IA redacta el mensaje utilizando esos datos.
- Una persona revisa y aprueba el mensaje.
- Sillón no envía el recordatorio.

### Chatbot y atención humana

- El chatbot responde usando el catálogo real del salón,
  consultado desde la base de datos.
- No agenda, modifica ni cancela citas.
- Ante esas solicitudes, escala a atención humana.
- Al escalar, crea una solicitud de contacto que incluye
  los mensajes del cliente.
- Java revisa si la respuesta menciona descuentos, cupones,
  algo gratis, promociones o reembolsos.
- Si detecta esas promesas, sustituye la respuesta por
  un mensaje seguro fijo y escala con el motivo
  REGLA_DE_SEGURIDAD.

### Respuestas y fallos

- Se solicita la respuesta de la IA en formato JSON.
- El backend valida el formato y los datos requeridos.
- Un JSON inválido o incompleto devuelve HTTP 502.
- Si ningún proveedor responde, se devuelve HTTP 503.
- Las claves se guardan en secrets.properties para desarrollo
  o en variables de entorno para producción.
- Nunca se incluyen claves en código, Git ni prompts.

## 13. Fuera de alcance

El proyecto no incluye:

- Envío real de mensajes por WhatsApp o correo electrónico.
- Recordatorios automáticos programados.
- Reportes.
- Pagos del cliente final al salón mediante Stripe Connect.
- Interfaz gráfica.

Los pagos implementados corresponden al plan PRO y a los
paquetes de créditos que compra el salón para usar la plataforma.

## 14. Contrato de la API

### 14.1 Formatos generales

- Fechas: AAAA-MM-DD.
- Fecha y hora: AAAA-MM-DDTHH:mm:ss.
- Horas enviadas: HH:mm; devueltas: HH:mm:ss.
- Precios: números enteros en pesos colombianos, sin separadores
  ni decimales. Ejemplo: 35000.

### 14.2 Formato de errores

Todos los errores, incluidos los de seguridad, devuelven:

- codigo: número del código HTTP.
- error: nombre estándar del código HTTP.
- mensaje: explicación en español. En validaciones, campos y
  mensajes separados por punto y coma.
- fecha: fecha y hora del error.

### 14.3 Códigos de error

| Código | Uso |
| --- | --- |
| 400 | Datos inválidos, JSON mal formado, parámetro faltante o firma de webhook inválida. |
| 401 | Token ausente o inválido, o credenciales incorrectas. |
| 402 | Límite del plan GRATIS o saldo insuficiente de créditos de IA. |
| 403 | Rol sin permiso o transición no permitida para el estilista. |
| 404 | Recurso inexistente o perteneciente a otro salón. |
| 409 | Conflicto con una regla de negocio. |
| 502 | Respuesta de Stripe o de IA que no se puede utilizar. |
| 503 | Ningún proveedor de IA disponible. |

### 14.4 Autenticación — rutas públicas

#### POST /api/auth/registro

Recibe:
- salonNombre: máximo 100 caracteres.
- ciudad: máximo 60 caracteres.
- direccion: máximo 150 caracteres.
- telefono: máximo 30 caracteres.
- nombre: máximo 100 caracteres.
- email: formato válido.
- password: entre 8 y 72 caracteres.

Responde 201 con:
token, tipo ("Bearer"), expiraEnMinutos, usuarioId, nombre,
rol, salonId y salonSlug.

Errores: 400 por datos inválidos y 409 por correo duplicado.

#### POST /api/auth/login

Recibe: email y password.

Responde 200 con el mismo objeto del registro.
Credenciales incorrectas: 401.

Las rutas privadas reciben la cabecera:
Authorization: Bearer <token>

### 14.5 Mi salón — rol DUENO

#### GET /api/salon

Responde 200 con:
id, nombre, slug, ciudad, direccion, telefono, plan,
creditosIa, configuracion y fechaCreacion.

configuracion contiene:
horaApertura, horaCierre, abreDomingos e intervaloMinutos.

#### PUT /api/salon/configuracion

Recibe, todos obligatorios:
horaApertura, horaCierre, abreDomingos e intervaloMinutos.

intervaloMinutos debe estar entre 15 y 60.
horaApertura debe ser anterior a horaCierre.

Responde 200 con el salón actualizado.
Errores: 400 por datos inválidos y 409 por horario incoherente.

### 14.6 Servicios

#### POST /api/servicios — DUENO

Recibe:
- nombre: máximo 80 caracteres.
- descripcion: opcional, máximo 300 caracteres.
- duracionMinutos: entre 15 y 480.
- precio: entero positivo en COP.
- activo: opcional; por defecto, true.

Responde 201 con:
id, nombre, descripcion, duracionMinutos, precio y activo.

Datos inválidos: 400.

#### GET /api/servicios — DUENO y ESTILISTA

Responde 200 con la lista de servicios del salón.

#### PUT /api/servicios/{id} — DUENO

Recibe los mismos campos que POST /api/servicios.

Responde 200.
Errores: 400 por datos inválidos y 404 si el servicio no existe
o pertenece a otro salón.

### 14.7 Estilistas — DUENO

#### POST /api/estilistas

Recibe:
- nombre.
- email.
- password: entre 8 y 72 caracteres.
- servicioIds: lista con al menos un identificador.

Responde 201 con:
id, nombre, email, activo y servicios (lista de nombres).

Errores:
- 400: datos inválidos.
- 402: límite de estilistas del plan GRATIS.
- 404: servicio inexistente o de otro salón.
- 409: correo duplicado.

#### GET /api/estilistas

Responde 200 con la lista de estilistas del salón.

### 14.8 Clientes — DUENO

#### GET /api/clientes

Responde 200 con la lista de clientes del salón.
Cada cliente contiene: id, nombre y telefono.

### 14.9 Agenda

#### POST /api/citas — DUENO

Recibe:
- clienteNombre.
- clienteTelefono: entre 7 y 30 caracteres.
- servicioId.
- estilistaId.
- inicio.
- nota: opcional, máximo 300 caracteres.

Responde 201 con la cita.
Errores: 400, 404 y 409.

#### GET /api/citas?fecha= — DUENO y ESTILISTA

fecha es opcional; si se omite, se consulta hoy.

Responde 200 con las citas del día ordenadas por hora.
El ESTILISTA solo ve sus propias citas.

#### PATCH /api/citas/{id}/estado — DUENO y ESTILISTA

Recibe: estado.

Responde 200 con la cita.
Se aplican las transiciones y permisos de la sección 9.
Errores: 400, 403, 404 y 409.

#### Objeto de respuesta de una cita

id, inicio, fin, estado, origen, precio, servicioId,
servicioNombre, estilistaId, estilistaNombre, clienteNombre,
clienteTelefono, nota, fechaCreacion y fechaActualizacion.

#### POST /api/citas/{id}/recordatorio — DUENO

No recibe cuerpo.

Responde 200 con:
citaId, telefonoCliente, mensaje, creditosRestantes y factura.

Errores: 402, 404, 409, 502 y 503.

La factura de IA contiene:
proveedor, modelo, tokensEntrada, tokensSalida,
costoEstimadoUsd y milisegundos.

### 14.10 Catálogo público y reservas — sin token

Ruta base: /api/publico/salones/{slug}

#### GET /api/publico/salones/{slug}

Responde 200 con:
nombre, slug, ciudad, direccion, telefono, horario (texto),
servicios (solo activos) y estilistas.

Cada estilista contiene: id, nombre y servicioIds.
La respuesta nunca incluye correos electrónicos.

Salón inexistente: 404.

#### GET /api/publico/salones/{slug}/disponibilidad

Parámetros obligatorios:
servicioId, estilistaId y fecha.

Responde 200 con:
fecha, servicio, estilista, duracionMinutos y horariosLibres
(lista de horas).

Errores: 400, 404 y 409.

#### POST /api/publico/salones/{slug}/reservas

Recibe los mismos campos que POST /api/citas.

Responde 201 con la cita en estado PENDIENTE y origen EN_LINEA.
Errores: 400, 404 y 409.

#### POST /api/publico/salones/{slug}/chat

Recibe:
- clienteNombre.
- clienteContacto: máximo 120 caracteres.
- historial: entre 1 y 20 turnos.

Cada turno contiene:
- rol: CLIENTE o ASISTENTE.
- texto: máximo 500 caracteres.

Responde 200 con:
respuesta, escalado (true o false), solicitudId (o null) y factura.

La factura tiene los campos definidos en la sección 14.9.
Errores: 400, 402, 404, 502 y 503.

### 14.11 Solicitudes del chatbot — DUENO

#### GET /api/solicitudes

Responde 200 con las solicitudes del salón, las más recientes primero.

Cada solicitud contiene:
id, clienteNombre, clienteContacto, mensajes, motivo,
estado y fechaCreacion.

#### PATCH /api/solicitudes/{id}/atendida

No recibe cuerpo.

Responde 200.
Errores: 404 si no existe o pertenece a otro salón;
409 si ya estaba atendida.

### 14.12 Pagos

#### POST /api/pagos/suscripcion — DUENO

No recibe cuerpo.

Responde 200 con:
tipo (SUSCRIPCION_PRO), sesionId y urlPago.

Errores: 409 si el salón ya es PRO y 502 por fallo de pasarela.

#### POST /api/pagos/creditos — DUENO

No recibe cuerpo.

Responde 200 con:
tipo (PAQUETE_CREDITOS), sesionId y urlPago.

Error: 502 por fallo de pasarela.

#### POST /api/pagos/webhook — Stripe

Recibe el evento de Stripe y la cabecera Stripe-Signature.

Responde 200 con recibido y procesado.
Firma ausente o inválida: 400.
Los eventos repetidos no vuelven a aplicar beneficios.

#### GET /api/pagos/resultado?estado= — público

Recibe estado con valor exito o cancelado.

Responde 200 con un mensaje.
Esta ruta no activa beneficios; eso corresponde al webhook.

### 14.13 Administración — ADMIN

#### GET /api/admin/salones

Responde 200 con todos los salones de la plataforma.
Un usuario autenticado con otro rol recibe 403.

El primer ADMIN se crea registrando una cuenta normal y
cambiando su rol directamente en la base de datos.