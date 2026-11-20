# Bitácora de desarrollo asistido

Esta bitácora describe el trabajo con Codex durante la construcción del proyecto. Los cambios se verificaron primero con el ZIP y luego con la guía escaneada de 40 páginas.

| Nº | Fase | Herramienta y modo | Para qué se usó | Verificación realizada | Corrección o decisión |
|---|---|---|---|---|---|
| 1 | 0 · Preparación | Codex, lectura de archivos y OCR | Identificar requisitos y contrato HTTP | Se compararon las colecciones del ZIP con el texto del PDF recuperado | Se reemplazó la primera combinación de Java y Spring por Java 17 y Boot 4.1 |
| 2 | 1–2 · Registro y seguridad | Codex, implementación y pruebas | Crear salones, usuarios, permisos y JWT | MockMvc comprobó registro, errores y acceso protegido | Se corrigió un conflicto de nombres de beans y se unificó el cuerpo de 401 y 403 |
| 3 | 3–5 · Catálogo y reservas | Codex, implementación | Implementar reservas de dos estilistas y aislamiento entre estudios | Newman verificó cruces de 45 minutos y la agenda por rol | Se corrigieron la duración válida, el precio entero, el cierre por defecto y los estados que ocupan agenda |
| 4 | 6 · Pagos | Codex, integración | Crear sesiones de Checkout y procesar eventos firmados | Se probaron activación, paquete, cancelación y reenvío en el simulador oficial | Se cambió al cliente Stripe Java y se añadió la referencia y la metadata de la suscripción |
| 5 | 7–9 · IA | Codex, desarrollo y revisión | Preparar recordatorios y derivar chats al equipo | Se contrastaron las respuestas con el simulador y los dos tipos de solicitud | Se separó la llamada HTTP de las transacciones y se añadió la ruta para atender solicitudes |
| 6 | 10–11 · Pruebas y Swagger | Codex, pruebas locales | Verificar el contrato y la versión del runtime | Se ejecutó Maven con JDK 17, la colección oficial con MySQL y el recorrido de producción con PostgreSQL | Se retiró una propiedad de Jackson 2 incompatible con Jackson 3 |
| 7 | 12–13 · Entrega | Codex, GitHub CLI y Render CLI | Crear el fork, abrir un único PR y preparar el despliegue | Se verificó el repositorio padre, el título del PR y la URL asignada por Render | Se mantuvieron los datos de cuenta y secretos fuera del repositorio |
