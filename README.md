# 💈 Sillón API — Backend SaaS para Gestión de Salones de Belleza

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-blue?style=flat-square&logo=postgresql)
![Stripe](https://img.shields.io/badge/Pagos-Stripe%20SDK-635BFF?style=flat-square&logo=stripe)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker)

API RESTful desarrollada con **Java 17 y Spring Boot** para la gestión integral de salones de belleza y barberías bajo un modelo SaaS multi-tenant. Permite administrar sedes mediante *slugs* públicos, catálogos de servicios, equipo de estilistas, agendamiento y estados de citas, atención mediante asistente virtual con Inteligencia Artificial y monetización por suscripciones y créditos integrada con **Stripe**.

---

## 🌐 Despliegue en Producción y Documentación

La aplicación se encuentra desplegada en **Render** conectada a una base de datos **PostgreSQL en Supabase**:

- **URL de producción:** `https://ds-java-ia-backend-tallermd4y5-0y6w.onrender.com`
- **URL de Swagger:** `https://ds-java-ia-backend-tallermd4y5-0y6w.onrender.com/swagger-ui.html`

> **Nota sobre el entorno gratuito de Render:** Si la instancia ha estado inactiva durante más de 15 minutos, la primera petición puede tardar alrededor de **50 segundos** mientras el contenedor se reactiva. Las peticiones posteriores responden en milisegundos.

---
## 🛠️ Stack Tecnológico

* **Lenguaje y Framework:** Java 17, Spring Boot 3 (Web, Security, Data JPA, Validation)
* **Seguridad y Autenticación:** Spring Security con JSON Web Tokens (**JWT**) y control de acceso basado en roles (Dueño, Estilista)
* **Base de Datos:** PostgreSQL alojado en **Supabase** (ORM Hibernate)
* **Pasarela de Pagos:** **Stripe Java SDK** (Checkout Sessions + Webhooks firmados con `Stripe-Signature` e idempotencia en tabla `eventos_stripe`)
* **Inteligencia Artificial:** Integración con API de Gemini para asistente conversacional
* **Documentación API:** SpringDoc OpenAPI 3 (Swagger UI)
* **Contenedores y CI/CD:** Docker (multi-stage build con Maven y JRE 17) y GitHub Actions
* **Pruebas Automatizadas:** JUnit 5, Mockito y Colección de Postman v2.1 ejecutada con Newman

---

## 🧩 Módulos y Endpoints Principales

| Módulo | Ruta Base / Endpoint | Descripción | Seguridad |
| :--- | :--- | :--- | :--- |
| **Autenticación** | `POST /api/auth/registro`<br>`POST /api/auth/login`<br>`POST /api/auth/estilistas/login` | Registro de dueños, inicio de sesión y emisión de token JWT para dueños y estilistas. | Pública |
| **Salones** | `GET /api/salones/...`<br>`POST /api/salones/...` | Creación, consulta del salón asociado al dueño y búsqueda pública por *slug*. | JWT / Pública |
| **Servicios** | `GET /api/servicios/...`<br>`POST /api/servicios/...` | Gestión del catálogo de servicios, precios y duración por salón. | JWT (Dueño) |
| **Estilistas** | `POST /api/estilistas/...` | Registro y administración del personal del salón. | JWT (Dueño) |
| **Citas** | `POST /api/citas/...`<br>`GET /api/citas/...`<br>`PATCH /api/citas/...` | Agendamiento de turnos, listado y actualización de estado de citas. | JWT |
| **Pagos y Suscripciones** | `POST /api/pagos/...`<br>`POST /api/pagos/webhook`<br>`GET /api/pagos/resultado` | Generación de sesiones de pago en Stripe, recepción de eventos (`checkout.session.completed`, `customer.subscription.deleted`) y vista de retorno. | JWT / Firma Stripe |
| **Asistente IA** | `POST /api/chat/...` | Consultas conversacionales inteligentes sobre servicios y atención del salón. | Pública / JWT |

---

## ⚙️ Variables de Entorno

Para ejecutar el proyecto en local o desplegarlo en la nube, configura las siguientes variables de entorno (nunca subas credenciales reales al repositorio):

| Variable | Descripción | Valor de Ejemplo |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | URL JDBC de conexión a PostgreSQL / Supabase | `jdbc:postgresql://aws-0-us-west-1.pooler.supabase.com:6543/postgres` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos | `postgres.tuproyecto` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos | `tu_password_seguro` |
| `JWT_SECRET` | Clave secreta en Base64/Hex para firmar los tokens JWT | `ClaveSecretaSuperSeguraDeAlMenos256BitsParaJWT2026` |
| `STRIPE_API_KEY` | Llave secreta de la API de Stripe (`sk_test_...`) | `sk_test_51EjemploClaveDePruebaStripe12345` |
| `STRIPE_SECRETO_WEBHOOK` | Secreto de firma del destino Webhook en Stripe (`whsec_...`) | `whsec_EjemploSecretoWebhookStripe67890` |
| `GEMINI_API_KEY` | Clave de API para el servicio de Inteligencia Artificial | `AIzaSyEjemploClaveGeminiApi12345` |
| `URL_PUBLICA` | URL base pública del servidor (sin barra `/` al final) | `https://ds-java-ia-backend-tallermd4y5-0y6w.onrender.com` |

---

## 🚀 Instalación y Ejecución Local

### Requisitos previos
* **JDK 17** o superior
* **Maven 3.8+** (o usar el wrapper `./mvnw` incluido)
* **Docker** (opcional, para ejecución en contenedor)

### 1. Clonar el repositorio
```bash
git clone [https://github.com/tu-usuario/ds-java-ia-backend-tallermd4y5.git](https://github.com/tu-usuario/ds-java-ia-backend-tallermd4y5.git)
cd ds-java-ia-backend-tallermd4y5
```

### 2. Compilar y ejecutar con Maven
```bash
# Empaquetar el proyecto
mvn clean package -DskipTests

# Ejecutar la aplicación
mvn spring-boot:run
```
La API quedará disponible en `http://localhost:8080` y Swagger UI en `http://localhost:8080/swagger-ui.html`.

### 3. Ejecutar con Docker
```bash
# Construir la imagen
docker build -t sillon-api .

# Ejecutar el contenedor pasando el archivo de variables de entorno
docker run -p 8080:8080 --env-file .env sillon-api
```

---

## 🧪 Pruebas Automatizadas y Evidencia Ejecutable

El proyecto cuenta con dos niveles de verificación automatizada para integración continua (CI/CD):

### 1. Pruebas Unitarias (JUnit 5 + Mockito)
Validan la lógica de negocio crítica, incluyendo el procesamiento e idempotencia de eventos webhook de Stripe (`PagoWebhookServiceTest`).
```bash
mvn test
```

### 2. Colección de Postman Automatizada (`sillon.postman_collection.json`)
En el repositorio se incluye la colección exportada en formato **Collection v2.1** con pruebas encadenadas:
* **Variables dinámicas:** Utiliza `{{baseUrl}}` apuntando al entorno desplegado y `{{token}}` para gestionar la sesión automáticamente.
* **Autenticación encadenada:** Las peticiones de `login` capturan el JWT mediante scripts *After response* (`pm.collectionVariables.set("token", ...)`) e inyectan el encabezado `Authorization: Bearer {{token}}` en los endpoints protegidos.
* **Aserciones automáticas:** Cada petición valida mediante `pm.test(...)` el código de estado HTTP esperado (`200` / `201`).

Para ejecutar la colección desde consola utilizando **Newman**:
```bash
npx newman run sillon.postman_collection.json --env-var "baseUrl=[https://ds-java-ia-backend-tallermd4y5-0y6w.onrender.com](https://ds-java-ia-backend-tallermd4y5-0y6w.onrender.com)"
```

---

## 👩‍💻 Autoría

Desarrollado por **Jhojaneth Sicacha Romero** — Programa de Ingeniería de Sistemas / Formación Backend Java & IA.