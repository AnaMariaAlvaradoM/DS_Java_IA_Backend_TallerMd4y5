# 💈 Sillón · SaaS Multi-inquilino de Agenda Inteligente para Salones y Barberías

> **Backend:** Java 17 · Spring Boot 3.4+ · Spring Security 6 (JWT) · Spring Data JPA · MySQL / PostgreSQL (Supabase)  
> **Inteligencia Artificial:** OpenAI API Spec (Google Gemini 2.5 Flash / Claude Fallback)  
> **Pagos:** Stripe Checkout & Webhooks Criptográficos HMAC-SHA256  
> **Despliegue:** Docker Multi-stage · Render Web Service · Supabase Session Pooler  

---

## 🌐 Enlaces del Proyecto en Producción

* **API Pública (Producción en Render):** [https://sillon-api-42n5.onrender.com](https://sillon-api-42n5.onrender.com)
* **Documentación Interactiva (Swagger UI):** [https://sillon-api-42n5.onrender.com/swagger-ui.html](https://sillon-api-42n5.onrender.com/swagger-ui.html)
* **OpenAPI 3 Specification (JSON):** [https://sillon-api-42n5.onrender.com/v3/api-docs](https://sillon-api-42n5.onrender.com/v3/api-docs)

---

## 📌 Descripción General

**Sillón** es una plataforma SaaS multi-tenant que soluciona los problemas de gestión operativa, inasistencias (*no-shows*) y atención en barberías y salones de belleza:
* **Autoservicio y Catálogo Público:** Cada salón dispone de un slug único (`/api/publico/salones/{slug}`) para consulta de servicios, estilistas y cálculo determinista de horarios libres en tiempo real sin requerir inicio de sesión.
* **Asistente Virtual (Chatbot IA):** Responde preguntas sobre servicios, precios y horarios con datos verificados de Java, con reglas activas de seguridad que bloquean promesas de cupones y escalan automáticamente a atención humana (`/api/solicitudes`).
* **Recordatorios de Cita para WhatsApp:** Genera borradores de confirmación personalizados y verificados (6 datos inmutables de la cita en locale `es-CO`) para revisión del equipo antes del envío manual.
* **Monetización con Stripe:** Modelo Freemium con upgrade a plan PRO (estilistas ilimitados y +300 créditos IA) y compra de paquetes adicionales (+200 créditos) con procesamiento de webhooks firmados e idempotentes.

---

## 💻 Requisitos para Desarrollo Local

Para compilar y ejecutar el proyecto en tu entorno local asegúrate de tener instalado:
* **Java Development Kit (JDK) 17** (Eclipse Temurin, OpenJDK o Amazon Corretto).
* **MySQL 8.0 o superior** (base de datos local `sillon_db`).
* **Maven Wrapper** (incluido en la raíz del proyecto como `mvnw` en Linux/macOS y `mvnw.cmd` en Windows).
* **Docker** (opcional, para construir y correr contenedores localmente).
* **Postman** (para ejecutar la suite de pruebas funcionales).

---

## ⚙️ Variables de Entorno Requeridas

La aplicación se configura mediante variables de entorno en entornos de producción (Render) y desarrollo. Todas cuentan con valores predeterminados seguros en `src/main/resources/application.properties` para permitir arranque local:

| Variable de Entorno | Descripción | Ejemplo / Valor por Defecto |
|---|---|---|
| `PORT` | Puerto HTTP donde se enlaza el servidor web | `8080` |
| `SPRING_DATASOURCE_URL` | URL JDBC de conexión a base de datos | Local: `jdbc:mysql://localhost:3306/sillon_db?createDatabaseIfNotExist=true`<br>Producción: `jdbc:postgresql://[host-pooler]:5432/postgres?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos | `root` (local) o usuario Supabase |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos | Secreto de base de datos |
| `JWT_SECRETO` | Clave secreta para firmar y validar tokens JWT (HMAC-SHA256) | Clave base64 segura (mín. 256 bits) |
| `URL_PUBLICA` | URL pública base del servicio para retornos de checkout | `https://sillon-api-42n5.onrender.com` (prod) o `http://localhost:8080` (local) |
| `STRIPE_CLAVE_SECRETA` | Clave API secreta de Stripe | `sk_test_...` o `sk_live_...` |
| `STRIPE_PRECIO_PRO` | ID del precio de suscripción mensual PRO en Stripe | `price_...` |
| `STRIPE_PRECIO_CREDITOS` | ID del precio del paquete de 200 créditos IA en Stripe | `price_...` |
| `STRIPE_SECRETO_WEBHOOK` | Clave secreta para verificar firmas de Webhooks Stripe | `whsec_...` |
| `GEMINI_LLAVE` | API Key para modelos de Google Gemini / OpenAI | `AIza...` |
| `SILLON_IA_OPENAI_URL` | Endpoint base del proveedor de IA compatible con OpenAI | `https://generativelanguage.googleapis.com/v1beta/openai/` |
| `STRIPE_URL_API` | URL base de la API de Stripe (permite simuladores locales) | `https://api.stripe.com` |

> [!WARNING]
> Nunca subas el archivo `secrets.properties` al repositorio. Utiliza la plantilla de ejemplo `secrets.example.properties` versionada en Git.

---

## 🚀 Compilación y Ejecución

### 1. Ejecución Local con Maven
Para compilar y arrancar la aplicación en tu máquina local:

```bash
# En Windows (PowerShell):
.\mvnw.cmd clean test-compile
.\mvnw.cmd spring-boot:run

# En Linux / macOS:
./mvnw clean test-compile
./mvnw spring-boot:run
```

La aplicación arrancará en `http://localhost:8080`. Puedes acceder a la documentación interactiva en:
`http://localhost:8080/swagger-ui.html`

### 2. Ejecutar la Suite de Pruebas Unitarias
Para ejecutar las 34 pruebas unitarias y de integración (JUnit 5 + Mockito):

```bash
# Windows:
.\mvnw.cmd test

# Linux / macOS:
./mvnw test
```

---

## 🐳 Despliegue y Ejecución con Docker

El proyecto incluye un [Dockerfile](file:///Dockerfile) con arquitectura multi-etapa (*multi-stage build*) y un archivo [.dockerignore](file:///.dockerignore) para generar imágenes seguras, ultraligeras y de rápida compilación:

### Construir la Imagen de Docker
```bash
docker build -t sillon-api:latest .
```

### Ejecutar el Contenedor
```bash
docker run -d \
  --name sillon-app \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/sillon_db" \
  -e SPRING_DATASOURCE_USERNAME="root" \
  -e SPRING_DATASOURCE_PASSWORD="tu_password" \
  -e JWT_SECRETO="tu_clave_secreta_jwt_de_al_menos_256_bits" \
  -e URL_PUBLICA="http://localhost:8080" \
  sillon-api:latest
```

---

## 🧪 Pruebas con la Colección de Postman

En la carpeta [postman/](file:///postman) encontrarás la colección oficial y los entornos preconfigurados:
* `postman/sillon.postman_collection.json`: Colección con más de 50 peticiones organizadas por módulos (Auth, Salón, Servicios, Estilistas, Clientes, Citas, IA, Pagos y Admin) con aserciones automáticas en JavaScript.
* `postman/local.postman_environment.json`: Variables de entorno para pruebas contra `http://localhost:8080`.
* `postman/produccion.postman_environment.json`: Variables de entorno configuradas con `https://sillon-api-42n5.onrender.com`.

### Instrucciones de Uso:
1. Abre **Postman** y selecciona **Import**.
2. Arrastra los archivos de la carpeta `postman/` (la colección y ambos entornos).
3. Selecciona el entorno deseado (**local** o **produccion**) en el selector superior derecho.
4. Ejecuta las peticiones en orden comenzando por:
   * `1. Auth / Registro exitoso`: Crea el salón y el usuario dueño, guardando automáticamente `tokenDueno` y `salonSlug`.
   * `2. Configuración / Actualizar horario`: Define la jornada comercial del salón.
   * `3. Catálogo / Crear servicio`: Registra servicios y captura `servicioCorteId`.
   * `4. Estilistas / Crear estilista`: Registra el profesional y asigna servicios.
   * `5. Citas / Disponibilidad y Reservas`: Prueba el motor de agenda y cálculo de horarios.
5. Puedes ejecutar toda la colección automáticamente mediante el **Collection Runner** de Postman.

---

## 🏗️ Documentación de Arquitectura y Referencias Técnicas

Para profundizar en el diseño técnico, consulta los documentos oficiales del proyecto:
* [SPEC.md](file:///SPEC.md): Especificación exhaustiva de 21k caracteres con reglas de negocio, orden de validaciones, transiciones de estados y contratos de API.
* [AGENTS.md](file:///AGENTS.md): Directivas inmutables para agentes de IA y desarrolladores (reloj `Clock`, multi-tenancy y desacoplamiento transaccional).
* [BITACORA-IA.md](file:///BITACORA-IA.md): Bitácora cronológica con 10 entradas detallando la asistencia de IA, decisiones técnicas y lecciones aprendidas.
* [BITACORA_PERSONAL.md](file:///BITACORA_PERSONAL.md): Playbook de ingeniería con bitácora de incidencias resueltas.

---

<small>© 2026 Sillón SaaS · Proyecto Integrador Módulos 4 y 5 · Dev Senior Code</small>
