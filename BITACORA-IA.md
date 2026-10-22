# Bitácora de uso de Inteligencia Artificial - Proyecto Sillón

## Entrada 1: Configuración de Spring Security y JWT
- **Prompts utilizados:** Generación de `SecurityConfig` stateless y filtro `JwtAuthFilter`.
- **Ajustes realizados:** Adaptación de las respuestas 401 y 403 al contrato con 4 campos obligatorios.

## Entrada 2: Corrección de dependencias y errores Bean
- **Prompts utilizados:** Resolución de conflictos de beans ambiguos en `GlobalExceptionHandler`.
- **Ajustes realizados:** Consolidación de excepciones de autenticación en un solo handler.

## Entrada 3: Aislamiento Multitenant en Salón
- **Prompts utilizados:** Extracción de `salonId` desde `@AuthenticationPrincipal UsuarioPrincipal`.
- **Ajustes realizados:** Eliminación de identificadores externos en URLs para `GET /api/salon`.

## Entrada 4: Pruebas unitarias con JUnit y Mockito
- **Prompts utilizados:** Creación de tests unitarios para las reglas de negocio de agenda y reservas.
- **Ajustes realizados:** Mockeo de repositorios JPA para pruebas independientes.

## Entrada 5: Colección de Postman e Integración
- **Prompts utilizados:** Automatización de aserciones en JavaScript para endpoints REST.
- **Ajustes realizados:** Validación de códigos de estado (200, 201, 400, 401, 409) en la suite.