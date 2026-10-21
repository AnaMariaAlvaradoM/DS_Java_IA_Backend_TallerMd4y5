# Verificación por fase contra el enunciado

Este registro distingue inspección del código, pruebas locales y comprobaciones externas pendientes. Los resultados corresponden a la versión que se cierra en cada fase, no a la nota del calificador.

Fuente: PDF de requerimientos entregado por el usuario, 40 páginas. Las referencias de página siguientes corresponden a ese PDF. Las aclaraciones del usuario se registran aparte en SPEC.md.

## Fase 10: pruebas automáticas

Fecha: 2026-10-21. El estudiante aprobó conservar solo los casos mínimos de las páginas 26–27. Suite actual: **70 pruebas aprobadas, sin fallos, errores ni omisiones**, BUILD SUCCESS en target/fase10-tests.log. Matriz de todos los casos y límites en TESTS-ENTREGA.md. Servicios probados sin Spring, repositorios/IA simulados, Clock fijo y ninguna llamada real a Gemini/Stripe; se conserva el test de contexto completo con MySQL. Se retiraron pruebas adicionales, incluidas regresiones anteriores, sin modificar lógica productiva. Las cifras históricas de las fases siguientes no representan el tamaño actual de la suite. JUnit gestionado por Boot conserva la excepción autorizada documentada.

## Paso 12.1: imagen Docker — fase 12 en curso

Fecha: 2026-10-21. Referencia: PDF página 28. Dockerfile de dos etapas: maven:3.9.16-eclipse-temurin-17 compila con mvn -B -DskipTests package; eclipse-temurin:17-jre ejecuta únicamente el JAR. Imágenes oficiales Maven y Eclipse Temurin. .dockerignore excluye target, .git y secrets.properties, además de archivos de secretos y material local.

Construcción real con docker build: aprobada, sin ejecutar tests como exige el paso. Log local target/paso12-docker-build.log. Contenedor comprobado con Java 17.0.20.1, sin mvn, javac, /app/target ni /app/secrets.properties. Prueba aislada con PostgreSQL 17 temporal y credenciales ficticias: PORT=9090 respetado (mapeado solo en localhost:18080), OpenAPI Sillón API accesible, registro 201, login 200 y consulta autenticada del salón 200. Evidencias: target/paso12-docker-arranque.log y target/paso12-docker-verificacion.log. Contenedores de prueba y red retirados al terminar; no se usó Supabase ni se enviaron llamadas a Stripe/Gemini.

El estudiante creó el proyecto Supabase y confirmó los datos de Session pooler: puerto 5432 y base postgres. No se recibió ni leyó su contraseña. Aún pendientes: conexión real, servicio Render, variables, webhook de destino Render, pago de prueba y URL real en README. No se declara cerrada la fase 12 ni se usa todavía el mensaje de commit de despliegue completo.

## Fase 11: Swagger

Fecha: 2026-10-21. Referencia: PDF página 27, fase 11. Plan revisado y aprobado por el estudiante antes de implementar.

| Requisito | Cumplimiento y evidencia |
|---|---|
| Swagger UI en /swagger-ui.html con título Sillón API | La ruta abre /swagger-ui/index.html. OpenAPI y encabezado visible muestran Sillón API, versión 1.0. ConfiguracionOpenApi y propiedad springdoc.swagger-ui.path |
| Bearer JWT y Authorize | Esquema bearerAuth de tipo http, scheme bearer, bearerFormat JWT. Controladores privados con SecurityRequirement; en PagoController solo suscripción/créditos lo declaran. Authorize mostró Authorized al aplicar el token ficticio de dueño |
| Etiquetas numeradas | Verificadas en pantalla y OpenAPI: 0 Público, 1 Autenticación, 2 Mi salón, 3 Servicios, 4 Estilistas, 5 Clientes, 6 Agenda, 7 Pagos, 8 Solicitudes, 9 Administración; orden alfabético configurado. Chat comparte Público y recordatorio comparte Agenda |
| Swagger muestra 22 rutas | OpenAPI contiene exactamente 22 paths distintos y 25 operaciones HTTP. La cifra del PDF corresponde a rutas, no al total de métodos; no fue necesario retirar endpoints |
| GET /api/salon con token de dueño desde Swagger responde 200 | Se ejecutó login del dueño ficticio desde Swagger, se aplicó su token en Authorize y se ejecutó GET /api/salon con Try it out/Execute. Server response: 200 y DTO del salón 949. Sin modificar sus datos |

Verificación local de seguridad OpenAPI para las 25 operaciones: cero discrepancias entre rutas públicas y privadas. Inventario ignorado target/fase11-operaciones.json. Captura local de resultado sin credenciales: target/fase11-swagger-200.png. Suite de entrega: **70 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS** en target/fase11-tests.log. Empaquetado aprobado en target/fase11-package.log y API reiniciada con ese JAR. Sin dependencias nuevas ni cambios a permisos de ejecución. No se hicieron llamadas a IA/Stripe ni se ejecutó calificador remoto o producción.

**Auditoría transversal del 2026-10-21:** la revisión inicial identificó tres hallazgos pese a aprobar 257 pruebas. Los tres se corrigieron antes de fase 10: ruta administrativa, contraseñas Unicode y almacenamiento de nombres largos. Suite final: 276 pruebas, sin fallos, errores ni omisiones. Arranque completo sin secrets.properties verificado en base temporal y limpieza confirmada. API reempaquetada: OpenAPI expone 25 operaciones, incluida GET /api/admin/salones. Evidencias, migración MySQL y límites en «Cierre de hallazgos» de AUDITORIA-FASES-0-9.md. La excepción de JUnit y entrega remota siguen documentadas; no se declara la entrega completa.

## Fase 4: agenda

Fecha de revisión: 2026-10-19. Referencias: páginas 7–8 (reglas), 11 (contrato), 19–20 (pasos de construcción) y 26 (casos mínimos de pruebas).

### Pasos de construcción

| Paso del PDF | Cumplimiento revisado | Evidencia |
|---|---|---|
| 4.1 | Tabla de transiciones en SPEC antes del enum; método permite y método ocupaAgenda; enum OrigenCita | EstadoCitaTest: 25 combinaciones de estados y 5 comprobaciones de ocupación |
| 4.2 | Cita con relaciones a Salon, Cliente, Usuario y Servicio; precio copiado; origen; auditoría de creación/actualización con Clock | Inspección Cita/CitaService; AgendaIntegracionTest.creaCitaConContratoCompletoYPrecioHistorico y confirmarReservaActualizaFechaEnLaRespuesta |
| 4.3 | ReglasAgenda sin repositorios: mismo día, horario, domingos y cálculo en intervalos; errores de horario describen el horario | ReglasAgendaTest: 10 casos, incluidos límites, cierre, apertura, cambio de día, domingo, cruces, estados finales y hora de Colombia |
| 4.4 | Un método privado agendar compartido por crear y reservar. Orden: servicio existente/activo; estilista del salón/activo/ofrece servicio; fin; pasado; horario; cruce; cliente/precio/guardado | Inspección CitaService; CitaServiceTest: casos de cita válida y rechazos sin guardar; mocks verifican que no se crea cliente cuando fallan reglas anteriores |
| 4.4 | Cruces consultados por método derivado, filtrando salón, estilista y solo PENDIENTE/CONFIRMADA; extremos contiguos permitidos | CitaRepository y ReglasAgenda; pruebas unitarias e integración de cruce y contigüidad |
| 4.5 | Agenda del día ordenada; dueño ve salón y estilista solo sus citas; fecha omitida usa hoy de Colombia | AgendaIntegracionTest y CitaServiceTest.agendaSinFechaUsaHoyDeColombiaYFiltraEstilista, con reloj en un instante cuyo día UTC difiere del colombiano |
| 4.5 | Cambio busca por id/salón; estilista ajeno 404; destino no permitido 403; transición inválida 409; guardar con saveAndFlush | CitaServiceTest y AgendaIntegracionTest; actualizar fecha se verifica sobre el objeto de respuesta HTTP |

### Comprobaciones expresas de la página 20

| Escenario | Evidencia local |
|---|---|
| Sara a las 10:30 crea CONFIRMADA | AgendaIntegracionTest.creaCitaConContratoCompletoYPrecioHistorico, HTTP 201 |
| Barba con Sara produce 409 | AgendaIntegracionTest.rechazaLasReglasDeHorarioYServicio, caso servicio |
| Corte 18:45 que termina después del cierre produce 409 | Mismo test, caso cierre |
| Cita en el pasado produce 409 | Mismo test, caso pasado; unitario comprueba ausencia de guardado/cliente |
| Domingo cerrado produce 409 | Mismo test, caso domingo; ReglasAgendaTest verifica también ausencia de disponibilidad |
| Confirmar reserva PENDIENTE devuelve CONFIRMADA y actualización posterior a creación | AgendaIntegracionTest.confirmarReservaActualizaFechaEnLaRespuesta |
| CONFIRMADA a PENDIENTE es inválida | EstadoCitaTest cubre ese par; CitaService valida permite y traduce transición inválida a 409, probado por CitaServiceTest. Este par concreto está probado en el enum, no como petición HTTP específica |
| Mateo ve solo sus citas | AgendaIntegracionTest.estilistaSoloVeSusCitasYCambiaLasSuyas |
| Mateo intenta cambiar cita de Sara: 404 | Mismo test |
| Mateo intenta cancelar su cita: 403 | AgendaIntegracionTest.estilistaNoPuedeCancelarSuCita |
| Mateo completa su cita: 200 | AgendaIntegracionTest.estilistaCompletaSuCitaYNoPuedeVolverAPendiente |

### Contrato y reglas adicionales

- POST /api/citas: 201 y los 15 campos del contrato; fin calculado y precio histórico. Datos obligatorios, teléfono 7–30 y nota máximo 300 declarados en CitaRequest. El test HTTP comprueba el número de campos y datos clave; los límites de DTO se revisaron también en código.
- GET /api/citas?fecha=: fecha opcional y filtro del principal, sin aceptar salonId del cuerpo o URL.
- PATCH /api/citas/{id}/estado: enum obligatorio; errores de JSON/validación usan el manejador común.
- Recursos de otro salón: consulta por pertenencia y prueba de agenda vacía/cambio 404.
- Teléfono conserva solo dígitos y +; se reutiliza ClienteService de fase 3. Por aclaración del usuario, teléfono sin dígitos produce 400, probado por HTTP.
- No se agregó una restricción de alineación de citas con la cuadrícula: el documento exige la cuadrícula para disponibilidad, no expresa ese rechazo para POST.
- Se bloquea el estilista al agendar y se consulta el cruce con bloqueo en la misma transacción. Se revisó esta protección en código; no se ejecutó una prueba de carga concurrente.

### Alcance pendiente

- Fase 5: exponer catálogo, disponibilidad y reservas públicas. Aquí solo están el cálculo Java y el método compartido, sin esos endpoints públicos.
- Fase 8: endpoint de recordatorio; no pertenece al cierre de fase 4.
- Postman y calificador oficial: pendientes. La verificación HTTP de esta fase utiliza MockMvc con seguridad real y MySQL; los tests unitarios de servicios usan mocks y reloj fijo.
- No se modificó .github/ ni se versionaron secretos. No se hizo push.

### Resultado de ejecución

Maven Wrapper con Java 17 y MySQL: **119 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**. De ellas, 67 corresponden a la agenda incorporada en esta fase: 30 de estados, 10 de reglas, 12 del servicio y 15 de integración HTTP. Evidencia local: target/fase4-final.log y target/surefire-fase4-final/ (ignorados por Git).

## Fase 5: catálogo y reservas públicas

Fecha de revisión: 2026-10-19. Referencias: página 11 (contrato) y páginas 20–21 (pasos 5.1/5.2 y COMPRUEBAS).

| Requisito del PDF | Resultado revisado | Evidencia |
|---|---|---|
| 5.1: catálogo por slug con datos públicos y horario descriptivo | CatalogoResponse contiene nombre, slug, ciudad, direccion, telefono, horario, servicios y estilistas; el horario se redacta con formato es-CO y espacios normales | Inspección de DTO/FormatoHorarioSalon; PublicoIntegracionTest.catalogoPublicoNoExigeTokenNiRevelaCorreos comprueba el horario del ejemplo |
| Servicios y estilistas activos; sin correos ni contraseñas | Consultas filtradas por salón/activo/rol, DTO público del estilista con id/nombre/servicioIds | catalogoPublicoNoExigeTokenNiRevelaCorreos y excluyeServiciosYEstilistasInactivos |
| Catálogo del escenario: dos servicios y dos estilistas; ninguna arroba | HTTP 200 sin token, listas de tamaño 2, texto sin @ | catalogoPublicoNoExigeTokenNiRevelaCorreos |
| 5.2: disponibilidad trae las citas ocupadas del estilista del día | Consulta derivada filtra salón, estilista, día y PENDIENTE/CONFIRMADA; ReglasAgenda calcula las horas | Inspección de CitaRepository/CatalogoService y reservaActualizaDisponibilidadYBloqueaCitaInterna |
| Disponibilidad exige servicioId, estilistaId y fecha; JSON con fecha/servicio/estilista/duracionMinutos/horariosLibres | Parámetros @RequestParam obligatorios; nombres exactos en DTO; horas HH:mm:ss | Inspección PublicoController/DisponibilidadResponse; faltanParametrosEs400, fechaInvalidaEs400 y comprobaciones del flujo de reserva |
| Valentina reserva con Mateo a las 10:00 | HTTP 201, PENDIENTE, EN_LINEA, fin 10:45, precio 35000, sin autenticación | reservaActualizaDisponibilidadYBloqueaCitaInterna |
| La disponibilidad pierde 09:30, 10:00 y 10:30 y conserva 11:00 | Verificado por HTTP después de la reserva | Mismo test |
| Cita interna con Mateo a las 10:30 produce 409 | Verificado por HTTP con token del dueño tras la reserva pública | Mismo test |
| Reserva usa el mismo método de agendar de fase 4 | CatalogoService.reservar delega a CitaService.reservar, que llama al método privado agendar | Inspección de código y cruce entre cita pública/interna |
| Salón inexistente o ids de otro salón no se revelan | 404 para slug inexistente e ids ajenos en disponibilidad | salonInexistenteEs404SinToken e idsDeOtroSalonSon404 (servicio y estilista) |

Comprobaciones adicionales: cancelar una reserva libera su horario; domingo cerrado devuelve lista vacía; estilista sin el servicio solicitado devuelve 409. La exclusión de horas pasadas y los límites de horario continúan cubiertos por ReglasAgendaTest de fase 4.

Dos puntos abiertos del contrato se resolvieron con el usuario antes de implementar sus DTOs: servicio/estilista son nombres como texto; los servicioIds públicos contienen solo servicios activos. Ambos se comprueban en PublicoIntegracionTest.

Resultado: **130 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**, incluidas 11 nuevas de esta fase. Evidencia local: target/fase5-tests.log y target/surefire-fase5/. Verificación HTTP mediante MockMvc con filtros de seguridad y MySQL; datos de prueba revertidos. Postman y calificador remoto siguen pendientes. El endpoint público de chat se construirá en fase 9; no forma parte de la fase 5. Sin cambios en .github/ ni secretos versionados; sin push.

## Fase 6: Stripe verificada

Contraste con pasos 6.1–6.4 de páginas 21–22 y contrato de pagos de página 12. Productos y precios configurados por el estudiante; Checkout en modo prueba comprobado con pagos manuales.

| Comprobación | Evidencia y estado |
|---|---|
| Suite anterior y nuevas pruebas de pagos | 148 pruebas aprobadas; BUILD SUCCESS en target/fase6-final.log. SDK probado contra servidor HTTP local; integración de webhook con firmas ficticias y MySQL, sin llamadas externas en tests |
| Checkout PRO y paquete | Backend construye modo, precio configurado, cantidad, referencia del salón, correo y metadata salonId/tipoCompra; comprobado por PagoServiceTest |
| Firma y transacción | Pruebas HTTP de firma ausente/inválida; compra y evento en la misma transacción, con rollback real al fallar el registro del evento |
| Idempotencia | Evento firmado repetido devuelve procesado=false sin duplicar saldo, probado en integración. Reenvío real con Stripe CLI events resend del evento del paquete: dos entregas del mismo id con HTTP 200 y saldo sin cambios en 510. La CLI muestra el código HTTP, no el cuerpo de respuesta; procesado=false se comprueba mediante MockMvc |
| Compatibilidad de evento | Fallback deserializeUnsafe cuando el SDK no entrega el objeto; probado con versión de evento distinta |
| Pago PRO real en modo de prueba | Salón ficticio 380 pasó de GRATIS/10 a PRO/310, comprobado mediante GET /api/salon después de Checkout |
| Tercer estilista en PRO | Creado mediante POST /api/estilistas después del pago |
| Intento de comprar PRO estando en PRO | POST /api/pagos/suscripcion devolvió HTTP 409 |
| Paquete de 200 créditos real en modo de prueba | Pago manual completado; GET /api/salon comprobó PRO/510 |
| Cancelación de suscripción | Probada en integración y con cancelación real de la suscripción ficticia en Stripe: estado canceled y GET /api/salon GRATIS/510 |
| Webhook sin firma y pago como estilista | Peticiones contra la API local devolvieron HTTP 400 y 403 respectivamente; también cubiertas en integración |
| Resultado público | JSON mensaje para exito/cancelado; estado distinto produce 400, conforme a aclaración del usuario. No confirma compras ni cambia créditos |

Resultado: **148 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS** y JAR generado. Evidencia local: target/fase6-final.log; sesiones y tokens de prueba solo en target ignorado. Listener Docker verificado con destino /api/pagos/webhook y eventos checkout.session.completed, customer.subscription.deleted. La suite usa JUnit transitivo gestionado por Boot por instrucción del usuario. Colección Postman y calificador remoto pendientes de sus fases; no se atribuyen resultados a ellos.

Aclaraciones del usuario: metadata salonId/tipoCompra, valores SUSCRIPCION_PRO/PAQUETE_CREDITOS; resultado con campo mensaje e inválido 400; procesado indica registro nuevo, incluso si el evento no aplica compra. Un id ya registrado devuelve false. No se añadieron recargas por renovación ni desactivación de estilistas al volver a GRATIS.

Incidencias locales: el listener inicial enviaba a /api/stripe/webhook y recibió 401; se corrigió a /api/pagos/webhook y se reinició la aplicación con el secreto correspondiente, sin exponerlo. La versión instalada de Stripe CLI exige --events; se seleccionaron checkout.session.completed y customer.subscription.deleted. El primer enlace comunicado omitía el fragmento final de Checkout; se corrigió entregando la URL completa. El primer pago sin entrega válida no se contabilizó como verificación exitosa.

## Fase 7: motor de IA y créditos verificados

Contraste con pasos 7.1–7.2 y reglas de página 23 del PDF. Código reutilizado de la ruta proporcionada por el estudiante: Modulo5/Clase3_19/src/main/java/com/devsenior/atiende/ia. El proyecto de origen solo se leyó. Se conservaron las clases y subpaquetes del motor; los dominios ticket, pedido y chat de Atiende no se trasladaron.

| Requisito | Implementación y evidencia |
|---|---|
| Interfaz y dos proveedores por RestClient, sin SDK ni Spring AI | ProveedorIA, openai/ProveedorOpenAi, claude/ProveedorClaude y sus formatos reutilizados. ProveedoresHttpTest prueba peticiones/respuestas contra un servidor HTTP local, incluida separación system/user y headers propios de cada formato |
| Asistente con fallback | AsistenteIA reutilizado y orden @Order(1)/@Order(2). AsistenteIATest cubre éxito principal, éxito de respaldo y ambos fallidos; ProveedorIAExceptionTest cubre clasificación de errores. Respuesta HTTP ilegible/sin mensaje también permite intentar el respaldo |
| Configuración tipada sillon.ia y llave de Gemini mediante variable con relleno | IaProperties registrada con ConfiguracionIa; GEMINI_LLAVE con REEMPLAZAR por defecto. Se conserva la importación local opcional de secrets.properties. Configuración protegida frente a impresión accidental de la llave mediante toString |
| URL OpenAI configurable, sin endpoint fijo en Java | Proveedor usa exactamente config.url(); application.properties conserva sillon.ia.openai.url y SILLON_IA_OPENAI_URL. Test HTTP usa /ruta-simulada, no /chat/completions, y comprueba recepción correcta |
| Respaldo configurado aunque no tenga llave | Por aclaración del estudiante, API Claude sin llave en lugar de Ollama local del origen. Falta de llave produce error de configuración al intentar completar; AsistenteIA lo registra y, agotados proveedores, produce 503. ProveedoresHttpTest comprueba ausencia de HTTP con relleno y fallback agotado |
| Timeout HTTP | FabricaHttp reutilizada: tiempo máximo de conexión y lectura configurado por sillon.ia.timeout-segundos; inspección de fábrica y construcción de ambos proveedores. No se ejecutó una prueba de latencia deliberada |
| Biblioteca de prompts, lector JSON y factura | BibliotecaPrompts, LectorJsonIA y dto/FacturaIA reutilizados; LectorJsonYPromptsTest prueba recurso de test, variables, prompt ausente, JSON con Markdown, JSON inválido y seis campos de factura. Texto inválido produce 502 sin exponer la respuesta de IA |
| Tokens, costo y duración | RespuestaIA y cálculo BigDecimal por millón reutilizados; ProveedoresHttpTest verifica contadores y costo. Tarifas configurables; costo estimado no equivale al cobro real de la cuenta |
| Verificar saldo: cero produce 402 sin descuento | CreditosService.verificar y consulta escalar por id; CreditosIntegracionTest con MySQL comprueba saldo antes/después. Salón inexistente produce 404 |
| Consumir con una sola actualización condicionada a saldo positivo y devolver restante | SalonRepository.consumirCredito usa update con creditosIa > 0. CreditosService consume y consulta saldo dentro de una transacción breve. Test de dos hilos, transacciones independientes y un único crédito: un éxito, un 402, saldo final 0 |
| Sin llamada externa dentro de transacción de base de datos | Motor y proveedores no tienen transacciones; solo verificar/consumir son transaccionales. Comprobador temporal de conexión verifica ausencia de transacción antes de llamar a Gemini. El orden completo validar negocio/verificar/cargar/llamar/consumir/leer/aplicar se integrará y probará en las funcionalidades de fases 8–9; esta fase deja los componentes preparados. Test confirma que un error JSON posterior no revierte un consumo ya confirmado |

Resultado automatizado: **182 pruebas, 0 fallos, 0 errores, 0 omitidas** en target/fase7-final.log (34 nuevas, incluidas 11 reutilizadas de Atiende). El empaquetado posterior de esa ejecución falló por JAR abierto por la API de fase 6; se detuvo aquel proceso y se ejecutó package con skipTests, sin repetir ni omitir la suite previamente aprobada: **BUILD SUCCESS** en target/fase7-package.log.

Comprobación manual independiente de la suite: el ejecutable temporal target/VerificarGemini.java cargó la configuración mediante Spring y llamó al proveedor principal con texto ficticio. Gemini gemini-3.5-flash-lite respondió con JSON válido: 33 tokens de entrada, 6 de salida; factura estimada USD 0.000025. Evidencia local target/fase7-gemini.log. No se imprimieron llaves ni se consultó/consumió saldo de un salón. Los tests automatizados no hacen llamadas externas de IA.

Incidencias de pruebas: se corrigieron dos datos de las pruebas nuevas, sin cambiar sus aserciones: ConfiguracionSalon.porDefecto para cumplir columnas obligatorias y serialización mediante JsonMapper de una respuesta HTTP ficticia para evitar comillas JSON mal escapadas. Se conservaron los logs de la ejecución fallida en target/fase7-tests.log.

Fuentes oficiales contrastadas antes de adaptar HTTP y configuración: [compatibilidad OpenAI de Gemini](https://ai.google.dev/gemini-api/docs/openai), [tarifas Gemini](https://ai.google.dev/gemini-api/docs/pricing), [modelo Gemini utilizado](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite), [formato Messages de Claude](https://platform.claude.com/docs/fr/api/http/messages/create) y [modelo de respaldo configurado](https://platform.claude.com/docs/en/models/haiku-4-5/overview). Gemini sigue siendo el proveedor principal; Claude no se probó con una llave real. No se copiaron los prompts de negocio de Atiende; los de recordatorio/chat corresponden a fases 8–9 y los escribe el estudiante conforme al PDF. Sin nuevas dependencias, cambios en .github/ ni ejecución del calificador remoto.

## Fase 8: recordatorio con IA verificado

Contraste con reglas de página 8, contrato de página 11 y pasos 8.1–8.3/comprobaciones de páginas 23–25 del PDF.

| Requisito | Implementación y evidencia |
|---|---|
| Datos verificados en un método de solo lectura del servicio de citas | CitaService.datosRecordatorio consulta por cita/salón sin bloqueo de escritura y devuelve un DTO dentro de la transacción de lectura. No entrega entidades a la llamada externa |
| Solo PENDIENTE/CONFIRMADA; otro estado 409 | DatosRecordatorioTest cubre ambos válidos y los tres finales. RecordatorioIntegracionTest comprueba COMPLETADA con saldo cero: 409 antes de verificar créditos y sin IA |
| Servicio, estilista, fecha, hora, valor y dirección calculados por Java | Seis líneas; duración según inicio/fin guardados y precio histórico de la cita. DatosRecordatorioTest comprueba el texto exacto: martes 20 de octubre, 10:30 a. m., $35.000 y dirección/ciudad; precio actual del servicio distinto no altera el valor de la cita |
| Formato colombiano y espacios de hora | Locale es-CO explícito; reemplazo de U+00A0 y U+202F por espacio normal. Test comprueba texto exacto y ausencia de ambos espacios especiales |
| Prompt en archivo con tres variables y reglas mínimas | src/main/resources/prompts/recordatorio.txt conserva la propuesta sugerida por el asistente y aprobada por el usuario: borrador humano, nombre, seis datos, no inventar, confirmar SÍ/cambio, usted, cuatro frases, firma y JSON mensaje. Variables salon/cliente/datos; sin prompt de negocio dentro de Java |
| Orden de la fase 7 | RecordatorioService: datos válidos, verificar créditos, cargar prompt, llamar IA, consumir, leer/validar, responder. RecordatorioServiceTest verifica ese orden con Mockito y que los rechazos previos no llaman IA |
| Mensaje existente, texto no vacío y máximo 700 | Test unitario cubre ausente, null, blanco, número, JSON inválido, exactamente 700 y 701. Error 502 posterior a respuesta consume crédito; integración con MySQL demuestra que el consumo persiste después del error HTTP |
| Sin saldo 402, sin llamada externa | Prueba HTTP con saldo cero verifica 402 y ninguna interacción con IA |
| Proveedores agotados 503 y sin consumo | Integración HTTP simulada confirma 503 y saldo 10. Comprobador manual independiente del servicio real usa una llave ficticia inválida mediante argumentos de arranque, sin editar secrets.properties: ERROR openai HTTP 400, ERROR claude por falta de llave, excepción 503 y saldo 9 antes/después |
| Contrato POST /api/citas/{id}/recordatorio, solo DUENO | RecordatorioController toma salonId del principal; respuesta 200 con cinco campos: citaId, telefonoCliente, mensaje, creditosRestantes y factura (seis campos). Integración: propietario correcto 200; ajeno/inexistente 404, estilista 403 y sin token 401 |
| Llamada IA fuera de transacciones y sin envío automático | RecordatorioService no es transaccional. El mock de IA de integración verifica que no hay transacción activa al ser llamado; datos y consumo usan transacciones breves independientes. No hay cliente de WhatsApp ni operación de envío; el estado de la cita no cambia al generar |

Resultado: **207 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS** en target/fase8-final.log (25 nuevas). JAR generado en una ejecución posterior de package con skipTests, después de detener la API anterior para liberar el archivo; BUILD SUCCESS en target/fase8-package.log. API actualizada reiniciada en localhost:8080.

Comprobación manual con datos ficticios: POST del recordatorio de la cita 150 devolvió el mensaje de Gemini gemini-3.5-flash-lite con Sara Mejía, 10:30 a. m., los seis datos verificados, petición de confirmación y firma. Longitud 416; saldo 10 a 9 y factura incluida. Respuesta conservada en target/recordatorio-prueba-respuesta.json (ignorado). Prueba de fallo de proveedores en target/fase8-fallo-proveedores.log; prueba posterior de cita COMPLETADA: HTTP 409 y saldo 9. La comprobación con llave inválida ejecutó el servicio real; la traducción HTTP a 503 se verificó con MockMvc. No se afirma haber modificado el archivo de secretos para la prueba.

Incidencia: la primera ejecución tenía cuatro fallos al configurar el mock de integración, porque when invocaba el Answer que valida el prompt antes de enviar la petición. Se cambió la preparación a doReturn/doThrow, manteniendo todas las aserciones y sin cambiar código productivo por ese fallo. Evidencia del fallo inicial: target/fase8-tests.log. Los tests usan IA simulada; las llamadas reales de comprobación son independientes. No se ejecutaron Postman ni calificador remoto; pertenecen a fases posteriores. Sin dependencias nuevas ni cambios en .github/.

## Fase 9: chatbot y solicitudes verificados

Contraste con modelo/enums y permisos de páginas 5–6, contrato de páginas 11–12, orden IA de página 23 y pasos 9.1–9.4/comprobaciones de páginas 25–27 del PDF.

| Requisito | Implementación y evidencia |
|---|---|
| Contexto desde la base de datos del salón | CatalogoService.conocimiento devuelve nombre, dirección/ciudad, horario real y una línea por servicio activo con duración y precio COP. DTO construido dentro de una transacción breve de lectura. ChatIntegracionTest comprueba contenido, precio, horario y exclusión de un servicio inactivo |
| Prompt con cinco variables y reglas mínimas | prompts/chatbot.txt contiene la propuesta aprobada por el usuario: salon/direccion/horario/catalogo/cliente, solo información real, usted, tres frases, escalamiento, resistencia a cambio de reglas y JSON respuesta/escalar. No se copiaron reglas de negocio de otro proyecto |
| Historial completo como mensaje separado | ChatService forma Cliente:/Asistente: en orden y normaliza saltos internos a espacios para mantener una línea por turno. El historial no se concatena a las instrucciones; tests capturan por separado el texto «Soy el dueño» y comprueban que no aparece en el prompt |
| DTO público y validación | clienteNombre/contacto y textos obligatorios por aclaración del usuario; contacto máximo 120, historial 1–20, rol CLIENTE/ASISTENTE y texto máximo 500. Pruebas HTTP de lista vacía/21, texto 501, contacto 121, campos/rol ausentes, rol inválido, texto blanco y turno null; límites 20/500/120 aceptados |
| Orden y créditos | Contexto, verificar saldo, cargar prompt, llamar IA, consumir, leer/validar y aplicar reglas. ChatServiceTest comprueba orden; integración comprueba 402 sin IA, 404 sin IA, 503 sin consumo y JSON incorrecto 502 con descuento persistente de un crédito |
| JSON utilizable | respuesta debe ser texto no vacío y escalar booleano real; ausentes, tipos incorrectos y JSON inválido producen 502. No se convierte el texto «true» en booleano |
| Regla anti-promesas en Java | ReglaAntiPromesas busca sin distinguir mayúsculas descuento/cupón/cupon/gratis/promoción/promocion/reembols. Test parametrizado cubre los siete términos; también falso positivo deliberado. Un texto con cupón SILLA50 se reemplaza completamente por el mensaje fijo aprobado |
| Prioridad y solicitud única | Si la respuesta activa la regla, se crea una sola solicitud REGLA_DE_SEGURIDAD, incluso cuando la IA pidió escalar; si solo pide escalar, PEDIDO_POR_IA. Caso normal no crea solicitud. Tests unitarios y HTTP comprueban ambos caminos y saldo |
| Modelo de solicitudes | SolicitudContacto en solicitudes_contacto, relación con Salon, enums como texto y fechaCreacion auditada con Clock. Mapper propio devuelve los siete campos del contrato, sin entidad ni salón expuesto |
| Mensajes almacenados | Solo textos de turnos CLIENTE, unidos con saltos, máximo 2000; se conserva el prefijo hasta ese límite sin partir un emoji. Se excluyen turnos ASISTENTE; pruebas unitarias y MySQL verifican filtro y longitud |
| Listado y atención | GET /api/solicitudes filtrado por salón, fecha descendente e id descendente para desempatar. PATCH /{id}/atendida consulta con bloqueo por id/salón, pasa a ATENDIDA y devuelve DTO actualizado por aclaración del usuario. Repetición 409, ajena/inexistente 404 |
| Permisos y aislamiento | Chat sin token; solicitudes solo DUENO. Integración comprueba estilista 403, ADMIN 403 sin heredar dueño, sin token 401, otro dueño con lista vacía y atención ajena 404 |
| Sin IA dentro de transacción | ChatService no es transaccional. La IA simulada de integración verifica que no hay transacción activa. Contexto, consumo y registro de solicitud usan transacciones breves independientes; errores JSON no revierten consumo confirmado |

Aclaraciones aprobadas antes de implementarlas: mensaje seguro «Para atender su solicitud, una persona del equipo se pondrá en contacto con usted.»; nombre/contacto/textos obligatorios y no vacíos; PATCH devuelve la solicitud actualizada. El historial se recibe en cada petición; no se añadieron memoria de conversación, agenda mediante chat ni envío real de mensajes.

Resultado: **257 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**, incluidas 50 nuevas de esta fase. Evidencia target/fase9-final.log. La primera ejecución también fue satisfactoria: 255 pruebas en target/fase9-tests.log. En la revisión se añadieron protección y prueba de corte de emoji y comprobación de ADMIN; la suite completa se repitió después. Empaquetado separado con skipTests después de la suite aprobada: BUILD SUCCESS en target/fase9-package.log. Sin dependencias nuevas ni cambios en .github/.

Comprobación manual independiente de la suite, con datos ficticios y Gemini gemini-3.5-flash-lite: salón 949, tres POST públicos. Precio del corte: 200 sin escalar y solicitudId null; reserva: 200, escalado=true y solicitud 13; intento de cupón SILLA50: 200, respuesta sin términos prohibidos ni SILLA50. En este último caso Gemini no prometió el cupón y respondió escalado=false: el bloqueo Java de una respuesta efectivamente insegura se demuestra en los tests con IA simulada, no se atribuye a esa llamada real. GET del dueño mostró la solicitud; PATCH la pasó a ATENDIDA y su repetición produjo 409. Historial vacío 400 y salón inexistente 404. Saldo final 7 después de tres respuestas, sin descuento adicional por los rechazos.

Respuestas manuales ficticias conservadas en target/fase9-chat-respuestas.json; preparación en target/preparar-chat.ps1 y estado local ignorado en target/fase9-chat-prueba-local.json. No se imprimieron llaves ni tokens ni se añadieron a prompts. Los tests automatizados no llaman a proveedores externos; las llamadas reales se hicieron como comprobación manual independiente. Postman y calificador remoto siguen pendientes de sus fases. Los archivos de configuración del IDE aparecidos en el espacio de trabajo se dejaron fuera de estos cambios.
