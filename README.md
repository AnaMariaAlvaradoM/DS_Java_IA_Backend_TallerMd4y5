# 💈 Sillón

Agenda inteligente para barberías y salones de belleza: un SaaS multi-inquilino en Java donde cada salón configura su horario, servicios y equipo, sus clientes reservan en línea solo en horarios realmente libres, y la IA redacta recordatorios y responde preguntas con el catálogo real del salón.

Proyecto integrador de los Módulos 4 y 5 · **Java AI Engineer · Dev Senior Code**.

---

## 🌐 En producción

| | URL |
|---|---|
| API | https://ds-java-ia-backend-tallermd4y5-238z.onrender.com/ |
| Swagger | https://ds-java-ia-backend-tallermd4y5-238z.onrender.com//swagger-ui.html |
| OpenAPI (JSON) | https://ds-java-ia-backend-tallermd4y5-238z.onrender.com//v3/api-docs |

> Render en plan gratuito "duerme" el servicio cuando no se usa: la primera petición puede tardar cerca de un minuto.

---

## 🧱 Stack

| Pieza | Tecnología |
|---|---|
| Lenguaje y framework | Java 17, Spring Boot 4.1, Maven |
| Persistencia | Spring Data JPA · MySQL (local) · PostgreSQL en Supabase (producción) |
| Seguridad | Spring Security, JWT (jjwt), BCrypt, roles DUENO · ESTILISTA · ADMIN |
| Documentación | springdoc-openapi 3 (Swagger UI) |
| Pagos | Stripe en modo prueba: Checkout, suscripción, pago único y webhook firmado e idempotente |
| IA | Gemini (formato OpenAI) por HTTP con `RestClient`, respaldo con formato Claude y fallback, créditos por respuesta |
| Tests | JUnit 5 + Mockito (119 tests, sin llamadas a Gemini ni a Stripe) |
| Despliegue | Docker (dos etapas) · Render · Supabase |

Organización por dominios con capas internas: `compartido`, `seguridad`, `auth`, `salon`, `usuario`, `servicio`, `cliente`, `cita`, `publico`, `pago`, `chat`, `ia`.

---

## ▶️ Correrlo en local

**Requisitos:** Java 17, MySQL 8 encendido, (opcional) Stripe CLI y una llave de Gemini.

1. Copia la plantilla de secretos y llénala con tus valores (este archivo nunca se sube a Git):
   ```bash
   cp secrets.example.properties secrets.properties
   ```
   Como mínimo: `spring.datasource.password`. Para IA: `sillon.ia.openai.llave`. Para pagos: la `sk_test_`, los dos `price_` y el `whsec_` de la Stripe CLI.
2. (Opcional, para pagos) En otra terminal, el puente de webhooks:
   ```bash
   stripe listen --events checkout.session.completed,customer.subscription.deleted --forward-to localhost:8080/api/pagos/webhook
   ```
3. Arranca la API (crea la base `sillon_db` sola):
   ```bash
   ./mvnw spring-boot:run
   ```
4. Tests:
   ```bash
   ./mvnw test
   ```

---

## ✅ Cómo verificarlo

La evidencia es ejecutable: una colección de Postman con pruebas automáticas en cada petición.

1. Importa en Postman la carpeta `postman/`:
   - `sillon.postman_collection.json`
   - `local.postman_environment.json` y `produccion.postman_environment.json` (una sola variable: `baseUrl`)
2. En la colección, pon como **Current value** de `secretoWebhook` el `whsec_` que imprime `stripe listen` (local) o el del destino de Stripe (producción). Lo usan las peticiones que firman webhooks.
3. **Run collection** → selecciona las carpetas **1 a 6**, en orden, con el entorno `local` o `produccion`. Todo debe quedar en verde.
4. Carpeta **7 (manual)**: abre la `urlPago` que imprime "Crear pago del plan PRO" (también queda en la variable `urlPagoPro`), paga con la tarjeta de prueba **4242 4242 4242 4242** (cualquier fecha futura, cualquier CVC) y luego corre la carpeta 7.

Cada ejecución usa un sufijo con la hora y calcula sus propias fechas: se puede correr muchas veces contra producción sin chocar.

---

## 📚 Endpoints

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/auth/registro` | Público |
| POST | `/api/auth/login` | Público |
| GET | `/api/salon` | DUENO |
| PUT | `/api/salon/configuracion` | DUENO |
| POST | `/api/servicios` | DUENO |
| GET | `/api/servicios` | DUENO, ESTILISTA |
| PUT | `/api/servicios/{id}` | DUENO |
| POST | `/api/estilistas` | DUENO |
| GET | `/api/estilistas` | DUENO |
| GET | `/api/clientes` | DUENO |
| POST | `/api/citas` | DUENO |
| GET | `/api/citas?fecha=` | DUENO (todas), ESTILISTA (las suyas) |
| PATCH | `/api/citas/{id}/estado` | DUENO, ESTILISTA (solo COMPLETADA / NO_ASISTIO) |
| POST | `/api/citas/{id}/recordatorio` | DUENO · gasta 1 crédito de IA |
| GET | `/api/publico/salones/{slug}` | Público |
| GET | `/api/publico/salones/{slug}/disponibilidad?servicioId=&estilistaId=&fecha=` | Público |
| POST | `/api/publico/salones/{slug}/reservas` | Público |
| POST | `/api/publico/salones/{slug}/chat` | Público · gasta 1 crédito del salón |
| GET | `/api/solicitudes` | DUENO |
| PATCH | `/api/solicitudes/{id}/atendida` | DUENO |
| POST | `/api/pagos/suscripcion` | DUENO |
| POST | `/api/pagos/creditos` | DUENO |
| POST | `/api/pagos/webhook` | Stripe (firma `Stripe-Signature`) |
| GET | `/api/pagos/resultado?estado=` | Público |
| GET | `/api/admin/salones` | ADMIN |

Todos los errores responden con el mismo formato: `codigo`, `error`, `mensaje`, `fecha`.

---

## ⚙️ Variables de entorno (Render)

Solo los nombres; los valores viven en Render, nunca en el repositorio.

| Variable | Para qué |
|---|---|
| `SPRING_DATASOURCE_URL` | JDBC de Supabase (Session Pooler, `sslmode=require`) |
| `SPRING_DATASOURCE_USERNAME` | Usuario del pooler |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base |
| `JWT_SECRETO` | Secreto de firma de los JWT (32+ caracteres) |
| `URL_PUBLICA` | URL de la API (para volver de Stripe) |
| `STRIPE_CLAVE_SECRETA` | Llave secreta de prueba |
| `STRIPE_PRECIO_PRO` | Id del precio del plan PRO |
| `STRIPE_PRECIO_CREDITOS` | Id del precio del paquete de créditos |
| `STRIPE_SECRETO_WEBHOOK` | Secreto del destino de webhook de producción |
| `GEMINI_LLAVE` | Llave de Gemini |

`STRIPE_URL_API` y `SILLON_IA_OPENAI_URL` no se configuran en Render: sus valores por defecto apuntan a Stripe y a Gemini reales. Existen para que el calificador use sus simuladores.

---

## 📄 Documentos del proyecto

- [`SPEC.md`](SPEC.md): reglas de negocio y contrato de la API (fuente de verdad).
- [`AGENTS.md`](AGENTS.md): reglas para los agentes de IA que trabajaron en el código.
- [`BITACORA-IA.md`](BITACORA-IA.md): cómo se usó la IA en cada fase, qué se revisó y qué se corrigió.

---

## 🤔 Si Sillón tuviera 500 salones…
- La autentificación y autorización, es fundamental tener trazabilidad de quien hace las cosas.
- El control de las citas, que no vayan a haber errores en el core.
- Y los pagos, es como la parte más delicada.