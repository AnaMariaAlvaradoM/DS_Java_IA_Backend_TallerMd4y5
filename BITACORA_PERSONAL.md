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
4. [Fase 2: Seguridad, Autenticación y Configuración de Inquilinos](#4-fase-2-seguridad-autenticación-y-configuración-de-inquilinos)
5. [Fase 3: Catálogo de Servicios, Estilistas (ManyToMany) y Clientes](#5-fase-3-catálogo-de-servicios-estilistas-manytomany-y-clientes)
6. [Fase 4: Agenda Inteligente, Disponibilidad y Reservas en Línea](#6-fase-4-agenda-inteligente-disponibilidad-y-reservas-en-línea)
7. [Fase 5: Pagos e Integración con Stripe (Suscripciones, Créditos y Webhooks)](#7-fase-5-pagos-e-integración-con-stripe-suscripciones-créditos-y-webhooks)
8. [Bitácora de Incidencias y Soluciones Técnicas](#8-bitácora-de-incidencias-y-soluciones-técnicas)
9. [Próximos Pasos](#9-próximos-pasos)

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
  * `NoResourceFoundException`: Devuelve `404 Not Found` ("La ruta solicitada no existe") para URLs no mapeadas, evitando que caigan como error 500.
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

## 4. Fase 2: Seguridad, Autenticación y Configuración de Inquilinos

### 📋 Checklist de Entregables (Fase 2)
* **Bloque A: Seguridad con Spring Security 6 & JWT (✅ Completado)**
  - [x] Dependencias añadidas en `pom.xml`: `spring-boot-starter-security` y suite `jjwt` (versión 0.12.6).
  - [x] Servicio criptográfico `JwtService` desacoplado temporalmente con `Clock` y vigencia de 24 horas (1440 min).
  - [x] Contexto de usuario `UsuarioPrincipal` (implementación de `UserDetails`) y servicio de carga `UsuarioDetailsService`.
  - [x] Filtro de interceptación `JwtFiltro` (`OncePerRequestFilter`) para autorización basada en encabezado `Authorization: Bearer`.
  - [x] Cadena de filtros `SecurityConfig` en modo *Stateless*, CSRF deshabilitado y control de acceso RBAC (`DUENO`, `ESTILISTA`, `ADMIN`).
  - [x] Estandarización de respuestas de error 401 (Unauthorized) y 403 (Forbidden) con DTO `ErrorRespuesta` en lugar de respuestas por defecto de Spring.
* **Bloque B: Flujo de Autenticación y Registro (`AuthService` y `AuthController`) (✅ Completado)**
  - [x] DTO de petición `LoginRequest` con validaciones de email y password.
  - [x] Lógica de registro transaccional: normalización de email, validación de unicidad, generación determinista de slug con resolución de colisiones y alta del dueño con `BCryptPasswordEncoder`.
  - [x] Lógica de inicio de sesión: verificación criptográfica de credenciales y estado activo de la cuenta.
  - [x] Endpoints públicos expuestos: `POST /api/auth/registro` (201 Created) y `POST /api/auth/login` (200 OK) retornando `AuthResponse`.
* **Bloque C: Configuración del Salón y Dominio del Inquilino (`SalonService` y `SalonController`) (✅ Completado)**
  - [x] DTOs de configuración: `SalonResponse`, `ConfiguracionSalonDto` y `ConfiguracionSalonRequest` (intervalo validado entre 15 y 60 min).
  - [x] Regla de validación temporal: restricción de negocio exigiendo que la hora de apertura sea estrictamente anterior a la de cierre.
  - [x] Endpoints protegidos para el inquilino: `GET /api/salon` y `PUT /api/salon/configuracion` con acceso restringido a rol `DUENO`.
* **Bloque D: Robustecimiento Transversal de Errores (✅ Completado)**
  - [x] Soporte para `NoResourceFoundException` en `ManejadorGlobalErrores` respondiendo `404 Not Found` en rutas inexistentes sin degradar a error 500.

---

### 4.1 Bloque A: Seguridad y Control de Acceso (Spring Security 6 + JJWT) — ✅ COMPLETADO

Se diseñó la arquitectura de seguridad basada en tokens sin estado (stateless), evitando el uso de sesiones de servidor HTTP y garantizando el aislamiento seguro entre inquilinos (*tenants*):

#### 1. Dependencias en `pom.xml`
Se integró Spring Security junto con la biblioteca JJWT (Java JWT) versión 0.12.6 en tres módulos:
* `jjwt-api`: Interfaz de programación y firma.
* `jjwt-impl` (runtime): Motor de implementación criptográfica.
* `jjwt-jackson` (runtime): Serializador JSON para claims y payloads.

#### 2. Servicio de Tokens JWT (`JwtService`)
* **Archivo:** `com.devsenior.sillon.seguridad.JwtService`
* **Criptografía:** Clave HMAC-SHA (`Keys.hmacShaKeyFor`) leída desde la propiedad `sillon.jwt.secreto` (inyectable vía variable de entorno `JWT_SECRETO`).
* **Claims incluidos en el token:**
  * `sub`: Email normalizado del usuario (sujeto principal).
  * `rol`: Rol del usuario en el sistema (`DUENO`, `ESTILISTA`, `ADMIN`).
  * `salonId`: Identificador del salón asignado al usuario (anclaje multi-tenant).
* **Desacoplamiento temporal:** Utiliza el bean inyectado `Clock` tanto para la emisión (`issuedAt`) como para la expiración (`expiration = ahora + 1440 min`).
* **Operaciones clave:**
  * `generarToken(UsuarioPrincipal usuario)`: Construye y firma el token compacto.
  * `extraerEmail(String token)`: Recupera el claim `subject`.
  * `esTokenValido(String token)`: Valida la firma criptográfica y que la fecha de expiración no haya sido superada.

#### 3. Adaptador de Usuario (`UsuarioPrincipal` y `UsuarioDetailsService`)
* **`UsuarioPrincipal` (`com.devsenior.sillon.seguridad.UsuarioPrincipal`):**
  * Implementa `UserDetails`.
  * Encapsula los campos de negocio requeridos en cada petición: `id`, `salonId`, `nombre`, `email`, `rol` y bandera `activo`.
  * Mapea el rol a una autoridad de Spring Security con prefijo canónico: `ROLE_DUENO`, `ROLE_ESTILISTA`, `ROLE_ADMIN`.
* **`UsuarioDetailsService` (`com.devsenior.sillon.seguridad.UsuarioDetailsService`):**
  * Implementa `UserDetailsService`.
  * Carga y autentica al usuario desde `UsuarioRepository` normalizando el email (`trim().toLowerCase()`).
  * `@Transactional(readOnly = true)` para garantizar lectura eficiente sin bloqueo.

#### 4. Filtro de Interceptación (`JwtFiltro`)
* **Archivo:** `com.devsenior.sillon.seguridad.JwtFiltro`
* **Tipo:** Extiende `OncePerRequestFilter`.
* **Flujo de procesamiento:**
  1. Extrae la cabecera HTTP `Authorization`.
  2. Verifica el prefijo `Bearer `.
  3. Si el token es válido y no hay autenticación previa en `SecurityContextHolder`, carga los detalles del usuario a través de `UsuarioDetailsService`.
  4. Si el usuario está activo (`isEnabled()`), inyecta un `UsernamePasswordAuthenticationToken` con sus autoridades en el contexto de seguridad.

#### 5. Configuración de la Cadena de Seguridad (`SecurityConfig`)
* **Archivo:** `com.devsenior.sillon.seguridad.SecurityConfig`
* **Anotaciones:** `@Configuration`, `@EnableWebSecurity`.
* **Políticas clave:**
  * **Stateless:** `SessionCreationPolicy.STATELESS` (sin sesiones `HttpSession`).
  * **CSRF:** Deshabilitado (`AbstractHttpConfigurer::disable`) al ser una API REST basada en tokens.
  * **Filtro JWT:** Registrado antes de `UsernamePasswordAuthenticationFilter`.
  * **Codificador de contraseñas:** Bean `PasswordEncoder` implementado con `BCryptPasswordEncoder`.
* **Matriz de Control de Rutas:**
  | Ruta | Métodos | Acceso Permitido |
  |---|---|---|
  | `/api/auth/**`, `/api/publico/**`, `/error` | Cualquiera | Público (`permitAll`) |
  | `/api/pagos/webhook`, `/api/pagos/resultado/**` | Cualquiera | Público (`permitAll`) |
  | `/swagger-ui/**`, `/v3/api-docs/**` | Cualquiera | Público (`permitAll`) |
  | `/api/salon/**` | Cualquiera | Rol `DUENO` |
  | `/api/admin/**` | Cualquiera | Rol `ADMIN` |
  | `/api/servicios/**` | GET | Roles `DUENO` o `ESTILISTA` |
  | `/api/servicios/**`, `/api/estilistas/**`, `/api/clientes/**` | POST, PUT, DELETE | Rol `DUENO` |
  | `/api/solicitudes/**`, `/api/pagos/**` | Cualquiera | Rol `DUENO` |
  | Cualquier otra ruta no listada | Cualquiera | Autenticado (`authenticated`) |
* **Personalización de Errores 401 y 403:**
  * Se configuraron `AuthenticationEntryPoint` y `AccessDeniedHandler` personalizados.
  * En lugar de devolver respuestas HTML genéricas del contenedor, serializan el DTO unificado `ErrorRespuesta` en formato JSON (`application/json;charset=UTF-8`) con el código HTTP correspondiente y fecha generada mediante `Clock`.

---

### 4.2 Bloque B: Servicios y Endpoints de Autenticación (`AuthService` y `AuthController`) — ✅ COMPLETADO

Se implementó el ciclo de vida de registro de inquilinos y autenticación de usuarios:

#### 1. DTO de Login (`LoginRequest`)
* **Archivo:** `com.devsenior.sillon.salon.dto.LoginRequest`
* **Campos y validaciones:**
  * `email`: `@NotBlank(message = "El correo es obligatorio")`, `@Email(message = "El formato del correo es inválido")`.
  * `password`: `@NotBlank(message = "La contraseña es obligatoria")`.

#### 2. Servicio de Autenticación (`AuthService`)
* **Archivo:** `com.devsenior.sillon.salon.servicio.AuthService`
* **Lógica del método `registrar(RegistroRequest request)`:**
  1. **Normalización:** Convierte el email a minúsculas y elimina espacios en blanco.
  2. **Validación de unicidad:** Comprueba `usuarioRepository.existsByEmail(...)`. Si ya existe, arroja `ReglaNegocioException("El correo ya está registrado")` (HTTP 409).
  3. **Generación de Slug único:** Invoca `SlugUtil.generarSlug(salonNombre)`. Si el slug ya está en uso por otro salón, aplica un sufijo secuencial incremental determinista (`-2`, `-3`, etc.) hasta encontrar uno libre.
  4. **Inicialización del Salón:** Se crea con `Plan.GRATIS`, configuración por defecto (8:00 a 19:00, lunes a sábado, intervalos de 30 min) y créditos de IA iniciales leídos de la propiedad `sillon.negocio.creditos-registro` (10 créditos).
  5. **Creación del Usuario Dueño:** Asigna rol `DUENO`, estado activo, fecha de auditoría con `Clock` y hashea la contraseña con `BCryptPasswordEncoder`.
  6. **Retorno:** Emite el token JWT y responde con `AuthResponse` (datos del usuario, token, tipo "Bearer", expiración en 1440 min e identificadores del salón).
* **Lógica del método `login(LoginRequest request)`:**
  1. Busca al usuario por su email normalizado.
  2. Verifica que el usuario exista, que su estado sea `activo = true` y que la contraseña coincida con `passwordEncoder.matches(...)`.
  3. Si falla cualquier validación, lanza `CredencialesInvalidasException("Credenciales inválidas")` (HTTP 401), previniendo ataques de enumeración de usuarios.
  4. Retorna el DTO `AuthResponse` con un token JWT recién firmado.

#### 3. Controlador REST (`AuthController`)
* **Archivo:** `com.devsenior.sillon.salon.controlador.AuthController`
* **Ruta base:** `/api/auth`
* **Endpoints:**
  * `POST /api/auth/registro`: Valida el payload con `@Valid`. Responde HTTP `201 Created` con el `AuthResponse`.
  * `POST /api/auth/login`: Valida el payload con `@Valid`. Responde HTTP `200 OK` con el `AuthResponse`.

---

### 4.3 Bloque C: Gestión y Configuración del Salón (`SalonService` y `SalonController`) — ✅ COMPLETADO

Se construyó la interfaz para que el dueño del salón consulte los datos de su negocio y ajuste sus parámetros de agenda:

#### 1. DTOs de Salón y Configuración
* **`SalonResponse` (`com.devsenior.sillon.salon.dto.SalonResponse`):** Proyecta la información del salón (`id`, `nombre`, `slug`, `ciudad`, `direccion`, `telefono`, `plan`, `creditosIa`, `configuracion`, `fechaCreacion`) ocultando detalles sensibles internos.
* **`ConfiguracionSalonDto` (`com.devsenior.sillon.salon.dto.ConfiguracionSalonDto`):** Mapeo inmutable de los 4 campos de configuración horaria (`horaApertura`, `horaCierre`, `abreDomingos`, `intervaloMinutos`).
* **`ConfiguracionSalonRequest` (`com.devsenior.sillon.salon.dto.ConfiguracionSalonRequest`):**
  * `horaApertura` y `horaCierre`: `@NotNull`.
  * `abreDomingos`: `@NotNull`.
  * `intervaloMinutos`: `@NotNull`, `@Min(value = 15, message = "El intervalo mínimo es de 15 minutos")`, `@Max(value = 60, message = "El intervalo máximo es de 60 minutos")`.

#### 2. Servicio del Salón (`SalonService`)
* **Archivo:** `com.devsenior.sillon.salon.servicio.SalonService`
* **`obtenerMiSalon(Long salonId)`:**
  * Consulta el salón por su ID.
  * Si no existe, arroja `RecursoNoEncontradoException("Salón no encontrado")` (HTTP 404).
  * Devuelve la respuesta convertida a `SalonResponse`.
* **`actualizarConfiguracion(Long salonId, ConfiguracionSalonRequest request)`:**
  * **Regla de Negocio:** Verifica estrictamente `request.horaApertura().isBefore(request.horaCierre())`. Si la condición no se cumple, arroja `ReglaNegocioException("La hora de apertura debe ser anterior a la hora de cierre")` (HTTP 409 Conflict).
  * Actualiza los valores en el objeto embebido `ConfiguracionSalon` y persiste los cambios transaccionalmente.

#### 3. Controlador del Salón (`SalonController`)
* **Archivo:** `com.devsenior.sillon.salon.controlador.SalonController`
* **Ruta base:** `/api/salon` (protegida con `hasRole('DUENO')`).
* **Inyección de seguridad:** Obtiene el usuario autenticado directamente con `@AuthenticationPrincipal UsuarioPrincipal usuario`. Ningún ID de salón viene en la URL ni en el body: se extrae estrictamente del token verificado.
* **Endpoints:**
  * `GET /api/salon`: Obtiene el salón del dueño autenticado (`200 OK`).
  * `PUT /api/salon/configuracion`: Actualiza la configuración horaria y de intervalos del salón autenticado (`200 OK`).

---

## 5. Fase 3: Catálogo de Servicios, Estilistas (ManyToMany) y Clientes

### 📋 Checklist de Entregables (Fase 3)
* **Bloque A: Catálogo de Servicios (`servicio`) (✅ Completado)**
  - [x] Entidad `Servicio` con relación obligatoria al `Salon` (`ManyToOne`).
  - [x] Repositorio `ServicioRepository` con métodos multi-tenant (`findBySalonId`, `findByIdAndSalonId`, `findAllByIdInAndSalonId`).
  - [x] DTOs `ServicioRequest` (con Bean Validation de duraciones y precios) y `ServicioResponse`.
  - [x] Componente `ServicioMapper` para transformación limpia entidad-DTO con método `aResponse`.
  - [x] Servicio de negocio `ServicioService` con operaciones de creación, consulta por inquilino y actualización.
  - [x] Controlador `ServicioController` en `/api/servicios` (`POST`, `GET`, `PUT /{id}`) protegido con roles `DUENO` y `ESTILISTA`.
* **Bloque B: Gestión de Estilistas y Asignación de Servicios (`usuario`) (✅ Completado)**
  - [x] Relación `@ManyToMany` en entidad `Usuario` mapeada a tabla intermedia `estilista_servicios`.
  - [x] Método de ayuda `ofreceServicio(Long servicioId)`.
  - [x] Consultas en `UsuarioRepository`: conteo y filtrado de estilistas por salón (`countBySalonIdAndRol`, `findBySalonIdAndRol`).
  - [x] DTOs `EstilistaRequest` (con contraseña 8-72 caracteres y servicios obligatorios) y `EstilistaResponse` (servicios ordenados alfabéticamente).
  - [x] Reglas de negocio en `EstilistaService`:
    - **Prioridad 1 (402 Payment Required):** Límite estricto de estilistas en plan GRATIS (máximo 2 activos) validado en primer lugar.
    - **Prioridad 2 (409 Conflict):** Unicidad de correo electrónico.
    - **Prioridad 3 (404 Not Found):** Verificación de que todos los servicios asignados pertenezcan estrictamente al salón del usuario autenticado.
  - [x] Controlador `EstilistaController` en `/api/estilistas` (`POST` 201 Created y `GET` 200 OK).
* **Bloque C: Gestión y Normalización de Clientes (`cliente`) (✅ Completado)**
  - [x] Entidad `Cliente` con restricción de unicidad compuesta `@UniqueConstraint(columnNames = {"salon_id", "telefono"})`.
  - [x] Repositorio `ClienteRepository` con consultas por salón y teléfono normalizado.
  - [x] DTO `ClienteResponse`.
  - [x] Algoritmo de normalización de teléfono (`replaceAll("[^0-9+]", "")`) en `ClienteService`.
  - [x] Método `buscarOCrear` transaccional para reutilización eficiente de clientes en agenda.
  - [x] Controlador `ClienteController` en `/api/clientes` (`GET` 200 OK).

---

### 5.1 Bloque A: Catálogo de Servicios (`com.devsenior.sillon.servicio`) — ✅ COMPLETADO
Se desarrolló el módulo de catálogo asegurando que los salones administren sus propios servicios con aislamiento estricto:
* **Entidad `Servicio`:** Mapeada a la tabla `servicios`. Contiene `id`, `nombre` (máx. 80), `descripcion` (máx. 300), `duracionMinutos` (entre 15 y 480 min), `precio` (> 0), bandera `activo` y relación `@ManyToOne(fetch = FetchType.LAZY)` hacia `Salon`.
* **Consultas Multi-tenant (`ServicioRepository`):**
  * `findBySalonId(Long salonId)`: Lista completa de servicios del salón.
  * `findBySalonIdAndActivoTrue(Long salonId)`: Lista de servicios disponibles para agendamiento.
  * `findByIdAndSalonId(Long id, Long salonId)`: Búsqueda segura que impide ver o mutar servicios de otros salones.
  * `findAllByIdInAndSalonId(Collection<Long> ids, Long salonId)`: Validación por lote para verificar pertenencia de múltiples servicios al inquilino.
* **Componente de Mapeo (`ServicioMapper`):** Componente `@Component` que desacopla el modelo relacional de la capa de transporte mediante `aResponse(Servicio servicio)`.
* **Servicio y Controlador (`ServicioService`, `ServicioController`):**
  * `POST /api/servicios` (201 Created): Creación de servicio vinculado al `salonId` del token.
  * `GET /api/servicios` (200 OK): Consulta del catálogo del inquilino.
  * `PUT /api/servicios/{id}` (200 OK): Modificación de nombre, descripción, duración, precio y estado, validando previamente que el servicio pertenezca al salón.

---

### 5.2 Bloque B: Equipo de Estilistas y Roles (`com.devsenior.sillon.usuario`) — ✅ COMPLETADO
Se implementó la gestión del equipo de trabajo y la asignación de habilidades por estilista:
* **Relación Muchos a Muchos (`estilista_servicios`):**
  * `Usuario` incluye `Set<Servicio> servicios` asociado mediante `@JoinTable(name = "estilista_servicios")`.
  * Método de negocio `ofreceServicio(Long servicioId)` para validaciones rápidas durante la reserva de citas.
* **Secuencia Estricta de Validación en `EstilistaService`:**
  1. **Límite de Plan (`402 Payment Required`):** Si el salón está en `Plan.GRATIS` y `countBySalonIdAndRol(salonId, Rol.ESTILISTA) >= limiteEstilistasGratis` (2), se bloquea la creación inmediatamente antes de verificar correo o servicios.
  2. **Unicidad de Email (`409 Conflict`):** Se verifica `existsByEmail(emailNormalizado)`.
  3. **Aislamiento de Servicios (`404 Not Found`):** Se valida `findAllByIdInAndSalonId(idsUnicos, salonId)`. Si la cantidad de servicios encontrados no coincide con la cantidad de IDs únicos solicitados, arroja `RecursoNoEncontradoException`.
* **Endpoints (`EstilistaController`):**
  * `POST /api/estilistas` (201 Created): Registra el nuevo estilista con contraseña hasheada (BCrypt) y rol `ESTILISTA`.
  * `GET /api/estilistas` (200 OK): Retorna la nómina del salón con sus respectivos servicios ordenados alfabéticamente.

---

### 5.3 Bloque C: Directorio de Clientes (`com.devsenior.sillon.cliente`) — ✅ COMPLETADO
Se diseñó el módulo de clientes con normalización automática y garantía de unicidad por salón:
* **Aislamiento Multi-tenant y Unicidad Compuesta:**
  * Tabla `clientes` protegida con `@UniqueConstraint(columnNames = {"salon_id", "telefono"})`. Un mismo número telefónico puede pertenecer a salones distintos sin entrar en conflicto.
* **Normalización Telefónica Determinista:**
  * `normalizarTelefono(String telefono)`: Filtra cualquier espacio, guion o carácter no numérico excepto `+` (`replaceAll("[^0-9+]", "")`).
* **Operación Atómica `buscarOCrear`:**
  * Si el cliente ya existe en el salón por su teléfono normalizado, lo retorna; de lo contrario, lo crea y persiste de forma transparente.
* **Controlador (`ClienteController`):**
  * `GET /api/clientes` (200 OK): Retorna los clientes registrados del salón autenticado.

---

## 6. Fase 4: Agenda Inteligente, Disponibilidad y Reservas en Línea

### 📋 Checklist de Entregables (Fase 4)
* **Bloque A: Modelo de Dominio de Agenda (`cita.modelo` y `cita.repositorio`) (✅ Completado)**
  - [x] Entidad `Cita` con relaciones hacia `Salon`, `Servicio`, `Usuario` (estilista) y `Cliente`.
  - [x] Enums `EstadoCita` (`PENDIENTE`, `CONFIRMADA`, `CANCELADA`) y `OrigenCita` (`MANUAL`, `ONLINE`, `CHATBOT`).
  - [x] Repositorio `CitaRepository` con consultas de solapamiento y filtrado de citas ocupadas por estilista y rango temporal.
* **Bloque B: Motor de Reglas Temporales y Disponibilidad (`ReglasAgenda`) (✅ Completado)**
  - [x] Algoritmo determinista para calcular horarios libres (`calcularHorariosLibres`) considerando intervalos de apertura/cierre.
  - [x] Validación de política dominical (`abreDomingos`) y descarte automático de turnos pasados anclado al `Clock`.
  - [x] Exclusión de slots con colisión temporal frente a citas existentes en estados `PENDIENTE` o `CONFIRMADA`.
* **Bloque C: Gestión y Ciclo de Vida de Citas (`CitaService` y `CitaController`) (✅ Completado)**
  - [x] Creación administrativa de citas (`POST /api/citas`) con validación de rol `DUENO`.
  - [x] Creación de reservas públicas en línea (`POST /api/publico/salones/{slug}/reservas`).
  - [x] Detección de colisiones de horario arrojando `ReglaNegocioException` (409 Conflict).
  - [x] Validación de que el estilista ofrezca el servicio y esté activo.
  - [x] Transición de estados (`PATCH /api/citas/{id}/estado`) para `DUENO` y `ESTILISTA`.
* **Bloque D: Catálogo Público y Autoservicio (`CatalogoService` y `PublicoController`) (✅ Completado)**
  - [x] DTOs `CatalogoPublicoResponse`, `DisponibilidadResponse` y `EstilistaPublicoDto`.
  - [x] Endpoint de catálogo público: `GET /api/publico/salones/{slug}` sin autenticación.
  - [x] Endpoint de consulta de disponibilidad: `GET /api/publico/salones/{slug}/disponibilidad`.
* **Bloque E: Seguridad y Autorización Granular (`SecurityConfig`) (✅ Completado)**
  - [x] Restricción HTTP por método en `/api/citas` para separar permisos entre dueño y estilista.

---

### 6.1 Bloque A: Modelo de Citas (`com.devsenior.sillon.cita`) — ✅ COMPLETADO
Se modeló el núcleo de reservas multi-tenant:
* **Entidad `Cita`:** Mapeada a la tabla `citas`. Almacena `id`, `inicio` (`LocalDateTime`), `fin` (`LocalDateTime`), `estado` (`EstadoCita`), `origen` (`OrigenCita`), `notas` opcionales, `fechaCreacion` y las referencias obligatorias `@ManyToOne` hacia `Salon`, `Servicio`, `Usuario` (como estilista) y `Cliente`.
* **Estados y Ocupación:**
  * Estados ocupados: `PENDIENTE` y `CONFIRMADA`. Bloquean la agenda del estilista impidiendo traslapes.
  * Estado liberador: `CANCELADA`. Permite que el horario quede disponible de inmediato para nuevas reservas.
* **Consultas de Agenda (`CitaRepository`):**
  * `findByEstilistaIdAndEstadoInAndInicioBetweenOrderByInicioAsc(...)`: Recupera la agenda ocupada del estilista para un día específico ordenada cronológicamente.
  * `existsByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(...)`: Detección directa de cruce de horarios para validar reservas en tiempo constante.

---

### 6.2 Bloque B: Algoritmo de Disponibilidad (`ReglasAgenda`) — ✅ COMPLETADO
Se implementó el componente de cálculo temporal desacoplado del sistema operativo:
* **Generación de Slots:** Itera desde `horaApertura` hasta `horaCierre` avanzando por pasos definidos en `intervaloMinutos`.
* **Filtros de Validación:**
  1. **Día Laborable:** Si la fecha solicitada es domingo y el salón no abre domingos (`!config.isAbreDomingos()`), retorna lista vacía.
  2. **Límite de Cierre:** El fin estimado de la cita (`slot.plusMinutes(duracionMinutos)`) no puede superar la `horaCierre`.
  3. **Turnos Pasados:** Compara la fecha y hora del slot contra `LocalDateTime.now(clock)` para descartar horarios vencidos en consultas del mismo día.
  4. **Solapamiento:** Descarta cualquier slot donde el rango `[slot, slot + duracion]` colisione con el intervalo `[inicio, fin]` de una cita ocupada del estilista.

---

### 6.3 Bloque C: Orquestación de Citas y Reservas (`CitaService`, `CitaController`) — ✅ COMPLETADO
* **Flujo de Creación de Cita (`POST /api/citas` y `/api/publico/salones/{slug}/reservas`):**
  1. Verifica que el servicio pertenezca al salón y esté activo (`404` / `409`).
  2. Verifica que el estilista pertenezca al salón, esté activo y ofrezca dicho servicio (`404` / `409`).
  3. Valida que el horario esté dentro de la jornada comercial y día hábil del salón (`409 Conflict`).
  4. Valida ausencia de traslape con otra cita ocupada del mismo estilista (`ReglaNegocioException` 409).
  5. Localiza o crea al cliente automáticamente mediante `ClienteService.buscarOCrear`.
  6. Persiste la cita con origen correspondiente (`MANUAL` u `ONLINE`) y estado inicial `CONFIRMADA` o `PENDIENTE`.
* **Transición de Estados (`PATCH /api/citas/{id}/estado`):**
  * Permite modificar el estado de la cita (`CONFIRMADA`, `CANCELADA`) garantizando que pertenezca al salón del usuario autenticado.

---

### 6.4 Bloque D: Catálogo Público (`CatalogoService`, `PublicoController`) — ✅ COMPLETADO
* **Endpoint de Catálogo (`GET /api/publico/salones/{slug}`):** Retorna la información general del salón, horario formateado legiblemente (ej. "Lunes a sábado (domingos cerrado), de 08:00 a 19:00"), listado de servicios activos y estilistas activos con sus servicios ofrecidos.
* **Endpoint de Disponibilidad (`GET /api/publico/salones/{slug}/disponibilidad`):** Expone los horarios libres exactos calculados para un servicio y estilista en una fecha específica.

---

## 7. Fase 5: Pagos e Integración con Stripe (Suscripciones, Créditos y Webhooks)

### 📋 Checklist de Entregables (Fase 5)
* **Bloque A: Configuración y Cliente de Stripe (`pago.configuracion`) (✅ Completado)**
  - [x] Configuración centralizada `StripeConfig` instanciando `StripeClient` con soporte para URLs simuladas de prueba (`sillon.stripe.url-api`).
* **Bloque B: Creación de Sesiones de Checkout (`pago.servicio`) (✅ Completado)**
  - [x] Enum `TipoCompra` (`SUSCRIPCION_PRO`, `PAQUETE_CREDITOS`).
  - [x] DTO `SesionPagoResponse` (`tipo`, `sessionId`, `url`).
  - [x] Método `crearSesionPago`: Validación de plan previo (409 si ya es PRO), generación de URLs de retorno (`/api/pagos/resultado`), inyección de metadatos multi-tenant (`salonId`, `tipoCompra`) y captura de `StripeException` traduciendo a `ErrorPasarelaException` (502).
* **Bloque C: Procesamiento de Webhooks e Idempotencia (`pago.modelo`, `pago.repositorio`) (✅ Completado)**
  - [x] Entidad `EventoStripe` para registro de auditoría e idempotencia con clave primaria `id` (identificador del evento Stripe).
  - [x] Repositorio `EventoStripeRepository`.
  - [x] DTO `WebhookResponse` (`recibido`, `procesado`).
  - [x] Verificación estricta de firma HMAC (`Webhook.constructEvent`) arrojando `FirmaInvalidaException` (400) si falta o es inválida la cabecera `Stripe-Signature`.
  - [x] Manejo de idempotencia: si el evento ya fue registrado, retorna `recibido: true, procesado: false` sin reprocesar.
  - [x] Evento `checkout.session.completed`: Upgrade a `Plan.PRO` (+300 créditos IA) o recarga de paquete (+200 créditos IA).
  - [x] Evento `customer.subscription.deleted`: Degradación automática a `Plan.GRATIS` y desvinculación de suscripción.
* **Bloque D: Endpoints y Administración (`pago.controlador`, `salon.controlador`) (✅ Completado)**
  - [x] Endpoints en `PagoController`: `POST /api/pagos/suscripcion`, `POST /api/pagos/creditos`, `POST /api/pagos/webhook` y `GET /api/pagos/resultado`.
  - [x] Endpoint en `AdminController`: `GET /api/admin/salones` protegido para rol `ADMIN`.

---

### 7.1 Bloque A: Integración y Cliente de Stripe (`com.devsenior.sillon.pago.configuracion`) — ✅ COMPLETADO
* **Bean `StripeClient`:** Configurado a partir de `sillon.stripe.clave-secreta` y parametrizado con `sillon.stripe.url-api`. Esto permite alternar entre la API real de Stripe (`https://api.stripe.com`) y el simulador local (`stripe-mock`) del calificador automático.

---

### 7.2 Bloque B: Sesiones de Pago Checkout (`com.devsenior.sillon.pago.servicio`) — ✅ COMPLETADO
* **Suscripción PRO (`POST /api/pagos/suscripcion`):**
  * Valida que el salón no tenga ya activo el plan PRO (`ReglaNegocioException` 409 Conflict).
  * Crea una sesión en modo `SessionCreateParams.Mode.SUBSCRIPTION` vinculando el precio de suscripción configurado (`sillon.stripe.precio-pro`).
  * Inyecta metadatos `salonId` y `tipoCompra` tanto en la sesión como en los datos de la suscripción para vincular el webhook entrante.
* **Paquete de Créditos IA (`POST /api/pagos/creditos`):**
  * Crea una sesión en modo `SessionCreateParams.Mode.PAYMENT` con el precio correspondiente a la recarga (`sillon.stripe.precio-creditos`).
* **Manejo de Errores de Red:** Captura cualquier `StripeException` y la mapea a `ErrorPasarelaException` (HTTP 502 Bad Gateway).

---

### 7.3 Bloque C: Procesamiento de Webhooks e Idempotencia — ✅ COMPLETADO
* **Verificación Criptográfica (`Stripe-Signature`):**
  * Valida el payload crudo y la firma contra `sillon.stripe.secreto-webhook`.
  * Si la cabecera no está presente o la firma es inválida, arroja `FirmaInvalidaException` (HTTP 400 Bad Request).
* **Garantía de Idempotencia:**
  * Si `eventoStripeRepository.existsById(evento.getId())` es verdadero, no se aplican mutaciones y se retorna inmediatamente `WebhookResponse(true, false)`.
* **Procesamiento de Eventos:**
  * `checkout.session.completed`: Verifica que el estado de pago sea `"paid"`. Extrae el `salonId` de los metadatos o del `clientReferenceId`. Si el tipo es `SUSCRIPCION_PRO`, actualiza el salón a `Plan.PRO`, le asigna los 300 créditos correspondientes y almacena el `stripeSubscriptionId`. Si es `PAQUETE_CREDITOS`, incrementa el saldo de créditos IA en 200.
  * `customer.subscription.deleted`: Busca el salón por `stripeSubscriptionId`, revierte el plan a `Plan.GRATIS` y limpia el identificador de suscripción.
  * Al finalizar, persiste la auditoría en `EventoStripe` (id, tipo de evento, fecha de recepción vía `Clock`).

---

### 7.4 Bloque D: Endpoints y Auditoría de Administración — ✅ COMPLETADO
* **Controlador de Pagos (`PagoController`):**
  * Expone los endpoints protegidos para el dueño (`/api/pagos/suscripcion`, `/api/pagos/creditos`) y el endpoint público para webhooks de pasarela (`/api/pagos/webhook`).
  * Expone `GET /api/pagos/resultado` retornando confirmación o cancelación del flujo de checkout con `ResponseEntity<Map<String, String>>`.
* **Controlador de Administración (`AdminController`):**
  * Expone `GET /api/admin/salones` protegido exclusivamente para el rol `ADMIN`, retornando la totalidad de salones registrados y su estado actual de suscripción.

---

## 8. Bitácora de Incidencias y Soluciones Técnicas

| # | Problema / Incidencia | Causa Raíz | Solución Aplicada |
|---|---|---|---|
| 1 | `secrets.properties` aparecía como *Untracked* en Git | Faltaba la regla limpia en `.gitignore` | Se añadió `secrets.properties` al `.gitignore` y se verificó con `git status`. |
| 2 | Intento de reutilizar URL de base de datos anterior | Confusión con el proyecto previo `vetturno` | Se corrigió la base de datos a `sillon_db`, asegurando independencia total de esquemas. |
| 3 | Error al clonar URL en GitHub Desktop | La pestaña activa era *GitHub.com* (búsqueda de repositorios) | Se cambió a la pestaña *URL* para clonar directamente vía HTTPS. |
| 4 | No aparecía el botón "Open pull request" | Sesión iniciada en el navegador con cuenta de GitHub distinta | Se inició sesión con `jhonnlealDev` y se generó el PR oficial vía *Contribute*. |
| 5 | Error `Failed to clean project` con `mvnw clean` | Bloqueo de archivos `.class` en `target/` por OneDrive / IDE | Se pausó la sincronización y se compiló directamente con `.\mvnw.cmd compile`. |
| 6 | Error de ejecución en comando Maven con guiones | Sintaxis errónea `compile-test-compile` | Se corrigió el comando a `.\mvnw.cmd test-compile`, obteniendo `BUILD SUCCESS`. |
| 7 | Clases ubicadas en subcarpeta `sillon/` en raíz | Creación de archivos fuera de la ruta canónica del calificador | Se reubicaron en `src/main/java/com/devsenior/sillon/` respetando sus paquetes y se eliminó la carpeta residual. |
| 8 | Respuestas por defecto de Spring Security rompían el contrato JSON | Spring Security devuelve respuestas vacías o HTML en errores 401 y 403 | Se implementaron `AuthenticationEntryPoint` y `AccessDeniedHandler` personalizados en `SecurityConfig` inyectando `ObjectMapper` y `Clock` para responder estrictamente con el DTO `ErrorRespuesta`. |
| 9 | Rutas no mapeadas arrojaban 500 en lugar de 404 | En Spring Boot 3 / Spring MVC 6, los recursos inexistentes lanzan `NoResourceFoundException`, que caía en el manejador genérico de errores 500 | Se agregó `@ExceptionHandler(NoResourceFoundException.class)` en `ManejadorGlobalErrores` devolviendo `404 Not Found` con mensaje "La ruta solicitada no existe". |
| 10 | Bloqueo de concurrencia en `.m2/repository/.locks` de Maven en entorno sandbox | Maven detecta bloqueos de archivo en el repositorio global fuera del workspace | Se verificó la compilación limpia del proyecto ejecutando el wrapper Maven con éxito total (`BUILD SUCCESS`). |
| 11 | Control de acceso fino en operaciones de citas según rol | La creación manual de citas y recordatorios debe ser exclusiva del dueño (`DUENO`), mientras que la consulta y cambio de estado debe permitirse tanto a `DUENO` como a `ESTILISTA` | Se configuraron directivas explícitas `HttpMethod.POST /api/citas` con `hasRole('DUENO')`, `HttpMethod.GET` y `HttpMethod.PATCH` con `hasAnyRole('DUENO', 'ESTILISTA')` en `SecurityConfig`. |
| 12 | Error de compilación en `PagoController.java:[42,26]` | En el método `resultado`, faltaba la declaración del tipo genérico `<Map<String, String>>` en `ResponseEntity` | Se corrigió la firma a `public ResponseEntity<Map<String, String>> resultado(...)`, logrando compilación limpia (`BUILD SUCCESS`). |

---

## 9. Próximos Pasos

- [x] Desarrollar servicio de negocio (`SalonService` / `AuthService`) para orquestar la transacción atómica de registro y generación de slug único.
- [x] Implementar capa de seguridad Spring Security 6, hashing de contraseñas con `BCryptPasswordEncoder` y generación/validación de tokens JWT (Fase 2).
- [x] Exponer controladores REST con validación `@Valid` para endpoints públicos (`/api/auth/registro`, `/api/auth/login`) y privados (`/api/salon`).
- [x] Implementar el Catálogo y Equipo (Fase 3: Entidades `Servicio`, `Estilista`, `Cliente`, reglas de asignación y límite de estilistas para plan GRATIS).
- [x] Implementar la Fase 4: Módulo de Agenda (Citas, Disponibilidad, cálculo de horarios libres con `Clock`, catálogo público y reservas en línea).
- [x] Implementar la Fase 5: Pasarela de Pagos Stripe (Suscripciones PRO, compra de paquetes de créditos y Webhooks firmados).
- [x] Completar las 6 entradas de la bitácora de IA en `BITACORA-IA.md` (superando el umbral de 5 requerido para el Criterio 08).
- [x] Ejecutar compilación limpia del proyecto con el wrapper Maven (`BUILD SUCCESS` con 74 fuentes).
- [ ] Implementar el Módulo de IA (Recordatorios deterministas y Chatbot con Gemini / OpenAI spec y control de créditos IA).
- [ ] Implementar suite de pruebas unitarias JUnit 5 y Mockito para cumplir con el mínimo de tests requeridos (Criterio 07).
- [ ] Completar los documentos técnicos `SPEC.md` y `AGENTS.md` requeridos por el calificador automático (Criterio 08).
- [ ] Diseñar la colección oficial de pruebas en Postman (Criterio 08).





