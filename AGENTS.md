
# AGENTS.md — Reglas del agente para Sillón

## 1. Fuente de verdad

- SPEC.md contiene las reglas funcionales y el contrato de la API.
- Respetar exactamente las rutas, nombres de campos, estados y códigos HTTP definidos en SPEC.md.
- No inventar reglas de negocio ni funcionalidades adicionales.
- Ante una contradicción o ambigüedad, detenerse y solicitar una decisión al desarrollador.
- No modificar requisitos aprobados sin autorización.

## 2. Tecnologías obligatorias

- Java 25, versión configurada y verificada para este proyecto.
- Spring Boot 4.1.x y Maven.
- Spring Data JPA.
- MySQL en desarrollo local y PostgreSQL en producción.
- Spring Security, JWT y BCrypt.
- JUnit 5 y Mockito.
- Swagger/OpenAPI.
- Stripe para pagos.
- RestClient para integrar proveedores de IA.
- Docker, Render y Supabase.

No utilizar Lombok, Spring AI ni SDKs de proveedores de inteligencia artificial.

No agregar dependencias sin autorización previa.

## 3. Organización del proyecto

Paquete base: com.devsenior.sillon.

Organizar las clases en los siguientes paquetes:

- compartido: manejo de errores, reloj y Swagger.
- seguridad: JWT, autenticación y autorización.
- auth: registro y login.
- salon: información, planes, créditos y configuración.
- usuario: usuarios, roles y estilistas.
- servicio: catálogo de servicios.
- cliente: clientes finales.
- cita: agenda, estados y disponibilidad.
- publico: endpoints públicos.
- pago: Stripe, sesiones y webhooks.
- chat: chatbot y solicitudes de contacto.
- ia: proveedores de IA, prompts y procesamiento de respuestas.

## 4. Arquitectura multi-inquilino

- Cada salón constituye un inquilino independiente.
- Filtrar todas las operaciones por el salonId del usuario autenticado.
- No confiar en identificadores de salón enviados por el cliente.
- Las operaciones sobre recursos de otro salón deben responder HTTP 404.
- Los endpoints públicos identifican el salón mediante su slug.
- Aplicar exactamente los permisos definidos para DUENO, ESTILISTA y ADMIN.

## 5. Fechas y reglas de agenda

- Utilizar un bean Clock con la zona America/Bogota.
- Inyectar el reloj por constructor en los componentes que dependan de fechas u horas.
- No utilizar directamente la hora del servidor para las reglas de negocio.
- Calcular disponibilidad, horarios, duración y cruces exclusivamente en Java.
- La IA no puede decidir la disponibilidad ni modificar las reglas de agenda.
- Respetar las transiciones de estado definidas en SPEC.md.

## 6. Inteligencia artificial

- Integrar proveedores mediante HTTP con RestClient.
- Mantener los prompts del producto en archivos separados.
- Java obtiene y verifica los datos; la IA redacta los mensajes.
- Solicitar las respuestas de IA en formato JSON y validarlas.
- Respetar las reglas de créditos, fallback y escalamiento.
- Aplicar en Java la protección contra promesas comerciales.
- Nunca enviar credenciales ni información secreta en los prompts.

## 7. Seguridad y configuración

- Utilizar variables de entorno para las credenciales.
- Utilizar secrets.properties únicamente en desarrollo local.
- No incluir secretos reales en el código ni en archivos versionados.
- Respetar .gitignore y .cursorignore.
- No modificar la carpeta .github.
- No registrar contraseñas, JWT o claves privadas en logs.

## 8. Implementación y revisión

- Trabajar una fase a la vez, siguiendo el orden del enunciado.
- Primero presentar un plan en Cursor (modo Plan).
- Esperar la aprobación del desarrollador antes de implementar mediante Agent.
- Mantener las decisiones y el código previamente aprobados.
- Revisar los cambios realizados antes de aceptarlos.
- No modificar archivos ajenos al alcance de la fase.
- No cambiar el contrato oficial para facilitar la implementación.

## 9. Pruebas

- Definir los casos de prueba a partir de las reglas del negocio.
- Utilizar JUnit 5 y Mockito.
- No modificar ni eliminar pruebas para conseguir que pasen.
- Ante una prueba fallida, determinar si el problema está en el código o en el test.
- Verificar las respuestas HTTP, el aislamiento por salón y los permisos.
- Ejecutar las pruebas correspondientes antes de cerrar una fase.

## 10. Control de versiones

- Realizar commits por etapas verificadas.
- No realizar commits ni push sin revisión y aprobación del desarrollador.
- Conservar intacta la carpeta .github.
- No subir archivos privados, contraseñas ni credenciales reales.
- Mantener un único Pull Request para la entrega oficial.
- Registrar los usos importantes de IA en BITACORA-IA.md.

## 11. Arquitectura y convenciones de código

- Respetar la arquitectura Controller → Service → Repository.
- Los controladores no contienen lógica de negocio.
- Utilizar DTOs como record dentro del paquete dto de cada dominio.
- Nunca exponer directamente entidades JPA mediante la API.
- Utilizar un mapper propio por dominio, anotado con @Component.
- No incorporar librerías externas de mapeo.
- Utilizar inyección de dependencias por constructor.
- No utilizar @Autowired sobre atributos.
- Persistir las enumeraciones mediante @Enumerated(EnumType.STRING).
- Utilizar excepciones propias y @RestControllerAdvice en compartido.
- Utilizar nombres en español para clases, métodos y variables.
- No utilizar Lombok; implementar getters y setters explícitos.
- Validar los DTOs de entrada con Jakarta Validation.

## 12. Reglas adicionales de pruebas

- Los tests unitarios de Service deben utilizar JUnit 5 y Mockito sin levantar el contexto de Spring.
- Cuando falle una prueba, explicar si la causa se encuentra en el código o en el test.
- Esperar la decisión del desarrollador antes de realizar correcciones.
- No modificar lo que una prueba comprueba para conseguir que pase.
- Si un cambio autorizado del contrato obliga a ajustar los datos de preparación de una prueba, solicitar aprobación y conservar sus verificaciones.

## 13. Implementación de proveedores de IA

- Los proveedores de inteligencia artificial implementan la interfaz ProveedorIA dentro del paquete ia.
- Utilizar RestClient para realizar las solicitudes HTTP.
- Antes de utilizar endpoints, headers, parámetros o modelos externos, verificar la documentación oficial vigente.
- Los prompts deben almacenarse en src/main/resources/prompts/*.txt.
- Cargar los prompts mediante BibliotecaPrompts.
- No incorporar prompts directamente dentro de las clases Java.
- No modificar archivos de prompts sin autorización.
- El mensaje del cliente debe enviarse como mensaje, separado de las instrucciones del sistema.
- Procesar y validar las respuestas JSON mediante LectorJsonIA.
- Java realiza los cálculos de fechas, horarios y dinero.
- No registrar en logs mensajes privados de clientes ni credenciales.

## 14. Límites de actuación del agente

- No modificar application.properties sin autorización expresa.
- No crear archivos application.yml ni utilizar perfiles de configuración.
- No leer secrets.properties ni archivos que contengan credenciales privadas.
- No agregar dependencias a pom.xml sin autorización.
- No cambiar reglas de negocio para conseguir que una prueba pase.
- Ante cualquier ambigüedad de SPEC.md, solicitar una decisión al desarrollador.
- No modificar archivos previamente aprobados fuera del alcance autorizado.
