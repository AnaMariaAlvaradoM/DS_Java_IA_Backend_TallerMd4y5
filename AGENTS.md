# Sillón · Instrucciones para el agente

## Contexto y fase actual
Backend SaaS multi-inquilino (multi-tenant) para gestión de salones de belleza (**Sillón**).
- **Estado:** Fase 0 (inicio desde cero). Construye de forma incremental según la fase o tarea que te pida; no adelantes módulos ni endpoints que no correspondan a la tarea actual.
- **Fuente de verdad:** Este archivo y `SPEC.md` (si existe). Si algo es ambiguo, pregunta antes de suponer.

## Stack y configuración
- **Base:** Java 17, Spring Boot 4.1.3, Maven, MySQL(en local) y PostgreSQL(en producción).
- **Zona horaria obligatoria:** Todo cálculo de "hoy" y "ahora" usa `America/Bogota` (`ZoneId.of("America/Bogota")`), nunca el reloj del sistema (Render corre en UTC).
- **Secretos:** Las llaves (JWT, Stripe, IA) viven en `secrets.properties` (local) o en variables de entorno (Render). Nunca las leas, nunca las pongas en código, Git, logs ni prompts.

## Arquitectura y código
- **Organización por dominio (paquetes permitidos):**
    - `compartido/` (excepciones, `@RestControllerAdvice`, utilidades transversales como reloj/zona horaria)
    - `seguridad/` (configuración de seguridad, JWT, filtros, contexto del inquilino/usuario)
    - `auth/` (registro, login, emisión de tokens)
    - `salon/` (datos del salón, slug, horario, plan y créditos)
    - `usuario/` (dueños, estilistas y sus límites/permisos)
    - `servicio/` (catálogo de servicios y relación con estilistas)
    - `cliente/` (clientes por salón y normalización de teléfono)
    - `cita/` (agenda, disponibilidad, validaciones de cruces y estados)
    - `publico/` (endpoints públicos por `slug` para reservas en línea)
    - `pago/` (Stripe, checkout, webhooks e idempotencia de eventos)
    - `chat/` (chatbot de catálogo, regla anti-promesas y escalamiento)
    - `ia/` (clientes/proveedores de IA, validación JSON y consumo de créditos)
- **Capas:** `Controller → Service → Repository`. El Controller no contiene lógica de negocio.
- **Idioma:** Clases, métodos, variables y mensajes de dominio en español.
- **DTOs:** Nunca expongas entidades JPA en la API. Valida entradas con Jakarta Validation en los DTO de request.
- **Inyección:** Siempre por constructor. Nunca `@Autowired` sobre atributos.
- **Enums:** Persistidos como texto (`@Enumerated(EnumType.STRING)`).
- **Errores:** Excepciones de dominio propias manejadas en un `@RestControllerAdvice` centralizado, respetando estrictamente los códigos HTTP de las reglas de negocio.

---

## Reglas de negocio (inviolables)

### 1. Salones, cuentas y aislamiento
- **Aislamiento total:** Todo dato pertenece a un salón. Un usuario nunca ve ni modifica datos de otro salón. Cualquier intento responde **404** (nunca 403, para no revelar existencia).
- **Registro:** Crea salón y dueño en la misma operación: plan `GRATIS`, **10 créditos de IA**, horario por defecto `08:00` a `19:00`, sin domingos, intervalo de `30` minutos.
- **Correo único:** Único en toda la plataforma (incluye estilistas). Duplicado → **409**.
- **Slug:** Generado desde el nombre del salón en minúsculas, sin tildes ni espacios. Si ya existe, agrega un sufijo numérico.
- **Horario:** Apertura debe ser anterior al cierre (**409**). Intervalo entre `15` y `60` minutos (**400**).

### 2. Catálogo y equipo
- **Servicios:** Duración entre `15` y `480` minutos; precio entero en pesos mayor que `0` (**400** si incumple).
- **Estilistas:** Ofrecen uno o más servicios de su mismo salón. Servicio de otro salón → **404**.
- **Límite por plan:** En `GRATIS`, intentar crear el tercer estilista responde **402**. En `PRO` no hay límite.

### 3. Agenda y citas
- **Duración y precio:** El sistema calcula la hora de fin según la duración del servicio. La cita congela el precio del servicio al agendarse (cambios futuros no la afectan).
- **Validaciones al agendar (409):** Prohibido agendar en el pasado, fuera del horario del salón, en domingo si el salón no abre, o con un estilista que no ofrece el servicio.
- **Sin cruces (409):** Un estilista no puede tener dos citas solapadas. Solo ocupan agenda las citas en estado `PENDIENTE` y `CONFIRMADA`.
- **Origen y estado inicial:** Creada por el salón → `CONFIRMADA` (origen `SALON`). Reservada en línea → `PENDIENTE` (origen `EN_LINEA`).
- **Clientes:** Al agendar, busca por teléfono dentro del salón; si no existe, lo crea. Guarda el teléfono solo con dígitos y el signo `+`.
- **Máquina de estados (409 si es inválida):**
    - `PENDIENTE` → `CONFIRMADA` | `CANCELADA`
    - `CONFIRMADA` → `COMPLETADA` | `CANCELADA` | `NO_ASISTIO`
    - `COMPLETADA`, `CANCELADA`, `NO_ASISTIO` son estados finales.
- **Permisos de estilista:** Solo puede modificar sus propias citas (cita de otro estilista → **404**) y solo pasarlas a `COMPLETADA` o `NO_ASISTIO` (otra transición → **403**).
- **Disponibilidad:** La calcula Java, nunca la IA. Son las horas de inicio, cada intervalo desde apertura, donde el servicio cabe completo antes del cierre, sin cruces y nunca en el pasado.

### 4. Pagos (Stripe)
- **Precios en el backend:** El cliente solo solicita `"plan PRO"` o `"paquete de créditos"`. Nunca acepta precios ni `price_id` desde el request.
- **Suscripción duplicada:** Si el salón ya es `PRO`, pedir suscripción responde **409**.
- **Webhook:** Verifica siempre la firma de Stripe. Sin firma o firma inválida → **400**.
- **Idempotencia:** Guarda el `id` de cada evento procesado; si llega repetido, se ignora sin error.
- **Efectos:**
    - `PRO` confirmado → pasa a `PRO`, guarda `subscription_id` y suma **300 créditos**.
    - Paquete pagado → suma **200 créditos**.
    - Suscripción cancelada → vuelve a `GRATIS` y **conserva** sus créditos.

### 5. Inteligencia Artificial
- **Créditos:** 1 crédito por respuesta de IA. Verifica saldo antes de llamar; si no hay → **402** y no se llama a la IA. Descuenta solo cuando la IA responde.
- **Recordatorios:** Solo para citas `PENDIENTE` o `CONFIRMADA` (otra → **409**). Java calcula los datos verificados (servicio, estilista, fecha, hora, valor, dirección); la IA solo redacta. Una persona lo aprueba; Sillón no lo envía.
- **Chatbot:** Usa el catálogo real de la base de datos. No agenda, cambia ni cancela citas: en esos casos escala creando una solicitud de contacto con los mensajes del cliente.
- **Regla anti-promesas (en Java):** Si la respuesta del chatbot menciona descuentos, cupones, algo gratis, promociones o reembolsos, se reemplaza por un mensaje seguro fijo y se escala con motivo `REGLA_DE_SEGURIDAD`.
- **Validación de salida:** Toda respuesta de IA se solicita en JSON y se valida. JSON inválido o incompleto → **502**. Ningún proveedor responde → **503**.
- **Seguridad:** Nunca registres en logs mensajes de clientes ni llaves de API.

---

## Tests
- JUnit 5 + Mockito. Los tests unitarios de Service no levantan contexto de Spring.
- Cada regla de negocio debe quedar cubierta por al menos un test.
- Nunca modifiques una aserción de un test para hacerlo pasar. Si un test falla, explica si el error está en el código o en el test y espera confirmación.

## Fuera de alcance (NO implementar)
- Envío real de mensajes por WhatsApp o correo electrónico.
- Recordatorios automáticos programados (`@Scheduled`) ni reportes.
- Pagos del cliente final al salón (Stripe Connect).
- Interfaz gráfica (frontend).

## Límites del agente
- No agregues dependencias al `pom.xml` sin preguntar primero.
- No modifiques archivos de configuración (`application.properties`, `secrets.properties`) salvo que se te pida explícitamente.
- No inventes endpoints, campos ni reglas fuera de este documento.