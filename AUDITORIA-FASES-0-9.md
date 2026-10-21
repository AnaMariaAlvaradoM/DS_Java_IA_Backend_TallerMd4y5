# Auditoría contra los documentos: fases 0 a 9

Fecha: 2026-10-21, America/Bogota. Código revisado: commit 493e7c3. Esta revisión no declara la entrega completa ni reemplaza el PDF.

## Resultado

**Actualización tras las correcciones del 2026-10-21:** A01–A03 resueltos y 276 pruebas aprobadas. Se comprobó también el arranque completo sin secrets.properties. El diagnóstico inicial y su matriz se conservan abajo como evidencia histórica; el estado vigente se detalla en «Cierre de hallazgos».

La implementación principal de las fases 0–9 tiene evidencia local y la suite se ejecutó de nuevo: **257 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**. Sin embargo, no corresponde declarar cumplimiento total sin reservas: falta una operación del contrato y se reprodujeron dos desajustes de validación que la suite actual no detecta.

No se modificó lógica productiva ni se avanzó a la fase 10 durante la auditoría. Las comprobaciones HTTP utilizaron datos ficticios; los archivos de IDE ajenos se conservaron sin cambios.

## Fuentes y alcance

- PDF original proporcionado por el estudiante: 01M4YHCY154CAVZRDT1TYT9QM2.pdf, 40 páginas. Se revisaron modelo, roles, reglas y contrato (páginas 4–12), preparación y fases 0–9 (14–26), casos mínimos (26–27) y escenarios de referencia (31–32).
- README.md del repositorio y copia proporcionada en Downloads: conservan el mismo contenido de guía al normalizar finales de línea.
- Código en src/main/java, DTOs, repositorios, configuración, prompts y tests en src/test/java.
- Historial local desde c248ec1 y evidencias previas de VERIFICACION.md, distinguidas de las pruebas nuevas de esta revisión.
- OpenAPI de la API en ejecución en localhost:8080, metadatos públicos de GitHub y consulta de precios configurados en Stripe, sin efectuar compras.

## Hallazgos que deben resolverse

### A01 — Falta GET /api/admin/salones

**Referencia:** PDF páginas 6 y 12: ADMIN debe poder listar todos los salones, con HTTP 200; los demás roles deben recibir 403.

**Evidencia:** OpenAPI publicó **24 operaciones**, frente a las **25 operaciones** del contrato. No existe controlador/servicio que implemente GET /api/admin/salones. ConfiguracionSeguridad.java:48 protege /api/admin/**, pero proteger la URL no implementa el listado.

**Por qué los tests pasan:** AuthIntegracionTest comprueba que el dueño recibe 403; no comprueba el caso positivo de un ADMIN que recibe 200 y los salones. La seguridad puede devolver 403 antes de descubrir que no existe un controlador.

**Acción requerida:** implementar el listado administrativo con su DTO y prueba positiva de ADMIN, más las negativas por rol. Esta ruta aparece en el contrato general aunque el paso a paso no le dedica una fase de implementación específica; no debe perderse entre las fases posteriores.

### A02 — Contraseñas Unicode admitidas por el DTO producen HTTP 500

**Referencia:** PDF página 10: contraseña de 8 a 72 en registro y alta de estilistas. El DTO expresa ese intervalo como caracteres.

**Reproducción:** contraseña ficticia formada por 37 letras «á»; son 37 caracteres y 74 bytes UTF-8. POST /api/auth/registro devolvió **500**. POST /api/estilistas también devolvió **500**, con los demás datos válidos y por debajo del límite GRATIS.

**Causa confirmada:** los DTOs usan @Size(min=8,max=72), mientras BCryptPasswordEncoder rechaza más de 72 bytes. El log mostró IllegalArgumentException con el límite de bytes; el manejador termina respondiendo error interno. Puntos revisados: RegistroRequest.java:15, AuthService.java:57, EstilistaRequest.java:14 y EstilistaService.java:57.

**Acción requerida:** acordar cómo representar el límite de BCrypt en el contrato de entrada y responder de forma controlada; añadir casos ASCII y Unicode de frontera. No se decidió silenciosamente cambiar 72 caracteres por 72 bytes ni sustituir BCrypt, porque ambas decisiones deben respetar el documento.

### A03 — Nombre del cliente sin límite contractual choca con la columna de base de datos

**Referencia:** PDF página 11: POST /api/citas y reserva pública reciben clienteNombre sin máximo declarado.

**Reproducción válida:** cita futura dentro del horario, con servicio y estilista del salón; clienteNombre con 256 letras. POST /api/citas devolvió **409** con el mensaje genérico de conflicto con un registro existente. El log confirmó Data too long for column 'nombre'.

**Causa:** CitaRequest.java:10 exige nombre no vacío, sin máximo; Cliente.nombre usa una columna String sin longitud explícita, cuyo límite efectivo en MySQL es 255. Un exceso de longitud llega al guardado y se presenta como conflicto de unicidad. La reserva pública comparte el mismo flujo y queda afectada por el mismo desajuste, aunque esta reproducción HTTP concreta se hizo sobre la ruta privada.

**Acción requerida:** alinear almacenamiento y validación. El PDF no autoriza un máximo de nombre para esta entrada: ampliar almacenamiento o añadir un límite de entrada requiere una decisión explícita, evitando inventar una regla. Añadir prueba de regresión al flujo compartido.

Las primeras preparaciones de esta prueba devolvieron 400 por identificadores no escalares y 404 por ausencia de estilista; se descartaron como errores de preparación. El hallazgo anterior se basa en la petición corregida con ids numéricos y un estilista real del salón ficticio, no en aquellas respuestas.

## Matriz de fases

| Fase | Requisitos contrastados | Resultado y evidencia |
|---|---|---|
| 0 — Preparación | Raíz Maven/Wrapper, Java 17/Boot 4.1.1, dependencias aprobadas, propiedades, secretos, SPEC/AGENTS/bitácora e historial | Base local conforme por inspección y tests. Todas las variables requeridas por README están mapeadas con valores de relleno. .github/ sin diferencias desde c248ec1. secretos ignorados y sin secrets.properties en archivos/historial versionados. Publicación/PR pendientes; arranque completo sin archivo de secretos no se volvió a demostrar de forma aislada |
| 1 — Base SaaS | Error de cuatro campos, excepciones HTTP, Clock, entidades/embebido, enums, slugs con sufijos y relaciones | Conforme en las comprobaciones locales. ManejadorGlobalErroresTest y GeneradorSlugTest, más contexto MySQL. Fechas de negocio/auditoría reciben Clock con America/Bogota |
| 2 — Seguridad | Principal con salón/rol, JWT firmado, usuario activo recargado, BCrypt, registro transaccional, login genérico, permisos, mi salón/configuración | Flujo ordinario y roles conformes en tests. **A02** impide cerrar validación sin observaciones. **A01** afecta al permiso administrativo positivo del contrato general |
| 3 — Catálogo/equipo/clientes | Servicios y validaciones, filtrado id/salón, límite/correo/servicios en ese orden, relación muchos a muchos, cliente por teléfono normalizado | Flujo ordinario conforme; CatalogoEquipoIntegracionTest y entidades revisadas. **A02** afecta al alta de estilistas; **A03** al almacenamiento posterior del cliente |
| 4 — Agenda | Estados, auditoría, precio histórico, horario, cruces y contigüidad, orden de agendar, dueño/estilista, cambios con saveAndFlush | Conforme para los casos del documento y cubierto por EstadoCitaTest, ReglasAgendaTest, CitaServiceTest y AgendaIntegracionTest. **A03** deja un caso de entrada sin resolver |
| 5 — Público | Catálogo sin correos/contraseñas, activos, disponibilidad por intervalos, misma agenda para reserva PENDIENTE/EN_LINEA | PublicoIntegracionTest comprueba ejemplos del PDF, aislamiento y actualización de horarios. **A03** es compartido con la reserva pública |
| 6 — Stripe | Precios, configuración tipada/base configurable, metadata y URLs, PRO repetido 409, firma, idempotencia, transacción y cancelación | Conforme en pruebas locales y evidencias manuales registradas. En esta auditoría se consultaron los precios configurados: PRO 1500 centavos USD cada mes, intervalo 1; paquete 500 centavos USD no recurrente; ambos modo prueba. Sin compras nuevas |
| 7 — Motor IA/créditos | Paquete Clase 19 reutilizado, RestClient/timeouts, URLs configurables, Gemini/Claude, fallback, prompts/JSON/factura, actualización atómica de créditos | Conforme en pruebas de HTTP simulado, fallback y MySQL, incluida concurrencia. Lector JSON conserva la tolerancia a bloques Markdown del código reutilizado; no equivale a un validador estricto del texto completo. Timeout configurado por inspección, sin ensayo de latencia deliberada |
| 8 — Recordatorio | Seis datos de Java, es-CO/espacios normales/precio histórico, prompt aprobado, estados, orden de crédito y validación mensaje 700 | Conforme en DatosRecordatorioTest, RecordatorioServiceTest y RecordatorioIntegracionTest. Verificación histórica con Gemini y fallo con llave ficticia en VERIFICACION.md; no se repitieron llamadas IA reales durante esta auditoría |
| 9 — Chat/solicitudes | Catálogo real, cinco variables, historial separado, JSON tipado, siete términos anti-promesas, solicitud única y prioridad, mensajes cliente 2000, listado/atención/permisos | Conforme en ReglaAntiPromesasTest, ChatServiceTest, SolicitudServiceTest y ChatIntegracionTest. Verificación manual histórica con Gemini documentada; la respuesta insegura se fuerza en tests, sin atribuir a Gemini una promesa que no produjo |

## Excepciones autorizadas y límites de evidencia

- **JUnit:** el PDF menciona JUnit 5. Por instrucción expresa del estudiante se usa la versión transitiva de spring-boot-starter-webmvc-test, **JUnit 6.0.3**, gestionada por Boot. La incompatibilidad encontrada con JUnit 5.14.4 y la decisión están en DEPENDENCIAS.md. Es una excepción autorizada por el estudiante, no una prueba de aprobación de la instructora ni cumplimiento literal de «JUnit 5».
- Se conservaron las aclaraciones ya aprobadas sobre JWT, campos obligatorios, slugs/teléfonos inválidos, representación de disponibilidad, metadata/procesado de Stripe, respuesta de resultado, respaldo Claude y respuestas/validación de solicitudes. No se transformaron nuevas ambigüedades en reglas durante esta revisión.
- **Arranque sin secrets.properties:** el import es opcional y los secretos externos tienen rellenos; el contexto normal con MySQL pasa. No se afirma una nueva prueba de arranque completo sin archivo y con credenciales de base de datos suministradas externamente; queda una comprobación explícita pendiente del paso 0.3.
- Las 257 pruebas son de la aplicación, no la colección oficial ni una nota académica. No existe evidencia nueva del calificador remoto ni de producción.
- La inspección de secretos se limitó a rutas ignoradas/versionadas e historial de secrets.properties; no se leyeron secretos reales ni se afirma un escaneo exhaustivo de credenciales en todo el historial.

## Fork, PR y entrega

Consulta de GitHub en esta auditoría: giovanni104/DS_Java_IA_Backend_TallerMd4y5 **sí es fork** de [AnaMariaAlvaradoM/DS_Java_IA_Backend_TallerMd4y5](https://github.com/AnaMariaAlvaradoM/DS_Java_IA_Backend_TallerMd4y5). La consulta de PR con estado abierto/cerrado y head giovanni104:main devolvió **0**. No se creó PR ni se hizo push.

El README de guía exige un solo PR y el paso 0 indica abrirlo después del primer push. Falta materializar esa evidencia de entrega; esto es independiente de que los tests locales pasen. El historial sí contiene commits por fase, en lugar de un único commit final. No se fabricó historial por cada subpaso retrospectivamente.

Swagger con título/seguridad/etiquetas, Docker, Render/Supabase, colección Postman propia, entornos, README final y evidencias de entrega corresponden a las fases 11–13. No se señalan como defectos de las funciones 1–9, pero siguen pendientes para la entrega completa. El contrato administrativo A01 sí debe implementarse, aunque no tenga un paso separado en la guía.

## Evidencia de esta ejecución

- target/auditoria-fases0-9-tests.log: 257 pruebas y BUILD SUCCESS del 2026-10-21.
- OpenAPI de localhost:8080/v3/api-docs: 24 operaciones; falta GET /api/admin/salones.
- Pruebas HTTP ficticias: registro Unicode 500; estilista Unicode 500; cliente con nombre de 256 caracteres 409, con causa de longitud confirmada.
- target/auditoria-precios-stripe.log: consulta de importes/periodicidad/modo de los precios configurados. Programa temporal target/AuditarPreciosStripe.java, independiente de la suite y sin imprimir llaves.
- git diff c248ec1 -- .github: sin diferencias. git check-ignore secrets.properties: ignorado; git ls-files e historial de esa ruta sin archivo versionado.

La conclusión inicial fue cumplimiento principal con tres hallazgos pendientes y una excepción de JUnit documentada. Las correcciones posteriores se registran a continuación; la publicación/PR y los entregables de fases posteriores siguen pendientes.

## Cierre de hallazgos

Fecha: 2026-10-21. Se corrigieron los hallazgos antes de avanzar a fase 10.

| Hallazgo o comprobación | Corrección y evidencia |
|---|---|
| A01: administración | GET /api/admin/salones delega en SalonService y devuelve DTOs dentro de transacción de lectura. Prueba positiva ADMIN con todos los salones; DUENO/ESTILISTA reciben 403 y anónimo 401. No expone contraseñas ni identificadores Stripe |
| A02: BCrypt | El usuario confirmó mantener BCrypt y rechazar más de 72 bytes UTF-8 con HTTP 400 y mensaje claro. Restricción compartida en registro y alta de estilistas; se conserva mínimo de 8 caracteres. Fronteras ASCII, acentuadas y mixtas: exactamente 72 bytes aceptados y login válido; exceso rechazado sin altas parciales. Login inválido conserva 401 genérico |
| A03: clienteNombre | Cliente.nombre usa TEXT sin inventar un máximo de entrada. Pruebas privadas y públicas guardan nombres acentuados de 256 caracteres con HTTP 201. Para bases MySQL existentes se ejecutó scripts/001-clientes-nombre-text.mysql.sql: columna TEXT confirmada y el único registro previo conservado. En una base nueva Hibernate crea TEXT directamente |
| Arranque sin secretos | Contexto completo iniciado desde directorio sin secrets.properties, variables externas de proveedores retiradas y valores de relleno comprobados. Base y usuario MySQL temporales con credenciales ficticias; ambos eliminados al terminar. Sin llamadas a Stripe/Gemini ni cambios en datos de negocio |

Suite final: **276 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS** en target/correcciones-auditoria-final.log. Las 19 regresiones nuevas están en CorreccionesAuditoriaIntegracionTest. La primera ejecución detectó que ddl-auto=update no ampliaba la columna existente: se aplicó la migración explícita y se repitió la suite sin modificar expectativas.

Evidencia adicional: target/migracion-cliente-nombre.log, target/verificacion-arranque-sin-secretos.log y su log hijo. El primer intento de comprobación aislada falló por construir una ruta Windows con asterisco; se corrigió el programa temporal y la repetición confirmó arranque y limpieza. No fue un fallo de la aplicación.

La excepción autorizada de JUnit 6 continúa documentada. Este cierre no acredita calificador remoto, producción ni entrega completa.
