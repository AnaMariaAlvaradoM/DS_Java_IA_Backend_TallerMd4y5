# Bitácora de uso de IA

| # | Fase | Herramienta y modo | Para qué la usé | Qué revisé | Qué rechacé o corregí |
|---|------|--------------------|-----------------|------------|-----------------------|
| 1 | 0 | Claude (chat) | Entendimiento mejor del taller. | Comprender de otra forma el taller |nada |
| 2 | 0 | Claude (chat) | Revisar la base del proyecto antes del primer commit | El estado de Git y el .gitignore | El .gitignore de Initializr había borrado la regla de secrets.properties; lo fusioné con el de la profesora |
| 3 | 2 | Claude (chat) | Autenticación Stateles en Filtro JWT | Para optimizar el rendimiento y evitar latencia en base de datos en cada petición HTTP, JwtAuthenticationFilter instancia UsuarioPrincipal directamente a partir de las claims del JWT con activo = true |
