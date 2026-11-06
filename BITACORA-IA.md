# Bitácora de uso de IA — Sillón

Registro del apoyo de inteligencia artificial utilizado durante el desarrollo del proyecto.

| N.º | Fase | Herramienta y modo | Para qué se utilizó | Qué se revisó | Qué se rechazó o corrigió |
|---|---|---|---|---|---|
| 1 | Fase 0 | ChatGPT — chat del navegador | Apoyo para redactar SPEC.md y AGENTS.md a partir del enunciado. | Reglas de negocio, planes, horarios, estados de citas, permisos y alcance del proyecto. | Se corrigió FREE por GRATIS, se aclaró que el horario pertenece al salón y que solo PENDIENTE y CONFIRMADA bloquean la agenda. Se descartó inventar un plazo de cancelación. |
| 2 | Fase 0 | ChatGPT — chat del navegador | Análisis de los errores del calificador de GitHub y revisión de variables de entorno. | Cambios en application.properties, BUILD SUCCESS, revisión del diff y commit e75ff06c. | Se corrigió la configuración de MySQL para admitir las variables SPRING_DATASOURCE_* y la URL de Gemini para admitir SILLON_IA_OPENAI_URL. |

## Observaciones

- Las reglas se documentaron en SPEC.md y AGENTS.md.
- Los cambios de configuración fueron aplicados y comprobados mediante comandos ejecutados en VS Code.
- La calificación automática sigue siendo 0,3/5,0 porque faltan funcionalidades de las fases posteriores.
- Las funcionalidades y pruebas de negocio continúan pendientes de implementación.