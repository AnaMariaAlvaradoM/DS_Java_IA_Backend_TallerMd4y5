
# SPEC — Sillón

## 1. Descripción del proyecto

Sillón es una plataforma SaaS de gestión de agendas para barberías y salones de belleza.

Su propósito es facilitar la administración de servicios, estilistas, clientes y citas, evitando conflictos de horarios y mejorando la atención al cliente mediante herramientas de inteligencia artificial.

El sistema consiste en una API REST desarrollada en Java con Spring Boot, sin interfaz gráfica propia.

## 2. Problema que resuelve

Las barberías y salones de belleza suelen administrar sus citas mediante mensajes, llamadas o registros manuales.

Esto puede provocar:

- Reservas duplicadas.
- Conflictos de horarios entre clientes.
- Pérdida de citas por inasistencia.
- Dificultad para consultar disponibilidad.
- Demoras en la atención de preguntas frecuentes.

Sillón centraliza la información y aplica reglas automáticas para gestionar estos procesos.

## 3. Objetivo general

Desarrollar una API REST multi-inquilino que permita administrar salones, usuarios, servicios, clientes y citas, incorporando autenticación JWT, integración con Stripe e inteligencia artificial.

## 4. Arquitectura multi-inquilino

Cada salón constituye un inquilino independiente.

Todos los salones utilizan la misma aplicación y base de datos, pero sus registros deben permanecer aislados.

Un usuario no puede consultar ni modificar información perteneciente a otro salón.

Cuando se intenta acceder a un recurso de otro salón, la API debe responder HTTP 404, evitando revelar su existencia.


## 5. Actores, roles y permisos

Sillón contempla tres roles de usuarios autenticados: DUENO, ESTILISTA y ADMIN. También permite determinadas operaciones públicas sin autenticación.

### 5.1. DUENO

Es el responsable de administrar su salón.

Puede:

- Consultar y configurar su salón.
- Crear, consultar y modificar servicios.
- Registrar estilistas y consultar su equipo.
- Consultar los clientes de su salón.
- Crear citas y consultar toda la agenda de su salón.
- Modificar el estado de las citas conforme a las transiciones permitidas.
- Generar recordatorios mediante inteligencia artificial.
- Contratar el plan PRO y comprar créditos de IA.
- Consultar y atender las solicitudes generadas por el chatbot.

No puede acceder a información perteneciente a otros salones.

### 5.2. ESTILISTA

Es el profesional encargado de prestar los servicios.

Puede:

- Consultar los servicios de su salón.
- Consultar únicamente sus propias citas.
- Cambiar el estado de sus citas a COMPLETADA o NO_ASISTIO, respetando las reglas de transición.

No puede administrar el salón, crear servicios, contratar planes ni modificar citas de otros estilistas.

### 5.3. ADMIN

Es el administrador general de la plataforma Sillón.

Puede:

- Consultar todos los salones registrados mediante las rutas administrativas.

No tiene automáticamente los permisos operativos del dueño de un salón.

### 5.4. Usuario público

No necesita autenticarse.

Puede:

- Registrar un salón e iniciar sesión mediante las rutas de autenticación.
- Consultar la información pública de un salón mediante su slug.
- Consultar su catálogo y disponibilidad.
- Realizar reservas en línea.
- Utilizar el chatbot público del salón.

### 5.5. Control de acceso

- Las operaciones protegidas requieren autenticación JWT.
- Los permisos se verifican según el rol del usuario.
- La información de cada salón debe permanecer aislada.
- El acceso a recursos de otro salón devuelve HTTP 404.
- Una operación no autorizada por el rol devuelve HTTP 403.
- La ausencia de autenticación válida devuelve HTTP 401.


## 6. Planes comerciales y créditos de IA

Sillón ofrece dos planes comerciales: GRATIS y PRO. También permite comprar paquetes adicionales de créditos de inteligencia artificial.

### 6.1. Plan GRATIS

Es el plan asignado automáticamente cuando se registra un salón.

Características:

- No requiere pago.
- Permite un máximo de dos estilistas.
- Entrega 10 créditos de IA al registrar el salón.
- El intento de registrar un tercer estilista devuelve HTTP 402.
- Permite utilizar las funcionalidades del sistema sujetas a las restricciones del plan y los permisos del usuario.

### 6.2. Plan PRO

Es un plan de suscripción mensual, gestionado mediante Stripe.

Características:

- Permite registrar estilistas sin límite.
- Al confirmarse el pago de activación del plan, se suman 300 créditos de IA.
- El salón conserva su información, servicios, estilistas y citas al cambiar de plan.
- Si el salón ya tiene PRO, solicitar otra suscripción devuelve HTTP 409.
- Cuando Stripe confirma la cancelación de la suscripción, el salón vuelve al plan GRATIS y conserva sus créditos existentes.

### 6.3. Paquete de créditos de IA

El salón puede comprar un paquete de créditos mediante un pago único en Stripe.

- Cada paquete confirmado añade 200 créditos de IA.
- La compra no cambia el plan comercial.
- El backend determina los precios y los identificadores de Stripe; el cliente no puede establecerlos.

### 6.4. Consumo de créditos

- Un crédito equivale a una respuesta de la IA.
- Antes de llamar a un proveedor de IA, el sistema verifica el saldo.
- Si no hay créditos disponibles, responde HTTP 402 sin llamar al proveedor.
- El crédito se descuenta cuando la IA responde.

## 7. Reglas de negocio: salones y cuentas

### 7.1. Registro del salón

- El registro debe crear simultáneamente el salón y su usuario propietario, con rol DUENO.
- La operación debe realizarse en una sola transacción.
- El salón comienza con el plan GRATIS.
- Al registrarse recibe 10 créditos de IA.
- La configuración inicial del salón es:
  - Apertura: 08:00.
  - Cierre: 19:00.
  - No abre los domingos.
  - Intervalo de agenda: 30 minutos.

### 7.2. Correo electrónico

- Cada correo electrónico debe ser único en toda la plataforma.
- La restricción aplica tanto a dueños como a estilistas.
- Los correos se almacenan en minúsculas y sin espacios al principio o al final.
- Si se intenta registrar un correo existente, la API responde HTTP 409.

### 7.3. Identificador público del salón

Cada salón dispone de un slug único, generado a partir de su nombre.

Reglas:

- Convertir el nombre a minúsculas.
- Eliminar tildes.
- Sustituir caracteres que no sean letras o números por guiones.
- Eliminar guiones de los extremos.
- Si el slug ya existe, agregar un sufijo numérico: -2, -3 y así sucesivamente.

Ejemplo:

Barbería La Navaja → barberia-la-navaja

### 7.4. Configuración del horario

El dueño puede configurar:

- Hora de apertura.
- Hora de cierre.
- Apertura los domingos.
- Intervalo de agenda.

Validaciones:

- La apertura debe ser anterior al cierre; de lo contrario, HTTP 409.
- El intervalo debe estar entre 15 y 60 minutos; de lo contrario, HTTP 400.

### 7.5. Aislamiento de información

- Todos los datos operativos pertenecen a un salón.
- Los usuarios solo acceden a información de su propio salón.
- El identificador del salón se obtiene del usuario autenticado.
- Si un recurso pertenece a otro salón, la API responde HTTP 404.


## 8. Reglas de negocio: catálogo y equipo

### 8.1. Servicios

Cada salón administra su propio catálogo de servicios.

Reglas:

- La duración de un servicio debe estar entre 15 y 480 minutos.
- El precio debe ser un número entero de pesos colombianos mayor que cero.
- Si la duración o el precio son inválidos, la API responde HTTP 400.
- Los servicios pertenecen exclusivamente al salón que los registra.
- La consulta o modificación de un servicio perteneciente a otro salón devuelve HTTP 404.

### 8.2. Estilistas

Los estilistas son usuarios vinculados a un salón y tienen el rol ESTILISTA.

Reglas:

- Cada estilista debe ofrecer uno o más servicios.
- Los servicios asignados deben pertenecer al mismo salón que el estilista.
- Si se intenta asignar un servicio de otro salón, la API responde HTTP 404.
- Un mismo correo electrónico no puede utilizarse para registrar dos usuarios en la plataforma.

### 8.3. Restricciones por plan

Plan GRATIS:

- Permite registrar un máximo de dos estilistas.
- Al intentar crear un tercer estilista, la API responde HTTP 402.

Plan PRO:

- Permite registrar estilistas sin límite por plan.

### 8.4. Orden de validaciones al registrar estilistas

El registro de estilistas debe ejecutar las siguientes validaciones en este orden:

1. **Límite del plan (HTTP 402):** verificar si el salón tiene capacidad para registrar otro estilista. En el plan GRATIS se permiten máximo dos. Esta validación tiene prioridad porque representa una restricción comercial de la plataforma y permite rechazar inmediatamente una operación que el plan no autoriza.

2. **Unicidad del correo electrónico (HTTP 409):** normalizar el correo y verificar que no esté registrado en la plataforma. La unicidad debe reforzarse mediante una restricción UNIQUE en la base de datos para evitar duplicaciones ante solicitudes concurrentes.

3. **Pertenencia de los servicios (HTTP 404):** comprobar que todos los identificadores de servicios recibidos existan y pertenezcan al salón del usuario autenticado. Las consultas deben utilizar conjuntamente el identificador del servicio y el del salón, sin confiar en los datos enviados por el cliente.

Este orden prioriza las reglas comerciales, protege la integridad de los usuarios y garantiza el aislamiento multi-inquilino.

El registro debe realizarse de forma transaccional: si alguna validación falla, no se debe persistir parcialmente el estilista ni sus asociaciones con servicios.

La respuesta del registro debe incluir los nombres de los servicios asignados y nunca exponer la contraseña del usuario.

## 9. Reglas de negocio: agenda

### 9.1. Creación de citas

- Cada cita se asocia a un salón, un cliente, un estilista y un servicio.
- La duración de la cita corresponde exactamente a la duración del servicio.
- El sistema calcula automáticamente la hora de finalización.
- La cita conserva el precio del servicio vigente al momento de reservar.
- Los cambios posteriores de precio no modifican las citas existentes.

### 9.2. Validaciones de agenda

No se permite crear una cita:

- En una fecha u hora pasada.
- Fuera del horario de funcionamiento del salón.
- Un domingo cuando el salón no abre los domingos.
- Con un estilista que no ofrece el servicio seleccionado.
- Cuando exista un cruce con otra cita que ocupe agenda del mismo estilista.

Estas infracciones devuelven HTTP 409.

### 9.3. Prevención de cruces

- Un estilista no puede tener dos citas que ocupen simultáneamente su agenda.
- Los estados que ocupan agenda son PENDIENTE y CONFIRMADA.
- Las citas CANCELADA, COMPLETADA y NO_ASISTIO no ocupan agenda.
- Si una nueva cita se cruza con otra que ocupa agenda, la API responde HTTP 409.

### 9.4. Estado y origen inicial

- Una cita creada por el dueño del salón inicia en estado CONFIRMADA y origen SALON.
- Una reserva realizada desde la API pública inicia en estado PENDIENTE y origen EN_LINEA.

### 9.5. Registro del cliente

- Al crear una cita, el sistema busca al cliente por teléfono dentro del salón.
- Si el cliente no existe, se registra automáticamente.
- Si ya existe, se reutiliza su registro.
- El teléfono se almacena únicamente con dígitos y el signo +.

### 9.6. Transiciones de estado

Transiciones permitidas:

- PENDIENTE a CONFIRMADA.
- PENDIENTE a CANCELADA.
- CONFIRMADA a COMPLETADA.
- CONFIRMADA a CANCELADA.
- CONFIRMADA a NO_ASISTIO.

Los estados COMPLETADA, CANCELADA y NO_ASISTIO son finales.

Cualquier transición diferente devuelve HTTP 409.

### 9.7. Permisos del estilista

- Un estilista solo puede modificar sus propias citas.
- Intentar modificar una cita de otro estilista devuelve HTTP 404.
- Solo puede cambiar sus citas a COMPLETADA o NO_ASISTIO, respetando las transiciones permitidas.
- Intentar otro cambio de estado devuelve HTTP 403.

### 9.8. Disponibilidad

La disponibilidad es calculada exclusivamente por Java, nunca por inteligencia artificial.

El sistema debe:

- Generar horarios de inicio según el intervalo configurado por el salón, comenzando desde su hora de apertura.
- Verificar que el servicio termine completamente antes o a la hora de cierre.
- Excluir los horarios que se crucen con citas PENDIENTE o CONFIRMADA.
- Excluir los horarios pasados.
- Considerar si el salón abre los domingos.

### 9.9. Zona horaria

Las reglas relacionadas con fechas y horas utilizan la zona America/Bogota.

El cálculo de "hoy" y "ahora" no debe depender de la zona horaria del servidor.

## 10. Reglas de negocio: pagos con Stripe

### 10.1. Precios y tipos de compra

- El backend determina los precios de los productos.
- El cliente de la API únicamente solicita la suscripción al plan PRO o la compra de un paquete de créditos.
- El cliente no puede enviar precios ni identificadores de precios de Stripe.

Tipos de compra:

- SUSCRIPCION_PRO.
- PAQUETE_CREDITOS.

### 10.2. Suscripción PRO

- El salón puede solicitar una sesión de pago para contratar el plan PRO.
- Si el salón ya tiene el plan PRO, solicitar nuevamente una suscripción devuelve HTTP 409.
- El cambio de plan se realiza cuando Stripe confirma el pago mediante el webhook.
- Al confirmar el pago, el salón pasa al plan PRO, guarda el identificador de la suscripción y recibe 300 créditos de IA.

### 10.3. Compra de créditos

- El salón puede realizar un pago único para comprar créditos de IA.
- Cuando Stripe confirma el pago, se agregan 200 créditos al saldo del salón.
- La compra de créditos no modifica el plan del salón.

### 10.4. Webhook de Stripe

- El webhook recibe eventos enviados por Stripe.
- Cada evento debe tener una firma válida.
- Si no existe firma o la firma es incorrecta, la API devuelve HTTP 400.
- Cada evento se identifica mediante su identificador único de Stripe.
- Un evento solo se procesa una vez.
- Si llega nuevamente un evento ya procesado, se ignora para evitar repetir sus efectos.

### 10.5. Cancelación de la suscripción

- Cuando Stripe confirma la cancelación de una suscripción, el salón vuelve al plan GRATIS.
- El salón conserva los créditos de IA que tenga disponibles.

## 11. Reglas de negocio: inteligencia artificial

### 11.1. Créditos de IA

- Cada respuesta de inteligencia artificial consume un crédito.
- Antes de realizar una solicitud al proveedor, el sistema verifica que el salón tenga créditos disponibles.
- Si el salón no dispone de créditos, la API responde HTTP 402 y no llama al proveedor.
- El crédito se descuenta cuando la IA responde.

### 11.2. Recordatorios de citas

- Solo se pueden generar recordatorios para citas en estado PENDIENTE o CONFIRMADA.
- Si la cita tiene otro estado, la API responde HTTP 409.
- Java obtiene y verifica los datos de la cita: servicio, estilista, fecha, hora, precio y dirección.
- La inteligencia artificial únicamente redacta el mensaje usando los datos proporcionados.
- El recordatorio queda disponible para aprobación humana.
- Sillón no envía automáticamente el recordatorio.

### 11.3. Chatbot del salón

- El chatbot responde preguntas utilizando la información real del catálogo del salón.
- Los datos del catálogo se obtienen de la base de datos.
- El chatbot no puede crear, modificar ni cancelar citas.
- Si un cliente solicita estas operaciones, el chatbot debe escalar la conversación.
- Al escalar, el sistema crea una solicitud de contacto que conserva los mensajes del cliente.

### 11.4. Protección contra promesas comerciales

- Java debe revisar el contenido de las respuestas del chatbot.
- Si la respuesta menciona descuentos, cupones, productos o servicios gratis, promociones o reembolsos, no debe entregarse esa promesa al cliente.
- En esos casos, el sistema devuelve un mensaje seguro fijo.
- También crea una solicitud de contacto con motivo REGLA_DE_SEGURIDAD.

### 11.5. Validación de respuestas

- Todas las respuestas de los proveedores de IA se solicitan en formato JSON.
- Java valida que el JSON sea correcto y contenga los datos requeridos.
- Si el proveedor devuelve JSON inválido o incompleto, la API responde HTTP 502.
- Si ningún proveedor de IA está disponible, la API responde HTTP 503.

### 11.6. Seguridad de credenciales

- Las claves de los proveedores se almacenan en secrets.properties para desarrollo local o en variables de entorno para producción.
- Nunca se incluyen credenciales reales en el código fuente.
- Nunca se suben claves reales a GitHub.
- Nunca se incluyen credenciales en los prompts enviados a la inteligencia artificial.

## 12. Contrato de la API REST

La API de Sillón debe respetar exactamente los métodos HTTP, rutas, campos y códigos de respuesta establecidos en el enunciado oficial.

Los endpoints protegidos reciben el token JWT mediante la cabecera Authorization con el formato Bearer.

### 12.1. Autenticación

#### POST /api/auth/registro

Acceso: público.

Campos de entrada:

- salonNombre: máximo 100 caracteres.
- ciudad: máximo 60 caracteres.
- direccion: máximo 150 caracteres.
- telefono: máximo 30 caracteres.
- nombre: máximo 100 caracteres.
- email: formato válido.
- password: entre 8 y 72 caracteres.

Respuesta exitosa: HTTP 201.

Campos de respuesta:

- token
- tipo: "Bearer"
- expiraEnMinutos
- usuarioId
- nombre
- rol
- salonId
- salonSlug

Errores previstos: HTTP 400 y 409.

#### POST /api/auth/login

Acceso: público.

Campos de entrada:

- email
- password

Respuesta exitosa: HTTP 200, con el mismo objeto de respuesta del registro.

Error previsto: HTTP 401.

### 12.2. Mi salón

#### GET /api/salon

Acceso: DUENO.

Respuesta exitosa: HTTP 200.

Campos:

- id
- nombre
- slug
- ciudad
- direccion
- telefono
- plan
- creditosIa
- configuracion
  - horaApertura
  - horaCierre
  - abreDomingos
  - intervaloMinutos
- fechaCreacion

#### PUT /api/salon/configuracion

Acceso: DUENO.

Campos de entrada, todos obligatorios:

- horaApertura
- horaCierre
- abreDomingos
- intervaloMinutos: entre 15 y 60.

Respuesta exitosa: HTTP 200 con el salón actualizado.

Errores previstos: HTTP 400 y HTTP 409 cuando la apertura no es anterior al cierre.

### 12.3. Servicios

#### POST /api/servicios

Acceso: DUENO.

Campos de entrada:

- nombre: máximo 80 caracteres.
- descripcion: opcional, máximo 300 caracteres.
- duracionMinutos: entre 15 y 480.
- precio: entero mayor que cero.
- activo: opcional; por defecto true.

Respuesta exitosa: HTTP 201.

Campos de respuesta:

- id
- nombre
- descripcion
- duracionMinutos
- precio
- activo

Error previsto: HTTP 400.

#### GET /api/servicios

Acceso: DUENO y ESTILISTA.

Respuesta exitosa: HTTP 200 con la lista de servicios del salón.

#### PUT /api/servicios/{id}

Acceso: DUENO.

Recibe los mismos campos que POST /api/servicios.

Respuesta exitosa: HTTP 200.

Errores previstos: HTTP 400 y 404.


### 12.4. Estilistas y clientes

#### POST /api/estilistas

Acceso: DUENO.

Campos de entrada:

- nombre
- email
- password: entre 8 y 72 caracteres.
- servicioIds: lista con al menos un identificador de servicio.

Respuesta exitosa: HTTP 201.

Campos de respuesta:

- id
- nombre
- email
- activo
- servicios: lista de nombres de los servicios.

Errores previstos: HTTP 400, 402, 404 y 409.

#### GET /api/estilistas

Acceso: DUENO.

Respuesta exitosa: HTTP 200 con la lista de estilistas del salón.

#### GET /api/clientes

Acceso: DUENO.

Respuesta exitosa: HTTP 200 con la lista de clientes del salón.

Cada cliente contiene:

- id
- nombre
- telefono

### 12.5. Agenda

#### POST /api/citas

Acceso: DUENO.

Campos de entrada:

- clienteNombre
- clienteTelefono: entre 7 y 30 caracteres.
- servicioId
- estilistaId
- inicio
- nota: opcional, máximo 300 caracteres.

Respuesta exitosa: HTTP 201 con los datos de la cita.

Errores previstos: HTTP 400, 404 y 409.

#### GET /api/citas?fecha=

Acceso: DUENO y ESTILISTA.

Parámetro de consulta:

- fecha: opcional; si no se proporciona, se utiliza la fecha actual en la zona horaria de Colombia.

Respuesta exitosa: HTTP 200.

- Devuelve las citas del día, ordenadas por hora.
- El dueño consulta las citas de su salón.
- El estilista consulta únicamente sus propias citas.

#### PATCH /api/citas/{id}/estado

Acceso: DUENO y ESTILISTA.

Campo de entrada:

- estado

Respuesta exitosa: HTTP 200 con los datos actualizados de la cita.

Errores previstos: HTTP 400, 403, 404 y 409.

#### POST /api/citas/{id}/recordatorio

Acceso: DUENO.

No requiere cuerpo de solicitud.

Respuesta exitosa: HTTP 200.

Campos de respuesta:

- citaId
- telefonoCliente
- mensaje
- creditosRestantes
- factura

Errores previstos: HTTP 402, 404, 409, 502 y 503.

### 12.5.1. Campos de respuesta de una cita

Una cita se devuelve con los siguientes campos:

- id
- inicio
- fin
- estado
- origen
- precio
- servicioId
- servicioNombre
- estilistaId
- estilistaNombre
- clienteNombre
- clienteTelefono
- nota
- fechaCreacion
- fechaActualizacion

### 12.5.2. Factura de inteligencia artificial

El objeto factura contiene:

- proveedor
- modelo
- tokensEntrada
- tokensSalida
- costoEstimadoUsd
- milisegundos

### 12.6. API pública

Las siguientes operaciones no requieren autenticación JWT.

#### GET /api/publico/salones/{slug}

Respuesta exitosa: HTTP 200.

Campos de respuesta:

- nombre
- slug
- ciudad
- direccion
- telefono
- horario: texto descriptivo.
- servicios: únicamente los servicios activos.
- estilistas: lista con id, nombre y servicioIds.

La respuesta nunca debe incluir correos electrónicos de los estilistas.

Error previsto: HTTP 404.

#### GET /api/publico/salones/{slug}/disponibilidad

Parámetros de consulta, todos obligatorios:

- servicioId
- estilistaId
- fecha

Respuesta exitosa: HTTP 200.

Campos de respuesta:

- fecha
- servicio
- estilista
- duracionMinutos
- horariosLibres: lista de horas disponibles.

Errores previstos: HTTP 400, 404 y 409.

La disponibilidad debe calcularse en Java respetando las reglas de agenda establecidas en la sección 9.

#### POST /api/publico/salones/{slug}/reservas

Recibe los mismos campos que POST /api/citas:

- clienteNombre
- clienteTelefono
- servicioId
- estilistaId
- inicio
- nota

Respuesta exitosa: HTTP 201 con la cita creada.

La cita debe iniciar con:

- estado: PENDIENTE
- origen: EN_LINEA

Errores previstos: HTTP 400, 404 y 409.

#### POST /api/publico/salones/{slug}/chat

Campos de entrada:

- clienteNombre
- clienteContacto: máximo 120 caracteres.
- historial: entre 1 y 20 turnos.

Cada turno contiene:

- rol: CLIENTE o ASISTENTE.
- texto: máximo 500 caracteres.

Respuesta exitosa: HTTP 200.

Campos de respuesta:

- respuesta
- escalado: true o false.
- solicitudId: identificador de la solicitud o null.
- factura: objeto de consumo de IA.

Errores previstos: HTTP 400, 402, 404, 502 y 503.

### 12.7. Solicitudes del chatbot

#### GET /api/solicitudes

Acceso: DUENO.

Respuesta exitosa: HTTP 200.

Devuelve las solicitudes de contacto del salón, ordenadas desde la más reciente hasta la más antigua.

Cada solicitud contiene:

- id
- clienteNombre
- clienteContacto
- mensajes
- motivo
- estado
- fechaCreacion

#### PATCH /api/solicitudes/{id}/atendida

Acceso: DUENO.

No requiere cuerpo de solicitud.

Respuesta exitosa: HTTP 200.

Errores previstos:

- HTTP 404: la solicitud no existe o pertenece a otro salón.
- HTTP 409: la solicitud ya estaba atendida.

### 12.8. Pagos con Stripe

#### POST /api/pagos/suscripcion

Acceso: DUENO.

No requiere cuerpo de solicitud.

Respuesta exitosa: HTTP 200.

Campos de respuesta:

- tipo: SUSCRIPCION_PRO
- sesionId
- urlPago

Errores previstos:

- HTTP 409: el salón ya tiene el plan PRO.
- HTTP 502: error en la integración con Stripe.

#### POST /api/pagos/creditos

Acceso: DUENO.

No requiere cuerpo de solicitud.

Respuesta exitosa: HTTP 200.

Campos de respuesta:

- tipo: PAQUETE_CREDITOS
- sesionId
- urlPago

Error previsto: HTTP 502.

#### POST /api/pagos/webhook

Acceso: Stripe, sin autenticación JWT.

Recibe:

- Evento enviado por Stripe.
- Cabecera Stripe-Signature.

Respuesta exitosa: HTTP 200.

Campos de respuesta:

- recibido
- procesado

Error previsto: HTTP 400 si la firma no es válida.

#### GET /api/pagos/resultado?estado=

Acceso: público.

Parámetro de consulta:

- estado: exito o cancelado.

Respuesta exitosa: HTTP 200 con un mensaje.

Esta ruta representa el destino al que vuelve el usuario después del proceso de pago de Stripe.

### 12.9. Administración

#### GET /api/admin/salones

Acceso: ADMIN.

No requiere parámetros de entrada.

Respuesta exitosa: HTTP 200 con la lista de todos los salones registrados en la plataforma.

Error previsto:

- HTTP 403: un usuario con un rol diferente de ADMIN intenta acceder a esta operación.

### 12.10. Creación del administrador inicial

El primer usuario ADMIN se configura siguiendo el procedimiento establecido por la profesora:

- Registrar inicialmente una cuenta normal.
- Cambiar directamente su rol a ADMIN en la base de datos.
- Este procedimiento corresponde a la configuración inicial de la plataforma y no constituye un endpoint adicional.


## 13. Formato único de errores

Todas las respuestas de error de la API, incluidos los errores de autenticación y autorización, deben utilizar el mismo objeto JSON.

### 13.1. Campos obligatorios

- codigo: código numérico HTTP.
- error: nombre estándar del estado HTTP.
- mensaje: explicación del error en español.
- fecha: fecha y hora del error.

En los errores de validación, el campo mensaje debe incluir los campos que fallaron y sus respectivos mensajes, separados por punto y coma.

### 13.2. Códigos de error

- HTTP 400: datos inválidos, JSON mal formado, parámetros faltantes o firma de webhook inválida.
- HTTP 401: ausencia de token, token inválido o credenciales incorrectas.
- HTTP 402: límite del plan GRATIS o ausencia de créditos de IA.
- HTTP 403: operación no permitida por el rol o transición no autorizada para un estilista.
- HTTP 404: recurso inexistente o perteneciente a otro salón.
- HTTP 409: incumplimiento de reglas de negocio, como cruces de agenda, horarios inválidos, transiciones no permitidas, correo duplicado o suscripción PRO ya activa.
- HTTP 502: respuesta inválida o no utilizable de Stripe o del proveedor de IA.
- HTTP 503: ningún proveedor de inteligencia artificial disponible.

### 13.3. Ejemplo de error

El siguiente ejemplo ilustra la estructura exigida:

```json
{
  "codigo": 409,
  "error": "Conflict",
  "mensaje": "El horario solicitado no está disponible",
  "fecha": "2026-11-18T10:30:00"
}
```
## 14. Convenciones de datos

### 14.1. Fechas y horas

- Las fechas se representan con el formato AAAA-MM-DD.
- Las fechas y horas se representan con el formato AAAA-MM-DDTHH:mm:ss.
- Las horas se envían con el formato HH:mm.
- Jackson devuelve las horas con el formato HH:mm:ss.
- Las reglas relacionadas con el tiempo utilizan la zona horaria America/Bogota.

### 14.2. Precios

- Los precios se representan como números enteros en pesos colombianos.
- No se utilizan puntos de miles ni valores decimales.
- Ejemplo: 35000 representa treinta y cinco mil pesos colombianos.

## 15. Funcionalidades fuera de alcance

El proyecto Sillón no contempla:

- Envío real de mensajes por WhatsApp o correo electrónico.
- Recordatorios automáticos programados.
- Generación de reportes.
- Pagos del cliente final directamente al salón mediante Stripe Connect.
- Desarrollo de una interfaz gráfica propia.

La aplicación será una API REST que se consumirá mediante Postman y otros clientes HTTP.

## 16. Modelo de datos y enumeraciones

### 16.1. Entidades y relaciones

El sistema utiliza Spring Data JPA para gestionar la persistencia de información.

Las entidades y atributos deben corresponder al modelo establecido en el enunciado oficial.

#### Salon (tabla: salones)

Atributos:
- id
- nombre
- slug (único)
- ciudad
- direccion
- telefono
- plan
- creditosIa
- stripeSuscripcionId
- configuracion
- fechaCreacion

Relaciones:
- Tiene usuarios, servicios, clientes, citas y solicitudes de contacto.

#### ConfiguracionSalon

Clase embebida en Salon mediante @Embeddable.

Atributos:
- horaApertura
- horaCierre
- abreDomingos
- intervaloMinutos

#### Usuario (tabla: usuarios)

Atributos:
- id
- nombre
- email (único)
- password (BCrypt)
- rol
- activo

Relaciones:
- Muchos a uno con Salon.
- Muchos a muchos con Servicio mediante la tabla estilista_servicios.

#### Servicio (tabla: servicios)

Atributos:
- id
- nombre
- descripcion
- duracionMinutos
- precio
- activo

Relaciones:
- Muchos a uno con Salon.

#### Cliente (tabla: clientes)

Atributos:
- id
- nombre
- telefono

Relaciones:
- Muchos a uno con Salon.
- El teléfono es único dentro de cada salón.

#### Cita (tabla: citas)

Atributos:
- id
- inicio
- fin
- precio
- estado
- origen
- nota
- fechaCreacion
- fechaActualizacion

Relaciones:
- Muchos a uno con Salon.
- Muchos a uno con Cliente.
- Muchos a uno con Usuario (estilista).
- Muchos a uno con Servicio.

#### SolicitudContacto (tabla: solicitudes_contacto)

Atributos:
- id
- clienteNombre
- clienteContacto
- mensajes
- motivo
- estado
- fechaCreacion

Relaciones:
- Muchos a uno con Salon.

#### EventoStripe (tabla: eventos_stripe)

Atributos:
- id (identificador del evento de Stripe)
- tipo
- fechaProcesado

No tiene relaciones con otras entidades.
Su finalidad es evitar procesar dos veces un mismo evento.

### 16.2. Enumeraciones

#### Plan
- GRATIS
- PRO

#### Rol
- DUENO
- ESTILISTA
- ADMIN

#### EstadoCita
- PENDIENTE
- CONFIRMADA
- COMPLETADA
- CANCELADA
- NO_ASISTIO

#### OrigenCita
- SALON
- EN_LINEA

#### TipoCompra
- SUSCRIPCION_PRO
- PAQUETE_CREDITOS

#### MotivoSolicitud
- PEDIDO_POR_IA
- REGLA_DE_SEGURIDAD

#### EstadoSolicitud
- PENDIENTE
- ATENDIDA

### 16.3. Persistencia de enumeraciones

Todas las enumeraciones se almacenan como texto utilizando EnumType.STRING.

Los identificadores deben conservar exactamente los nombres establecidos en el enunciado, sin tildes ni modificaciones.


