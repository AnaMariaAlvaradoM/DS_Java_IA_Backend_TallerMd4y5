# AGENTS — Reglas para el agente de IA del proyecto Sillón

Estas son las reglas que todo agente de IA (Cursor u otro) debe respetar al trabajar en este repositorio. Provienen del documento oficial "Sillón · Proyecto integrador · Módulos 4 y 5". `SPEC.md` es la fuente de verdad de las reglas de negocio y del contrato de la API.

## 1. Estructura del proyecto

- Paquete base: `com.devsenior.sillon`.
- El proyecto se organiza por dominios de negocio, en estos 12 paquetes:

| Paquete | Responsabilidad |
|---|---|
| compartido | Formato de error, manejador global, reloj del sistema, configuración de Swagger |
| seguridad | JWT, filtro de autenticación, reglas de acceso por rol |
| auth | Registro del salón con su dueño, login |
| salon | El salón (inquilino), su plan, sus créditos y su configuración |
| usuario | Usuarios, roles y estilistas |
| servicio | Catálogo de servicios del salón |
| cliente | Clientes finales del salón |
| cita | Citas, estados, reglas de agenda, disponibilidad y recordatorio con IA |
| publico | Lo que ve el cliente final sin iniciar sesión |
| pago | Sesiones de pago y webhook de Stripe |
| chat | Chatbot del salón y solicitudes de contacto |
| ia | Proveedores de IA, fallback, prompts y lectura de JSON |

## 2. Multi-inquilino (regla de oro)

- Todo dato pertenece a un salón. Toda consulta y toda modificación se filtra por el salón del usuario autenticado.
- El `salonId` se toma del usuario autenticado (`@AuthenticationPrincipal`), nunca de un id que llegue en la URL o en el cuerpo.
- Si un dato existe pero pertenece a otro salón, la respuesta es 404, como si no existiera.

## 3. Fechas y horas

- Toda regla que dependa de "hoy" o "ahora" usa el reloj del proyecto: un bean de tipo `Clock` con la zona horaria del negocio (`America/Bogota`), recibido por constructor.
- Nunca usar la hora del servidor directamente: Render corre en UTC.

## 4. Inteligencia artificial

- Los prompts del producto viven en archivos dentro de `src/main/resources/prompts/`, no dentro del código.
- Java calcula y arma los datos verificados; la IA solo redacta. La disponibilidad la calcula Java, nunca la IA.
- Las llamadas a la IA se hacen por HTTP con `RestClient`. Nada de Spring AI ni SDKs de IA.
- Ninguna llamada a la IA va dentro de una transacción de base de datos.
- Toda respuesta de la IA se pide en JSON y se valida en Java. Un prompt pide; solo el código garantiza.

## 5. Tests

- Nunca modificar un test para que pase. Si un test falla, la causa se decide contra la SPEC: la duda es si el error está en el test o en el código.
- Los casos de prueba salen de las reglas de negocio; el agente puede escribir el código de los tests, pero no decide los casos.

## 6. Dependencias y stack

- No agregar dependencias sin preguntar.
- Nada de Lombok: getters y setters explícitos.
- Los enums se guardan como texto (`EnumType.STRING`) y sus valores van sin tildes (DUENO, no DUEÑO).
- La configuración vive en `application.properties` (nunca .yml, nunca perfiles).

## 7. Contrato de la API

- El contrato de la sección 7 de `SPEC.md` se respeta al pie de la letra: rutas, métodos, nombres de campos y códigos de respuesta. Un campo con otro nombre hace fallar la calificación.
- Todos los errores usan el formato único de cuatro campos: codigo, error, mensaje, fecha.

## 8. Secretos

- Las llaves viven en `secrets.properties` (local) o en variables de entorno (Render). Nunca en el código, nunca en Git, nunca en un prompt.
- Las direcciones de Stripe (`STRIPE_URL_API`) y del proveedor de IA (`sillon.ia.openai.url`) son configurables: nunca escribirlas fijas en el código.

## 9. Límites del agente

- No modificar la carpeta `.github`: ahí vive el calificador.
- Las reglas de negocio, estados, permisos y planes ya están decididos en `SPEC.md`: el agente no los cambia.
- Todo diff se revisa y se prueba antes de aceptarse; nada se acepta sin entenderlo.


| 3 | Fase 0 | GitHub Copilot — Agent | Incorporar las dependencias de Springdoc, Stripe y JJWT al pom.xml | Se revisaron las versiones, el alcance de los cambios y los resultados de clean compile y dependency:tree | Se descartaron versiones preliminares de Stripe y se conservaron las dependencias existentes |
