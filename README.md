# Sillón

API SaaS para salones y barberías. Permite administrar servicios, estilistas, clientes y citas; ofrece catálogo y reservas públicas; genera borradores de recordatorio y respuestas de chat con IA; y vende el plan PRO y paquetes de créditos mediante Stripe Checkout.

## Enlaces

- Repositorio de trabajo: https://github.com/Fhv5/DS_Java_IA_Backend_TallerMd4y5
- Pull request de entrega: https://github.com/AnaMariaAlvaradoM/DS_Java_IA_Backend_TallerMd4y5/pull/18
- API en Render: https://sillon-bser.onrender.com
- Swagger en Render: https://sillon-bser.onrender.com/swagger-ui.html
- Swagger local: http://localhost:8080/swagger-ui.html

## Tecnología y reglas

Java 17, Spring Boot 4.1, Maven, Spring Security con JWT, JPA, MySQL local, PostgreSQL en Supabase, Stripe Checkout y webhooks, Gemini por la API compatible con OpenAI, respaldo Claude, Swagger, JUnit 5, Mockito y Docker. No hay interfaz gráfica ni envío automático de mensajes.

Cada entidad de negocio pertenece a un salón. El backend obtiene `salonId` del JWT o del slug público y filtra los datos por ese salón. Las reglas verificables están en [SPEC.md](SPEC.md), las pautas en [AGENTS.md](AGENTS.md) y el registro de uso de IA en [BITACORA-IA.md](BITACORA-IA.md).

## Arranque local

1. Tener Java 17 o posterior, Maven y MySQL 8. Crear la base `sillon_db`.
2. Copiar `secrets.example.properties` a `secrets.properties` en la raíz y completar los valores. El archivo está ignorado por Git.
3. Configurar `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` para MySQL. La URL predeterminada es `jdbc:mysql://localhost:3306/sillon_db?createDatabaseIfNotExist=true`.
4. Ejecutar `mvn spring-boot:run`. Swagger queda en `http://localhost:8080/swagger-ui.html` salvo que se defina otro `PORT`.

La zona horaria predeterminada del enunciado es `America/Bogota`; para Chile se define `SILLON_ZONA_HORARIA=America/Santiago`. Los IDs de precio `price_...` deben pertenecer al mismo sandbox que `STRIPE_CLAVE_SECRETA`. El backend elige los precios; las peticiones de compra no los reciben.

### Variables

| Variable | Uso |
|---|---|
| `PORT` | Puerto HTTP |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Conexión JDBC |
| `JWT_SECRETO` | Firma de JWT; usar una cadena aleatoria de al menos 32 caracteres |
| `SILLON_ZONA_HORARIA` | Zona de la agenda |
| `URL_PUBLICA` | URL usada por Stripe al volver de Checkout |
| `STRIPE_CLAVE_SECRETA`, `STRIPE_SECRETO_WEBHOOK` | Clave de prueba y firma del webhook |
| `STRIPE_PRECIO_PRO`, `STRIPE_PRECIO_CREDITOS` | IDs de precios creados en Stripe |
| `STRIPE_URL_API` | API de Stripe; permite el simulador de pruebas |
| `GEMINI_LLAVE` | Clave de Google AI Studio |
| `SILLON_IA_OPENAI_URL` | Base compatible con OpenAI; permite el simulador de pruebas |
| `SILLON_IA_CLAUDE_LLAVE` | Respaldo opcional de Claude |

Las claves reales viven en `secrets.properties` local o en Environment de Render; no se guardan en el repositorio.

## Verificación

`mvn test` ejecuta pruebas de agenda, errores, slug, webhook y chatbot, además del test de contexto conectado a MySQL. Las pruebas de webhook generan firmas locales y comprueban pagos, repetición y cancelación sin llamar a Stripe. Construir el contenedor con `docker build -t sillon-local .`.

En Render se verificaron `GET /v3/api-docs` con HTTP 200 y `POST /api/auth/registro` con HTTP 201, token y rol `DUENO`, conectados a PostgreSQL de Supabase.

La colección [postman/Sillon.postman_collection.json](postman/Sillon.postman_collection.json) contiene 59 peticiones en siete carpetas y pruebas automáticas en cada una. Importar también `local.postman_environment.json` o `produccion.postman_environment.json`. Ejecutar carpetas 1 a 6 en orden: la colección crea correos y salones únicos, calcula fechas futuras y guarda tokens e IDs. Antes de repetir el recorrido completo, borrar la variable de colección `runId`. La carpeta 7 se verifica manualmente después de interactuar con Stripe Checkout.

En CLI: `newman run postman/Sillon.postman_collection.json -e postman/local.postman_environment.json`. Para otro puerto, agregar `--env-var baseUrl=http://localhost:18081`.

## Despliegue

El [Dockerfile](Dockerfile) compila con Java 17 y ejecuta el JAR en una imagen JRE. En Render se crea un Web Service Docker desde `fase-0-base`. En Supabase se usa el **Session pooler** en el puerto 5432. Su URL JDBC tiene la forma `jdbc:postgresql://<host-del-pooler>:5432/postgres?sslmode=require`; el usuario tiene la forma `postgres.<ref-del-proyecto>`. Se configuran en Render las variables de base, JWT, URL pública, Stripe, Gemini y zona horaria.

Después se registra en el mismo sandbox de Stripe un webhook hacia `https://sillon-bser.onrender.com/api/pagos/webhook` para `checkout.session.completed` y `customer.subscription.deleted`; su secreto `whsec_...` se guarda como `STRIPE_SECRETO_WEBHOOK`. Verificar `GET /v3/api-docs`, registro, login, catálogo público, una reserva y ambos flujos de pago. Stripe confirma el plan y los créditos únicamente mediante el webhook firmado.

## Reflexión

La IA redacta borradores y responde consultas, pero Java conserva las decisiones de negocio: comprueba el saldo, inyecta datos verificados, valida el JSON y bloquea afirmaciones sobre ofertas o devoluciones. Los recordatorios requieren revisión humana antes de enviarse. Los webhooks se firman y registran por ID para evitar aplicar un pago dos veces. La separación entre MySQL local, PostgreSQL de producción y simuladores de proveedores permitió verificar cada integración sin depender de cobros reales.
