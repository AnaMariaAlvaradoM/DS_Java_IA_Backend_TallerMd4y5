# Sillón

Sillón es una API de agenda para barberías y salones de belleza. Cada salón administra su equipo y sus citas. Sus clientes consultan el catálogo, reservan en línea y pueden pedir ayuda a una persona desde el chat.

**Dirección del servicio en Render** · [API](https://sillon-dilson-rivera.onrender.com/v3/api-docs) · [Swagger interactivo](https://sillon-dilson-rivera.onrender.com/swagger-ui.html)

## Ejecutar en local

1. Instala Java 17 y arranca MySQL 8. El usuario debe tener acceso a la base `sillon_db`. El valor de conexión de la plantilla permite crearla si tiene ese permiso.
2. Desde la raíz del repositorio, ejecuta `Copy-Item secrets.example.properties secrets.properties` en PowerShell. Completa el usuario y la contraseña de MySQL. Para conservar las sesiones entre reinicios, configura un secreto JWT aleatorio de al menos 32 bytes.
3. Ejecuta `.\mvnw.cmd clean verify` para compilar y probar. Ejecuta `.\mvnw.cmd spring-boot:run` para arrancar la API. En macOS y Linux usa `./mvnw`. Los tests usan H2 y proveedores simulados.
4. Abre [Swagger local](http://localhost:8080/swagger-ui.html). Ejecuta el registro y pega el campo `token` en **Authorize**. Con `GET /api/salon` podrás ver los 10 créditos iniciales.

El backend usa Spring Boot 4.1, JPA, Spring Security, JJWT, BCrypt y springdoc 3. Stripe Java crea las sesiones de pago. Las peticiones de IA se hacen con RestClient. El Dockerfile compila con un JDK 17 y ejecuta con un JRE 17.

## Verificar con Postman

Importa los tres archivos de `postman/`. Elige **local** o **produccion** y ejecuta las carpetas 1 a 6 en orden. Cada ejecución crea dos salones de prueba y calcula sus propias fechas. Para probar IA y Checkout necesitas un proveedor configurado y productos de Stripe de prueba. La carpeta 7 usa el salón y el token de la ejecución previa. Mantén las variables de la colección si usas el Runner.

Para probar el pago manual, crea un precio mensual de 15 USD para Sillón PRO y un precio único de 5 USD para 200 créditos en el entorno de prueba de Stripe. Configura los dos IDs de precio y la clave de prueba. En local ejecuta `stripe listen --forward-to localhost:8080/api/pagos/webhook`, guarda el secreto de firma que muestra la CLI en tu configuración local y reinicia la API. Abre la `urlPago` de la suscripción, usa la tarjeta de prueba `4242 4242 4242 4242`, una fecha futura y un CVC de prueba. Cuando llegue el webhook, ejecuta la carpeta 7. Reenviar el evento debe conservar el mismo saldo.

## Variables de Render

| Grupo | Nombres |
|---|---|
| Aplicación | `PORT`, `URL_PUBLICA`, `JWT_SECRETO` |
| PostgreSQL | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| Stripe | `STRIPE_CLAVE_SECRETA`, `STRIPE_SECRETO_WEBHOOK`, `STRIPE_PRECIO_PRO`, `STRIPE_PRECIO_CREDITOS` |
| IA | `GEMINI_LLAVE`, `SILLON_IA_MODELO`, `ANTHROPIC_API_KEY` para el respaldo opcional |
| Simuladores locales | `STRIPE_URL_API`, `SILLON_IA_OPENAI_URL`, `SILLON_IA_CLAUDE_URL` |

En Supabase usa el **Session pooler** del proyecto, con puerto 5432 y TLS. Copia el host y el usuario de la sección **Connect** y usa la URL con el prefijo JDBC. En Render selecciona el runtime Docker y la rama `entrega-sillon`. Registra la ruta pública `/api/pagos/webhook` en Stripe para los eventos `checkout.session.completed` y `customer.subscription.deleted`. El secreto de ese destino es distinto del que entrega Stripe CLI. [Despliegue Docker en Render](https://render.com/docs/docker) · [Conexión PostgreSQL en Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres)

## Rutas

| Acceso | Método y ruta | Uso |
|---|---|---|
| Público | `POST /api/auth/registro`, `POST /api/auth/login` | Registrar salón y dueño, obtener JWT |
| Dueño | `GET /api/salon`, `PUT /api/salon/configuracion` | Consultar el plan, créditos y horario |
| Dueño o estilista | `GET /api/servicios` | Consultar servicios del propio salón |
| Dueño | `POST /api/servicios`, `PUT /api/servicios/{id}` | Crear, editar o desactivar un servicio |
| Dueño | `GET /api/estilistas`, `POST /api/estilistas`, `GET /api/clientes` | Administrar el equipo y consultar clientes |
| Dueño | `POST /api/citas`, `POST /api/citas/{id}/recordatorio` | Agendar o preparar un borrador para WhatsApp |
| Dueño o estilista | `GET /api/citas?fecha=AAAA-MM-DD`, `PATCH /api/citas/{id}/estado` | Revisar la agenda o cambiar el estado según el rol |
| Público | `GET /api/publico/salones/{slug}`, `GET /api/publico/salones/{slug}/disponibilidad` | Consultar catálogo o franjas por fecha, servicio y estilista |
| Público | `POST /api/publico/salones/{slug}/reservas`, `POST /api/publico/salones/{slug}/chat` | Reservar en línea o consultar al asistente |
| Dueño | `GET /api/solicitudes`, `PATCH /api/solicitudes/{id}/atendida` | Gestionar consultas derivadas por el chatbot |
| Dueño | `POST /api/pagos/suscripcion`, `POST /api/pagos/creditos` | Crear una sesión de Stripe Checkout |
| Stripe | `POST /api/pagos/webhook` | Recibir el evento con su firma |
| Público | `GET /api/pagos/resultado?estado=exito` | Mostrar el regreso desde Checkout |
| Administrador | `GET /api/admin/salones` | Listar todos los salones |

## Si Sillón atendiera 500 salones

Cuidaría primero los datos sensibles de los clientes, especialmente sus teléfonos y el historial de citas.  
Comprobaría que cada salón solo pueda consultar los datos de sus propios clientes.  
Revisaría los permisos y los registros de la aplicación para evitar exponer esos datos al atender una consulta.
