# Bitácora de IA · Sillón

Uso de IA ampliado con autorización de la instructora: la IA genera código y documentos a partir de las reglas del enunciado; yo reviso, ejecuto, pruebo en Postman y con tests, y decido qué se acepta.

| # | Fase | Herramienta y modo | Para qué la usé | Qué revisé | Qué rechacé o corregí |
|---|---|---|---|---|---|
| 1 | 0 | Claude (chat, rol de tech lead) | Analizar el enunciado, los resúmenes de clase y el calificador del repositorio oficial; armar el calendario de entrega | Que el plan cubriera las 14 fases y los 9 criterios | El primer plan suponía que yo tenía el código de Atiende (Módulo 5); lo corregimos para construir el paquete `ia` desde cero |
| 2 | 0 | Claude (chat) | Borrador de SPEC.md, AGENTS.md, application.properties y plantilla de secretos | Reglas contra la sección 3 del enunciado; rutas y campos contra la sección 4; variables de entorno contra el README del calificador | (completar) |
| 3 | 1 | Claude (chat) | Código de la base SaaS: errores, reloj, Salon, slug, Usuario | Que el error tenga los 4 campos y que el slug cumpla "barberia-la-navaja" y "-2" | (completar) |
| 4 | 2 | Claude (chat) | Seguridad JWT, registro, login y mi salón | Que sin token responda 401 y no 403, y que el login no diga qué falló | (completar) |
| 5 | 3 | Claude (chat) | Servicios, estilistas con ManyToMany, clientes y sus tests | Orden 402→409→404 al crear estilista; que la respuesta no traiga la contraseña; ids enviados como texto | (completar) |
| 6 | 4 | Claude (chat) | Agenda: EstadoCita, ReglasAgenda, CitaService y 41 tests | Orden de las 7 validaciones al agendar; cruce solo con PENDIENTE y CONFIRMADA; 404→403→409 al cambiar estado | (completar) |
