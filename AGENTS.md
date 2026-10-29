# AGENTS.md — Reglas para agentes de IA en este repositorio

> Borrador Fase 0. Ajustar a medida que avance el proyecto.

## Paquetes y estructura
- Paquete base: `com.devsenior.sillon`.
- Organización **por dominio** (no por capa técnica): p. ej. `com.devsenior.sillon.agenda`, `com.devsenior.sillon.catalogo`, `com.devsenior.sillon.pagos`, `com.devsenior.sillon.ia`, `com.devsenior.sillon.usuarios`, etc. Dentro de cada dominio pueden existir subcapas (`controller`, `service`, `repository`, `dto`, `model`).

## Dependencias y librerías
- **No usar Lombok.** Getters y setters se escriben explícitos a mano.
- **No usar Spring AI ni SDKs de proveedores de IA** (ni de OpenAI, ni de Gemini, ni similares). Las llamadas a IA se hacen con HTTP directo (p. ej. `RestClient`, ya incluido en el `pom.xml`).
- **No agregar dependencias nuevas al `pom.xml` sin preguntar antes** al usuario, aunque parezcan necesarias o convenientes.

## Datos y seguridad multi-tenant
- Todo dato de negocio (clientes, citas, equipo, catálogo, pagos, etc.) debe estar **filtrado por el salón (tenant) del usuario autenticado**. Ninguna consulta ni endpoint debe devolver ni modificar datos de otro salón.

## Fechas y zona horaria
- Nunca usar `LocalDate.now()`, `LocalDateTime.now()`, `Instant.now()`, etc. directamente.
- Usar siempre el `Clock` del proyecto, configurado en zona `America/Bogota`, inyectado donde se necesite la fecha/hora actual. Esto permite pruebas deterministas y evita inconsistencias de zona horaria.

## IA: prompts y responsabilidades
- Los prompts viven en archivos dentro de `src/main/resources/prompts/` (no se construyen como strings concatenados en el código Java).
- Separación de responsabilidades: **Java calcula, la IA redacta**. Toda regla de negocio, validación o cálculo (totales, fechas, disponibilidad, límites, etc.) se hace en Java. La IA solo se usa para generar o reformular texto en lenguaje natural a partir de datos ya verificados por Java; nunca para decidir ni calcular.
- La IA simulada del calificador no razona: responde según lo que el prompt le pida. Las reglas de negocio (p. ej. bloquear que el chatbot "prometa" cupones) se validan en Java, no se confían a la respuesta de la IA.

## Tests
- **Nunca modificar un test para que pase.** Si un test falla, se corrige el código de producción, no el test. Si el test parece estar mal planteado, se pregunta al usuario antes de tocarlo.

## Secretos
- Nunca leer, abrir ni mostrar el contenido de `secrets.properties` ni de ningún archivo `.env`.
- Nunca escribir llaves reales (Stripe, Gemini, etc.) en código, commits, ni en `secrets.example.properties`.
