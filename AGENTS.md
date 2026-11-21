# Reglas para Agentes de IA en Sillón (AGENTS.md)

Este documento rige el desarrollo y las decisiones técnicas de los agentes autónomos de IA y desarrolladores en el proyecto **Sillón**.

## 1. Paquetes y Organización por Dominio
- Paquete base obligatorio: `com.devsenior.sillon`.
- Organización por dominios de negocio:
  - `compartido`: Respuestas de error, excepciones, Clock, configuración Swagger.
  - `seguridad`: JWT, filtros, configuración de Spring Security, UsuarioPrincipal.
  - `auth`: Registro y login de usuarios y salones.
  - `salon`: Entidad Salon, ConfiguracionSalon, controladores de configuración.
  - `usuario`: Entidades de usuarios, roles, relación con estilistas.
  - `servicio`: Catálogo de servicios.
  - `cliente`: Gestión de clientes finales.
  - `cita`: Citas, disponibilidad, reglas de agenda y estados.
  - `publico`: Endpoints abiertos para catálogo, disponibilidad, reservas y chatbot.
  - `pago`: Integración de Stripe, Checkout Sessions y Webhooks.
  - `chat`: Solicitudes de contacto y chatbot.
  - `ia`: Clientes de IA (RestClient), prompts y fallback.

## 2. Reglas Técnicas Inviolables
1. **Sin Lombok**: Todos los getters, setters y constructores deben ser explícitos en Java tradicional.
2. **Sin Spring AI ni SDKs de IA**: Las llamadas a Gemini / OpenAI se realizan mediante `RestClient` de Spring Boot 4.
3. **Multi-inquilino estricto**: Cada consulta y mutación debe estar restringida al `salonId` del usuario autenticado (`@AuthenticationPrincipal UsuarioPrincipal`). Si no pertenece al salón, retornar `404 Not Found`.
4. **Manejo de Tiempos**: Todas las operaciones temporales ("hoy", "ahora", disponibilidad) deben consumir el bean `Clock` inyectado (configurado en `America/Bogota`).
5. **No tocar pruebas para forzar aprobación**: Los tests reflejan el contrato de negocio. Si un test falla, se evalúa si el código cumple la especificación descrita en `SPEC.md`.
6. **No añadir dependencias sin autorización**: No incluir librerías adicionales a las requeridas en el `pom.xml`.
7. **Seguridad y Secretos**: Nunca escribir claves secretas en el código, en archivos subidos al repositorio ni en prompts de IA.
