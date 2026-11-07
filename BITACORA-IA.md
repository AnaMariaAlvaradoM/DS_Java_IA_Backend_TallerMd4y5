# Bitácora de uso de Inteligencia Artificial

**Proyecto:** Sillón  
**Programa:** Java AI Engineer — Dev Senior Code

## Registro de actividades

| # | Fase | Herramienta y modo | Para qué se utilizó | Qué se revisó | Qué se rechazó o corrigió |
|---|---|---|---|---|---|
| 1 | 0.4 | Codex, agente de programación | Preparar las instrucciones operativas del proyecto y organizar la documentación inicial. | README oficial, estructura del repositorio y estado de Git. | Se identificó que la especificación debía basarse en el documento oficial, evitando inventar reglas de negocio. |
| 2 | 0.4 | ChatGPT, asistente de documentación | Elaborar la especificación funcional y técnica inicial del proyecto (`SPEC.md`). | Requisitos del documento oficial: arquitectura, roles, entidades, reglas de negocio, contrato de API e integraciones. | Se estableció que los nombres de campos, rutas y códigos HTTP deben conservarse exactamente como en el enunciado. |
| 3 | 1 | Codex, agente de programación | Implementar y revisar la base SaaS: salones, usuarios, configuración y manejo de errores. | Entidades JPA, relaciones, enums, generador de slugs, manejador global, configuración del reloj y compilación Maven. | Se retiró una contraseña de `application.properties`. Se identificó que `contextLoads` requiere credenciales válidas de MySQL y que los errores 401/403 se integrarán en la Fase 2. |
## Criterios de registro

- Registrar únicamente actividades de IA realizadas.
- Documentar qué se revisó manualmente en cada actividad.
- Describir las correcciones o decisiones reales, sin inventar resultados.
- Agregar al menos una entrada por cada fase en la que se utilice IA.
- No registrar pruebas como exitosas sin haberlas ejecutado.

## Observaciones

La inteligencia artificial se utiliza como herramienta de apoyo para documentación, planificación, implementación y revisión.

La responsabilidad de validar el cumplimiento de las reglas de negocio y del contrato oficial de la API corresponde al desarrollador.
