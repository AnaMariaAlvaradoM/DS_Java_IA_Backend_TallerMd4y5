
# Bitácora de uso de IA — Sillón

Registro del apoyo de inteligencia artificial utilizado durante el desarrollo del proyecto.

| N.º | Fase | Herramienta y modo | Para qué se utilizó | Qué se revisó | Qué se rechazó o corrigió |
|---|---|---|---|---|---|
| 1 | Fase 0 | ChatGPT — chat del navegador | Apoyo para redactar SPEC.md y AGENTS.md a partir del enunciado. | Reglas de negocio, planes, horarios, estados de citas, permisos y alcance. | Se corrigió FREE por GRATIS, se aclaró que el horario pertenece al salón y que solo PENDIENTE y CONFIRMADA bloquean la agenda. Se descartó inventar un plazo de cancelación. |
| 2 | Fase 0 | ChatGPT — chat del navegador | Análisis de errores del calificador de GitHub y revisión de variables de entorno. | application.properties, BUILD SUCCESS, diferencias del código y commit e75ff06c. | Se ajustó la configuración de MySQL para admitir variables SPRING_DATASOURCE_* y la URL de Gemini para admitir SILLON_IA_OPENAI_URL. |
| 3 | Fase 1 | ChatGPT — chat del navegador | Apoyo para desarrollar el manejo de errores, el reloj del sistema y las entidades Salon y Usuario. | Compilación, inicio de Spring Boot y tablas de MySQL. | Se mantuvieron el registro, JWT y BCrypt para la siguiente fase, respetando el orden del enunciado. |
| 4 | Fase 2 | ChatGPT — chat del navegador | Apoyo durante la implementación y validación del registro, login y configuración del salón. | Respuestas HTTP 200, 201, 400 y 409 mediante Postman. | Se corrigieron problemas de autenticación y respuestas de validación que no coincidían con el contrato. |
| 5 | Fase 3 | ChatGPT — chat del navegador | Apoyo para implementar el catálogo, los estilistas y los clientes. | Validaciones de servicios, relaciones entre estilistas y servicios, aislamiento entre salones y pruebas Postman. | Se revisó la validación de correos duplicados y la asignación de servicios, evitando aceptar datos de otros salones. |
| 6 | Fase 4 | ChatGPT — chat del navegador | Apoyo para implementar la agenda, sus reglas de negocio y las pruebas de concurrencia. | Cruces de citas, estados, permisos, formato de errores, pruebas JUnit y Postman. | Se corrigió el manejo de errores de permisos y se agregó bloqueo pesimista para impedir reservas simultáneas. También se corrigió la interpretación del secreto JWT que impedía iniciar el evaluador. |
| 7 | Fase 5 | ChatGPT — chat del navegador | Apoyo para implementar el catálogo público, la disponibilidad y las reservas en línea. | Respuestas de Postman, conflictos de horarios, permisos y pruebas de concurrencia. |
| 8 | Fase 6 | ChatGPT — chat del navegador | Apoyo para integrar Stripe Checkout, los pagos de prueba y el procesamiento de webhooks. | Sesiones de pago, respuestas HTTP, firma del webhook, cambios de plan, créditos y eventos duplicados. |
| 9 | Fase 7 | ChatGPT — chat del navegador | Compilación Maven, pruebas JUnit con Mockito, manejo de errores de proveedores y descuento condicional de créditos en MySQL. | Se corrigió la ubicación de las pruebas JUnit. Se comprobó el descuento de un crédito y su recuperación mediante ROLLBACK. |




## Observaciones

- Las reglas funcionales se mantienen documentadas en SPEC.md.
- Se reutilizaron las clases existentes para evitar duplicar la lógica de agenda.
- Las pruebas de Postman permitieron comprobar los endpoints públicos sin JWT.
- La prueba inicial de concurrencia pública falló porque permitió registrar dos reservas en el mismo horario.
- Después de corregir la transacción, una nueva prueba devolvió HTTP 201 y HTTP 409.
- Se eliminaron los dos registros duplicados de prueba y se verificó la información en MySQL.
- La evaluación automática de GitHub Actions después de la Fase 6 alcanzó 3.7/5.0, con 108 de 145 pruebas aprobadas.
- La Fase 6 superó las 29 pruebas automáticas de Stripe. Se verificaron la suscripción PRO, compra de créditos, firma del webhook, permisos, idempotencia y cancelación.
- En la Fase 7 se implementó el motor de IA con dos proveedores, fallback, lectura de JSON, biblioteca de prompts, cálculo de costos y servicio de créditos.
- Las pruebas JUnit de la Fase 7 finalizaron con BUILD SUCCESS. También se comprobó el descuento condicional en MySQL y su reversión mediante ROLLBACK.
- Quedan pendientes las pruebas de concurrencia real de créditos y la integración con el proveedor de IA simulado.
- Las pruebas automáticas locales finalizaron con BUILD SUCCESS. La Fase 6 está pendiente de evaluación en GitHub Actions.
- Las siguientes fases del proyecto incluyen la integración de inteligencia artificial y el despliegue en producción.
