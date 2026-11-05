# Bitácora de Uso de IA - Sillón

|# Bitácora de Uso de IA - Sillón

| # | Fase | Herramienta y modo | Para qué la usaste | Qué revisaste | Qué rechazaste o corregiste |
|---|------|--------------------|--------------------|---------------|-----------------------------|
| 1 | Fase 0 | Cursor / Asistente | Estructura base y configuración | Dependencias y variables de entorno | Se descartaron dependencias de Lombok y SDKs de Spring AI |
| 2 | Fase 1 | Asistente IA | Creación del dominio salón, usuario, manejo global de errores y seguridad base | Estructura de DTOs, 4 campos de error y gobernanza horaria | Se corrigieron nombres de archivos a PascalCase para compilador Java |
| 3 | Fase 2 | Asistente IA | Autenticación con JWT, filtro OncePerRequestFilter y configuración del salón | Claims del token (salonId, rol), compatibilidad de UserDetailsService | Se descartó bean explícito DaoAuthenticationProvider redundante en Spring Boot 4 |
| 4 | Fase 3 | Asistente IA | Catálogo de servicios, entidad Servicio, relación ManyToMany con Usuario y gestión de estilistas | Validación de aislamiento multi-tenant en servicios de estilista, DTOs de catálogo | Se corrigieron nombres de archivos tipográficos en DTOs de estilistas para compilador Java |