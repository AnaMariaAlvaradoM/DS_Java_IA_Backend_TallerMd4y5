# Pruebas de la entrega — fase 10

Fuente: PDF original, páginas 26–27, fase 10. El estudiante aprobó conservar únicamente los casos mínimos de esa lista. No se usa la cifra de 58 de la referencia como requisito: el PDF permite otro número si están todos los casos.

## Correspondencia con el documento

| Requisito | Evidencia en src/test/java/com/devsenior/sillon |
|---|---|
| Cinco transiciones permitidas y al menos seis bloqueadas, parametrizadas | cita/EstadoCitaTest.respetaLaTablaCompleta: 25 combinaciones, cinco permitidas y veinte bloqueadas |
| Dentro del horario, después del cierre y antes de abrir | cita/ReglasAgendaTest: horarioValidoIncluyeLosLimites, terminaDespuesDelCierreEs409ConHorario, empiezaAntesDeAbrirEs409 |
| Domingo cerrado y día cerrado sin horarios | cita/ReglasAgendaTest.domingoCerradoEs409YNoTieneHorarios comprueba ambas condiciones |
| Horarios libres excluyen cruces y hoy excluye horas pasadas | cita/ReglasAgendaTest: disponibilidadExcluyeCrucesYRespetaCuadricula, hoyUsaLaHoraDeColombiaYExcluyePasado |
| Cita válida CONFIRMADA con precio del servicio | cita/CitaServiceTest.citaValidaCalculaFinPrecioEstadoYOrigen |
| Pasado y cruce no guardan; estilista no ofrece servicio | cita/CitaServiceTest: pasadoNoGuardaNiCreaCliente, cruceNoGuardaNiCreaCliente, estilistaSinServicioEs409 |
| Estilista toca cita ajena (404), intenta cancelar (403), transición no permitida no guarda | cita/CitaServiceTest: citaAjenaAlEstilistaEs404, estilistaNoPuedeCancelarSuCita, transicionInvalidaNoGuarda; comprueban excepciones de negocio correspondientes y ausencia de guardado |
| Tercer estilista GRATIS (402) y tercero PRO permitido | usuario/EstilistaServiceTest: tercerEstilistaGratisEs402 y tercerEstilistaProSeCrea; repositorios y codificador simulados |
| PRO activa plan y suma créditos; paquete solo suma | pago/WebhookServiceTest: pagoProActivaPlanSumaCreditosYEventoRepetidoNoSeAplica, paqueteSoloSumaCreditos |
| Evento repetido no se aplica; cancelación vuelve a GRATIS; firma falsa | pago/WebhookServiceTest: repetición comprobada sin segundo guardado, cancelacionVuelveAGratis, firmaFalsaEs400YNoProcesa |
| Chat sin créditos no llama IA; normal consume uno sin escalar | chat/ChatbotServiceTest: sinCreditosNoCargaPromptNiLlamaIa, preguntaNormalRespetaOrdenYSeparaHistorial |
| IA pide escalar y crea solicitud; cupón se sustituye por mensaje seguro | chat/ChatbotServiceTest: iaEscalaYGuardaSoloMensajesDelCliente, reglaDeSeguridadTienePrioridadYCreaUnaSolaSolicitud; este último parametrizado con escalar falso/verdadero |
| Recordatorio devuelve mensaje/saldo; JSON sin mensaje produce 502 | cita/RecordatorioServiceTest: respuestaValidaRespetaOrdenYContrato, respuestaInvalidaEs502PeroConsumeCredito, entrada {} y estado BAD_GATEWAY explícito |
| Motor reutilizado: fallback, clasificación de errores, prompts y lector JSON | ia/AsistenteIATest, ia/ProveedorIAExceptionTest e ia/LectorJsonYPromptsTest, adaptados del paquete reutilizado |
| Contexto completo con MySQL encendido | SillonApplicationTests.contextLoads |

## Ejecución y límites

Fecha: 2026-10-21, America/Bogota. Java 17 y Maven Wrapper. Comando de pruebas: `./mvnw.cmd test` (esta ejecución usó además repositorio Maven local y modo offline disponibles en el equipo).

Resultado real: **70 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**. Log local ignorado: target/fase10-tests.log. Se eliminaron previamente las clases de tests compiladas y reportes antiguos para evitar ejecutar clases retiradas.

Actualización del 2026-10-27: suite actual de **75 pruebas aprobadas**, sin omisiones, tras la compatibilidad de metadata Stripe expresamente aprobada por el estudiante. Los casos mínimos de PRO/paquete se parametrizan con tipo, tipoCompra y ambas iguales; una prueba comprueba que valores contradictorios produzcan 502 sin cambios ni evento. Las cinco ejecuciones adicionales verifican esa aclaración. El test de chatbot se renombró a ChatbotServiceTest sin alterar sus aserciones. Evidencia: target/compatibilidad-webhook-final.log y VERIFICACION.md. No se modificó el calificador.

Los tests de servicios construyen sus objetos directamente y simulan dependencias con Mockito; no levantan Spring. Los escenarios de agenda y webhook usan Clock.fixed con America/Bogota. La verificación criptográfica y deserialización de Stripe son locales, con firma ficticia; no se llama a Stripe. La IA se simula y no se llama a Gemini. Solo el test de contexto usa Spring y MySQL.

Por decisión expresa del estudiante, JUnit sigue gestionado por spring-boot-starter-webmvc-test (JUnit 6.0.3). Se conserva la diferencia frente a JUnit 5 mencionado en el PDF; véase DEPENDENCIAS.md.

Las pruebas adicionales de fases anteriores y las regresiones de auditoría se retiraron por solicitud del estudiante; su evidencia histórica permanece en Git y en los registros. Las correcciones productivas siguen vigentes. Las cifras 257/276 de informes anteriores corresponden a aquellas versiones, no a la suite actual. No se desactivaron ni omitieron tests para ocultar un fallo: la reducción responde al alcance solicitado. Este resultado no acredita Postman, calificador remoto ni producción.
