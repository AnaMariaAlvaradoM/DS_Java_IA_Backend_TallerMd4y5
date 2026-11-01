# Bitácora de Uso de IA · Proyecto Sillón

| # | Fase | Herramienta / Modo | Para qué se usó | Qué revisaste | Qué rechazaste o corregiste |
|---|---|---|---|---|---|
| 01 | Fase 0 | Chat / PM | Planificación y configuración base | Coherencia del `pom.xml`, variables de entorno y aislamiento multi-tenant | Se postergó Spring Security a la Fase 2 para evitar bloqueos tempranos en Fase 1 |
| 02 | Fase 1 | Chat / PM | Paquete compartido, excepciones y entidades base | Validación del formato de 4 campos de error, reloj inyectado, modelo multi-tenant y generador de slug | Se postergó la lógica de controladores y JWT a la Fase 2 para respetar el orden incremental de fases |