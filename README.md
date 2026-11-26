# 💈 Sillón

Agenda inteligente para barberías y salones de belleza: un SaaS multi-inquilino en Java donde cada salón configura su horario, servicios y equipo, sus clientes reservan en línea viendo solo horarios realmente libres, y la IA redacta recordatorios y responde preguntas con el catálogo real del salón.

Proyecto integrador de los Módulos 4 y 5 · Java AI Engineer · Dev Senior Code.

## Producción

- **API:** https://TU-APP.onrender.com
- **Swagger:** https://TU-APP.onrender.com/swagger-ui.html

> En el plan gratuito de Render el servicio "duerme" cuando no se usa: la primera petición puede tardar unos segundos mientras despierta.

## Stack

| Pieza | Tecnología |
|---|---|
| Lenguaje y framework | Java 25, Spring Boot 4.1, Maven |
| Persistencia | Spring Data JPA · MySQL (local) · PostgreSQL en Supabase (producción) |
| Seguridad | Spring Security, JWT (jjwt), BCrypt, roles DUENO / ESTILISTA / ADMIN |
| Documentación | Swagger / OpenAPI (springdoc 3) |
| Pagos | Stripe en modo prueba: Checkout, suscripción, pago único y webhooks firmados |
| IA | Gemini por HTTP con RestClient (formato OpenAI), respaldo con formato Claude |
| Tests | JUnit 5 + Mockito, reloj fijo |
| Despliegue | Docker + Render + Supabase |

## Cómo correrlo en local

1. **MySQL** encendido. La base `sillon_db` se crea sola.
2. **Secretos:** copia `secrets.example.properties` como `secrets.properties` (está en `.gitignore`, nunca se sube) y pon tus valores: usuario y clave de MySQL, llave de Gemini, clave secreta de Stripe de prueba, ids de los dos precios y el secreto del webhook.
3. **Stripe CLI** (para recibir webhooks en local):
   ```bash
   stripe listen --forward-to localhost:8080/api/pagos/webhook
   ```
   Copia el `whsec_...` que imprime en `secrets.properties` y reinicia la aplicación.
4. **Arrancar:**
   ```bash
   ./mvnw spring-boot:run
   ```
5. **Tests:**
   ```bash
   ./mvnw test
   ```

La aplicación arranca aunque `secrets.properties` no exista: cada secreto tiene un valor de relleno.

## Cómo verificarlo

1. Importa en Postman `postman/sillon.postman_collection.json` y los entornos `postman/local.postman_environment.json` y `postman/produccion.postman_environment.json` (cada uno con una sola variable: `baseUrl`).
2. Clic derecho sobre la colección → **Run collection** → selecciona las carpetas **1 a 6** y el entorno `local` (o `produccion`). Todas las pruebas deben quedar en verde.
3. La carpeta **7. Después de pagar (manual)**: ejecuta la petición 47, abre la `urlPago` que aparece en la consola de Postman y paga con la tarjeta de prueba **4242 4242 4242 4242** (cualquier fecha futura y cualquier código). Luego corre la carpeta 7.

La colección no depende de datos creados a mano: usa un sufijo con la hora para correos y nombres, y calcula las fechas (un día de lunes a sábado al menos 2 días adelante, y el próximo domingo).

## Endpoints

| Método y ruta | Rol | Qué hace |
|---|---|---|
| POST `/api/auth/registro` | Público | Registra el salón y su dueño (plan GRATIS, 10 créditos) |
| POST `/api/auth/login` | Público | Inicia sesión y devuelve el token |
| GET `/api/salon` | DUENO | Mi salón |
| PUT `/api/salon/configuracion` | DUENO | Horario e intervalo de agenda |
| POST `/api/servicios` | DUENO | Crear servicio |
| GET `/api/servicios` | DUENO, ESTILISTA | Listar servicios del salón |
| PUT `/api/servicios/{id}` | DUENO | Editar servicio |
| POST `/api/estilistas` | DUENO | Crear estilista (402 si el plan GRATIS ya tiene 2) |
| GET `/api/estilistas` | DUENO | Listar estilistas |
| GET `/api/clientes` | DUENO | Listar clientes |
| POST `/api/citas` | DUENO | Agendar desde el salón (nace CONFIRMADA) |
| GET `/api/citas?fecha=` | DUENO, ESTILISTA | Agenda del día (el estilista solo ve las suyas) |
| PATCH `/api/citas/{id}/estado` | DUENO, ESTILISTA | Cambiar estado de una cita |
| POST `/api/citas/{id}/recordatorio` | DUENO | Borrador de recordatorio con IA |
| GET `/api/publico/salones/{slug}` | Público | Catálogo público del salón |
| GET `/api/publico/salones/{slug}/disponibilidad` | Público | Horarios libres |
| POST `/api/publico/salones/{slug}/reservas` | Público | Reserva en línea (nace PENDIENTE) |
| POST `/api/publico/salones/{slug}/chat` | Público | Chatbot del salón |
| GET `/api/solicitudes` | DUENO | Solicitudes que dejó el chatbot |
| PATCH `/api/solicitudes/{id}/atendida` | DUENO | Marcar solicitud como atendida |
| POST `/api/pagos/suscripcion` | DUENO | Sesión de pago del plan PRO |
| POST `/api/pagos/creditos` | DUENO | Sesión de pago del paquete de 200 créditos |
| POST `/api/pagos/webhook` | Stripe | Webhook firmado (idempotente) |
| GET `/api/pagos/resultado?estado=` | Público | Página de regreso después del pago |
| GET `/api/admin/salones` | ADMIN | Todos los salones de la plataforma |

## Variables de entorno en Render

Solo los nombres; los valores viven en Render, nunca en el repositorio.

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (Supabase, Session Pooler)
- `JWT_SECRETO`
- `URL_PUBLICA`
- `STRIPE_CLAVE_SECRETA`, `STRIPE_SECRETO_WEBHOOK`
- `STRIPE_PRECIO_PRO`, `STRIPE_PRECIO_CREDITOS`
- `GEMINI_LLAVE`

`STRIPE_URL_API` y `SILLON_IA_OPENAI_URL` no se configuran en Render: sus valores por defecto apuntan a Stripe y a Gemini reales. Solo el calificador automático las cambia por sus simuladores.

## Documentos del proyecto

- [`SPEC.md`](SPEC.md): problema, reglas de negocio y contrato de la API.
- [`AGENTS.md`](AGENTS.md): reglas para el agente de IA.
- [`BITACORA-IA.md`](BITACORA-IA.md): cómo usé la IA en cada fase y qué corregí.

## Si Sillón tuviera 500 salones

<!-- ESCRIBE AQUÍ TUS TRES LÍNEAS: ¿qué parte del sistema cuidarías primero y por qué? -->
