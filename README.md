# Sillón

Agenda inteligente para barberías y salones de belleza: un SaaS multi-inquilino en Java con reservas en línea, pagos con Stripe y un asistente con IA. Cada salón configura su horario, servicios y equipo; sus clientes reservan solo en horarios realmente libres y un chatbot responde precios y horarios.


## Stack

- Java 21, Spring Boot 4.1, Maven
- Spring Data JPA: MySQL (local) y PostgreSQL en Supabase (producción)
- Spring Security, JWT (jjwt) y BCrypt
- Swagger / OpenAPI (springdoc 3.x)
- Stripe en modo prueba (Checkout, suscripciones, pagos únicos y webhooks)
- IA: Gemini por HTTP con `RestClient` y fallback entre proveedores
- Tests: JUnit 5 y Mockito
- Docker, Render y Supabase

## Cómo correrlo en local

**Requisitos:** Java 21, MySQL encendido, cuenta de Stripe en modo prueba, Stripe CLI y llave de Gemini (Google AI Studio, plan gratuito).

1. Clona el repositorio. El `pom.xml` está en la raíz.
2. Copia `secrets.example.properties` a `secrets.properties` (este archivo no se sube a Git) y completa tus valores reales: clave secreta de Stripe, ids de los dos precios, secreto del webhook, llave de Gemini y secreto de JWT.
3. La aplicación crea la base `sillon_db` automáticamente en tu MySQL local. Si tus credenciales de MySQL son distintas a las de `application.properties`, ajústalas.
4. En una terminal aparte, reenvía los webhooks de Stripe:
   `stripe listen --forward-to localhost:8080/api/pagos/webhook`
   Copia el secreto `whsec_...` que imprime en `secrets.properties` y reinicia la aplicación.
5. Arranca la aplicación: `./mvnw spring-boot:run`
6. Abre http://localhost:8080/swagger-ui.html

Para correr los tests: `./mvnw test`

## Cómo verificarlo

1. En Postman importa, desde la carpeta `postman/`, la colección `sillon.postman_collection.json` y los entornos `local` y `produccion` (cada uno con la variable `baseUrl`).
2. Elige el entorno `local`, haz clic derecho en la colección → **Run collection** y selecciona las carpetas **1 a 6**. Todas las pruebas deben quedar en verde.
3. Cambia al entorno `produccion` y repite contra Render.
4. Para la carpeta **7 (manual)**: crea el pago del plan PRO, abre la `urlPago` en el navegador y paga con la tarjeta de prueba `4242 4242 4242 4242` (cualquier fecha futura y cualquier código). Luego corre la carpeta 7.
5. Tarjeta de pago rechazado: `4000 0000 0000 0002`.

La colección usa datos únicos por ejecución y fechas calculadas, así que se puede correr muchas veces.

## Endpoints

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `POST /api/auth/registro` | Público | Registra el salón y su dueño; devuelve el token |
| `POST /api/auth/login` | Público | Inicia sesión |
| `GET /api/salon` | DUENO | Datos de mi salón, plan y créditos |
| `PUT /api/salon/configuracion` | DUENO | Horario e intervalo de agenda |
| `POST /api/servicios` | DUENO | Crea un servicio |
| `GET /api/servicios` | DUENO, ESTILISTA | Lista los servicios del salón |
| `PUT /api/servicios/{id}` | DUENO | Edita un servicio |
| `POST /api/estilistas` | DUENO | Crea un estilista (límite del plan) |
| `GET /api/estilistas` | DUENO | Lista los estilistas |
| `GET /api/clientes` | DUENO | Lista los clientes |
| `POST /api/citas` | DUENO | Crea una cita desde el salón |
| `GET /api/citas?fecha=` | DUENO, ESTILISTA | Agenda del día |
| `PATCH /api/citas/{id}/estado` | DUENO, ESTILISTA | Cambia el estado de una cita |
| `POST /api/citas/{id}/recordatorio` | DUENO | Recordatorio redactado con IA (1 crédito) |
| `GET /api/publico/salones/{slug}` | Público | Catálogo público del salón |
| `GET /api/publico/salones/{slug}/disponibilidad` | Público | Horarios libres |
| `POST /api/publico/salones/{slug}/reservas` | Público | Reserva en línea |
| `POST /api/publico/salones/{slug}/chat` | Público | Chatbot del salón (1 crédito) |
| `GET /api/solicitudes` | DUENO | Solicitudes de contacto del chatbot |
| `PATCH /api/solicitudes/{id}/atendida` | DUENO | Marca una solicitud como atendida |
| `POST /api/pagos/suscripcion` | DUENO | Sesión de pago del plan PRO |
| `POST /api/pagos/creditos` | DUENO | Sesión de pago del paquete de créditos |
| `POST /api/pagos/webhook` | Stripe | Webhook firmado e idempotente |
| `GET /api/pagos/resultado?estado=` | Público | Página de retorno tras el pago |
| `GET /api/admin/salones` | ADMIN | Todos los salones de la plataforma |

Todos los errores responden con el formato único `codigo`, `error`, `mensaje` y `fecha`.

## Variables de entorno en Render

Solo los nombres; los valores nunca se suben al repositorio.

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRETO`
- `URL_PUBLICA`
- `STRIPE_CLAVE_SECRETA`
- `STRIPE_PRECIO_PRO`
- `STRIPE_PRECIO_CREDITOS`
- `STRIPE_SECRETO_WEBHOOK`
- `GEMINI_LLAVE`

`STRIPE_URL_API` y `SILLON_IA_OPENAI_URL` no se configuran en Render: sus valores por defecto apuntan a Stripe y a Gemini reales.

## Documentación del proceso

- `SPEC.md`: problema, reglas de negocio y contrato de la API
- `AGENTS.md`: reglas para el agente de IA
- `BITACORA-IA.md`: registro de usos de IA


