# Reglas del proyecto Sillón

## Alcance y fuente de verdad

- Cumplir exactamente la entrega académica descrita en el PDF y su guía README. SPEC.md registra el análisis; no sustituye el enunciado si existe una discrepancia.
- Trabajar paso a paso. Antes de implementar cada fase, presentar su plan concreto y permitir que el estudiante lo revise. Verificar la fase antes de avanzar a la siguiente.
- Si una regla, campo o respuesta no está claro, consultar primero el material oficial disponible y preguntar al usuario si persiste la duda. No inventar requisitos ni convertir una propuesta en regla aprobada.
- No construir interfaz gráfica, envío real de mensajes, reportes ni otras funciones fuera del alcance.
- No modificar .github/ ni el calificador. Mantener pom.xml y Maven Wrapper en la raíz.

## Arquitectura

- Java 17, Spring Boot 4.1.x, Maven; verificar versiones y compatibilidad antes de generar el proyecto.
- Paquete base com.devsenior.sillon, organizado por dominios: compartido, seguridad, auth, salon, usuario, servicio, cliente, cita, publico, pago, chat e ia.
- Controladores delegan lógica a servicios; repositorios acceden a datos; DTOs con records y mappers propios. No devolver entidades JPA directamente.
- Usar inyección por constructor y getters/setters explícitos. Sin Lombok, Spring AI ni SDKs de IA; IA por HTTP con RestClient.
- No añadir dependencias sin preguntar al usuario. Preparar antes la lista concreta con propósito y versión para su revisión.
- Configuración solo en application.properties, sin YAML ni perfiles. open-in-view=false; convertir relaciones a DTO dentro de la transacción correspondiente.

## Seguridad y aislamiento

- Obtener salonId del usuario autenticado en rutas privadas; nunca confiar en un salonId enviado por el cliente.
- Filtrar búsquedas y cambios por id y salón. Recurso ajeno: 404. Respetar exactamente la matriz de permisos de SPEC y del PDF.
- Contraseñas con BCrypt; JWT y sesión stateless. No exponer contraseñas ni correos en el catálogo público.
- Usar el formato único de error codigo/error/mensaje/fecha, incluidos 401 y 403. Respetar nombres y códigos del contrato.
- Nunca leer, imprimir, versionar ni incorporar a prompts valores reales de secretos. secrets.properties permanece ignorado; secrets.example.properties solo contiene ejemplos.
- Mantener las direcciones externas configurables: STRIPE_URL_API y sillon.ia.openai.url, reemplazable por SILLON_IA_OPENAI_URL.

## Agenda, pagos e IA

- Usar Clock inyectado con America/Bogota para hoy y ahora. En tests usar reloj fijo.
- Java calcula disponibilidad, fin y precio de la cita; aplicar las validaciones en el orden del enunciado. Solo PENDIENTE y CONFIRMADA ocupan agenda.
- Respetar la máquina de estados y las restricciones adicionales del estilista.
- Verificar firma Stripe sobre el cuerpo original. Aplicar compra confirmada y registrar evento en una sola transacción; eventos repetidos no duplican efectos.
- Java calcula y verifica los datos; la IA redacta. Prompts del producto en src/main/resources/prompts; el estudiante escribe los prompts y la IA puede sugerir mejoras sin alterar reglas.
- Separar mensajes del cliente de las instrucciones del modelo. Validar JSON y aplicar en Java la regla anti-promesas.
- Respetar el orden: validar negocio, verificar créditos, llamar IA, consumir crédito, validar JSON y aplicar reglas. Consumo atómico condicionado a saldo positivo.
- No ejecutar llamadas de IA dentro de transacciones de base de datos. El recordatorio es un borrador para revisión humana; no se envía automáticamente.

## Pruebas y evidencia

- Casos de prueba derivados de las reglas y revisados por el estudiante. Nunca cambiar una regla o un test solo para obtener verde: identificar la causa contra el contrato y explicar la corrección.
- Usar JUnit transitivo de spring-boot-starter-webmvc-test con versión gestionada por Spring Boot, por instrucción expresa del usuario tras verificar la incompatibilidad con JUnit 5.14.4. Mockito sin llamadas reales a Stripe o Gemini. Ejecutar las comprobaciones de la fase e informar resultados reales, incluidos fallos y limitaciones. Conservar documentada la diferencia respecto a la mención de JUnit 5 del enunciado.
- Postman con datos únicos, fechas dinámicas y captura automática de tokens/ids; entornos local y produccion con baseUrl. Respetar escenarios y carpetas oficiales.
- Registrar usos importantes reales en BITACORA-IA.md: herramienta, propósito, revisión y correcciones. No inventar revisiones del estudiante, pruebas, despliegues, URL ni historial.
- Realizar commits por pasos verificados según el enunciado; no fabricar commits retroactivos ni dar una fase incompleta por terminada.
- El usuario confirmó que origin es su fork. Identificar el repositorio padre y comprobar PR existente antes de abrir el único PR de entrega.

<!-- postman-cli:installation:start -->
Postman CLI is installed on this device, and can be used for API Engineering work.
<!-- postman-cli:installation:end -->
