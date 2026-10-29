# Bitácora de uso de IA

| # | Fase | Herramienta y modo | Para qué la usaste | Qué revisaste | Qué rechazaste o corregiste |
|---|------|--------------------|--------------------|---------------|-----------------------------|
| 1 | 1 | Claude Code (modo plan, aprobación manual de ediciones) | Crear compartido (errores, manejador global, Clock), salon y usuario | Sin Lombok, enums con EnumType.STRING, paquete com.devsenior.sillon, pom.xml sin cambios, compila y la app arranca con 2 repositorios | Rechacé que la entidad Salon asignara fechaCreacion con LocalDateTime.now(); queda como setter simple y el servicio la asigna con el Clock |