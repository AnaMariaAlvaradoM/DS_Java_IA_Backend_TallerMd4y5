# Bitácora de IA — Proyecto Sillón

Registro de los usos importantes de IA durante el proyecto. Una entrada por cada uso importante, y al menos una por fase en la que se haya usado IA.

| # | Fase | Herramienta y modo | Para qué la usé | Qué revisé | Qué rechacé o corregí |
|---|------|--------------------|-----------------|------------|-----------------------|
| 1 | 0 | Copilot (agente en VS Code), modo solo lectura | Revisar SPEC.md contra el PDF oficial y proponer el plan del paso 0.4 (sección 7, AGENTS.md y BITACORA-IA.md) | Comparé el plan propuesto con las páginas 9 a 12 (contrato), 15 y 16 (paso 0.4) del PDF | Corregí el conteo de dominios del agente (propuso 10; son 12) y le exigí incluir los reportes en el fuera de alcance y el 409 del recordatorio |
| 2 | 0 | Copilot (agente en VS Code), modo agente con alcance restringido | Completar la sección 7 de SPEC.md y crear AGENTS.md y BITACORA-IA.md según el PDF | Pendiente: revisión del diff completo contra las páginas 7 a 12 del PDF antes de aceptar los cambios | Pendiente: se registrará lo que se corrija tras la revisión del diff |
| 3 | Fase 0 | GitHub Copilot — Agent | Incorporar las dependencias de Springdoc, Stripe y JJWT al pom.xml | Se revisó el informe de versiones y los resultados de compilación de Maven | Se descartaron versiones preliminares de Stripe y se conservaron las dependencias existentes |
| 4 | Fase 1 | GitHub Copilot — Agent | Implementar entidades Salon y Usuario, manejo global de errores, reloj y generador de slugs | Se ejecutó Maven y se verificaron 12 pruebas aprobadas; se revisó el informe de los archivos creados | Se identificó y corrigió el problema de conexión con MySQL mediante configuración local de credenciales, sin modificar las pruebas |
