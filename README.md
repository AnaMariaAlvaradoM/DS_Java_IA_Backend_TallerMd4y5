# Sillón

Sillón es una API para gestionar la agenda de barberías y salones: servicios, estilistas, clientes y citas separados por salón.
Incluye reservas públicas, pagos Stripe en modo prueba y borradores/asistencia con IA; no envía mensajes automáticamente.

## Producción

- API: https://ds-java-ia-backend-tallermd4y5.onrender.com
- [Swagger UI](https://ds-java-ia-backend-tallermd4y5.onrender.com/swagger-ui.html)
- [OpenAPI](https://ds-java-ia-backend-tallermd4y5.onrender.com/v3/api-docs)

Render con PostgreSQL de Supabase mediante Session pooler, puerto 5432. El 2026-10-27 se verificaron Swagger, registro, login y un pago de prueba: el salón pasó de GRATIS/10 créditos a PRO/310, confirmado también por consulta del estudiante en Supabase. El servicio gratuito puede tardar en responder al despertar.

## Stack

Java 17, Spring Boot 4.1.1, Maven Wrapper, Spring Web MVC, Spring Data JPA, Spring Security, BCrypt, JWT, MySQL local, PostgreSQL/Supabase en producción, Stripe, RestClient para Gemini y respaldo Claude, springdoc/Swagger y Docker. No usa Lombok ni SDK de IA.

JUnit y Mockito provienen de spring-boot-starter-webmvc-test. Por decisión del estudiante se utiliza JUnit 6.0.3 gestionado por Boot, en lugar de JUnit 5 mencionado en el PDF; historia y motivo en [DEPENDENCIAS.md](DEPENDENCIAS.md).

## Ejecutar en local

Requisitos: Java 17, MySQL en localhost:3306 y acceso a Stripe de prueba/Gemini para los flujos externos. Docker es necesario si se utiliza la CLI de Stripe en contenedor.

1. Copiar la plantilla, sin versionar el archivo resultante:

   ```powershell
   Copy-Item secrets.example.properties secrets.properties
   ```

2. Editar secrets.properties con el usuario/contraseña de MySQL, un secreto JWT de al menos 32 caracteres, la clave secreta Stripe de prueba, los dos IDs de precios y la llave de Gemini. La aplicación usa por defecto la base sillon_db y permite crearla si el usuario MySQL tiene permiso. La plantilla indica las propiedades que se deben completar. No incluir llaves reales en el repositorio.

3. Preparar en Stripe los precios del enunciado: PRO recurrente de 15 USD al mes y paquete único de créditos de 5 USD, en la misma cuenta/entorno de prueba que la clave. Los IDs se configuran en el backend, no se envían desde el cliente.

4. Autenticar Stripe CLI, si aún no se hizo, y mantener abierto el listener:

   ```powershell
   docker run --rm -it -v stripe-config:/root/.config/stripe stripe/stripe-cli login
   docker run --rm -it --name sillon-stripe -v stripe-config:/root/.config/stripe stripe/stripe-cli listen --events "checkout.session.completed,customer.subscription.deleted" --forward-to http://host.docker.internal:8080/api/pagos/webhook
   ```

   Guardar en secrets.properties el secreto de firma de esa ejecución del listener, en sillon.stripe.secreto-webhook. Para Render se utiliza el secreto de su propio destino HTTPS, no el de la CLI.

5. Iniciar la API desde la raíz:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   Swagger local: http://localhost:8080/swagger-ui.html. La configuración está en application.properties, sin perfiles ni YAML. Clock usa America/Bogota. En una base MySQL existente con clientes.nombre VARCHAR, la ampliación a TEXT se encuentra en scripts/001-clientes-nombre-text.mysql.sql; una base nueva crea TEXT directamente.

Tests de la entrega (MySQL encendido; sin llamadas reales a Stripe/Gemini):

```powershell
.\mvnw.cmd test
```

La suite actual tiene 75 pruebas: casos mínimos del PDF y variantes de compatibilidad Stripe aprobadas por el estudiante. Matriz en [TESTS-ENTREGA.md](TESTS-ENTREGA.md); resultados de ejecución en [VERIFICACION.md](VERIFICACION.md).

Para construir la imagen Docker de dos etapas, que compila el JAR sin ejecutar tests:

```powershell
docker build -t sillon .
```

Al ejecutar la imagen deben suministrarse las variables de base de datos y secretos. El puerto se obtiene de PORT; la imagen final contiene JRE 17 y el JAR. .dockerignore excluye secretos, target y .git.

## Verificar con Postman

Importar estos tres archivos:

- [Colección](postman/sillon.postman_collection.json).
- [Entorno local](postman/local.postman_environment.json).
- [Entorno produccion](postman/produccion.postman_environment.json).

Cada entorno tiene únicamente baseUrl. Los tokens e IDs se capturan en variables de colección durante la ejecución, no se copian manualmente ni se publican. Los usuarios y teléfonos de las pruebas son ficticios. Cada ejecución genera correos y nombres de salón únicos, fechas de lunes a sábado al menos dos días adelante y el próximo domingo en America/Bogota.

La colección contiene 55 peticiones para los 52 escenarios obligatorios. Los escenarios que crean dos recursos o consultan dos listados tienen dos peticiones. Las siete carpetas conservan el orden y los nombres del PDF:

1. Registro, login y configuración
2. Catálogo y equipo
3. Agenda: reservas, cruces y estados
4. Aislamiento entre salones y roles
5. IA: recordatorio y chatbot
6. Pagos con Stripe (modo prueba)
7. Después de pagar (manual)

En el Runner, seleccionar solo carpetas 1–6 y ejecutar una iteración con local. Repetir con produccion contra Render. Se realizan cuatro llamadas reales de IA por ejecución y se crean dos sesiones Checkout de prueba; no se completan pagos automáticamente. En local, mantener MySQL y la API activos. No cambiar las expectativas para ocultar un fallo de IA, credenciales o negocio.

Para la carpeta 7, conservar los valores de variables de la ejecución correspondiente y abrir urlPagoPro, creada en la carpeta 6. Completar manualmente Checkout con tarjeta de prueba 4242 4242 4242 4242, vencimiento futuro y CVC de prueba. No usar tarjetas reales. Esperar el webhook y ejecutar solo la carpeta 7 en el mismo entorno: comprueba PRO con más de 300 créditos y crea el tercer estilista. No repetir carpeta 1 entre pago y verificación: generaría otro salón. La carpeta 7 renueva automáticamente el token con las credenciales ficticias de esa ejecución. No repetir su alta del tercero después de haberla aprobado, porque el correo ya existiría.

Conservar variables para esa comprobación manual no significa publicarlas: no exportar ni subir tokens, respuestas de login, secretos o sesiones de prueba como valores del archivo de colección. La nota oficial no es el resultado del Runner: procede del calificador en el PR.

## Endpoints

JWT se introduce en Authorize de Swagger como token, sin escribir el prefijo Bearer. Las rutas privadas obtienen el salón del usuario autenticado. Un recurso de otro salón responde 404. Todos los errores usan codigo/error/mensaje/fecha.

| Método | Ruta | Acceso |
|---|---|---|
| POST | /api/auth/registro | Público |
| POST | /api/auth/login | Público |
| GET | /api/salon | DUENO |
| PUT | /api/salon/configuracion | DUENO |
| GET | /api/servicios | DUENO, ESTILISTA |
| POST | /api/servicios | DUENO |
| PUT | /api/servicios/{id} | DUENO |
| GET | /api/estilistas | DUENO |
| POST | /api/estilistas | DUENO |
| GET | /api/clientes | DUENO |
| GET | /api/citas | DUENO; ESTILISTA solo sus citas |
| POST | /api/citas | DUENO |
| PATCH | /api/citas/{id}/estado | DUENO; ESTILISTA con restricciones del contrato |
| POST | /api/citas/{id}/recordatorio | DUENO |
| GET | /api/publico/salones/{slug} | Público |
| GET | /api/publico/salones/{slug}/disponibilidad | Público |
| POST | /api/publico/salones/{slug}/reservas | Público |
| POST | /api/publico/salones/{slug}/chat | Público |
| POST | /api/pagos/suscripcion | DUENO |
| POST | /api/pagos/creditos | DUENO |
| POST | /api/pagos/webhook | Firma Stripe, sin JWT |
| GET | /api/pagos/resultado | Público |
| GET | /api/solicitudes | DUENO |
| PATCH | /api/solicitudes/{id}/atendida | DUENO |
| GET | /api/admin/salones | ADMIN |

Son 22 rutas distintas y 25 operaciones HTTP.

## Variables de entorno de Render

Solo los nombres; sus valores se configuran directamente en Render:

- PORT
- SPRING_DATASOURCE_URL
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD
- JWT_SECRETO
- URL_PUBLICA
- STRIPE_CLAVE_SECRETA
- STRIPE_SECRETO_WEBHOOK
- STRIPE_PRECIO_PRO
- STRIPE_PRECIO_CREDITOS
- GEMINI_LLAVE

No configurar STRIPE_URL_API ni SILLON_IA_OPENAI_URL en Render, conforme al PDF: los valores predeterminados apuntan a Stripe/Gemini. Ambas direcciones sí son configurables para los simuladores del calificador. Supabase utiliza Session pooler, no la conexión directa. El destino de Stripe debe pertenecer al mismo entorno de prueba y escuchar checkout.session.completed y customer.subscription.deleted con carga Snapshot.

## Estado de entrega

Evaluación oficial del 2026-10-27: **5.0/5.0 y 145/145 pruebas aprobadas**, nueve criterios completos, en la [ejecución 40547139089](https://github.com/AnaMariaAlvaradoM/DS_Java_IA_Backend_TallerMd4y5/actions/runs/40547139089) para el commit 1a0c7f6. Resultado publicado en el [PR #5](https://github.com/AnaMariaAlvaradoM/DS_Java_IA_Backend_TallerMd4y5/pull/5#issuecomment-6446761040). El PR sigue abierto para la revisión de la instructora.

Colección y README verificados el 2026-10-27: carpetas 1–6 aprobadas en local y Render (53 peticiones, 107 aserciones sin fallos en cada entorno) y 70 tests Java aprobados. Carpeta 7 aprobada en Render tras el pago manual de la misma ejecución: cuatro aserciones sin fallos; salón 5 en PRO/306 créditos y tres estilistas. La carpeta 7 no se ejecutó en local. El fallo inicial de IA en Render se resolvió tras el ajuste del estudiante y se repitió el recorrido completo. Detalle en VERIFICACION.md.

PR de entrega: [#5 — Giovanny Hernandez Arbelaez](https://github.com/AnaMariaAlvaradoM/DS_Java_IA_Backend_TallerMd4y5/pull/5), abierto el 2026-10-27 desde main del fork hacia main del repositorio oficial. Las evaluaciones oficiales se consultan en ese único PR. Tras corregirse el bloqueo inicial del calificador, la ejecución 40545100582 obtuvo 4.4/5.0 (129/145). Se preparó compatibilidad para metadata tipo/tipoCompra y se renombró el test de chatbot sin cambiar sus aserciones; suite actual de 75 pruebas aprobadas. El resultado oficial de cada revisión se consulta en el PR y su historial en VERIFICACION.md. No se modificó .github/. La guía original se encuentra en el [repositorio de la instructora](https://github.com/AnaMariaAlvaradoM/DS_Java_IA_Backend_TallerMd4y5). Evidencias y pendientes reales: [VERIFICACION.md](VERIFICACION.md); usos de IA: [BITACORA-IA.md](BITACORA-IA.md).

## Si Sillón tuviera 500 salones

Reflexión propuesta con asistencia de IA y revisada y aprobada por el estudiante:

Cuidaría primero la agenda y la separación de los datos de cada salón.<br>
Con 500 salones, las reservas simultáneas podrían aumentar los conflictos de horarios y el riesgo de mostrar información ajena.<br>
Por eso vigilaría las consultas, los bloqueos al reservar y los permisos de acceso.
