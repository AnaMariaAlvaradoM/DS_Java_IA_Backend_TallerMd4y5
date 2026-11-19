# Reglas del agente · Sillón

## Stack
- Java 17 (release 17), Spring Boot 4.1, Maven. Código compatible con Java 17: sin características de Java 21 o superior.
- Paquete base `com.devsenior.sillon`, organizado por dominios: compartido, seguridad, auth, salon, usuario, servicio, cliente, cita, publico, pago, chat, ia.
- Sin Lombok, sin Spring AI, sin SDKs de IA. Getters y setters explícitos. La IA se llama por HTTP con RestClient.
- Jackson 3: el ObjectMapper está en `tools.jackson.databind`.
- Configuración solo en `application.properties` (nunca yml ni perfiles).

## Multi-inquilino
- TODA consulta filtra por el `salonId` del usuario autenticado (`@AuthenticationPrincipal`).
- Nunca recibir el salonId desde la URL o el cuerpo.
- Un dato de otro salón responde 404, como si no existiera.

## Código
- DTOs como records y mapper propio. Las entidades nunca salen en las respuestas.
- Todos los errores usan el formato único: `codigo`, `error`, `mensaje`, `fecha` (incluidos 401 y 403).
- Enums con `EnumType.STRING` y valores sin tildes.
- "Hoy" y "ahora" salen del bean `Clock` (America/Bogota), nunca de `LocalDate.now()` ni `LocalDateTime.now()` directos.
- Reglas de agenda en una clase sin dependencias de base de datos.
- Rutas, campos y códigos HTTP exactamente como en SPEC.md.

## IA
- Los prompts viven en `src/main/resources/prompts`.
- Java calcula y valida; la IA solo redacta.
- Ninguna llamada a la IA dentro de una transacción.
- Las URLs de Stripe (`STRIPE_URL_API`) y de la IA (`sillon.ia.openai.url`) son configurables, nunca fijas en el código.

## Seguridad
- Nunca llaves en el código, en Git ni en prompts. Solo `secrets.properties` o variables de entorno.
- `secrets.properties` nunca se sube.

## Proceso
- Seguir SPEC.md. Si algo no está claro, preguntar antes de inventar.
- Nunca modificar un test para que pase. Si un test falla, decidir contra SPEC.md si el error está en el test o en el código.
- No agregar dependencias sin preguntar.
- Un commit por paso en verde.
- Código compatible con Java 17 (release 17): sin características de Java 21 o superior (por ejemplo, sin pattern matching en switch ni SequencedCollection).