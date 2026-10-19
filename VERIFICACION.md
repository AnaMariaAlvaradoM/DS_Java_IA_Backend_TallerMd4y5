# Verificación por fase contra el enunciado

Este registro distingue inspección del código, pruebas locales y comprobaciones externas pendientes. Los resultados corresponden a la versión que se cierra en cada fase, no a la nota del calificador.

Fuente: PDF de requerimientos entregado por el usuario, 40 páginas. Las referencias de página siguientes corresponden a ese PDF. Las aclaraciones del usuario se registran aparte en SPEC.md.

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
