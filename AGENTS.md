# Reglas de implementación · Sillón

- Trabajar desde `SPEC.md` y el contrato exacto de la sección 4 del enunciado. Verificar rutas, DTO, estados y códigos antes de cerrar una fase.
- Mantener Java 17 como nivel de compilación, Spring Boot 4.1.x y Maven. No usar Lombok, Spring AI ni SDK de IA; usar `RestClient` para proveedores de IA.
- Organizar el código bajo `com.devsenior.sillon` por dominios: `compartido`, `seguridad`, `auth`, `salon`, `usuario`, `servicio`, `cliente`, `cita`, `publico`, `pago`, `chat`, `ia`.
- Filtrar consultas y modificaciones por el salón del usuario autenticado. No exponer entidades JPA directamente; convertir a DTO dentro de la transacción.
- Para tiempo de negocio, inyectar un reloj y leer `sillon.zona-horaria`; el valor por defecto del enunciado es `America/Bogota` y el entorno local del usuario usa `America/Santiago`.
- Java calcula disponibilidad, valida reglas y prepara datos verificados. Los prompts viven en `src/main/resources/prompts`; la IA redacta y su JSON se valida antes de responder.
- Guardar secretos solo en `secrets.properties` ignorado o variables de entorno. Nunca escribir llaves en código, prompts, documentación o tests.
- Escribir tests a partir de las reglas. Investigar los fallos contra la especificación; no cambiar una expectativa solo para dejar el test verde.
- No modificar `.github/`. Hacer commits descriptivos por fase, sin mencionar herramientas de asistencia en nombres de ramas o mensajes.
- Registrar en `BITACORA-IA.md` los usos importantes de IA, con lo que se revisó y corrigió. No inventar revisiones.
