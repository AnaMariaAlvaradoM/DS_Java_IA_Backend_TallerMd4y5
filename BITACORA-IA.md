# Bitácora de uso de IA

Registrar cada uso importante. Esta tabla refleja trabajo real; completar la revisión y las correcciones conforme ocurran.

| N.º | Fase | Herramienta y modo | Para qué se usó | Qué revisé | Qué rechacé o corregí |
|---:|---|---|---|---|---|
| 1 | 0 | Chat de IA | Preparar el proyecto base y borradores de SPEC y reglas de implementación | El usuario condicionó su validación a no omitir requisitos; se cotejó el borrador con el PDF y se hizo una auditoría independiente | Se reemplazó el resumen incompleto por reglas, modelo, contrato por campos, restricciones de IA y entrega; se detectó la discrepancia entre las 22 rutas mencionadas en Swagger y las 25 operaciones enumeradas |
| 2 | 0 | Chat de IA y herramientas de desarrollo | Completar dependencias y verificar la base local | Se contrastaron versiones con Maven Central, se compiló el proyecto y `mvn test` cargó el contexto conectado a MySQL | El test inicial fallaba por falta de MySQL local; se preparó un contenedor propio y un secreto ignorado por Git, sin cambiar el test |
