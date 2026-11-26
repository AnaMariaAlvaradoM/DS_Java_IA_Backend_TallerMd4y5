# AGENTS.md · Reglas para el agente en Sillón

## Contexto
- Proyecto: Sillón, SaaS multi-inquilino de agenda para barberías y salones.
- La fuente de verdad es `SPEC.md`. Si algo no está en la SPEC, pregunta antes de inventarlo.
- Stack: Java 17+, Spring Boot 4.1, Maven, Spring Data JPA, Spring Security + JWT (jjwt), springdoc 3.x, stripe-java, RestClient.

## Paquetes
Paquete base `com.devsenior.sillon`, organizado por dominios:
`compartido`, `seguridad`, `auth`, `salon`, `usuario`, `servicio`, `cliente`, `cita`, `publico`, `pago`, `chat`, `ia`.
Cada dominio tiene su entidad, repositorio, servicio, controlador, DTOs (records) y mapper propio.

## Reglas de código
1. Nada de Lombok, Spring AI ni SDKs de IA. Getters y setters explícitos.
2. DTOs como `record`. Nunca devolver entidades en los controladores.
3. Multi-inquilino: TODA consulta filtra por el `salonId` del usuario autenticado (`@AuthenticationPrincipal UsuarioPrincipal`). Nunca tomar el salonId de la URL ni del cuerpo. Un dato de otro salón responde 404.
4. Fechas y horas: siempre con el bean `Clock` (zona America/Bogota), inyectado por constructor. Nunca `LocalDateTime.now()` sin reloj.
5. Errores: siempre con el formato único `{codigo, error, mensaje, fecha}` y las excepciones propias de `compartido`.
6. Enums con `@Enumerated(EnumType.STRING)` y sin tildes.
7. Rutas, nombres de campos y códigos HTTP exactamente como la sección "Contrato" de la SPEC.
8. Spring Boot 4 usa Jackson 3: `tools.jackson.databind`. Las anotaciones siguen en `com.fasterxml.jackson.annotation`.

## IA dentro del producto
9. Java calcula, la IA redacta: los datos (fechas, precios, disponibilidad) los arma Java.
10. Los prompts viven en `src/main/resources/prompts/*.txt`, nunca en el código.
11. Ninguna llamada a la IA dentro de una transacción de base de datos.
12. Las URLs de Stripe y del proveedor de IA se leen de la configuración, nunca fijas en el código.

## Proceso
13. Nunca modificar un test para que pase. Si un test falla, explica si la causa está en el test o en el código.
14. No agregar dependencias sin preguntar.
15. Nunca escribir llaves, secretos ni contraseñas reales en el código ni en prompts.
16. No tocar la carpeta `.github/`.
17. Proponer un plan antes de escribir código y esperar aprobación.
