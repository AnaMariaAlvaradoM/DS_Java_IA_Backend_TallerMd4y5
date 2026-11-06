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