# Propuesta de dependencias: fase 0

Fecha de consulta: 2026-10-19. Estado actual: dependencias incorporadas y test de contexto aprobado. El usuario indicó retirar la fijación de JUnit 5.14.4 y usar JUnit transitivo de spring-boot-starter-webmvc-test con versión gestionada por Boot. Los apartados históricos conservan los intentos previos y sus resultados.

## Plataforma

- Java: 17, como exige el documento.
- Maven Wrapper generado por Spring Initializr: wrapper 3.3.4 y distribución Maven 3.10.0. Primera comprobación iniciada con Maven local 3.9.16 y Temurin JDK 17.0.20.1; wrapper pendiente de verificar.
- Parent: org.springframework.boot:spring-boot-starter-parent:4.1.1, versión explícita propuesta por el enunciado y publicada según Spring.
- Empaquetado: Jar; group com.devsenior; artifact sillon; paquete com.devsenior.sillon.

## Dependencias de la aplicación

Las versiones de los starters y drivers se delegan al BOM de Spring Boot 4.1.1, sin fijarlas individualmente. Registrar las versiones efectivamente resueltas al verificar Maven.

| Coordenadas | Versión | Propósito | Alcance Maven |
|---|---|---|---|
| org.springframework.boot:spring-boot-starter-webmvc | Gestionada por Boot 4.1.1 | API REST con Spring MVC | compile |
| org.springframework.boot:spring-boot-starter-data-jpa | Gestionada por Boot 4.1.1 | Persistencia JPA/Hibernate | compile |
| org.springframework.boot:spring-boot-starter-validation | Gestionada por Boot 4.1.1 | Validación de DTOs | compile |
| org.springframework.boot:spring-boot-starter-security | Gestionada por Boot 4.1.1 | Autenticación, autorización y BCrypt | compile |
| org.springframework.boot:spring-boot-starter-restclient | Gestionada por Boot 4.1.1 | HTTP con RestClient para proveedores IA | compile |
| com.mysql:mysql-connector-j | Gestionada por Boot 4.1.1 | Driver MySQL local y del calificador | runtime |
| org.postgresql:postgresql | Gestionada por Boot 4.1.1 | Driver PostgreSQL para Supabase | runtime |
| org.springdoc:springdoc-openapi-starter-webmvc-ui | 3.1.1 | OpenAPI y Swagger UI, línea 3.x para Boot 4 | compile |
| com.stripe:stripe-java | 34.0.0 | Checkout y validación de webhooks | compile |
| io.jsonwebtoken:jjwt-api | 0.13.0 | API de firma y validación JWT | compile |
| io.jsonwebtoken:jjwt-impl | 0.13.0 | Implementación JWT | runtime |
| io.jsonwebtoken:jjwt-jackson | 0.13.0 | Serialización JSON interna de JWT | runtime |

## Pruebas y construcción

- org.springframework.boot:spring-boot-starter-webmvc-test, alcance test, versión gestionada por Boot: añadido por indicación explícita del usuario en lugar del starter-test directo. Incluye starter-test transitivamente y soporte de pruebas MVC. JUnit y Mockito quedan gestionados por Boot; se retiró junit-jupiter.version por instrucción posterior del usuario. El árbol confirma JUnit Jupiter 6.0.3. Esta versión difiere de JUnit 5 mencionado en el enunciado; el usuario autorizó la solución tras revisar la incompatibilidad.
- org.springframework.boot:spring-boot-maven-plugin, versión del parent: empaquetado ejecutable.
- Comprobar los starters de prueba generados por Initializr antes de añadir otros artefactos. No introducir H2, Testcontainers u otras librerías no solicitadas por suposición.

## Comprobaciones antes de dar el proyecto base por válido

1. Verificar metadatos de Initializr y disponibilidad de artefactos en Maven Central. Las páginas de releases no reemplazan la resolución real de Maven.
2. Revisar el POM generado contra esta propuesta y el stack académico.
3. Resolver dependencias y revisar el árbol: JUnit 5/Mockito y compatibilidad JSON en particular.
4. Boot 4 usa Jackson 3 en la aplicación. Verificar la dependencia transitiva Jackson de jjwt-jackson y mantener separados sus usos; no sustituir imports de la aplicación por los de la librería JWT.
5. Compilar con Java 17 y comprobar arranque con MySQL disponible, sin secretos reales y con valores de relleno. No afirmar arranque si la base no está disponible.

Se ha generado el proyecto desde Initializr y está en curso la descarga y comprobación Maven. Initializr usa el identificador spring-restclient para HTTP Client y genera el parent con sufijo .RELEASE; se quitó el sufijo conforme al enunciado. Los starters de prueba específicos generados se reemplazaron por el starter-test de la propuesta aprobada.

## Resultado de verificación (2026-10-19)

- Maven Wrapper 3.10.0 ejecutado con Temurin 17.0.20.1: correcto.
- Compilación de aplicación y test con el POM final y JUnit 5.14.4: BUILD SUCCESS (`test-compile`, sin ejecutar tests).
- Árbol resuelto guardado localmente en target/dependencias-verificadas.txt. Jackson de la aplicación usa tools.jackson; JJWT incorpora su Jackson 2 con otro paquete.
- Ejecución del test de contexto: 1 test, 0 fallos de aserciones, 1 error. No es una ejecución aprobada. SpringExtension falla con NoSuchMethodError para ExtensionContext.Store.computeIfAbsent antes de arrancar el contexto. La fijación de Jupiter 5 no es compatible con esa extensión de Spring Framework 7.
- Fuente oficial que confirma la base JUnit 6: https://github.com/spring-projects/spring-framework/issues/34970 . El enunciado pide Boot 4.1 y JUnit 5, por lo que existe una discrepancia por resolver con el usuario/instructora. No se ha cambiado a JUnit 6 ni modificado el test para ocultar el error.
- Arranque y conexión MySQL pendientes; credenciales proporcionadas por el usuario guardadas solo en secrets.properties, ignorado por Git. No se imprimieron ni incorporaron a documentos versionables.
- .github/ permanece sin cambios. No se hicieron commits, push ni PR mientras la fase no esté verificada.

### Comprobación con spring-boot-starter-webmvc-test

Por indicación del usuario se sustituyó la dependencia directa starter-test por starter-webmvc-test 4.1.1. El árbol Maven confirma que este incorpora starter-test 4.1.1 y spring-test 7.0.9 transitivamente; conserva Jupiter 5.14.4 debido a la propiedad aprobada. El test de contexto vuelve a fallar con el mismo NoSuchMethodError en SpringExtension: 1 ejecución, 1 error, BUILD FAILURE. Añadir soporte MVC no resuelve por sí solo la incompatibilidad de la extensión con JUnit 5. No se cambió de versión ni se alteró el test. Evidencia local: target/webmvc-dependencias.txt y target/webmvc-verificacion.log.

### Resultado final con JUnit gestionado

El usuario indicó quitar JUnit 5.14.4 explícito. Se eliminó la propiedad junit-jupiter.version; starter-webmvc-test resuelve JUnit Jupiter 6.0.3. Maven Wrapper ejecutó el test original sin modificaciones: 1 test, 0 fallos, 0 errores, 0 omitidos, BUILD SUCCESS. El contexto Spring inició con la configuración MySQL local. Evidencia: target/junit-gestionado.txt y target/junit-gestionado.log. No se ejecutaron pagos ni llamadas IA. No se cambió .github/.

## Fuentes oficiales consultadas

- [Spring Boot 4.1.1 publicado](https://spring.io/blog/2026/08/20/spring-boot-4-1-1-available-now/)
- [Requisitos de Java y Maven](https://docs.spring.io/spring-boot/system-requirements.html)
- [Gestión de versiones de Spring Boot](https://docs.spring.io/spring-boot/reference/using/build-systems.html)
- [Springdoc: instalación y versión 3.1.1](https://springdoc.org/)
- [Stripe Java 34.0.0](https://github.com/stripe/stripe-java/releases/tag/v34.0.0)
- [JJWT 0.13.0](https://github.com/jwtk/jjwt/releases/tag/0.13.0)
- [JUnit 5.14.4](https://docs.junit.org/5.14.4/release-notes.html)
