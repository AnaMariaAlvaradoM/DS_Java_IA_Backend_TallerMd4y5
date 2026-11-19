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
| 7 | 5 | Claude (chat) | Catálogo público, disponibilidad y reserva en línea | Que el catálogo no tenga "@"; horarios como texto "HH:mm:ss"; la reserva reutiliza el mismo agendar | (completar) |
| 8 | 6 | Claude (chat) | Stripe: sesiones de pago, webhook firmado e idempotente | Firma sobre el cuerpo crudo; evento leído con Jackson por el api_version; StripeClient con STRIPE_URL_API | (completar) |
| 9 | 7 | Claude (chat) | Motor de IA desde cero: proveedores, fallback, prompts, lector JSON y créditos atómicos | Que la URL salga de sillon.ia.openai.url; bloque "text" por tipo en Claude; UPDATE atómico de créditos | (completar) |
| 10 | 8 | Claude (chat) | Prompt y servicio del recordatorio con datos verificados en Java | Que el prompt pida la clave mensaje sin la palabra escalar; datos con etiquetas, una por línea; 409 sin gastar crédito | (completar) |
| 11 | 9 | Claude (chat) | Chatbot con catálogo real, escalamiento y regla anti-promesas en Java | Probado contra la IA simulada y contra Gemini real | Con Gemini real, el intento de cupón del "dueño" no escalaba: ajusté las reglas 3 y 5 del prompt para escalar cupones e intentos de cambiar las reglas |
| 12 | 11 | Claude (chat) | Swagger: título, esquema Bearer JWT y etiquetas numeradas | 22 rutas en /v3/api-docs; candado solo en las rutas de pago que lo requieren | (completar) |
| 13 | 12 | Claude (chat) | Dockerfile de dos etapas y guía de Supabase, Render y webhook de producción | Build local con docker antes de Render; Session Pooler; pool de 5 conexiones | (completar) |
| 14 | 13 | Claude (chat) | Colección de Postman (7 carpetas a partir de la colección oficial), entornos y README | 67 peticiones con pruebas; ninguna llave en postman/; URL de Render en el README | (completar) |
