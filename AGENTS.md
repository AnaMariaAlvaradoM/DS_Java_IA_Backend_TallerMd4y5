# 🤖 Guía de Arquitectura y Directivas para Agentes de IA · Proyecto Sillón

> **Repositorio:** `DS_Java_IA_Backend_TallerMd4y5`  
> **Propósito:** Definir los principios arquitectónicos, directivas técnicas y restricciones obligatorias que cualquier agente de IA o desarrollador debe respetar al interactuar, refactorizar o extender el código del proyecto Sillón.

---

## 1. Paquete Base y Arquitectura Modular

Todo el código de la aplicación reside estrictamente bajo el paquete raíz:
```text
com.devsenior.sillon
```

### Estructura de Paquetes por Responsabilidad
* `compartido/`: Clases transversales a todo el sistema.
  * `configuracion/`: Beans globales (`RelojConfig`, `OpenApiConfig`).
  * `dto/`: DTOs comunes, notablemente `ErrorRespuesta` (contrato de 4 campos).
  * `excepcion/`: Jerarquía de excepciones de negocio y `ManejadorGlobalErrores`.
* `seguridad/`: Filtro JWT (`JwtFiltro`), servicio de tokens (`JwtService`), modelo de usuario autenticado (`UsuarioPrincipal`) y configuración de Spring Security (`SecurityConfig`).
* `salon/`: Modelo (`Salon`, `ConfiguracionSalon`, `Plan`), repositorios, servicios (`SalonService`, `CatalogoService`), controladores (`SalonController`, `PublicoController`) y utilidades (`SlugUtil`).
* `usuario/`: Modelo de credenciales y roles (`Usuario`, `Rol`) y repositorio.
* `auth/`: Controladores y servicios de registro y login (`AuthController`, `AuthService`).
* `servicio/`: Catálogo de servicios del salón (`Servicio`), DTOs y controladores.
* `estilista/`: Controladores y DTOs de nómina de estilistas.
* `cliente/`: Directorio de clientes (`Cliente`), repositorio y normalización telefónica (`ClienteService`).
* `cita/`: Motor de agenda (`Cita`, `EstadoCita`, `OrigenCita`), repositorio, algoritmo de horarios (`ReglasAgenda`), servicios y controlador.
* `ia/`: Motor de Inteligencia Artificial.
  * `dto/`: DTOs de request, response, auditoría de consumo (`FacturaIa`) y respuestas genéricas (`RespuestaIa`).
  * `proveedor/`: Abstracción `ProveedorIa`, cliente OpenAI/Gemini (`ProveedorOpenAi`) y fallback (`ProveedorClaude`).
  * `servicio/`: Orquestación multimodelo (`AsistenteIaService`), chatbot (`ChatbotIaService`), recordatorios (`RecordatorioIaService`) y créditos (`CreditosIaService`).
  * `util/`: Gestor de plantillas de prompts (`BibliotecaPrompts`) y parser seguro de JSON (`LectorJsonIa`).
* `solicitud/`: Gestión de solicitudes de contacto generadas por el chatbot (`Solicitud`, `MotivoSolicitud`, `EstadoSolicitud`).
* `pago/`: Integración con Stripe (`StripeConfig`, `PagoService`, `PagoController`, entidad `EventoStripe`).
* `admin/`: Funciones de auditoría para el rol `ADMIN` (`AdminController`).

---

## 2. Reglas Estrictas de Aislamiento Multi-Inquilino (*Multi-Tenancy*)

1. **Prohibido Confiar en el Cliente:**
   * Ningún endpoint autenticado debe recibir ni confiar en un parámetro `salonId` proveniente de `@RequestBody`, `@RequestParam` o `@PathVariable`.
   * El `salonId` debe extraerse **siempre y de forma exclusiva** desde el contexto de seguridad mediante `@AuthenticationPrincipal UsuarioPrincipal usuario` invocando `usuario.getSalonId()`.
2. **Consultas de Persistencia Filtradas:**
   * Todas las consultas en `Repository` que lean, actualicen o eliminen registros pertenecientes a un salón deben incluir el parámetro `salonId` (ej. `findBySalonId`, `findByIdAndSalonId`, `findAllByIdInAndSalonId`, `existsByEstilistaIdAndSalonId`).
   * Nunca invocar `findById(id)` simple para entidades de inquilino sin validar posteriormente que la entidad recuperada pertenezca al salón del usuario en sesión.

---

## 3. Uso Obligatorio de `java.time.Clock` (Consistencia Temporal)

1. **Prohibición de `now()` sin Reloj:**
   * **Queda terminantemente prohibido** llamar a `LocalDateTime.now()`, `LocalDate.now()`, `LocalTime.now()` o `Instant.now()` sin argumentos.
   * Toda consulta del tiempo presente debe invocar `LocalDateTime.now(clock)` o `Instant.now(clock)`.
2. **Inyección del Bean `Clock`:**
   * Se inyecta la instancia de `java.time.Clock` configurada en `RelojConfig` con la zona horaria oficial `America/Bogota`.
   * **Justificación técnica:** Los entornos cloud (como Render, AWS o contenedores Docker) ejecutan el sistema operativo en UTC. Sin el bean inyectado, a las 8:00 p.m. de Bogotá el sistema asume que es el día siguiente, rompiendo los cálculos de turnos y agendas. Además, el bean inyectado permite ejecutar pruebas unitarias con instantes fijos y reproducibles (`Clock.fixed`).

---

## 4. Manejo de Errores Estandarizado (Contrato de 4 Campos)

1. **Clase Centralizadora:**
   * Todos los errores deben gestionarse en `ManejadorGlobalErrores` mediante anotaciones `@ExceptionHandler`.
2. **Estructura Inmutable:**
   * El cuerpo de cualquier respuesta de error (4xx y 5xx) debe ser estrictamente una instancia de `ErrorRespuesta`:
     ```json
     {
       "codigo": 404,
       "error": "Not Found",
       "mensaje": "Mensaje legible y descriptivo",
       "fecha": "2026-11-20T15:30:00"
     }
     ```
3. **Manejo en Capa de Seguridad:**
   * Spring Security intercepta errores de autenticación (401) y autorización (403) antes de llegar a los controladores.
   * `SecurityConfig` debe utilizar implementaciones personalizadas de `AuthenticationEntryPoint` y `AccessDeniedHandler` que serialicen manualmente `ErrorRespuesta` en JSON usando `ObjectMapper`, impidiendo que Spring devuelva HTML o cuerpos vacíos.

---

## 5. Directivas para el Motor de Inteligencia Artificial

1. **Ubicación de Prompts:**
   * Los prompts de sistema residen obligatoriamente como archivos de texto en `src/main/resources/prompts/` (`recordatorio.txt` y `chatbot.txt`).
2. **Java es la Fuente de la Verdad:**
   * La IA no toma decisiones de negocio, no valida disponibilidad, no calcula precios ni inventa fechas.
   * Java calcula los horarios con `ReglasAgenda`, extrae y valida los datos con `CitaService`, y le entrega a la IA el contexto 100% verificado. La IA únicamente redacta el texto amigable.
3. **Formato JSON Estricto:**
   * Los prompts deben obligar al LLM a responder exclusivamente un objeto JSON válido (sin etiquetas markdown adicionales ni texto introductorio). `LectorJsonIa` se encarga de sanear posibles bloques ```json y deserializar con seguridad.
4. **Desacoplamiento Transaccional:**
   * **Nunca realizar llamadas a `asistenteIaService.generar()` dentro de métodos anotados con `@Transactional`.**
   * Las llamadas de red a la IA toman segundos; retener transacciones de base de datos abiertas durante una petición HTTP externa satura el pool de conexiones de base de datos.
5. **Cadena de Fallback Multimodelo:**
   * El sistema debe inyectar una lista ordenada de `ProveedorIa` (`@Order(1) ProveedorOpenAi`, `@Order(2) ProveedorClaude`).
   * Si un proveedor falla por timeout o cuota, el orquestador intenta automáticamente con el siguiente. Si todos fallan, se traduce la excepción a `ServicioNoDisponibleException` (HTTP 503).
6. **Defensa Activa Anti-Promesas:**
   * El backend debe filtrar en Java el texto emitido por el chatbot buscando palabras como `descuento`, `cupón` o `gratis`. Si aparecen, se anula la respuesta, se sustituye por el mensaje seguro institucional y se escala a atención humana.

---

## 6. Metodología de Pruebas Unitarias (Calidad y Rigor)

1. **Aislamiento con JUnit 5 y Mockito:**
   * Las pruebas unitarias de servicios se construyen con `@ExtendWith(MockitoExtension.class)`.
   * **Prohibido abusar de `@SpringBootTest`:** Levantar el contexto completo de Spring para pruebas unitarias degrada el tiempo de CI/CD. Solo `SillonApplicationTests` debe usar `@SpringBootTest`.
2. **Regla de Oro ante Fallos de Tests:**
   * **Nunca modificar una aserción o relajar un test solo para que pase.**
   * Si un test falla, la causa es una discrepancia de contrato o un error en la implementación de negocio. Siempre se debe contrastar la lógica contra la especificación en `SPEC.md` y corregir el código productivo.
3. **Tolerancia Temporal en Webhooks de Stripe:**
   * Al probar webhooks firmados, sincronizar el timestamp generado con el reloj del sistema para no violar la ventana de tolerancia de 300 segundos de Stripe (`System.currentTimeMillis()`).

---

## 7. Política Estricta de Secretos y Seguridad

1. **Exclusión de Secretos en Git:**
   * El archivo local `secrets.properties` debe permanecer en `.gitignore` y bajo ninguna circunstancia debe subirse al repositorio.
2. **Sin Llaves en Código Fuente:**
   * Queda terminantemente prohibido incluir llaves API reales de Stripe (`sk_live_...`, `whsec_...`), Google Gemini (`AIza...`) o secretos JWT en archivos `.java` o `.properties` versionados.
   * Utilizar variables de entorno (`System.getenv`) o propiedades respaldadas con valores placeholder por defecto en `application.properties` y la plantilla pública `secrets.example.properties`.

