# Sillón 💈
## Agenda inteligente para barberías y salones de belleza

**Proyecto integrador · Java AI Engineer · Dev Senior Code**

Sillón es una plataforma SaaS desarrollada con Java y Spring Boot que permite administrar barberías y salones de belleza desde una API REST centralizada.

El sistema facilita la gestión de salones, usuarios, estilistas, servicios, clientes, citas, solicitudes de contacto y pagos. Incorpora inteligencia artificial para generar recordatorios y atender consultas mediante un chatbot, con capacidad de escalar solicitudes a una persona cuando sea necesario.

Su arquitectura multi-tenant permite que múltiples salones compartan una misma plataforma y una misma base de datos, manteniendo aislada la información de cada negocio mediante controles de seguridad implementados en el backend.

Sillón integra autenticación JWT, autorización por roles, gestión de disponibilidad, prevención de reservas duplicadas, control de créditos de IA y procesamiento de pagos mediante Stripe.

> **Fuente de verdad:** `SPEC.md` contiene los requisitos funcionales, el modelo de dominio, las reglas de negocio y el contrato oficial de la API. Si existe alguna diferencia entre este README y `SPEC.md`, prevalece siempre `SPEC.md`.

---

# Tabla de contenido

1. [Descripción del proyecto](#1-descripción-del-proyecto)
2. [Objetivos](#2-objetivos-del-proyecto)
3. [Funcionalidades principales](#3-funcionalidades-principales)
4. [Arquitectura del sistema](#4-arquitectura-del-sistema)
5. [Modelo de dominio](#5-modelo-de-dominio)
6. [Tecnologías utilizadas](#6-tecnologías-utilizadas)
7. [Usuarios, roles y permisos](#7-usuarios-roles-y-permisos)
8. [Planes y créditos de IA](#8-planes-y-créditos-de-ia)
9. [Reglas de negocio principales](#9-reglas-de-negocio-principales)
10. [API REST](#10-api-rest)
11. [Formato de respuestas y errores HTTP](#11-formato-de-respuestas-y-errores-http)
12. [Instalación y ejecución local](#12-instalación-y-ejecución-local)
13. [Configuración y variables de entorno](#13-configuración-y-variables-de-entorno)
14. [Inteligencia artificial](#14-inteligencia-artificial)
15. [Pagos y suscripciones con Stripe](#15-pagos-y-suscripciones-con-stripe)
16. [Pruebas automatizadas](#16-pruebas-automatizadas)
17. [Documentación Swagger / OpenAPI](#17-documentación-swagger--openapi)
18. [Contenerización con Docker](#18-contenerización-con-docker)
19. [Base de datos de producción: Supabase](#19-base-de-datos-de-producción-supabase)
20. [Despliegue en Render](#20-despliegue-en-render)
21. [Estructura del repositorio](#21-estructura-del-repositorio)
22. [Fases de desarrollo](#22-fases-de-desarrollo)
23. [Seguridad del proyecto](#23-seguridad-del-proyecto)
24. [Verificación y evaluación académica](#24-verificación-y-evaluación-académica)
25. [Consideraciones técnicas y evolución](#25-consideraciones-técnicas-y-evolución)
26. [Autor y propósito académico](#26-autor-y-propósito-académico)
27. [Licencia](#27-licencia)

---

# 1. Descripción del proyecto

Muchas barberías y salones de belleza administran sus reservas mediante WhatsApp, llamadas telefónicas, cuadernos o registros manuales.

Estos métodos pueden provocar problemas como:

- Reservas duplicadas para un mismo estilista.
- Desorganización de los horarios de atención.
- Pérdida de clientes por falta de respuesta.
- Ausencias de clientes y espacios de agenda desaprovechados.
- Dificultad para consultar servicios, precios y disponibilidad.
- Falta de control sobre el personal y los servicios ofrecidos.
- Ausencia de herramientas para atender consultas fuera del horario laboral.

Sillón propone una solución digital que centraliza estos procesos en una plataforma SaaS.

Cada salón puede registrarse, configurar su información, administrar sus servicios y estilistas, organizar su agenda y permitir que sus clientes consulten la disponibilidad y realicen reservas en línea.

Además, incorpora herramientas de inteligencia artificial para generar borradores de recordatorios y responder consultas mediante un chatbot conectado al catálogo real del negocio.

La plataforma también permite contratar un plan PRO y adquirir paquetes adicionales de créditos de inteligencia artificial mediante Stripe.

## Alcance funcional

Sillón contempla:

- Administración multi-tenant de salones.
- Registro y autenticación de usuarios.
- Administración de estilistas y servicios.
- Gestión de clientes.
- Gestión de citas y disponibilidad.
- Reservas públicas en línea.
- Recordatorios asistidos por IA.
- Chatbot con escalamiento a atención humana.
- Gestión de solicitudes de contacto.
- Suscripciones y paquetes de créditos.
- Administración global de salones.
- Documentación de API mediante Swagger.
- Pruebas automatizadas y despliegue con Docker.

## Funcionalidades fuera del alcance

Las siguientes funcionalidades no forman parte del alcance actual:

- Envío automático de mensajes por WhatsApp o correo electrónico.
- Programación automática de recordatorios.
- Aplicación gráfica web o móvil.
- Procesamiento de pagos de los clientes finales a los salones mediante Stripe Connect.
- Sistema completo de indicadores y reportes operativos.
- Cobros reales de producción: la integración académica con Stripe se mantiene en modo de prueba.

---

# 2. Objetivos del proyecto

## Objetivo general

Desarrollar una plataforma backend SaaS multi-tenant que digitalice la administración de barberías y salones de belleza mediante herramientas de gestión, automatización e inteligencia artificial.

## Objetivos específicos

1. Centralizar la administración de múltiples salones en una misma plataforma.
2. Implementar aislamiento de datos entre negocios.
3. Gestionar usuarios, roles, estilistas y servicios.
4. Registrar clientes y administrar sus reservas.
5. Controlar la disponibilidad y evitar conflictos en las agendas.
6. Incorporar inteligencia artificial para mejorar la atención al cliente.
7. Implementar un sistema de créditos para controlar el consumo de IA.
8. Integrar Stripe para gestionar suscripciones y paquetes de créditos.
9. Proteger los recursos mediante JWT y Spring Security.
10. Documentar y probar los endpoints de la API REST.
11. Preparar el sistema para su despliegue en infraestructura cloud.

---

# 3. Funcionalidades principales

| Módulo | Funcionalidades |
|---|---|
| Salones | Registro, configuración y administración de negocios. |
| Autenticación | Registro, inicio de sesión y generación de JWT. |
| Usuarios | Administración de dueños, estilistas y administradores. |
| Servicios | Catálogo, precios, duración y estado de servicios. |
| Estilistas | Asociación de personal con los servicios que ofrece. |
| Clientes | Registro y consulta de clientes por salón. |
| Agenda | Creación, consulta y gestión del estado de citas. |
| Disponibilidad | Cálculo de horarios libres y prevención de cruces. |
| Reservas públicas | Consulta del catálogo y reservas sin autenticación. |
| Recordatorios IA | Generación de borradores de mensajes para WhatsApp. |
| Chatbot IA | Atención de consultas con información real del salón. |
| Solicitudes | Registro y seguimiento de escalamientos a personas. |
| Créditos | Control del consumo de inteligencia artificial. |
| Stripe | Suscripciones PRO y compra de paquetes de créditos. |
| Administración | Consulta global de salones mediante el rol ADMIN. |
| Documentación | Swagger y OpenAPI. |

---

# 4. Arquitectura del sistema

Sillón utiliza una arquitectura backend organizada por dominios de negocio y separación de responsabilidades.

El diseño busca mantener independientes las reglas de negocio, la persistencia, la exposición de endpoints y las integraciones externas.

## 4.1. Arquitectura por capas

| Capa | Responsabilidad |
|---|---|
| Entidad | Representación de los datos persistidos y sus relaciones. |
| Repositorio | Acceso y operaciones sobre la base de datos. |
| Servicio | Implementación de las reglas de negocio. |
| Controlador | Exposición de los endpoints REST. |
| DTO | Modelos de entrada y salida de la API. |
| Configuración | Configuración técnica, seguridad e integraciones. |

Las entidades JPA no se exponen directamente en las respuestas HTTP. Se utilizan DTO para controlar los datos recibidos y enviados.

## 4.2. Organización por dominios

Paquete base del proyecto:

`com.devSenior.sillon`

Estructura lógica:

```text
com.devSenior.sillon
│
├── compartido/
│   ├── configuracion/
│   ├── excepcion/
│   └── respuesta/
│
├── seguridad/
│   ├── jwt/
│   ├── filtro/
│   └── configuracion/
│
├── auth/
│   ├── controlador/
│   ├── servicio/
│   └── dto/
│
├── salon/
│   ├── entidad/
│   ├── repositorio/
│   ├── servicio/
│   ├── controlador/
│   └── dto/
│
├── usuario/
├── servicio/
├── cliente/
├── cita/
├── publico/
├── pago/
├── chat/
└── ia/
```

La estructura anterior representa la organización lógica. La estructura definitiva debe coincidir con los paquetes realmente existentes en el repositorio.

## 4.3. Arquitectura multi-tenant

Sillón implementa un modelo multi-tenant compartido.

Esto significa que varios salones utilizan:

- La misma aplicación backend.
- La misma infraestructura.
- La misma base de datos.
- Las mismas tablas principales.

Sin embargo, cada negocio mantiene sus propios recursos.

El aislamiento se realiza mediante el identificador del salón (`salonId`).

Reglas fundamentales:

- El `salonId` de una operación privada se obtiene del usuario autenticado.
- Nunca se confía en un identificador de salón enviado por el cliente para autorizar operaciones.
- Las consultas privadas filtran por identificador del recurso e identificador del salón.
- Los usuarios no pueden acceder a recursos de otros negocios.
- Un recurso perteneciente a otro salón responde 404.

### Flujo de una petición privada

```text
Cliente (Postman)
       |
       v
Filtro de autenticación JWT
       |
       v
Spring Security
       |
       v
Controlador REST
       |
       v
Servicio de negocio
       |
       v
Repositorio JPA
       |
       v
Base de datos
```

El aislamiento multi-tenant se implementa en el backend, no en el cliente.

---

# 5. Modelo de dominio

El modelo principal está compuesto por las siguientes entidades:

| Entidad | Tabla | Responsabilidad |
|---|---|---|
| `Salon` | `salones` | Representa un salón registrado. |
| `Usuario` | `usuarios` | Usuarios, dueños, estilistas y administradores. |
| `Servicio` | `servicios` | Servicios ofrecidos por un salón. |
| `Cliente` | `clientes` | Clientes finales de cada negocio. |
| `Cita` | `citas` | Reservas y agenda de atención. |
| `SolicitudContacto` | `solicitudes_contacto` | Solicitudes de atención humana. |
| `EventoStripe` | `eventos_stripe` | Control de eventos procesados por Stripe. |

## 5.1. Relaciones principales

- Un salón tiene múltiples usuarios.
- Un salón administra múltiples servicios.
- Un salón registra múltiples clientes.
- Un salón administra múltiples citas.
- Un salón registra múltiples solicitudes de contacto.
- Un usuario puede ofrecer varios servicios.
- Un servicio puede estar asociado con varios estilistas.
- Una cita pertenece a un cliente.
- Una cita pertenece a un estilista.
- Una cita utiliza un servicio.

La relación entre estilistas y servicios se implementa mediante la tabla `estilista_servicios`.

`ConfiguracionSalon` es un objeto embebido (`@Embeddable`) dentro de `Salon`, no una entidad independiente.

## 5.2. Enumeraciones principales

Las enumeraciones se almacenan como texto mediante `EnumType.STRING`.

| Enumeración | Valores |
|---|---|
| `Plan` | `GRATIS`, `PRO` |
| `Rol` | `DUENO`, `ESTILISTA`, `ADMIN` |
| `EstadoCita` | `PENDIENTE`, `CONFIRMADA`, `COMPLETADA`, `CANCELADA`, `NO_ASISTIO` |
| `OrigenCita` | `SALON`, `EN_LINEA` |
| `TipoCompra` | `SUSCRIPCION_PRO`, `PAQUETE_CREDITOS` |
| `MotivoSolicitud` | `PEDIDO_POR_IA`, `REGLA_DE_SEGURIDAD` |
| `EstadoSolicitud` | `PENDIENTE`, `ATENDIDA` |

## 5.3. Restricciones de integridad

- El slug de un salón es único.
- El correo de usuario es único en toda la plataforma.
- El teléfono del cliente es único dentro de cada salón.
- Las contraseñas se almacenan utilizando BCrypt.
- Los enums se almacenan sin tildes.
- No se agregan entidades ni atributos fuera de lo establecido en `SPEC.md`.

---

# 6. Tecnologías utilizadas

Las versiones exactas deben verificarse en `pom.xml`.

| Tecnología | Utilización |
|---|---|
| Java 17 | Lenguaje de programación. |
| Spring Boot 4.1.x | Framework principal del backend. |
| Maven | Gestión de dependencias y construcción. |
| Maven Wrapper | Ejecución reproducible de Maven. |
| Spring Web | Desarrollo de API REST. |
| Spring Data JPA | Persistencia de datos. |
| Hibernate | ORM. |
| Spring Security | Autenticación y autorización. |
| JWT (JJWT) | Autenticación mediante tokens. |
| BCrypt | Hash de contraseñas. |
| Jakarta Validation | Validación de datos. |
| MySQL | Base de datos del entorno local. |
| PostgreSQL | Base de datos de producción. |
| Supabase | Servicio administrado de PostgreSQL. |
| Spring RestClient | Comunicación HTTP con proveedores externos. |
| Gemini | Proveedor principal de inteligencia artificial. |
| Claude | Proveedor de respaldo de inteligencia artificial. |
| Stripe | Pagos, suscripciones y paquetes de créditos. |
| Springdoc OpenAPI | Documentación de la API. |
| JUnit 5 | Pruebas automatizadas. |
| Mockito | Simulación de dependencias. |
| Postman | Pruebas funcionales de endpoints. |
| Docker | Contenerización. |
| Render | Despliegue de la aplicación. |
| GitHub Actions | Calificación y automatización del proyecto. |

### Decisiones técnicas

- No se utiliza Lombok.
- No se utiliza Spring AI.
- La integración con IA se realiza mediante HTTP utilizando `RestClient`.
- Los getters y setters se implementan explícitamente.
- Los enums se almacenan mediante `EnumType.STRING`.
- Jackson 3 utiliza `tools.jackson.databind`; las anotaciones mantienen `com.fasterxml.jackson.annotation`.

No se agregan dependencias sin justificación técnica y sin respetar las instrucciones de `AGENTS.md`.

---

# 7. Usuarios, roles y permisos

Sillón implementa autorización basada en roles.

## 7.1. Roles

| Rol | Responsabilidad |
|---|---|
| `DUENO` | Propietario y administrador de su salón. |
| `ESTILISTA` | Profesional encargado de atender citas. |
| `ADMIN` | Administrador global de la plataforma. |

## 7.2. Permisos generales

| Operación | Público | DUENO | ESTILISTA | ADMIN |
|---|---|---|---|---|
| Registro e inicio de sesión | Sí | Sí | Sí | Sí |
| Consultar catálogo público | Sí | Sí | Sí | Sí |
| Consultar disponibilidad pública | Sí | Sí | Sí | Sí |
| Realizar reservas públicas | Sí | Sí | Sí | Sí |
| Consultar su salón | No | Sí | No | No |
| Configurar salón | No | Sí | No | No |
| Crear y editar servicios | No | Sí | No | No |
| Consultar servicios | No | Sí | Sí | No |
| Crear estilistas | No | Sí | No | No |
| Consultar clientes | No | Sí | No | No |
| Crear citas internas | No | Sí | No | No |
| Consultar agenda | No | Todas las del salón | Solo las propias | No |
| Cambiar estado de citas | No | Sí | Solo transiciones permitidas | No |
| Generar recordatorios | No | Sí | No | No |
| Consultar solicitudes | No | Sí | No | No |
| Comprar PRO y créditos | No | Sí | No | No |
| Consultar todos los salones | No | No | No | Sí |

El primer administrador se crea mediante una cuenta registrada normalmente y una asignación controlada del rol `ADMIN` directamente en la base de datos.

---

# 8. Planes y créditos de IA

Sillón utiliza un sistema de planes y créditos para controlar las funcionalidades de inteligencia artificial.

| Producto | Precio de referencia | Beneficios |
|---|---:|---|
| GRATIS | Sin costo | Máximo 2 estilistas y 10 créditos de IA. |
| PRO | 15 USD mensuales | Estilistas sin límite y 300 créditos adicionales al activarse. |
| Paquete de créditos | 5 USD | 200 créditos adicionales. |

## Reglas de consumo

- Cada respuesta válida de IA consume un crédito.
- Los créditos pertenecen al salón.
- El sistema verifica los créditos antes de llamar al proveedor.
- Si no hay créditos, responde 402.
- Una respuesta inválida de IA no consume créditos.
- Si ningún proveedor está disponible, responde 503 sin descontar créditos.
- El consumo se realiza mediante una actualización atómica para evitar saldos negativos.

El objetivo es controlar los costos de los proveedores externos y evitar el uso ilimitado de inteligencia artificial.

---

# 9. Reglas de negocio principales

Las reglas completas están documentadas en `SPEC.md`.

## 9.1. Registro y salones

Al registrarse:

- Se crea el salón.
- Se crea el usuario dueño.
- Se asigna el plan `GRATIS`.
- Se otorgan 10 créditos de IA.
- Se configura el horario predeterminado de 08:00 a 19:00.
- El salón no abre los domingos por defecto.
- El intervalo de agenda predeterminado es de 30 minutos.

El registro se realiza dentro de una misma transacción.

El correo se normaliza a minúsculas y sin espacios.

El slug se genera en minúsculas, sin tildes ni espacios. Si ya existe, se agrega un sufijo numérico.

## 9.2. Servicios y estilistas

- Duración permitida: entre 15 y 480 minutos.
- El precio debe ser un entero mayor que cero.
- Un estilista debe estar asociado a servicios de su propio salón.
- Los servicios inactivos no pueden utilizarse en nuevas reservas.
- El plan GRATIS permite un máximo de dos estilistas.
- El tercer estilista en plan GRATIS responde 402.

## 9.3. Clientes

- Los clientes pertenecen a un salón.
- El teléfono es único dentro de cada salón.
- El teléfono se normaliza conservando únicamente dígitos y el signo `+`.
- Los clientes pueden crearse al realizar una reserva o mediante el endpoint correspondiente.
- El teléfono repetido en el mismo salón responde 409.

## 9.4. Citas

El sistema valida:

- Que la fecha no esté en el pasado.
- Que no supere los 90 días de anticipación.
- Que el horario esté dentro de la jornada del salón.
- Que el salón esté abierto ese día.
- Que el inicio corresponda al intervalo configurado.
- Que el estilista ofrezca el servicio seleccionado.
- Que no exista otra cita que ocupe el mismo horario.

La duración de la cita se calcula a partir del servicio.

El precio se conserva según el valor del servicio al momento de agendar.

### Prevención de solapamientos

Dos citas se consideran cruzadas cuando:

```text
inicioA < finB && finA > inicioB
```

Una cita que termina a las 10:45 no se cruza con otra que comienza exactamente a las 10:45.

Solo las citas `PENDIENTE` y `CONFIRMADA` ocupan disponibilidad.

### Estados de las citas

| Estado actual | Transiciones permitidas |
|---|---|
| PENDIENTE | CONFIRMADA, CANCELADA |
| CONFIRMADA | COMPLETADA, CANCELADA, NO_ASISTIO |
| COMPLETADA | Ninguna |
| CANCELADA | Ninguna |
| NO_ASISTIO | Ninguna |

Las transiciones no permitidas responden 409.

Los estilistas solamente pueden cambiar sus propias citas a `COMPLETADA` o `NO_ASISTIO`.

## 9.5. Zona horaria

El sistema utiliza:

`America/Bogota`

Las operaciones relacionadas con fechas y horarios deben utilizar el bean `Clock` configurado para esta zona horaria.

Esto evita diferencias entre la hora local del negocio y la hora UTC del servidor.

## 9.6. Inteligencia artificial

- Java proporciona los datos reales del salón.
- La IA no determina disponibilidad.
- La IA no modifica citas.
- El chatbot no realiza reservas.
- El chatbot no cancela ni confirma citas.
- Las solicitudes que requieren atención humana se escalan.
- Las promesas comerciales prohibidas se bloquean mediante reglas Java.

## 9.7. Stripe

- El backend determina los precios.
- El cliente no envía importes ni identificadores de precios.
- Los webhooks verifican su firma.
- Los eventos se procesan de manera idempotente.
- Una suscripción cancelada devuelve el salón al plan GRATIS, conservando sus créditos.

---

# 10. API REST

Sillón expone sus funcionalidades mediante endpoints REST.

Las rutas privadas requieren autenticación mediante:

```http
Authorization: Bearer <token>
```

## 10.1. Autenticación

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/auth/registro` | Público |
| POST | `/api/auth/login` | Público |

## 10.2. Salón

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/api/salon` | DUENO |
| PUT | `/api/salon/configuracion` | DUENO |

## 10.3. Servicios

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/servicios` | DUENO |
| GET | `/api/servicios` | DUENO, ESTILISTA |
| PUT | `/api/servicios/{id}` | DUENO |

## 10.4. Estilistas

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/estilistas` | DUENO |
| GET | `/api/estilistas` | DUENO |

## 10.5. Clientes

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/clientes` | DUENO |
| GET | `/api/clientes` | DUENO |

## 10.6. Agenda

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/citas` | DUENO |
| GET | `/api/citas?fecha=` | DUENO, ESTILISTA |
| PATCH | `/api/citas/{id}/estado` | DUENO, ESTILISTA |
| POST | `/api/citas/{id}/recordatorio` | DUENO |

## 10.7. Funcionalidades públicas

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/api/publico/salones/{slug}` | Público |
| GET | `/api/publico/salones/{slug}/disponibilidad` | Público |
| POST | `/api/publico/salones/{slug}/reservas` | Público |
| POST | `/api/publico/salones/{slug}/chat` | Público |

La consulta de disponibilidad utiliza:

```text
servicioId
estilistaId
fecha
```

## 10.8. Solicitudes de contacto

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/api/solicitudes` | DUENO |
| PATCH | `/api/solicitudes/{id}/atendida` | DUENO |

## 10.9. Pagos

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/pagos/suscripcion` | DUENO |
| POST | `/api/pagos/creditos` | DUENO |
| POST | `/api/pagos/webhook` | Stripe |
| GET | `/api/pagos/resultado?estado=` | Público |

## 10.10. Administración

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/api/admin/salones` | ADMIN |

**Importante:** los endpoints, métodos, parámetros, cuerpos y respuestas son contratos de la API. Su implementación debe respetar exactamente lo definido en `SPEC.md`.

---

# 11. Formato de respuestas y errores HTTP

Los errores utilizan un formato JSON uniforme.

Ejemplo:

```json
{
  "codigo": 409,
  "error": "Conflict",
  "mensaje": "La cita se cruza con otra del estilista",
  "fecha": "2026-11-06T10:30:00"
}
```

## Códigos HTTP principales

| Código | Descripción |
|---|---|
| 200 | Operación exitosa. |
| 201 | Recurso creado. |
| 400 | Datos inválidos o petición incorrecta. |
| 401 | Falta autenticación o token inválido. |
| 402 | Límite del plan o créditos insuficientes. |
| 403 | Operación no autorizada. |
| 404 | Recurso inexistente o perteneciente a otro salón. |
| 409 | Conflicto con una regla de negocio. |
| 429 | Límite de solicitudes excedido. |
| 502 | Respuesta inutilizable de un proveedor externo. |
| 503 | Ningún proveedor de IA disponible. |

Las respuestas de error deben mantener la estructura establecida en `SPEC.md`, incluyendo los errores de autenticación y autorización.

---

# 12. Instalación y ejecución local

## 12.1. Requisitos previos

Para trabajar con Sillón se recomienda disponer de:

- JDK 17.
- IntelliJ IDEA, Cursor o Visual Studio Code.
- MySQL.
- Git.
- Postman.
- Docker.
- Stripe CLI para pruebas locales de webhooks.

## 12.2. Clonar el repositorio

```bash
git clone <URL_DEL_REPOSITORIO>
```

Entrar en el proyecto:

```bash
cd DS_Java_IA_Backend_TallerMd4y5
```

El proyecto debe encontrarse en la raíz del repositorio, con `pom.xml` junto a `README.md`.

## 12.3. Configurar MySQL

Crear la base de datos:

```sql
CREATE DATABASE sillon_db;
```

Configurar el usuario y contraseña correspondientes en el entorno local.

La aplicación utiliza MySQL para el desarrollo local.

## 12.4. Configurar los secretos

Copiar:

```text
secrets.example.properties
```

Como:

```text
secrets.properties
```

Completar los valores correspondientes.

El archivo `secrets.properties` está excluido del repositorio mediante `.gitignore`.

No se deben publicar credenciales reales.

## 12.5. Compilar el proyecto

Windows PowerShell:

```powershell
.\mvnw.cmd clean compile
```

Linux/macOS:

```bash
./mvnw clean compile
```

## 12.6. Ejecutar las pruebas

Windows:

```powershell
.\mvnw.cmd test
```

Linux/macOS:

```bash
./mvnw test
```

## 12.7. Ejecutar la aplicación

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

La aplicación utiliza el puerto configurado en:

```properties
server.port=${PORT:8080}
```

Por defecto:

```text
http://localhost:8080
```

---

# 13. Configuración y variables de entorno

La configuración principal se encuentra en:

```text
src/main/resources/application.properties
```

El proyecto utiliza las mismas propiedades en desarrollo y producción, modificando sus valores mediante variables de entorno.

## Variables principales

| Variable | Descripción |
|---|---|
| `PORT` | Puerto de ejecución. |
| `SPRING_DATASOURCE_URL` | URL JDBC de conexión. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de base de datos. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de base de datos. |
| `JWT_SECRETO` | Secreto de firma JWT. |
| `URL_PUBLICA` | URL pública de la aplicación. |
| `STRIPE_CLAVE_SECRETA` | Clave privada de Stripe. |
| `STRIPE_PRECIO_PRO` | Identificador del precio PRO. |
| `STRIPE_PRECIO_CREDITOS` | Identificador del paquete de créditos. |
| `STRIPE_SECRETO_WEBHOOK` | Secreto de verificación del webhook. |
| `GEMINI_LLAVE` | Credencial del proveedor Gemini. |

## Configuración adicional

También existen propiedades relacionadas con:

- Expiración de JWT.
- Créditos iniciales.
- Créditos del plan PRO.
- Créditos de paquetes adicionales.
- Límite de estilistas.
- URL configurable de Stripe.
- URL configurable del proveedor de IA.
- Tiempos máximos de espera.
- Configuración de proxies.
- Límites de solicitudes.

El calificador automático utiliza simuladores de Stripe e IA. Por este motivo, las URLs de proveedores externos deben mantenerse configurables.

No se deben introducir direcciones fijas de proveedores externos en el código.

En Render no se deben configurar `STRIPE_URL_API` ni `SILLON_IA_OPENAI_URL`, de acuerdo con la configuración de producción definida para el proyecto.

---

# 14. Inteligencia artificial

El módulo de inteligencia artificial permite mejorar la comunicación entre los salones y sus clientes.

La IA se integra mediante peticiones HTTP utilizando `RestClient`.

La arquitectura contempla un proveedor principal compatible con el formato OpenAI (Gemini), un proveedor de respaldo con formato Claude y un mecanismo de fallback.

## 14.1. Responsabilidades de Java

El backend es responsable de:

- Consultar información real de la base de datos.
- Validar servicios y precios.
- Calcular disponibilidad.
- Verificar citas.
- Controlar créditos.
- Aplicar reglas de negocio.
- Registrar solicitudes de contacto.
- Validar respuestas JSON.
- Controlar errores y tiempos de espera.

La IA solamente interpreta consultas y redacta respuestas utilizando el contexto proporcionado por Java.

## 14.2. Recordatorios inteligentes

Endpoint:

```http
POST /api/citas/{id}/recordatorio
```

El sistema genera un borrador de mensaje para WhatsApp.

Java proporciona información verificada:

- Nombre del cliente.
- Servicio contratado.
- Duración.
- Estilista.
- Fecha.
- Hora.
- Precio.
- Dirección del salón.

La IA redacta el mensaje en formato JSON:

```json
{
  "mensaje": "Hola, te recordamos tu cita..."
}
```

El mensaje tiene un máximo de 700 caracteres.

**Importante:** Sillón no envía automáticamente el mensaje. Una persona debe revisarlo y aprobarlo.

## 14.3. Chatbot del salón

Endpoint:

```http
POST /api/publico/salones/{slug}/chat
```

El chatbot responde preguntas utilizando el catálogo real del salón.

Puede proporcionar información sobre:

- Servicios.
- Precios.
- Duración.
- Información del negocio.
- Disponibilidad proporcionada por Java.

El historial de conversación se envía en cada petición.

El chatbot no tiene memoria persistente propia.

El cliente nunca debe introducir instrucciones directamente dentro del prompt del sistema.

## 14.4. Escalamiento a atención humana

Cuando una consulta requiere intervención de una persona, el chatbot puede activar el escalamiento.

Se crea una entidad `SolicitudContacto` con el motivo:

```text
PEDIDO_POR_IA
```

El dueño del salón puede consultar las solicitudes y marcarlas como atendidas.

## 14.5. Protección anti-promesas comerciales

Java aplica una regla de seguridad independiente del prompt.

Se bloquean respuestas que contengan términos relacionados con:

- Descuentos.
- Cupones.
- Servicios gratis.
- Promociones.
- Reembolsos.

Cuando se detecta una promesa comercial no autorizada:

1. Se sustituye la respuesta por un mensaje seguro.
2. Se crea una solicitud de contacto.
3. Se registra el motivo `REGLA_DE_SEGURIDAD`.

La protección se implementa en código porque las instrucciones del prompt, por sí solas, no garantizan el cumplimiento de las reglas.

## 14.6. Consumo de créditos

El flujo de una operación de IA es:

```text
Validar solicitud
       |
       v
Verificar créditos
       |
       v
Consultar proveedor de IA
       |
       v
Validar respuesta JSON
       |
       v
Aplicar reglas de negocio
       |
       v
Consumir un crédito
       |
       v
Responder al cliente
```

Errores:

- Créditos insuficientes: 402.
- JSON inválido: 502.
- Ningún proveedor disponible: 503.

Las llamadas externas no deben ejecutarse dentro de una transacción de base de datos.

---

# 15. Pagos y suscripciones con Stripe

Sillón integra Stripe para gestionar las compras realizadas por los dueños de los salones.

La integración se mantiene en modo de prueba.

## 15.1. Productos

| Producto | Modalidad | Precio |
|---|---|---:|
| Sillón PRO | Suscripción mensual | 15 USD |
| Paquete de 200 créditos | Pago único | 5 USD |

## 15.2. Flujo de pago

1. El dueño solicita una suscripción o un paquete de créditos.
2. El backend determina el precio.
3. Stripe crea una sesión de Checkout.
4. El backend devuelve la URL de pago.
5. El dueño completa el proceso en Stripe.
6. Stripe notifica el resultado mediante webhook.
7. El backend verifica la firma.
8. El sistema procesa el evento y actualiza el salón.

El cliente no puede determinar el precio ni enviar identificadores de precios arbitrarios.

## 15.3. Webhook

Endpoint:

```http
POST /api/pagos/webhook
```

Eventos contemplados:

```text
checkout.session.completed
customer.subscription.deleted
```

El webhook verifica la firma de Stripe mediante `Stripe-Signature`.

La entidad `EventoStripe` permite registrar los eventos procesados y evitar operaciones duplicadas.

### Comportamiento

| Evento | Resultado |
|---|---|
| Compra PRO confirmada | Activa PRO y suma 300 créditos. |
| Compra de paquete confirmada | Suma 200 créditos. |
| Evento repetido | No vuelve a aplicar la compra. |
| Suscripción cancelada | Devuelve el salón a GRATIS y conserva sus créditos. |

El procesamiento de la compra y el registro del evento deben realizarse dentro de una misma transacción.

## 15.4. Pruebas de Stripe

Tarjeta de prueba habitual:

```text
4242 4242 4242 4242
```

Utilizar una fecha futura y un CVC válido de prueba.

Para simular un pago rechazado:

```text
4000 0000 0000 0002
```

Para probar webhooks localmente:

```bash
stripe listen --forward-to localhost:8080/api/pagos/webhook
```

El secreto generado por Stripe CLI debe configurarse en el entorno local.

No utilizar credenciales de prueba en un entorno de cobros reales.

---

# 16. Pruebas automatizadas

El proyecto utiliza JUnit 5 y Mockito para verificar el comportamiento del sistema.

Las pruebas deben validar las reglas de negocio y evitar depender de servicios externos reales.

## Áreas principales de pruebas

| Área | Validaciones |
|---|---|
| Citas | Estados permitidos y transiciones bloqueadas. |
| Agenda | Horarios, disponibilidad y solapamientos. |
| Reservas | Fechas, servicios, estilistas y conflictos. |
| Seguridad | Roles, permisos y aislamiento multi-tenant. |
| Planes | Límites de estilistas y créditos. |
| Chatbot | Respuestas, escalamiento y reglas anti-promesas. |
| Recordatorios | JSON válido y consumo de créditos. |
| IA | Fallback, errores y lectura de respuestas. |
| Stripe | Firma, idempotencia y actualización de planes. |
| Aplicación | Integración y carga del contexto Spring. |

## Ejecutar todas las pruebas

Windows:

```powershell
.\mvnw.cmd test
```

Linux/macOS:

```bash
./mvnw test
```

## Ejecutar una prueba específica

```bash
./mvnw -Dtest=NombreDeLaClaseTest test
```

Los tests unitarios no deben realizar llamadas reales a Gemini, Claude ni Stripe.

El test de contexto completo puede requerir una base de datos disponible.

**Criterio de calidad:** no se modifican los tests oficiales para hacerlos pasar. Los resultados deben corresponder a ejecuciones reales.

---

# 17. Documentación Swagger / OpenAPI

Sillón utiliza Springdoc OpenAPI para documentar la API REST.

Con la aplicación ejecutándose localmente:

**Swagger UI**

```text
http://localhost:8080/swagger-ui.html
```

**OpenAPI JSON**

```text
http://localhost:8080/v3/api-docs
```

La documentación permite consultar:

- Endpoints disponibles.
- Métodos HTTP.
- Parámetros.
- DTO de entrada.
- DTO de salida.
- Respuestas HTTP.
- Autenticación JWT.
- Operaciones públicas y privadas.

## Autenticación en Swagger

1. Ejecutar el endpoint de login.
2. Copiar el token JWT recibido.
3. Pulsar el botón `Authorize`.
4. Introducir el token Bearer.
5. Ejecutar los endpoints protegidos.

El endpoint:

```http
GET /api/salon
```

permite verificar el acceso del dueño autenticado.

---

# 18. Contenerización con Docker

Sillón utiliza Docker para preparar una imagen ejecutable del backend.

El proyecto contempla un Dockerfile multietapa.

## Etapa de construcción

Utiliza Maven y Java 17 para compilar la aplicación y generar el archivo JAR.

## Etapa de ejecución

Utiliza una imagen JRE 17 para ejecutar la aplicación sin incluir herramientas innecesarias de compilación.

## Construir la imagen

```bash
docker build -t sillon-api .
```

## Ejecutar el contenedor

```bash
docker run -p 8080:8080 sillon-api
```

La ejecución requiere las variables de entorno correspondientes a la base de datos y a los servicios externos.

## Exclusiones de Docker

El archivo `.dockerignore` debe excluir como mínimo:

```text
target
.git
secrets.properties
```

Nunca se deben incorporar credenciales privadas dentro de la imagen.

---

# 19. Base de datos de producción: Supabase

El entorno de producción utiliza PostgreSQL administrado mediante Supabase.

La configuración de conexión se realiza mediante variables de entorno.

## Configuración de conexión

Se utiliza el Session Pooler de Supabase.

Puerto:

```text
5432
```

Variables:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

La URL JDBC debe corresponder a la cadena de conexión proporcionada por Supabase.

La conexión directa puede presentar problemas de compatibilidad de red debido al uso de IPv6. Por ello, el proyecto utiliza Session Pooler para la conexión desde Render.

MySQL continúa siendo la base de datos del entorno local.

No se deben modificar las entidades, tablas ni relaciones para solucionar problemas de conectividad.

---

# 20. Despliegue en Render

Sillón está preparado para ejecutarse como Web Service en Render mediante Docker.

La base de datos de producción se aloja en Supabase.

## 20.1. Configuración del servicio

1. Crear un servicio Web Service en Render.
2. Conectar el repositorio de GitHub.
3. Seleccionar el entorno Docker.
4. Configurar las variables de entorno.
5. Iniciar el proceso de construcción.
6. Verificar los logs de despliegue.
7. Comprobar la conexión con Supabase.
8. Verificar el acceso a Swagger.

## 20.2. Variables de entorno en Render

| Variable | Descripción |
|---|---|
| `SPRING_DATASOURCE_URL` | Conexión JDBC de Supabase. |
| `SPRING_DATASOURCE_USERNAME` | Usuario PostgreSQL. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña PostgreSQL. |
| `JWT_SECRETO` | Secreto JWT. |
| `URL_PUBLICA` | URL pública de Render. |
| `STRIPE_CLAVE_SECRETA` | Clave de Stripe. |
| `STRIPE_PRECIO_PRO` | Precio de suscripción PRO. |
| `STRIPE_PRECIO_CREDITOS` | Precio del paquete de créditos. |
| `STRIPE_SECRETO_WEBHOOK` | Secreto del webhook. |
| `GEMINI_LLAVE` | Clave del proveedor Gemini. |

No configurar `STRIPE_URL_API` ni `SILLON_IA_OPENAI_URL` en Render, de acuerdo con la configuración establecida para producción y el calificador automático.

## 20.3. Puerto de ejecución

La aplicación utiliza:

```properties
server.port=${PORT:8080}
```

Esto permite que Render proporcione el puerto mediante la variable `PORT`.

## 20.4. Webhook de Stripe en producción

En el Dashboard de Stripe se configura el endpoint:

```text
https://TU-SERVICIO.onrender.com/api/pagos/webhook
```

Eventos:

```text
checkout.session.completed
customer.subscription.deleted
```

El secreto del endpoint debe configurarse en:

```text
STRIPE_SECRETO_WEBHOOK
```

En producción no se utiliza Stripe CLI.

## 20.5. URLs de producción

Las siguientes direcciones deben actualizarse después de verificar el despliegue.

| Recurso | Dirección |
|---|---|
| API de producción | https://sillon-6rqb.onrender.com|
| Swagger UI | 	https://sillon-6rqb.onrender.com/swagger-ui/index.html|
| OpenAPI JSON | https://sillon-6rqb.onrender.com/v3/api-docs |

Cuando Render proporcione la dirección definitiva, se debe sustituir el marcador por la URL completa.

**Importante:** el calificador automático utiliza la URL de Render indicada en este README para realizar pruebas contra la API desplegada.

No dejar los marcadores de producción en la versión final que se entregue para calificación.

## 20.6. Consideraciones de Render

Los servicios gratuitos pueden suspenderse después de períodos de inactividad.

La primera petición posterior puede tardar mientras el servicio vuelve a estar disponible.

La aplicación utiliza `America/Bogota` para las operaciones de agenda, independientemente de que el servidor trabaje en UTC.

---

# 21. Estructura del repositorio

La estructura general del proyecto es:

```text
DS_Java_IA_Backend_TallerMd4y5/
│
├── .github/
│   └── workflows/                 # Calificador automático
│
├── .mvn/
│
├── postman/
│   ├── sillon.postman_collection.json
│   ├── local.postman_environment.json
│   └── produccion.postman_environment.json
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/devSenior/sillon/
│   │   │       ├── compartido/
│   │   │       ├── seguridad/
│   │   │       ├── auth/
│   │   │       ├── salon/
│   │   │       ├── usuario/
│   │   │       ├── servicio/
│   │   │       ├── cliente/
│   │   │       ├── cita/
│   │   │       ├── publico/
│   │   │       ├── pago/
│   │   │       ├── chat/
│   │   │       └── ia/
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       └── prompts/
│   │           ├── recordatorio.txt
│   │           └── chatbot.txt
│   │
│   └── test/
│       └── java/
│
├── .cursorignore
├── .dockerignore
├── .gitignore
├── AGENTS.md
├── BITACORA-IA.md
├── Dockerfile
├── README.md
├── SPEC.md
├── mvnw
├── mvnw.cmd
├── pom.xml
└── secrets.example.properties
```

Los archivos y paquetes deben corresponder con la estructura real del repositorio.

## Documentos principales

| Archivo | Responsabilidad |
|---|---|
| `README.md` | Presentación, instalación, configuración y despliegue. |
| `SPEC.md` | Requisitos, reglas de negocio y contrato oficial de API. |
| `AGENTS.md` | Instrucciones para agentes de IA y convenciones de desarrollo. |
| `BITACORA-IA.md` | Registro de actividades, decisiones, revisiones y correcciones asistidas por IA. |
| `pom.xml` | Dependencias y configuración de Maven. |
| `Dockerfile` | Construcción de la imagen Docker. |
| `.dockerignore` | Exclusiones de la imagen. |
| `.gitignore` | Archivos que Git debe ignorar. |
| `secrets.example.properties` | Plantilla de configuración de secretos. |

Antes de modificar una regla de negocio, una entidad o un contrato de API, se debe consultar `SPEC.md`.

---

# 22. Fases de desarrollo

El proyecto integrador está organizado en 14 fases, numeradas de 0 a 13.

| Fase | Descripción |
|---|---|
| 0 | Preparación del repositorio, proyecto y documentación inicial. |
| 1 | Base SaaS, salón, usuarios y configuración compartida. |
| 2 | Seguridad, JWT, registro y autenticación. |
| 3 | Catálogo, estilistas, servicios y clientes. |
| 4 | Agenda, disponibilidad, citas y reglas de horario. |
| 5 | Catálogo público y reservas en línea. |
| 6 | Integración con Stripe, suscripciones y créditos. |
| 7 | Motor de IA, proveedores y fallback. |
| 8 | Recordatorios inteligentes con datos verificados. |
| 9 | Chatbot, escalamiento y protección anti-promesas. |
| 10 | Pruebas automatizadas con JUnit y Mockito. |
| 11 | Documentación Swagger / OpenAPI. |
| 12 | Despliegue con Docker, Supabase y Render. |
| 13 | Evidencias ejecutables, Postman y entrega final. |

Cada fase debe cerrarse con:

- Implementación revisada.
- Pruebas ejecutadas.
- Resultados documentados.
- Pendientes identificados.
- Documentación actualizada.
- Commit descriptivo.

El historial de Git forma parte de la evidencia del desarrollo.

---

# 23. Seguridad del proyecto

La seguridad es uno de los principios fundamentales de Sillón.

## Autenticación

- JWT para autenticación stateless.
- BCrypt para proteger contraseñas.
- Tokens con expiración configurada.

## Autorización

- Control de acceso basado en roles.
- Protección de endpoints privados.
- Validación de permisos en los servicios.

## Aislamiento de datos

- Identificación del salón mediante el usuario autenticado.
- Filtrado de consultas por salón.
- Respuesta 404 para recursos pertenecientes a otro negocio.

## Protección de integraciones

- Verificación de firmas de Stripe.
- Control de eventos duplicados.
- Validación de respuestas de proveedores de IA.
- Tiempos máximos de espera.
- Protección de secretos.

## Protección de información sensible

No se deben publicar:

- Contraseñas.
- Claves API.
- Secretos JWT.
- Credenciales de MySQL.
- Credenciales de Supabase.
- Claves privadas de Stripe.
- Secretos de webhooks.
- Tokens de servicios externos.
- Datos privados de clientes.

Los archivos `secrets.properties` y `.env` con valores reales no deben subirse a GitHub.

Antes de realizar un commit:

```bash
git status
```

Revisar los cambios:

```bash
git diff --check
```

Consultar el resumen:

```bash
git diff --stat
```

La carpeta `.github` contiene el calificador automático y no debe modificarse.

---

# 24. Verificación y evaluación académica

El proyecto utiliza una colección Postman y un calificador automático mediante GitHub Actions.

## 24.1. Colección Postman

La colección principal se encuentra en:

```text
postman/sillon.postman_collection.json
```

Entornos:

```text
postman/local.postman_environment.json
postman/produccion.postman_environment.json
```

La colección organiza las verificaciones en siete grupos:

1. Registro, login y configuración.
2. Catálogo y equipo.
3. Agenda, reservas, cruces y estados.
4. Aislamiento entre salones y roles.
5. Inteligencia artificial: recordatorios y chatbot.
6. Pagos con Stripe en modo de prueba.
7. Verificaciones posteriores al pago.

Las pruebas deben utilizar datos únicos por ejecución y fechas calculadas dinámicamente para facilitar su repetición.

## 24.2. Calificador automático

El calificador de GitHub Actions verifica diferentes aspectos del proyecto:

- Compilación.
- Pruebas automatizadas.
- Inicio de la aplicación.
- Funcionamiento de la API.
- Integración con simuladores de IA y Stripe.
- Contratos HTTP.
- Colección oficial de Postman.
- Estructura del repositorio.
- Disponibilidad de la API desplegada.

Los resultados se publican en el Pull Request.

No se debe modificar la carpeta `.github`.

## 24.3. Criterios de calificación

| Criterio | Pruebas |
|---|---:|
| Registro, login y configuración | 14 |
| Catálogo y equipo | 9 |
| Agenda | 34 |
| Aislamiento y roles | 7 |
| IA: recordatorio y chatbot | 16 |
| Pagos con Stripe | 29 |
| Tests JUnit | 8 |
| Repositorio y entrega | 12 |
| Producción en Render | 16 |
| **Total** | **145** |

La calificación se calcula mediante:

```text
Nota = 5 × pruebas aprobadas / pruebas totales
```

El resultado se expresa en una escala de 0.0 a 5.0, con un decimal.

**Principio de evaluación:** lo que no se puede verificar, no se puede calificar.

## 24.4. Entregables

- [ ] Repositorio GitHub con el proyecto en la raíz.
- [ ] Historial de commits organizado por fases.
- [ ] `README.md` completo.
- [ ] `SPEC.md`.
- [ ] `AGENTS.md`.
- [ ] `BITACORA-IA.md`.
- [ ] `secrets.example.properties`.
- [ ] Prompts de IA en `src/main/resources/prompts/`.
- [ ] Pruebas automatizadas JUnit y Mockito.
- [ ] Dockerfile y `.dockerignore`.
- [ ] Colección Postman.
- [ ] Entornos local y producción.
- [ ] API desplegada en Render.
- [ ] Base de datos PostgreSQL en Supabase.
- [ ] Swagger accesible en producción.
- [ ] Webhook de producción configurado y probado.
- [ ] Pull Request abierto hacia el repositorio oficial, con el nombre completo del autor como título.

La presencia de un archivo o entregable en esta lista no significa automáticamente que esté implementado o aprobado. Cada elemento debe verificarse.

---

# 25. Consideraciones técnicas y evolución

Sillón se desarrolla con una arquitectura que permite ampliar sus funcionalidades sin comprometer las reglas de negocio fundamentales.

Para su evolución se consideran especialmente los siguientes aspectos:

### Escalabilidad

- Optimización de consultas SQL.
- Índices adecuados para consultas multi-tenant.
- Gestión eficiente de conexiones.
- Control del consumo de servicios externos.
- Supervisión de tiempos de respuesta.

### Concurrencia

Las reservas simultáneas requieren mecanismos adicionales de protección para garantizar que dos solicitudes concurrentes no ocupen el mismo horario.

### Seguridad

El aislamiento entre salones debe mantenerse en todas las operaciones, independientemente de la evolución de los módulos.

### Inteligencia artificial

La integración debe mantener separados los datos verificados por Java y el contenido generado por los proveedores externos.

### Infraestructura

La evolución del proyecto debe contemplar:

- Migraciones de base de datos.
- Copias de seguridad.
- Monitoreo.
- Gestión de errores.
- Protección de información personal.
- Administración de secretos.

### Reflexión técnica: ¿Qué protegería primero con 500 salones?

El aislamiento multi-tenant y la concurrencia de las reservas serían mis primeras prioridades. Una consulta incorrecta podría exponer información de otro negocio, mientras que una reserva simultánea podría generar conflictos en la agenda. Después optimizaría las consultas de PostgreSQL y supervisaría el consumo de los proveedores de inteligencia artificial para mantener el rendimiento y controlar los costos de la plataforma.

---

# 26. Autor y propósito académico

**Proyecto:** Sillón — Agenda inteligente para barberías y salones de belleza.

**Programa:** Java AI Engineer.

**Institución:** Dev Senior Code.

**Tipo de proyecto:** Proyecto integrador de los módulos 4 y 5.

**Autor:** Jean-René Barthélémy.

**Lenguaje principal:** Java 17.

**Framework:** Spring Boot.

**Arquitectura:** API REST SaaS multi-tenant.

**Base de datos local:** MySQL.

**Base de datos de producción:** PostgreSQL / Supabase.

**Contenerización:** Docker.

**Despliegue:** Render.

## Propósito académico

Sillón representa la integración práctica de conocimientos de programación orientada a objetos, desarrollo backend con Java, Spring Boot, persistencia de datos, arquitectura SaaS, seguridad, autenticación, APIs REST, integración de servicios externos, inteligencia artificial, pruebas automatizadas y despliegue cloud.

El proyecto busca demostrar la capacidad de diseñar, desarrollar, documentar, probar y desplegar una aplicación backend que responde a necesidades reales de un sector comercial.

---

# 27. Licencia

Proyecto desarrollado con fines académicos en el programa Java AI Engineer de Dev Senior Code.

La licencia de distribución y reutilización del código deberá definirse explícitamente por el propietario del repositorio.

---

**Sillón 💈 — Tecnología para organizar mejor cada cita y transformar la atención de los salones de belleza.**
