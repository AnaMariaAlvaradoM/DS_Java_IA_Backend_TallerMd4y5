# 📓 Playbook de Ingeniería · Proyecto Sillón (SaaS Multi-tenant)
> **Autor:** Jhonn Jairo Leal Hurtado  
> **Ubicación:** Bogotá, Colombia  
> **Proyecto:** Sillón — SaaS de agenda inteligente para barberías y salones de belleza  
> **Stack:** Java 17, Spring Boot, Spring Data JPA, MySQL (Local) / PostgreSQL (Supabase en Producción), JWT, Stripe, IA (Gemini / OpenAI API spec), Docker, GitHub Actions, Render.

---

## 📌 Índice de Contenidos
1. [Arquitectura y Principios de Diseño](#1-arquitectura-y-principios-de-diseño)
2. [Fase 0: Preparación del Terreno y Configuración Base](#2-fase-0-preparación-del-terreno-y-configuración-base)
3. [Fase 1: La Base SaaS (Multi-tenancy, Errores y Modelo)](#3-fase-1-la-base-saas-multi-tenancy-errores-y-modelo)
4. [Bitácora de Incidencias y Soluciones Técnicas](#4-bitácora-de-incidencias-y-soluciones-técnicas)
5. [Próximos Pasos](#5-próximos-pasos)

---

## 1. Arquitectura y Principios de Diseño

* **Multi-tenancy por discriminador:** Una sola aplicación y una sola base de datos atienden a múltiples salones. Cada tabla del negocio tiene la columna `salon_id`. Ninguna consulta o mutación puede confiar en un ID que venga del cliente; siempre se filtra por el salón del usuario autenticado.
* **Desacoplamiento temporal (`Clock`):** Los servidores cloud (Render) corren en UTC. Si en Bogotá son las 8:00 p.m., el servidor cree que es el día siguiente. Se inyecta un bean `java.time.Clock` configurado con `America/Bogota` en todos los servicios para garantizar consistencia y tests deterministas.
* **Estrategia Híbrida de Base de Datos:**
  * **Local:** MySQL (`sillon_db`).
  * **Producción:** PostgreSQL en Supabase vía JDBC Connection Pooler.
  * **Solución arquitectónica:** Incluir ambos drivers (`mysql-connector-j` y `postgresql`) en el `pom.xml`. JPA/Hibernate abstrae el dialecto sin cambiar una sola línea de código Java.

---

## 2. Fase 0: Preparación del Terreno y Configuración Base

### 2.1 Fork y Aislamiento de Proyectos en Git
* **Fork oficial:** Creado desde el repositorio de la instructora Ana Alvarado (`AnaMariaAlvaradoM/DS_Java_IA_Backend_TallerMd4y5`) hacia la cuenta personal (`jhonnlealDev`).
* **Aislamiento local de identidad:** Para evitar cruzar credenciales con otros proyectos laborales o personales, se configuró Git a nivel local:
  ```bash
  git config --local user.name "Jhonn Leal"
  git config --local user.email "correo_github"
  ```

### 2.2 Estructura Raíz y Regla de Oro del Calificador
* El archivo `pom.xml`, la carpeta `src/`, el wrapper `mvnw` y `.mvn/` deben ubicarse estrictamente en la raíz del repositorio, nunca dentro de una subcarpeta (ej. `sillon/`).
* La carpeta `.github/` contiene los flujos de GitHub Actions del calificador automático y es intocable.

### 2.3 Seguridad y Gestión de Secretos
* **`secrets.properties`:** Archivo privado local con contraseñas reales y secretos JWT/Stripe/Gemini. Ignorado estrictamente en `.gitignore`.
* **`secrets.example.properties`:** Plantilla pública versionada en Git para guiar la configuración sin exponer datos reales.
* **`application.properties`:** Configurado con variables de entorno y valores de relleno (*fallbacks*) por defecto. Esto garantiza que la aplicación siempre arranque, incluso si faltan llaves en entornos de prueba o CI/CD.

### 2.4 Integración Continua (CI/CD) y Primer Hito
* Se abrió un único Pull Request hacia el repositorio oficial con el título: **Jhonn Jairo Leal Hurtado**.
* **Resultado del Robot Calificador:**
  * **Nota inicial:** 0.4 / 5.0
  * **Tests aprobados:** 11 / 145 (Estructura base, compilación, validación de SPEC y AGENTS).

---

## 3. Fase 1: La Base SaaS (Multi-tenancy, Errores y Modelo)

### 📋 Checklist de Entregables (Fase 1)
* **Bloque A: Infraestructura Transversal y Compartida (✅ Completado)**
  - [x] Configuración del `Clock` inyectado con `America/Bogota`.
  - [x] Paquete compartido: DTO `ErrorRespuesta`, excepciones propias y `@RestControllerAdvice`.
* **Bloque B: Modelo Multi-tenant y Usuarios (✅ Completado)**
  - [x] Algoritmo `SlugUtil` con normalización de acentos.
  - [x] Entidad `Salon`, `@Embeddable ConfiguracionSalon` y repositorio.
  - [x] Entidad `Usuario`, enum `Rol` y repositorio.
  - [x] DTOs de entrada y salida (`RegistroRequest`, `AuthResponse`).

---

### 3.1 Bloque A: Paquete Compartido (`compartido`) — ✅ COMPLETADO

Se estructuró la capa compartida transversal encargada de estandarizar respuestas de error, desacoplar el tiempo y manejar excepciones globales:

#### 1. Configuración de Zona Horaria (`Clock`)
* **Archivo:** `com.devsenior.sillon.compartido.configuracion.RelojConfig`
* **Bean:** `Clock` configurado con `ZoneId.of("America/Bogota")`.
* **Propósito:** Desacopla la hora del sistema operativo y previene inconsistencias temporales en entornos de despliegue como Render (UTC) y en pruebas unitarias.

#### 2. DTO de Error Unificado (`ErrorRespuesta`)
* **Archivo:** `com.devsenior.sillon.compartido.dto.ErrorRespuesta`
* **Estructura (4 campos exactos del contrato de la API):**
  * `codigo` (`int`): Código numérico de estado HTTP (ej. 400, 401, 402, 403, 404, 409, 502).
  * `error` (`String`): Texto oficial del estado HTTP (ej. "Not Found", "Conflict", "Bad Request").
  * `mensaje` (`String`): Descripción clara en español. En errores de validación (`@Valid`), concatena los campos fallidos separados por punto y coma (`;`).
  * `fecha` (`LocalDateTime`): Marca de tiempo obtenida a partir del `Clock` inyectado.

#### 3. Jerarquía de Excepciones de Negocio (`compartido.excepcion`)
Todas extienden de `RuntimeException`:
| Excepción | Código HTTP | Caso de Uso |
|---|---|---|
| `RecursoNoEncontradoException` | `404 Not Found` | Recursos no existentes por ID/slug en el inquilino. |
| `ReglaNegocioException` | `409 Conflict` | Choques de agenda, duplicados, estados inválidos. |
| `PagoRequeridoException` | `402 Payment Required` | Salón sin créditos de IA disponibles. |
| `AccionNoPermitidaException` | `403 Forbidden` | Acceso a recursos de otro salón o permisos denegados. |
| `CredencialesInvalidasException` | `401 Unauthorized` | Falla en login de usuario. |
| `FirmaInvalidaException` | `400 Bad Request` | Fallo de verificación en firma de webhook de Stripe. |
| `ErrorPasarelaException` | `502 Bad Gateway` | Fallos de conectividad con Stripe o la IA. |

#### 4. Manejador Global de Errores (`ManejadorGlobalErrores`)
* **Archivo:** `com.devsenior.sillon.compartido.excepcion.ManejadorGlobalErrores`
* **Anotación:** `@RestControllerAdvice`
* **Responsabilidad:** Captura centralizada de:
  * Excepciones de negocio personalizadas mapeando su respectivo HTTP status.
  * `MethodArgumentNotValidException`: Extrae errores de validación de campos (`FieldError`) y los formatea como `campo: mensaje; ...`.
  * `HttpMessageNotReadableException`, `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`: Devuelve `400 Bad Request` con mensaje genérico estandarizado.
  * `Exception` (genérica): Devuelve `500 Internal Server Error` evitando fuga de trazas de pila al cliente.

---

### 3.2 Bloque B: Modelo de Inquilino y Usuarios — ✅ COMPLETADO

Se construyó el núcleo del dominio SaaS, implementando el modelo multi-inquilino, el generador de identificadores amigables y los contratos de datos de autenticación y registro:

#### 1. Generador de Slug Determinista (`SlugUtil`)
* **Archivo:** `com.devsenior.sillon.salon.util.SlugUtil`
* **Propósito:** Generar slugs normalizados, legibles y seguros para URLs a partir del nombre comercial del salón (ej. "Barbería La Navaja" → `barberia-la-navaja`).
* **Algoritmo:**
  * Reemplazo de espacios en blanco (`\s+`) por guiones (`-`).
  * Normalización con `java.text.Normalizer` en forma `NFD` para descomponer caracteres acentuados.
  * Remoción de marcas diacríticas mediante regex `\p{InCombiningDiacriticalMarks}+`.
  * Eliminación de caracteres no latinos (`[^\w-]`).
  * Colapso de guiones repetidos (`-+`) y conversión a minúsculas (`Locale.ROOT`).
  * Poda de guiones en los extremos (`^-|-$`).

#### 2. Modelo de Dominio del Salón (`salon.modelo` y `salon.repositorio`)
* **Enum `Plan` (`com.devsenior.sillon.salon.modelo.Plan`):**
  * Valores: `GRATIS` y `PRO`.
* **Objeto Embebido `ConfiguracionSalon` (`com.devsenior.sillon.salon.modelo.ConfiguracionSalon`):**
  * Anotación `@Embeddable` para aplanar columnas en la tabla `salones`.
  * Campos: `horaApertura` (`LocalTime`), `horaCierre` (`LocalTime`), `abreDomingos` (`boolean`), `intervaloMinutos` (`int`).
  * Método de fábrica `porDefecto()`: 8:00 a 19:00, domingos cerrado, intervalo de 30 minutos.
* **Entidad `Salon` (`com.devsenior.sillon.salon.modelo.Salon`):**
  * Mapeo a `@Table(name = "salones")`.
  * Clave primaria `@Id` con `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
  * Identificación comercial: `nombre` (100), `slug` único (120), `ciudad` (60), `direccion` (150), `telefono` (30).
  * Suscripción e IA: `plan` (`Plan.GRATIS` por defecto), `creditosIa` (10 créditos iniciales de cortesía), `stripeSubscriptionId` (100, nullable).
  * Configuración incrustada: `@Embedded ConfiguracionSalon` inicializada por defecto.
  * Auditoría: `fechaCreacion` (`LocalDateTime`).
* **Repositorio `SalonRepository` (`com.devsenior.sillon.salon.repositorio.SalonRepository`):**
  * Extiende `JpaRepository<Salon, Long>`.
  * Consultas derivadas: `findBySlug(String slug)`, `existsBySlug(String slug)` y `findByStripeSubscriptionId(String stripeSubscriptionId)`.

#### 3. Modelo de Dominio del Usuario (`usuario.modelo` y `usuario.repositorio`)
* **Enum `Rol` (`com.devsenior.sillon.usuario.modelo.Rol`):**
  * Valores: `DUENO`, `ESTILISTA`, `ADMIN`.
* **Entidad `Usuario` (`com.devsenior.sillon.usuario.modelo.Usuario`):**
  * Mapeo a `@Table(name = "usuarios")`.
  * Clave primaria `@Id` autoincremental (`IDENTITY`).
  * Datos personales y credenciales: `nombre` (100), `email` único (120), `password` (hasheado).
  * Control de acceso: `@Enumerated(EnumType.STRING)` para `rol` (`DUENO`, `ESTILISTA`, `ADMIN`), bandera `activo` (true por defecto).
  * **Aislamiento Multi-inquilino:** Relación obligatoria `@ManyToOne(fetch = FetchType.LAZY)` con `@JoinColumn(name = "salon_id", nullable = false)`. Todo usuario pertenece irrevocablemente a un salón.
  * Auditoría: `fechaCreacion` (`LocalDateTime`).
* **Repositorio `UsuarioRepository` (`com.devsenior.sillon.usuario.repositorio.UsuarioRepository`):**
  * Extiende `JpaRepository<Usuario, Long>`.
  * Consultas derivadas: `findByEmail(String email)`, `existsByEmail(String email)` y `findBySalonId(Long salonId)`.

#### 4. DTOs de Entrada y Salida (`salon.dto`)
* **`RegistroRequest` (`com.devsenior.sillon.salon.dto.RegistroRequest`):** Record de entrada con Bean Validation (`@NotBlank`, `@Size`, `@Email`) para registrar salón y dueño de manera conjunta (`salonNombre`, `ciudad`, `direccion`, `telefono`, `nombre`, `email`, `password`).
* **`AuthResponse` (`com.devsenior.sillon.salon.dto.AuthResponse`):** Record de salida con payload de sesión: `token`, `tipo` ("Bearer"), `expiraEnMinutos`, `usuarioId`, `nombre`, `rol`, `salonId`, `salonSlug`.

---

## 4. Bitácora de Incidencias y Soluciones Técnicas

| # | Problema / Incidencia | Causa Raíz | Solución Aplicada |
|---|---|---|---|
| 1 | `secrets.properties` aparecía como *Untracked` en Git | Faltaba la regla limpia en `.gitignore` | Se añadió `secrets.properties` al `.gitignore` y se verificó con `git status`. |
| 2 | Intento de reutilizar URL de base de datos anterior | Confusión con el proyecto previo `vetturno` | Se corrigió la base de datos a `sillon_db`, asegurando independencia total de esquemas. |
| 3 | Error al clonar URL en GitHub Desktop | La pestaña activa era *GitHub.com* (búsqueda de repositorios) | Se cambió a la pestaña *URL* para clonar directamente vía HTTPS. |
| 4 | No aparecía el botón "Open pull request" | Sesión iniciada en el navegador con cuenta de GitHub distinta | Se inició sesión con `jhonnlealDev` y se generó el PR oficial vía *Contribute*. |
| 5 | Error `Failed to clean project` con `mvnw clean` | Bloqueo de archivos `.class` en `target/` por OneDrive / IDE | Se pausó la sincronización y se compiló directamente con `.\mvnw.cmd compile`. |
| 6 | Error de ejecución en comando Maven con guiones | Sintaxis errónea `compile-test-compile` | Se corrigió el comando a `.\mvnw.cmd test-compile`, obteniendo `BUILD SUCCESS`. |
| 7 | Clases ubicadas en subcarpeta `sillon/` en raíz | Creación de archivos fuera de la ruta canónica del calificador | Se reubicaron en `src/main/java/com/devsenior/sillon/` respetando sus paquetes y se eliminó la carpeta residual. |

---

## 5. Próximos Pasos

- [ ] Implementar pruebas unitarias JUnit para `SlugUtil` (validación de diacríticos, mayúsculas, espacios y caracteres especiales).
- [ ] Desarrollar servicio de negocio (`SalonService` / `AuthService`) para orquestar la transacción atómica de registro y generación de slug único.
- [ ] Implementar capa de seguridad Spring Security 6, hashing de contraseñas con `BCryptPasswordEncoder` y generación/validación de tokens JWT (Fase 2).
- [ ] Exponer controladores REST con validación `@Valid` para endpoints públicos (`/api/auth/registro`, `/api/auth/login`).
- [ ] Ejecutar compilación y suite de pruebas automatizadas con el wrapper Maven.

