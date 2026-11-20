# Bitácora de Uso de IA · Proyecto Sillón

| Número | Fase | Herramienta y modo | Para qué se usó | Qué se revisó | Qué se rechazó o corrigió |
|---|---|---|---|---|---|
| 1 | Fase 0 | Cursor / Antigravity Agent | Generación de SPEC.md, AGENTS.md y configuración de propiedades base | Se verificaron las variables de entorno requeridas por el calificador (PORT, JWT_SECRETO, STRIPE_*, GEMINI_LLAVE) | Se ajustó el dialecto y formato de application.properties para evitar perfiles o formato YAML |
| 2 | Fase 1 | Cursor / Antigravity Agent | Creación de entidades Salon, Usuario, ConfiguracionSalon, Clock y ManejadorGlobalExcepciones | Se verificó el formato de 4 campos en ErrorRespuesta y el bean Clock inyectado en America/Bogota | Se aseguró la creación de getters/setters explícitos sin Lombok y la validación de slug único |
