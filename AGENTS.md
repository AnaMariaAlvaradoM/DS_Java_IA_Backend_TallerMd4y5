# Sillón · instrucciones para el agente

## Contexto
Backend multi-inquilino para agendar citas en barberías y salones (SaaS).
Antes de cambiar reglas de negocio, lee `SPEC.md`: es la fuente de verdad.
El dueño de cada decisión es humano; el agente propone y verifica contra la SPEC.

## Stack
- Java 17, Spring Boot 4.1, Maven, JPA, MySQL (local) / PostgreSQL (prod).
- Configuración solo en `application.properties`. Nunca `application.yml` ni perfiles.
- Spring Security con JWT (jjwt), BCrypt, roles DUENO/ESTILISTA/ADMIN.
- Swagger con springdoc 3.x. Stripe en modo prueba. IA por HTTP con RestClient.
- Sin Lombok, sin Spring AI, sin SDKs de IA.

## Arquitectura
- Paquete base `com.devsenior.sillon`, organizado por dominio (modelo campusFlow):
  `compartido/`, `seguridad/`, `auth/`, `salon/`, `usuario/`, `servicio/`,
  `cliente/`, `cita/`, `publico/`, `pago/`, `chat/`, `ia/`.
- Dentro de cada dominio: `model/`, `repository/`, `service/`, `controller/`,
  `dto/` (records), `mapper/` (propio, `@Component`, sin librerías de mapeo).
- El Controller no tiene lógica de negocio. Nunca expongas entidades JPA en la API.
- Inyección por constructor. Nunca `@Autowired` sobre atributos.
- Enums persistidos con `@Enumerated(EnumType.STRING)` (sin tildes: DUENO, no DUEÑO).
- Errores con excepciones propias (una por código) y `@RestControllerAdvice`
  en `compartido/`. Formato único: código, error, mensaje en español, fecha.

## Multi-inquilino
- Todo dato pertenece a un salón. Filtra SIEMPRE por el `salonId` del usuario
  autenticado (`@AuthenticationPrincipal`), nunca por un id que llegue en la URL
  o el cuerpo. Dato de otro salón = 404, como si no existiera.

## Código
- Clases, métodos y variables en español.
- Sin Lombok: getters y setters explícitos.
- Validación de entrada con Jakarta Validation en los DTO de request.
- Todo `throw` que use el constructor con causa `(mensaje, causa)` lleva el comentario
  `// throw con causa` en la línea anterior. No se agregan otros comentarios al código.
- "Hoy" y "ahora" salen del reloj del proyecto (bean `Clock`, `America/Bogota`),
  nunca de `LocalDate.now()` ni `LocalDateTime.now()` directamente.

## Tests
- JUnit 5 + Mockito. Los tests del Service no levantan Spring. Reloj fijo.
- Ningún test llama a Gemini ni a Stripe.
- Nunca modifiques un test para que pase. Si un test falla, explícame si la causa
  está en el test o en el código y espera mi decisión.
- Un test rojo contra la SPEC es una pregunta de negocio: no la resuelvas tú.

## IA
- Todo proveedor de IA implementa la interfaz del paquete `ia/`. Fallback en `AsistenteIA`.
- Las llamadas HTTP se hacen con `RestClient`. Sin SDKs de proveedores ni Spring AI.
- Nunca escribas llaves en el código ni en `application.properties`: van en
  `secrets.properties` o variables de entorno, que no puedes leer.
- Nunca registres en logs llaves ni el mensaje completo del cliente.
- Los prompts viven en `src/main/resources/prompts/*.txt` y se cargan con la
  biblioteca de prompts. Nunca escribas un prompt dentro de una clase Java ni
  modifiques un `.txt` sin que te lo pida.
- El texto del cliente va siempre como mensaje, nunca dentro de las instrucciones.
- Toda respuesta JSON de la IA se lee y se valida antes de usarla (si no: 502).
- Java calcula y la IA redacta: fechas, horas, precios, cruces y disponibilidad
  los hace Java; la IA solo redacta o extrae.
- Verificar créditos ANTES de llamar a la IA; ninguna llamada a la IA dentro de
  una transacción de base de datos.

## Límites
- No agregues dependencias al `pom.xml` sin preguntarme.
- No modifiques `application.properties` salvo que te lo pida.
- No modifiques nada bajo `.github/` (el calificador vive ahí).
- Si algo de la SPEC es ambiguo, pregunta antes de suponer.