# 💈 Sillón

Sillón es un SaaS multi-tenant de agenda para salones de belleza y barberías: catálogo público, reservas en línea, pagos con Stripe y recordatorios/chatbot con IA. Cada salón es un inquilino aislado y nunca ve datos de otro.

## Producción

```
https://sillon-80ns.onrender.com
```

Swagger:

```
https://sillon-80ns.onrender.com/swagger-ui.html
```

> Render Free duerme el servicio tras un rato de inactividad: la primera petición puede tardar unos 50 segundos en responder.

## Stack

- Java 17, Spring Boot 4.1.2, Maven
- Spring Data JPA
- MySQL (local) y PostgreSQL vía Supabase (producción)
- Spring Security con JWT y BCrypt
- springdoc (Swagger / OpenAPI)
- Stripe en modo prueba
- Gemini por HTTP directo con `RestClient`, con fallback
- JUnit 5 y Mockito
- Docker y Render

## Cómo correrlo en local

1. Tener MySQL encendido.
2. Copiar `secrets.example.properties` a `secrets.properties` y llenarlo con tus valores.
3. Levantar la Stripe CLI para el webhook:
   ```
   stripe listen --forward-to localhost:8080/api/pagos/webhook
   ```
   Copiar el `whsec_...` que genera a `secrets.properties`.
4. Ejecutar:
   ```
   .\mvnw spring-boot:run
   ```

## Cómo verificarlo

La colección de Postman se incluye en `postman/` (`sillon.postman_collection.json`), con dos entornos (local y producción) que usan la variable `baseUrl`. Corre las carpetas 1 a 6 con el Runner; para la carpeta 7 (pagos) usa la tarjeta de prueba `4242 4242 4242 4242`.

Tests del proyecto:

```
.\mvnw test
```

## Endpoints

| Método | Ruta | Rol |
|---|---|---|
| POST | /api/auth/registro | Público |
| POST | /api/auth/login | Público |
| GET | /api/salon | DUENO |
| PUT | /api/salon/configuracion | DUENO |
| POST | /api/servicios | DUENO |
| PUT | /api/servicios/{id} | DUENO |
| GET | /api/servicios | DUENO, ESTILISTA |
| POST | /api/estilistas | DUENO |
| GET | /api/estilistas | DUENO |
| GET | /api/clientes | DUENO |
| POST | /api/citas | DUENO |
| GET | /api/citas?fecha= | DUENO, ESTILISTA |
| PATCH | /api/citas/{id}/estado | DUENO, ESTILISTA |
| POST | /api/citas/{id}/recordatorio | DUENO |
| GET | /api/publico/salones/{slug} | Público |
| GET | /api/publico/salones/{slug}/disponibilidad | Público |
| POST | /api/publico/salones/{slug}/reservas | Público |
| POST | /api/publico/salones/{slug}/chat | Público |
| GET | /api/solicitudes | DUENO |
| PATCH | /api/solicitudes/{id}/atendida | DUENO |
| POST | /api/pagos/suscripcion | DUENO |
| POST | /api/pagos/creditos | DUENO |
| POST | /api/pagos/webhook | Público (firma Stripe) |
| GET | /api/pagos/resultado?estado= | Público |
| GET | /api/admin/salones | ADMIN |

## Variables de entorno en Render

```
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRETO
URL_PUBLICA
STRIPE_CLAVE_SECRETA
STRIPE_PRECIO_PRO
STRIPE_PRECIO_CREDITOS
STRIPE_SECRETO_WEBHOOK
GEMINI_LLAVE
```

`STRIPE_URL_API` y `SILLON_IA_OPENAI_URL` no se configuran en Render: usan su valor por defecto de producción real.

## Si Sillón tuviera 500 salones

Cuidaría primero la base de datos, porque cada consulta de agenda y de disponibilidad filtra por salón, estilista y fecha, y con 500 salones esas tablas crecen mucho más rápido que cualquier otra.
Agregaría índices en citas (salón, estilista, inicio) y vigilaría las conexiones: Supabase gratis permite pocas y cada petición usa una.
Si la agenda se vuelve lenta, el cliente deja de reservar y el negocio pierde dinero, que es justo el problema que Sillón quiere resolver.
