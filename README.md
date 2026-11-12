Sillón — Agenda inteligente para barberías y salones de belleza
Proyecto integrador · Java AI Engineer · Dev Senior Code
Sillón es una plataforma SaaS desarrollada con Java y Spring Boot para la gestión de barberías y salones de belleza. Centraliza la administración de salones, usuarios, estilistas, servicios, clientes y citas, y ofrece herramientas para organizar agendas, gestionar reservas en línea y mejorar la atención al cliente mediante inteligencia artificial (recordatorios y chatbot).
El sistema utiliza una arquitectura multi-tenant: múltiples salones comparten la misma plataforma y la misma base de datos, manteniendo aislada la información de cada negocio.
> **Fuente de verdad:** [`SPEC.md`](SPEC.md) define el modelo, las reglas de negocio y el contrato de la API. Si este README y el SPEC difieren, prevalece el SPEC.
---
Tabla de contenido
Descripción del proyecto
Producción
Funcionalidades del sistema
Arquitectura del sistema
Modelo de dominio
Convenciones de nombres
Tecnologías
Usuarios, roles y permisos
Planes y créditos
Reglas de negocio principales
API REST
Formato de errores HTTP
Instalación y ejecución local
Configuración y variables
Inteligencia artificial
Pagos y suscripciones
Pruebas y verificación
Despliegue
Fases de desarrollo
Documentación del repositorio
Entregables y calificación
Errores frecuentes
Criterios de evolución técnica
Autor y propósito académico
---
1. Descripción del proyecto
Muchas barberías y salones administran sus citas por WhatsApp, llamadas, cuadernos o de memoria. Eso provoca:
Citas cruzadas y reservas duplicadas del mismo estilista.
Pérdida de clientes por falta de respuesta.
Horarios desorganizados.
Ausencias de clientes y sillas vacías (una hora que ya no se recupera).
Imposibilidad de consultar precios y disponibilidad fuera del horario laboral.
Sillón propone una solución digital centralizada: cada salón se registra, configura su horario, servicios y equipo, y desde ese momento sus clientes reservan en línea viendo solo horarios realmente libres, el sistema impide citas cruzadas o fuera de horario, el dueño genera recordatorios con IA y un asistente virtual atiende consultas y escala a una persona lo que no puede resolver.
Objetivos principales
Administrar múltiples salones desde una misma plataforma.
Proteger los datos de cada negocio mediante aislamiento multi-tenant en el backend.
Gestionar usuarios, estilistas y servicios.
Registrar clientes automáticamente al agendar.
Controlar citas, estados y disponibilidad.
Incorporar IA para recordatorios (borrador que una persona aprueba) y chatbot con escalamiento.
Registrar solicitudes de contacto y escalamientos.
Integrar Stripe (modo prueba) para suscripción PRO y paquetes de créditos de IA.
Ofrecer una API documentada con Swagger y desplegada en Render.
Fuera de alcance
Envío real de mensajes por WhatsApp o correo.
Recordatorios automáticos programados y reportes o indicadores.
Pagos del cliente final al salón (requeriría Stripe Connect).
Interfaz gráfica: el cliente de la API es Postman (o, en la vida real, una app web o móvil).
Cobros reales con Stripe: el proyecto permanece en modo prueba.
---
2. Producción
> Reemplaza estas direcciones por las reales (completas, con `https://`) cuando el despliegue esté hecho y verificado. El calificador automático lee la URL de Render de esta sección.
```text
API:     https://TU-SERVICIO.onrender.com
Swagger: https://TU-SERVICIO.onrender.com/swagger-ui.html
```
El plan gratuito de Render "duerme" el servicio cuando no se usa: la primera petición puede tardar mientras despierta. No es un error.
---
3. Funcionalidades del sistema
Módulo	Funcionalidad
Salones	Registro del salón junto con su dueño; slug público único.
Configuración	Horario de apertura y cierre, domingos e intervalo de agenda (15 a 60 min).
Usuarios y seguridad	Cuentas con roles `DUENO`, `ESTILISTA` y `ADMIN`; autenticación JWT; acceso por rol.
Servicios	Catálogo del salón con duración (15–480 min) y precio entero en COP.
Estilistas	Asociación de estilistas con los servicios que ofrecen; límite de 2 en plan `GRATIS`.
Clientes	Creados automáticamente al agendar; únicos por teléfono dentro del salón.
Agenda	Creación, consulta por día y cambio de estado de citas, sin cruces ni fuera de horario.
Disponibilidad	Horarios libres calculados en Java según configuración, duración del servicio y citas ocupadas.
Cara pública	Catálogo del salón, disponibilidad y reservas en línea sin iniciar sesión.
Recordatorio con IA	Borrador de mensaje para WhatsApp con datos verificados por Java; una persona lo aprueba.
Chatbot	Asistente que responde con el catálogo real del salón y escala cuando corresponde.
Solicitudes de contacto	Registro y seguimiento de escalamientos (`PEDIDO_POR_IA`, `REGLA_DE_SEGURIDAD`).
Créditos de IA	Un crédito por respuesta de la IA; control por salón.
Stripe	Suscripción PRO y paquetes de créditos con webhook idempotente.
Administración	`ADMIN` lista todos los salones de la plataforma.
---
4. Arquitectura del sistema
Backend organizado por dominios de negocio y por capas. Paquete base: `com.devsenior.sillon`.
Paquete raíz
```text
com.devsenior.sillon
│
├── compartido      Formato de error, manejador global, Clock, configuración de Swagger
├── seguridad       JWT, filtro de autenticación, reglas de acceso por rol
├── auth            Registro del salón con su dueño, login
├── salon           Salón (inquilino), plan, créditos y configuración
├── usuario         Usuarios, roles y estilistas
├── servicio        Catálogo de servicios
├── cliente         Clientes finales del salón
├── cita            Citas, estados, reglas de agenda, disponibilidad y recordatorio
├── publico         Lo que ve el cliente final sin iniciar sesión
├── pago            Sesiones de pago y webhook de Stripe
├── chat            Chatbot del salón y solicitudes de contacto
└── ia              Proveedores de IA, fallback, prompts, lector de JSON y factura
```
Dentro de cada dominio se usan las capas `entidad`, `repositorio`, `servicio`, `controlador` y `dto`.
La estructura definitiva debe respetar los paquetes que existan realmente en el repositorio. No se crean paquetes duplicados ni se reorganiza el proyecto sin justificación.
Responsabilidad de las capas
Capa	Responsabilidad
Entidad	Representar los datos persistidos y sus relaciones.
Repositorio	Acceso a la base de datos.
Servicio	Implementar las reglas de negocio.
Controlador	Exponer los endpoints REST, sin reglas de negocio.
DTO	Datos de entrada y salida de la API (`record`).
Configuración	Seguridad, proveedores y componentes técnicos.
Las entidades JPA nunca se exponen directamente en las respuestas HTTP.
Arquitectura multi-tenant
Cada salón administra sus propios usuarios, servicios, clientes, citas y solicitudes.
El `salonId` se obtiene siempre del usuario autenticado (JWT, `UsuarioPrincipal`), nunca de la URL ni del cuerpo.
Toda búsqueda por id se hace por id y salón.
Acceder a datos de otro salón responde 404, como si no existieran (así no se revela que existen).
Regla fundamental: ningún usuario consulta ni modifica información de otro salón. El aislamiento se implementa en el backend.
Flujo de una petición privada
```text
Cliente (Postman) ──► Filtro JWT ──► Reglas de acceso por rol ──► Controlador
        ──► Servicio (con salonId del usuario) ──► Repositorio ──► Base de datos
```
---
5. Modelo de dominio
Entidad Java	Tabla	Responsabilidad
`Salon`	`salones`	Salón o barbería registrado; plan, créditos y configuración.
`Usuario`	`usuarios`	Cuenta del salón (dueño, estilista o administrador).
`Servicio`	`servicios`	Servicio ofrecido por el salón.
`Cliente`	`clientes`	Cliente final del salón.
`Cita`	`citas`	Reserva de un cliente con un estilista y un servicio.
`SolicitudContacto`	`solicitudes_contacto`	Solicitudes de atención humana o escalamiento.
`EventoStripe`	`eventos_stripe`	Eventos de Stripe procesados (idempotencia).
5.1 Atributos por entidad
Entidad	Atributos
`Salon`	`id`, `nombre`, `slug` (único), `ciudad`, `direccion`, `telefono`, `plan`, `creditosIa`, `stripeSuscripcionId`, `configuracion`, `fechaCreacion`
`Usuario`	`id`, `nombre`, `email` (único), `password` (BCrypt), `rol`, `activo`
`Servicio`	`id`, `nombre`, `descripcion`, `duracionMinutos`, `precio`, `activo`
`Cliente`	`id`, `nombre`, `telefono`
`Cita`	`id`, `inicio`, `fin`, `precio`, `estado`, `origen`, `nota`, `fechaCreacion`, `fechaActualizacion`
`SolicitudContacto`	`id`, `clienteNombre`, `clienteContacto`, `mensajes`, `motivo`, `estado`, `fechaCreacion`
`EventoStripe`	`id` (id del evento de Stripe), `tipo`, `fechaProcesado`
Solo `Cita` tiene `fechaActualizacion`; `Salon` solo tiene `fechaCreacion`.
5.2 Configuración del salón
`ConfiguracionSalon` es un objeto `@Embeddable` dentro de `Salon` (no es entidad ni tiene tabla propia). Contiene `horaApertura`, `horaCierre`, `abreDomingos` e `intervaloMinutos`.
Valores por defecto al registrarse: 08:00 a 19:00, sin domingos, intervalo de 30 minutos.
5.3 Relaciones entre entidades
Relación	Descripción
`Salon` 1–N `Usuario`	Un salón tiene varios usuarios.
`Salon` 1–N `Servicio`	Un salón tiene varios servicios.
`Salon` 1–N `Cliente`	Un salón registra varios clientes.
`Salon` 1–N `Cita`	Un salón administra varias citas.
`Salon` 1–N `SolicitudContacto`	Un salón registra varias solicitudes.
`Usuario` N–N `Servicio`	Mediante la tabla `estilista_servicios`.
`Cita` N–1 `Cliente`, `Usuario` (estilista), `Servicio`	Cada cita pertenece a uno de cada uno.
`EventoStripe` no tiene relaciones con otras entidades.
5.4 Enumeraciones
Se guardan como texto (`EnumType.STRING`) y sin tildes.
Enumeración	Valores
`Plan`	`GRATIS`, `PRO`
`Rol`	`DUENO`, `ESTILISTA`, `ADMIN`
`EstadoCita`	`PENDIENTE`, `CONFIRMADA`, `COMPLETADA`, `CANCELADA`, `NO_ASISTIO`
`OrigenCita`	`SALON`, `EN_LINEA`
`TipoCompra`	`SUSCRIPCION_PRO`, `PAQUETE_CREDITOS`
`MotivoSolicitud`	`PEDIDO_POR_IA`, `REGLA_DE_SEGURIDAD`
`EstadoSolicitud`	`PENDIENTE`, `ATENDIDA`
5.5 Restricciones
`salones.slug` único.
`usuarios.email` único en toda la plataforma (incluye estilistas).
`clientes`: único por (`salon_id`, `telefono`). Un mismo número puede existir en salones distintos.
Las contraseñas se guardan con BCrypt y nunca se devuelven en respuestas.
No se agregan entidades ni campos sin decisión documentada en el SPEC.
5.6 Estados de la cita
Desde	Transiciones permitidas
`PENDIENTE`	`CONFIRMADA`, `CANCELADA`
`CONFIRMADA`	`COMPLETADA`, `CANCELADA`, `NO_ASISTIO`
`COMPLETADA`, `CANCELADA`, `NO_ASISTIO`	Ninguna (estados finales)
Ocupan agenda solo `PENDIENTE` y `CONFIRMADA`. Cualquier otra transición responde 409.
---
6. Convenciones de nombres
Los nombres técnicos del dominio se escriben en español: entidades, tablas, atributos y columnas, enumeraciones, DTO, repositorios, servicios, controladores, excepciones y métodos del dominio. Tecnologías, frameworks, protocolos y contratos externos pueden conservarse en inglés.
Los atributos Java usan `camelCase` y las columnas SQL `snake_case`.
Concepto	Atributo Java	Columna SQL
Créditos de IA	`creditosIa`	`creditos_ia`
Identificador de suscripción Stripe	`stripeSuscripcionId`	`stripe_suscripcion_id`
Fecha de creación	`fechaCreacion`	`fecha_creacion`
Fecha de actualización	`fechaActualizacion`	`fecha_actualizacion`
Duración en minutos	`duracionMinutos`	`duracion_minutos`
Hora de apertura	`horaApertura`	`hora_apertura`
Hora de cierre	`horaCierre`	`hora_cierre`
Intervalo en minutos	`intervaloMinutos`	`intervalo_minutos`
Inicio / fin de cita	`inicio` / `fin`	`inicio` / `fin`
No se renombran campos del modelo sin documentar y aprobar el cambio en el SPEC.
---
7. Tecnologías
Verifica siempre las versiones exactas contra el `pom.xml`.
Tecnología	Utilización
Java 17	Lenguaje.
Spring Boot 4.1.x	Framework backend.
Maven (con Maven Wrapper)	Dependencias y construcción.
Spring Web	API REST.
Spring Data JPA / Hibernate	Persistencia y ORM.
MySQL	Base de datos local (`sillon_db`).
PostgreSQL (Supabase)	Base de datos de producción.
Spring Security	Seguridad y autorización por rol.
JWT (jjwt)	Autenticación stateless.
BCrypt	Hash de contraseñas.
Jakarta Validation	Validación de datos.
Spring RestClient (HTTP Client)	Llamadas HTTP a proveedores de IA.
Gemini (formato OpenAI)	Proveedor principal de IA, con respaldo en formato Claude y fallback.
Stripe (`stripe-java`)	Pagos en modo prueba.
springdoc-openapi 3.x	Swagger / OpenAPI.
JUnit 5 + Mockito	Pruebas automatizadas.
Postman	Pruebas de API (colección con scripts).
Docker	Contenerización.
Render + Supabase	Despliegue.
GitHub Actions	Calificador automático (no se modifica).
No se usan: Lombok, Spring AI ni SDKs de IA (getters y setters explícitos; la IA se consume por HTTP con `RestClient`). Jackson 3: el `ObjectMapper` vive en `tools.jackson.databind`; solo las anotaciones siguen en `com.fasterxml.jackson.annotation`.
No se agregan dependencias sin justificación técnica y sin respetar `AGENTS.md`.
---
8. Usuarios, roles y permisos
Rol técnico	Responsabilidad
`DUENO`	Propietario del salón: configura, administra equipo, agenda y paga.
`ESTILISTA`	Presta servicios: ve su agenda y marca sus citas.
`ADMIN`	Administrador de la plataforma: ve todos los salones.

Acción	Público	DUENO	ESTILISTA	ADMIN
Registrarse, iniciar sesión	Sí	Sí	Sí	Sí
Ver catálogo, disponibilidad, reservar, chatear	Sí	Sí	Sí	Sí
Ver y configurar su salón	No	Sí	No	No
Crear y editar servicios	No	Sí	No	No
Ver servicios del salón	No	Sí	Sí	No
Crear y ver estilistas, ver clientes	No	Sí	No	No
Crear citas, pedir recordatorios con IA	No	Sí	No	No
Ver la agenda	No	Todas las del salón	Solo las suyas	No
Cambiar estado de una cita	No	Cualquier transición válida	Solo las suyas, a `COMPLETADA` o `NO_ASISTIO`	No
Pagar PRO o créditos, ver solicitudes	No	Sí	No	No
Ver todos los salones	No	No	No	Sí
El primer `ADMIN` se crea registrando una cuenta normal y cambiando su rol a `ADMIN` directamente en la base de datos.
---
9. Planes y créditos
Producto	Pago en Stripe	Qué da al salón
Plan `GRATIS`	Ninguno (al registrarse)	Máximo 2 estilistas y 10 créditos de IA de cortesía
Plan `PRO`	Suscripción mensual (15 USD)	Estilistas sin límite y +300 créditos al activarse
Paquete de créditos	Pago único (5 USD)	+200 créditos
Un crédito = una respuesta de la IA. Cada respuesta cuesta tokens a la plataforma; los créditos evitan que un salón use la IA sin límite.
---
10. Reglas de negocio principales
Las reglas oficiales y completas están en `SPEC.md`. Resumen:
Salones y usuarios
Todo dato pertenece a un salón; datos de otro salón → 404.
Registro: crea salón y dueño en una sola transacción, plan `GRATIS`, 10 créditos y horario por defecto.
Correo único en toda la plataforma (409); se guarda en minúsculas y sin espacios.
Slug generado desde el nombre (minúsculas, sin tildes ni espacios); si existe se agrega `-2`, `-3`…
Horario: apertura anterior al cierre (409); intervalo de 15 a 60 minutos (400).
Servicios y estilistas
Servicio: 15 a 480 minutos; precio entero mayor que cero (400 si no).
Un estilista ofrece uno o más servicios de su mismo salón (servicio ajeno → 404).
Plan `GRATIS`: el 3.er estilista responde 402. Orden de validación: límite del plan, correo único, servicios del salón.
Los servicios inactivos no se ofrecen para nuevas reservas.
Clientes
Pertenecen a un salón; el teléfono es único dentro de él y se guarda solo con dígitos y `+`.
Nacen al agendar (se busca por teléfono; si no existe, se crea). El listado es de solo lectura.
Citas
La cita dura exactamente lo que dura su servicio; Java calcula el fin.
Conserva el precio del servicio al momento de agendar.
No se agenda en el pasado, fuera de horario, en domingo si el salón no abre, ni con un estilista que no ofrece el servicio (409).
Cruce: dos citas se cruzan si `inicioA < finB` y `finA > inicioB`. Una cita que termina a las 10:45 no se cruza con otra que empieza a las 10:45. Solo cuentan `PENDIENTE` y `CONFIRMADA`.
Cita del salón: `CONFIRMADA` / `SALON`. Reserva en línea: `PENDIENTE` / `EN_LINEA`.
Disponibilidad: la calcula Java, nunca la IA.
Zona horaria: "hoy" y "ahora" usan `America/Bogota` mediante un bean `Clock`, aunque el servidor esté en UTC.
Un estilista solo cambia sus citas (ajena → 404) y solo a `COMPLETADA` o `NO_ASISTIO` (otro destino → 403).
Inteligencia artificial
Un crédito por respuesta; se verifica antes de llamar (0 créditos → 402, sin llamar a la IA) y se consume cuando la IA responde.
Java calcula y redacta los datos verificados; la IA solo redacta el mensaje.
El chatbot no agenda, cambia ni cancela citas: escala y crea una solicitud de contacto.
Regla anti-promesas en Java: descuentos, cupones, gratis, promociones o reembolsos se bloquean con un mensaje seguro y motivo `REGLA_DE_SEGURIDAD`.
Respuestas de la IA en JSON validado: inválido → 502; sin proveedores → 503.
Stripe
El backend decide el precio; el cliente nunca envía importes ni ids de precio.
Salón ya `PRO` pidiendo suscripción → 409.
Webhook con firma verificada (400 si falla) y procesamiento idempotente por id de evento.
Pago PRO: plan `PRO`, guarda la suscripción y suma 300 créditos. Paquete: suma 200. Suscripción cancelada: vuelve a `GRATIS` conservando créditos.
---
11. API REST
Todas las rutas privadas reciben `Authorization: Bearer <token>`. Campos, validaciones y respuestas completas: `SPEC.md` §6 y Swagger.
Formatos: fecha `AAAA-MM-DD`; fecha y hora `AAAA-MM-DDTHH:mm:ss`; horas `HH:mm`; precios enteros en pesos (`35000`).
Grupo	Método y ruta	Acceso	Respuestas
Autenticación	`POST /api/auth/registro`	Público	201 · 400 · 409
	`POST /api/auth/login`	Público	200 · 401
Mi salón	`GET /api/salon`	DUENO	200
	`PUT /api/salon/configuracion`	DUENO	200 · 400 · 409
Servicios	`POST /api/servicios`	DUENO	201 · 400
	`GET /api/servicios`	DUENO, ESTILISTA	200
	`PUT /api/servicios/{id}`	DUENO	200 · 400 · 404
Estilistas	`POST /api/estilistas`	DUENO	201 · 400 · 402 · 404 · 409
	`GET /api/estilistas`	DUENO	200
Clientes	`GET /api/clientes`	DUENO	200
Agenda	`POST /api/citas`	DUENO	201 · 400 · 404 · 409
	`GET /api/citas?fecha=`	DUENO, ESTILISTA	200
	`PATCH /api/citas/{id}/estado`	DUENO, ESTILISTA	200 · 400 · 403 · 404 · 409
	`POST /api/citas/{id}/recordatorio`	DUENO	200 · 402 · 404 · 409 · 502 · 503
Público	`GET /api/publico/salones/{slug}`	Público	200 · 404
	`GET /api/publico/salones/{slug}/disponibilidad?servicioId=&estilistaId=&fecha=`	Público	200 · 400 · 404 · 409
	`POST /api/publico/salones/{slug}/reservas`	Público	201 · 400 · 404 · 409
	`POST /api/publico/salones/{slug}/chat`	Público	200 · 400 · 402 · 404 · 502 · 503
Solicitudes	`GET /api/solicitudes`	DUENO	200
	`PATCH /api/solicitudes/{id}/atendida`	DUENO	200 · 404 · 409
Pagos	`POST /api/pagos/suscripcion`	DUENO	200 · 409 · 502
	`POST /api/pagos/creditos`	DUENO	200 · 502
	`POST /api/pagos/webhook`	Stripe (firma)	200 · 400
	`GET /api/pagos/resultado?estado=`	Público	200
Administración	`GET /api/admin/salones`	ADMIN	200 · 403
Rutas públicas de seguridad: `/api/auth/**`, `/api/publico/**`, webhook y resultado de pago, Swagger y `/error`.
Los nombres de rutas son contratos HTTP: se respetan al pie de la letra.
---
12. Formato de errores HTTP
Todas las respuestas de error (incluidos 401 y 403) usan el mismo formato:
```json
{
  "codigo": 409,
  "error": "Conflict",
  "mensaje": "La cita se cruza con otra del estilista",
  "fecha": "2026-11-06T10:30:00"
}
```
En errores de validación, `mensaje` lista los campos que fallaron con su mensaje, separados por punto y coma.
Código	Cuándo se usa
400	Datos inválidos, JSON mal formado, parámetro faltante, firma de webhook inválida.
401	Sin token, token inválido o credenciales incorrectas.
402	Límite del plan `GRATIS` o salón sin créditos de IA.
403	El rol no tiene permiso o un estilista intenta una transición no permitida.
404	No existe o pertenece a otro salón.
409	Choca con una regla de negocio: cruce, horario, transición, correo repetido, plan ya activo.
502	Stripe o la IA respondieron algo inutilizable.
503	Ningún proveedor de IA está disponible.
---
13. Instalación y ejecución local
Requisitos previos
JDK 17.
IntelliJ IDEA, Cursor u otro IDE compatible.
MySQL en ejecución.
Git.
Postman.
Stripe CLI (para probar webhooks en local).
Docker (para el despliegue).
13.1. Clonar el repositorio
```bash
git clone URL_DE_TU_FORK
cd sillon
```
El proyecto vive en la raíz del fork: el `pom.xml` queda junto al README.
13.2. Base de datos
La aplicación usa MySQL local con la base `sillon_db` y la crea automáticamente. Si prefieres crearla a mano:
```sql
CREATE DATABASE sillon_db;
```
13.3. Secretos
Copia `secrets.example.properties` a `secrets.properties` en la raíz.
Completa tus valores reales: clave secreta de Stripe de prueba, ids de los dos precios, secreto del webhook, llave de Gemini y secreto JWT.
`secrets.properties` está en `.gitignore`: nunca se sube. La aplicación arranca aunque no exista.
Nunca deben publicarse contraseñas de bases de datos, secretos JWT, llaves de IA, claves privadas de Stripe ni tokens de servicios externos.
13.4. Compilar
```bash
./mvnw clean compile
```
En Windows PowerShell: `.\mvnw.cmd clean compile`.
13.5. Ejecutar las pruebas
```bash
./mvnw test
```
El test que levanta el contexto completo requiere MySQL encendido.
13.6. Webhooks de Stripe en local
En una terminal aparte:
```bash
stripe listen --forward-to localhost:8080/api/pagos/webhook
```
Copia el secreto `whsec_…` que imprime en `secrets.properties` y reinicia la aplicación.
13.7. Iniciar la aplicación
```bash
./mvnw spring-boot:run
```
El puerto se lee de la variable `PORT` (8080 por defecto).
13.8. Swagger
```text
http://localhost:8080/swagger-ui.html
```
Usa el botón Authorize con el token del dueño para probar rutas protegidas.
---
14. Configuración y variables
La configuración vive en `application.properties` (no `.yml`, sin perfiles). Cada valor secreto tiene una variable de entorno con valor de relleno para que la aplicación siempre arranque.
Categoría	Descripción
Aplicación	`PORT` (8080 por defecto), zona horaria `America/Bogota`, URL pública de la API.
Base de datos	URL JDBC, usuario y contraseña (`SPRING_DATASOURCE_*`); `ddl-auto=update`; `open-in-view=false`.
Seguridad	Secreto JWT (`JWT_SECRETO`) y expiración.
Negocio	Créditos de registro (10), créditos PRO (300), créditos del paquete (200), límite de estilistas `GRATIS` (2).
Stripe	Clave secreta, secreto del webhook, ids de precio PRO y de paquete, `STRIPE_URL_API` (por defecto `https://api.stripe.com`).
IA	Llave de Gemini y `sillon.ia.openai.url` (sobrescribible con `SILLON_IA_OPENAI_URL`).
Secretos locales	Importación opcional de `secrets.properties` desde la raíz.
¿Por qué dos direcciones configurables?
El calificador automático no usa el Stripe ni el Gemini reales: usa simuladores, sin llaves y sin costo. Para eso reemplaza `STRIPE_URL_API` y `SILLON_IA_OPENAI_URL`. Nunca se escriben fijas en el código. Hacer configurable la dirección de un servicio externo es una práctica profesional: permite probar sin gastar dinero.
Variables de entorno en Render (solo nombres)
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRETO`, `URL_PUBLICA`, `STRIPE_CLAVE_SECRETA`, `STRIPE_PRECIO_PRO`, `STRIPE_PRECIO_CREDITOS`, `STRIPE_SECRETO_WEBHOOK`, `GEMINI_LLAVE`.
No configurar en Render `STRIPE_URL_API` ni `SILLON_IA_OPENAI_URL`.
---
15. Inteligencia artificial
La IA asiste, pero no es la fuente de verdad del negocio. Las responsabilidades están separadas.
El backend (Java) es responsable de:
Obtener información real del salón desde la base de datos.
Validar servicios, precios y disponibilidad.
Controlar los créditos de IA.
Registrar solicitudes de contacto.
Aplicar las reglas de seguridad (anti-promesas).
Gestionar errores y tiempos de espera.
La IA solo interpreta consultas y redacta respuestas con los datos que Java le entrega.
Motor de IA (paquete `ia`)
Interfaz de proveedor; proveedor con formato OpenAI (Gemini); proveedor de respaldo con formato Claude; asistente con fallback entre proveedores; configuración tipada; fábrica HTTP con tiempo máximo; biblioteca de prompts; lector de JSON; y factura (`proveedor`, `modelo`, `tokensEntrada`, `tokensSalida`, `costoEstimadoUsd`, `milisegundos`).
Orden de cada funcionalidad con IA
Validar lo que no depende de la IA (que exista la cita o el salón).
Verificar créditos (402 si no hay; la IA no se llama).
Cargar el prompt y llamar a la IA.
Consumir el crédito (una sola actualización atómica; nunca queda negativo).
Leer y validar el JSON.
Aplicar reglas de negocio y responder.
Ninguna llamada a la IA va dentro de una transacción de base de datos.
Recordatorio de cita
`POST /api/citas/{id}/recordatorio` — solo citas `PENDIENTE` o `CONFIRMADA` (otra → 409, sin gastar créditos). Java arma los datos verificados (servicio y duración, estilista, fecha en español, hora tipo `10:30 a. m.`, valor `$35.000`, dirección). La IA redacta un borrador para WhatsApp en JSON con la clave `mensaje` (máximo 700 caracteres). Una persona lo aprueba: Sillón no envía mensajes.
Chatbot del salón
`POST /api/publico/salones/{slug}/chat` — responde con el catálogo real del salón; no agenda, cambia ni cancela citas. El historial completo llega en cada petición (1 a 20 turnos) porque el modelo no tiene memoria; el texto del cliente nunca va dentro de las instrucciones del prompt. Respuesta JSON con `respuesta` y `escalar`.
Si escala: se crea una `SolicitudContacto` con motivo `PEDIDO_POR_IA`.
Regla anti-promesas: si la respuesta contiene `descuento`, `cupón`/`cupon`, `gratis`, `promoción`/`promocion` o `reembols` (sin importar mayúsculas), el cliente recibe un mensaje seguro fijo y se crea la solicitud con motivo `REGLA_DE_SEGURIDAD`. Se acepta el falso positivo a propósito. Un prompt pide; solo el código garantiza.
Prompts
Viven en `src/main/resources/prompts/` (`recordatorio.txt` y `chatbot.txt`), nunca incrustados en el código. Piden las claves exactas: `mensaje`; `respuesta` y `escalar`.
Errores de la IA
JSON inválido o incompleto → 502. Ningún proveedor disponible → 503. En los logs, `ERROR` indica configuración y `WARN` un problema temporal.
---
16. Pagos y suscripciones
Integración con Stripe en modo prueba (Stripe no permite cobros reales a negocios con sede en Colombia).
Productos a crear en Stripe:
Producto	Precio	Qué copiar
Sillón PRO	Recurrente mensual, 15 USD	Id del precio (`price_…`)
Paquete de 200 créditos de IA	Pago único, 5 USD	Id del precio (`price_…`)
Flujo
El dueño llama a `POST /api/pagos/suscripcion` o `POST /api/pagos/creditos`.
El backend elige el precio (modo suscripción para PRO, modo pago para el paquete) y crea una sesión de Checkout con el correo del dueño, URLs de éxito/cancelación, el id del salón como referencia y metadata (salón y tipo de compra).
El dueño abre `urlPago` y paga. Tarjeta de prueba: `4242 4242 4242 4242`, cualquier fecha futura y cualquier CVC. Pago rechazado: `4000 0000 0000 0002`.
Stripe notifica al webhook `POST /api/pagos/webhook`.
Webhook
Recibe el cuerpo como texto plano y la cabecera `Stripe-Signature`; sin firma o falsa → 400.
Si el id del evento ya existe en `eventos_stripe`, responde `procesado: false` y no toca nada (idempotencia).
`checkout.session.completed` (estado `paid`): aplica la compra según la metadata. `customer.subscription.deleted`: devuelve el salón a `GRATIS` (conserva créditos).
Aplicar el cambio y guardar el evento ocurre en una sola transacción.
Para probar la cancelación: en el Dashboard de Stripe, cancela la suscripción de inmediato; llegará `customer.subscription.deleted`.
El backend nunca confía en importes enviados por el cliente.
---
17. Pruebas y verificación
La evidencia es ejecutable, no pantallazos.
17.1 Tests JUnit 5 + Mockito
Los tests de servicios no levantan Spring: usan mocks de repositorios e IA, y un reloj fijo. Ninguno llama a Gemini ni a Stripe. Casos mínimos:
Área	Casos
Estados de la cita	5 transiciones permitidas y al menos 6 bloqueadas (parametrizado)
Reglas de agenda	Dentro de horario, termina tras cierre, empieza antes de abrir, domingo cerrado, horarios libres sin cruces, día cerrado, hoy sin horas pasadas
Agendar y estado	Cita válida `CONFIRMADA` con precio del servicio, pasado, cruce, estilista sin el servicio, cita ajena (404), estilista cancelando (403), transición inválida
Plan	3.er estilista en `GRATIS` (402); en `PRO` sí se crea
Webhook	PRO activa plan y suma, paquete suma, evento repetido ignorado, cancelación a `GRATIS`, firma falsa
Chatbot	Sin créditos no llama a la IA, pregunta normal consume 1 crédito, escalamiento crea solicitud, cupón prometido → mensaje seguro
Recordatorio	Respuesta válida con créditos restantes; JSON sin `mensaje` → 502
Motor de IA	Fallback, clasificación de errores, biblioteca de prompts, lector de JSON
Aplicación	Test de contexto completo (requiere MySQL)
Nunca se modifica un test para que pase. No se afirma que una prueba pasó sin haberla ejecutado.
17.2 Colección de Postman
En `postman/`: `sillon.postman_collection.json` y los entornos `local` y `produccion` (cada uno con la variable `baseUrl`).
Siete carpetas, en este orden:
Registro, login y configuración
Catálogo y equipo
Agenda: reservas, cruces y estados
Aislamiento entre salones y roles
IA: recordatorio y chatbot
Pagos con Stripe (modo prueba)
Después de pagar (manual)
Reglas para poder correrla muchas veces: datos únicos por ejecución (sufijo con la hora), fechas calculadas por script (nunca fijas) y nada a mano entre peticiones en las carpetas 1 a 6.
17.3 Cómo verificar
Importa la colección y el entorno `local`.
Run collection con las carpetas 1 a 6, de arriba abajo. Todas las pruebas en verde.
Paga el plan PRO con la tarjeta `4242 4242 4242 4242` y ejecuta la carpeta 7 (manual).
Cambia al entorno `produccion` y repite contra Render.
Revisa el comentario del calificador automático en el Pull Request.
17.4 Calificador automático (GitHub Actions)
En cada push a tu fork, el calificador compila el proyecto, corre tus tests, arranca tu API con MySQL y simuladores de Stripe e IA, corre la colección oficial (más de 100 pruebas), revisa el repositorio y prueba tu API desplegada en la URL de Render del README. Deja la nota en el Pull Request. No se modifica la carpeta `.github`.
---
18. Despliegue
Docker: Dockerfile de dos etapas (Maven + Java 17 compila el jar sin tests; imagen final solo con JRE 17). `.dockerignore` excluye `target`, `.git` y `secrets.properties`.
Base de datos: Supabase (PostgreSQL) con la cadena del Session Pooler (puerto 5432); la conexión directa usa IPv6 y Render no la alcanza. No cambian las entidades.
Servicio web: Render, entorno Docker, conectado al repositorio, con las variables de entorno de la sección 14.
Webhook de producción: en el Dashboard de Stripe (Workbench → Webhooks) apuntando a `/api/pagos/webhook` de tu URL de Render, con los eventos `checkout.session.completed` y `customer.subscription.deleted`. Su secreto va en `STRIPE_SECRETO_WEBHOOK`. En producción no se usa la Stripe CLI.
Zona horaria: Render trabaja en UTC; Sillón usa el `Clock` con `America/Bogota`.
Si la aplicación no arranca, lee los logs de Render de abajo hacia arriba y busca la primera línea con la causa (casi siempre una variable de entorno mal escrita o la conexión a Supabase).
No se ejecutan migraciones destructivas ni se eliminan datos de producción sin autorización expresa.
---
19. Fases de desarrollo
El desarrollo se organiza en 14 fases (0 a 13). Cada fase deja la aplicación funcionando y termina con un commit.
Fase	Alcance
0	Terreno: fork, proyecto Initializr, configuración, secretos, SPEC, AGENTS y bitácora.
1	Base SaaS: formato de error, manejador global, `Clock`, salón, slug y usuarios.
2	Seguridad: JWT, roles, registro, login y respuestas 401/403.
3	Catálogo y equipo: servicios, estilistas, clientes y límite del plan.
4	Agenda: citas, reglas de horario, cruces, estados y disponibilidad.
5	Cara pública: catálogo, disponibilidad y reservas en línea.
6	Pagos con Stripe: plan PRO, créditos y webhook idempotente.
7	Motor de IA: proveedores, fallback y créditos.
8	Recordatorio con IA y datos verificados.
9	Chatbot con escalamiento y regla anti-promesas.
10	Tests automáticos.
11	Swagger.
12	Despliegue en Render con Supabase.
13	Evidencia ejecutable (Postman) y entrega.
Cada fase se cierra con implementación revisada, pruebas ejecutadas, resultados documentados, pendientes identificados, documentación actualizada y un commit descriptivo. El historial de commits también se revisa.
---
20. Documentación del repositorio
```text
sillon/
│
├── .github/                      (calificador: no se modifica)
├── .mvn/
├── postman/
│   ├── sillon.postman_collection.json
│   ├── local.postman_environment.json
│   └── produccion.postman_environment.json
├── src/
│   ├── main/resources/prompts/   (recordatorio.txt, chatbot.txt)
│   └── test/java/
│
├── .cursorignore
├── .dockerignore
├── .gitignore
├── AGENTS.md
├── BITACORA-IA.md
├── Dockerfile
├── README.md
├── SPEC.md
├── mvnw
├── mvnw.cmd
├── pom.xml
└── secrets.example.properties   (secrets.properties NO se sube)
```
Documento	Responsabilidad
`README.md`	Presentación, tecnologías, instalación, ejecución y verificación.
`SPEC.md`	Requisitos, modelo, reglas de negocio y contrato de la API (fuente de verdad).
`AGENTS.md`	Instrucciones y convenciones para agentes de IA (Cursor).
`BITACORA-IA.md`	Registro de usos de IA: qué se revisó y qué se rechazó o corrigió.
Antes de modificar reglas de negocio o entidades, consulta `SPEC.md`.
---
21. Entregables y calificación
Entregables
[ ] Pull Request abierto hacia el repositorio oficial (no hacia tu fork), con tu nombre completo como título.
[ ] Código fuente completo en la raíz del fork, con historial de commits por fase.
[ ] `SPEC.md`, `AGENTS.md` y `BITACORA-IA.md`.
[ ] `secrets.example.properties` (sí) y `secrets.properties` (no, ignorado por Git).
[ ] Prompts del producto en `src/main/resources/prompts`.
[ ] Tests JUnit + Mockito con los casos mínimos.
[ ] `Dockerfile` y `.dockerignore`.
[ ] Colección Postman y entornos `local` y `produccion`.
[ ] README con la URL completa de producción y Swagger.
[ ] API desplegada en Render con Swagger accesible.
[ ] Webhook de producción creado en Stripe y probado con un pago.
La inclusión de un entregable en esta lista no significa que ya esté implementado o aprobado.
Requisitos para que el proyecto se califique
El proyecto está en la raíz del fork y la URL de Render responde.
No se modificó la carpeta `.github`.
Ninguna llave real aparece en el repositorio ni en su historial (si se filtra, se revoca y se crea una nueva).
Cómo se califica
Nota = 5 × pruebas aprobadas ÷ pruebas totales (un decimal), de 0.0 a 5.0, con crédito parcial.
#	Criterio	Pruebas
01	Registro, login y configuración	14
02	Catálogo y equipo	9
03	Agenda	34
04	Aislamiento y roles	7
05	IA: recordatorio y chatbot	16
06	Pagos con Stripe	29
07	Tests JUnit	8
08	Repositorio y entrega	12
09	Producción en Render	16
Lo que no se puede verificar, no se califica.
---
22. Errores frecuentes
Lo que ves	Qué hacer
Maven no resuelve el POM padre	Quita el sufijo `.RELEASE` de la versión de Spring Boot.
Falta el bean `RestClient$Builder`	Agrega la dependencia HTTP Client (`spring-boot-starter-restclient`).
`com.fasterxml.jackson.databind` no existe	Usa `tools.jackson.databind` (Jackson 3).
Todo responde 401 tras un error del servidor	Haz pública la ruta `/error`.
403 con token válido	La autoridad debe ser `ROLE_` + rol y la regla usar `hasRole` sin prefijo.
`could not initialize proxy - no Session`	Convierte la entidad a DTO dentro del método transaccional.
Cita de esta noche rechazada "en el pasado" solo en Render	Usa el `Clock` con `America/Bogota`.
Webhook 400 por firma inválida	En local, el `whsec` de la Stripe CLI; en Render, el del destino de eventos; lee el cuerpo como texto plano.
`Data truncated for column` al guardar un enum	En desarrollo, borra la base para que se regenere.
Render no conecta con Supabase	Usa la cadena del Session Pooler.
503 en IA	Revisa la llave y el log (`ERROR` configuración, `WARN` temporal).
502 en IA	Revisa que el prompt pida solo JSON con las claves exactas.
Calificador: no encuentra `pom.xml`	Mueve el proyecto a la raíz del fork.
Calificador: 502 en pagos	Construye el cliente de Stripe con la base de `STRIPE_URL_API`.
Calificador: 503 en todo el criterio de IA	Lee `sillon.ia.openai.url` y nada fijo en el código.
Calificador: no encuentra URL de Render	Escribe la URL completa en este README.
---
23. Criterios de evolución técnica
Para que Sillón crezca y atienda a muchos salones es fundamental:
Garantizar el aislamiento multi-tenant (que ninguna consulta olvide filtrar por salón).
Optimizar consultas e índices de la base de datos.
Proteger la concurrencia de las reservas (cruces simultáneos).
Controlar el consumo de proveedores externos y los créditos de IA.
Supervisar errores y tiempos de respuesta.
Mantener migraciones y copias de seguridad verificables.
Proteger datos personales y credenciales.
Separar las reglas de negocio de las integraciones externas.
La escalabilidad no depende solo de más recursos de servidor: requiere arquitectura organizada, consultas eficientes, seguridad y reglas consistentes.
Reflexión: si Sillón tuviera 500 salones
> Completar con 3 líneas propias y argumentadas: ¿qué parte del sistema cuidarías primero y por qué? No hay una sola respuesta correcta; hay respuestas bien argumentadas.
---
24. Autor y propósito académico
Proyecto: Sillón — Agenda inteligente para barberías y salones de belleza.
Programa: Java AI Engineer.
Institución: Dev Senior Code.
Tipo: Proyecto integrador (Módulos 4 y 5).
Autor: Jean-René Barthélémy
El proyecto integra conocimientos de Java y programación orientada a objetos, Spring Boot, arquitectura SaaS multi-tenant, bases de datos y persistencia, seguridad y autenticación, API REST, integración de servicios externos, inteligencia artificial, pruebas automatizadas, contenerización y despliegue.
---
