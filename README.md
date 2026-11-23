# Sillón — API de gestión de salones y barberías

Sillón es una API REST para gestionar salones de belleza y barberías: usuarios, servicios, estilistas, clientes, disponibilidad y citas. Incluye recordatorios y un chatbot apoyados en IA, pagos con Stripe y separación de datos por salón.

## Aplicación desplegada

- **API en Render:** https://sillon-api-548o.onrender.com
- **Swagger UI:** https://sillon-api-548o.onrender.com/swagger-ui/index.html
- **Especificación OpenAPI:** https://sillon-api-548o.onrender.com/v3/api-docs

El despliegue utiliza un contenedor Docker en Render y PostgreSQL en Supabase. En el plan gratuito, Render puede tardar en responder a la primera petición después de un período de inactividad.

## Tecnologías

Java 17, Spring Boot, Maven, Spring Security con JWT y BCrypt, Spring Data JPA/Hibernate, MySQL en local, PostgreSQL (Supabase) en producción, Docker, Render, OpenAPI/Swagger, Stripe en modo de pruebas, integración de IA mediante HTTP (`RestClient`), JUnit, Mockito y Postman.

## Cómo ejecutar el proyecto en local

1. Tener Java 17, MySQL y Maven Wrapper (incluido en el repositorio). Iniciar MySQL y disponer de la base de datos `sillon_db` y de un usuario con permisos para ella.
2. Copiar la plantilla `secrets.example.properties` como `secrets.properties` en la raíz del proyecto. Completar los valores locales. Este archivo **no debe subirse a Git**. La aplicación importa sus propiedades desde ese archivo.
3. Para las funciones de IA, configurar `sillon.ia.openai.llave` y un modelo válido mediante `sillon.ia.openai.modelo`. La API de IA puede tener límites o costos según la cuenta utilizada.
4. Para probar pagos localmente, configurar las claves y los identificadores de precios de Stripe en modo de prueba. Con Stripe CLI instalada y autenticada, se puede reenviar el webhook local:

   ```bash
   stripe listen --forward-to localhost:8080/api/pagos/webhook
   ```

   Configurar el secreto de firma de ese listener en `sillon.stripe.secreto-webhook` y reiniciar la API. Nunca copiar este secreto al repositorio.
5. Desde la raíz del proyecto, iniciar la aplicación:

   ```bash
   ./mvnw spring-boot:run
   ```

6. Abrir Swagger en http://localhost:8080/swagger-ui/index.html.

Para ejecutar las pruebas Java:

```bash
./mvnw test
```

## Cómo verificar la API con Postman

Los archivos de evidencia están en la carpeta `postman/`:

- `sillon.postman_collection.json`: colección final de evidencia ejecutable.
- `local.postman_environment.json`: entorno local (`baseUrl=http://localhost:8080`).
- `produccion.postman_environment.json`: entorno de producción (`baseUrl=https://sillon-api-548o.onrender.com`).

Importar la colección y los dos entornos en Postman. Elegir **Sillón Local** con la aplicación y MySQL iniciados; abrir **Run collection** y ejecutar las carpetas **1 a 6** en el orden establecido, dejando sin seleccionar la carpeta **7. Después de pagar (manual)**. La colección genera datos de prueba y reutiliza tokens, IDs y fechas mediante variables. Repetir con el entorno de producción para comprobar el despliegue.

Para la verificación manual de Stripe, abrir la URL de pago creada en modo prueba, completar el pago con la tarjeta de pruebas `4242 4242 4242 4242`, una fecha futura y un CVC de prueba. Una vez procesado el webhook, verificar el plan PRO y la creación del tercer estilista con la carpeta 7. El pago requiere claves y precios de Stripe configurados para ese entorno; no usar una tarjeta real.

Las colecciones históricas de fases previas pueden conservarse como evidencia adicional; la colección principal de entrega es `sillon.postman_collection.json`.

## Endpoints principales

| Módulo | Método y ruta | Función |
|---|---|---|
| Autenticación | `POST /api/auth/registro` | Registrar salón y dueño |
| Autenticación | `POST /api/auth/login` | Iniciar sesión y obtener JWT |
| Salón | `GET /api/salon` | Consultar el salón autenticado |
| Salón | `PUT /api/salon/configuracion` | Configurar horario |
| Servicios | `GET /api/servicios`, `POST /api/servicios` | Consultar y crear servicios |
| Servicios | `PUT /api/servicios/{id}` | Actualizar servicio |
| Estilistas | `GET /api/estilistas`, `POST /api/estilistas` | Consultar y crear estilistas |
| Clientes | `GET /api/clientes` | Consultar clientes |
| Agenda | `GET /api/citas`, `POST /api/citas` | Consultar y crear citas |
| Agenda | `PATCH /api/citas/{id}/estado` | Cambiar estado de una cita |
| IA | `POST /api/citas/{id}/recordatorio` | Generar borrador de recordatorio |
| Catálogo público | `GET /api/publico/salones/{slug}` | Consultar servicios y estilistas |
| Reservas públicas | `GET /api/publico/salones/{slug}/disponibilidad` | Consultar horarios disponibles |
| Reservas públicas | `POST /api/publico/salones/{slug}/reservas` | Crear reserva en línea |
| Chatbot | `POST /api/publico/salones/{slug}/chat` | Consultar y solicitar atención |
| Solicitudes | `GET /api/solicitudes` | Ver solicitudes escaladas |
| Solicitudes | `PATCH /api/solicitudes/{id}/atendida` | Marcar solicitud atendida |
| Stripe | `POST /api/pagos/suscripcion` | Iniciar pago del plan PRO |
| Stripe | `POST /api/pagos/creditos` | Iniciar pago de créditos |
| Stripe | `POST /api/pagos/webhook` | Recibir y verificar eventos Stripe |
| Stripe | `GET /api/pagos/resultado?estado=exito` | Página de resultado del pago |
| Administración | `GET /api/admin/salones` | Consulta para rol ADMIN |

La documentación detallada de parámetros, respuestas y esquemas está disponible en Swagger.

## Variables de entorno de Render

Se usan nombres de variables, **nunca sus valores reales**:

- **Infraestructura y base de datos:** `PORT`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `URL_PUBLICA`.
- **Autenticación:** `JWT_SECRETO`.
- **Stripe:** `STRIPE_URL_API`, `STRIPE_CLAVE_SECRETA`, `STRIPE_SECRETO_WEBHOOK`, `STRIPE_PRECIO_PRO`, `STRIPE_PRECIO_CREDITOS`.
- **Gemini:** `GEMINI_LLAVE`, `GEMINI_MODELO`, `SILLON_IA_OPENAI_URL`, `GEMINI_PRECIO_ENTRADA`, `GEMINI_PRECIO_SALIDA`.
- **Proveedor de respaldo:** `CLAUDE_LLAVE`, `CLAUDE_URL_API`, `CLAUDE_MODELO`, `CLAUDE_PRECIO_ENTRADA`, `CLAUDE_PRECIO_SALIDA`.

`secrets.properties` se utiliza únicamente en local y está excluido de Git. Las credenciales de producción se configuran en el panel de Render.

## Documentación y evidencias

- `SPEC.md`: reglas de negocio y contrato de la API.
- `AGENTS.md`: instrucciones de trabajo y verificaciones.
- `BITACORA-IA.md`: registro del uso de IA durante el desarrollo.
- `src/main/resources/prompts/`: prompts de recordatorios y chatbot.
- `src/test/java/`: pruebas automatizadas con JUnit y Mockito.
- `Dockerfile` y `.dockerignore`: configuración del contenedor.

La entrega se revisa mediante el Pull Request abierto hacia el repositorio oficial y su calificador automático de GitHub Actions.

## Si Sillón tuviera 500 salones

Primero cuidaría la base de datos y la creación de reservas, porque muchas barberías podrían intentar ocupar horarios al mismo tiempo.
Revisaría el rendimiento de las consultas de disponibilidad y los índices para que la agenda siga respondiendo con rapidez.
También comprobaría el aislamiento de datos y los permisos, para asegurar que cada salón acceda únicamente a su información.
