# Sillón · Instrucciones para el agente

## Contexto
Backend de gestión para salones de belleza y barberías Sillón.
Antes de cambiar reglas de negocio, lee `SPEC.md`: es la fuente de verdad.

## Stack
- Java 25, Spring Boot, Maven, MySQL.
- Configuración solo en `application.properties`. Nunca `application.yml` ni perfiles.

## Arquitectura y Multitenancy
- Paquete base `com.sillon`, organizado por dominio: `auth/`, `salon/`, `estilista/`, `servicio/`, `cita/`, `cliente/`, `pago/`, `ia/`, `compartido/`.
- Capas: Controller → Service → Repository. El Controller no tiene lógica de negocio.
- **Aislamiento por Salón:** Filtrar todo estrictamente por el salón del usuario autenticado. Ninguna consulta, edición o eliminación debe exponer ni alterar datos de otro salón.
- DTOs como `record` en `<dominio>/dto`. Nunca expongas entidades JPA en la API.
- Un mapper propio por dominio (`@Component`), sin librerías de mapeo.
- Inyección por constructor. Nunca `@Autowired` sobre atributos.
- Enums persistidos con `@Enumerated(EnumType.STRING)`.
- Errores con excepciones propias y un `@RestControllerAdvice` en `compartido/`.

## Código y Manejo del Tiempo
- Clases, métodos y variables en español.
- Sin Lombok: getters y setters explícitos.
- Validación de entrada con Jakarta Validation en los DTO de request.
- **Reloj del Proyecto:** "hoy" y "ahora" se calculan con la zona horaria del negocio (`America/Bogota`) utilizando el reloj centralizado del proyecto. Prohibido usar `LocalDateTime.now()` directamente sin el reloj del proyecto.

## Tests
- JUnit 5 + Mockito. Los tests del Service no levantan Spring.
- **Nunca modifiques un test para que pase.** Si un test falla, explícame si la causa está en el test o en el código y espera mi decisión.
- Un test rojo contra la SPEC es una pregunta de negocio: no la resuelvas tú.
- Si un cambio de contrato pedido en la SPEC obliga a ajustar cómo se construyen los datos de un test, puedes ajustarlo, pero nunca cambies lo que el test comprueba.
- Cero modificaciones en la carpeta `.github/`.

## IA
- Todo proveedor de IA implementa la interfaz `ProveedorIA` en el paquete `ia/`.
- Las llamadas HTTP se hacen con `RestClient`. Sin SDKs de proveedores ni Spring AI.
- Nunca escribas llaves en el código ni en `application.properties`: van en `secrets.properties`, que no puedes leer.
- Antes de usar una URL, un header, un parámetro o un nombre de modelo de una API de IA, verifícalo en la documentación oficial actual. Tu conocimiento puede estar desactualizado.
- Nunca registres en logs el mensaje del cliente ni las llaves.
- **Prompts en archivos:** Los prompts viven en `src/main/resources/prompts/*.txt` y se cargan con `BibliotecaPrompts`. Nunca escribas un prompt dentro de una clase Java ni modifiques un `.txt` sin que te lo pida.
- El texto del cliente va siempre como mensaje, nunca dentro de las instrucciones.
- Toda respuesta JSON de la IA se lee con `LectorJsonIA` y se valida antes de usarla.
- **Java calcula y la IA redacta:** Los cálculos (fechas, disponibilidad, precios, créditos, límites de plan y reglas anti-promesas) los realiza Java. La IA se limita únicamente a redactar mensajes o procesar texto.

## Límites
- No agregues dependencias al `pom.xml` sin preguntarme.
- No modifiques `application.properties` salvo que te lo pida.
- Si algo de la SPEC es ambiguo, pregunta antes de suponer.