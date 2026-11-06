# Bitácora de uso de IA — Sillón

## 1. Preparación del proyecto

### Herramienta utilizada
ChatGPT.

## Documentación de reglas e instrucciones

### Apoyo solicitado
Ayuda para redactar SPEC.md y AGENTS.md a partir de la actividad.

### Revisión y correcciones
- Se corrigió el nombre del plan FREE por GRATIS.
- Se aclaró que el horario corresponde al salón.
- Se precisó que solo las citas PENDIENTE y CONFIRMADA
  bloquean la agenda.
- La IA propuso elegir un plazo de cancelación.
  Al consultar el enunciado, se descartó agregar ese plazo
  porque la actividad no lo establece.
- Se documentaron las transiciones y permisos de las citas,
  junto con las reglas de registro, catálogo, pagos e IA.

### Estado
Las reglas están documentadas; su implementación y sus pruebas
de negocio siguen pendientes.

## 2. Ajuste de configuración — 6 de noviembre de 2026

### Apoyo de IA
ChatGPT revisó el enunciado y orientó los cambios paso a paso.
Los cambios y comandos se ejecutaron manualmente en VS Code.

### Cambios realizados
- Configuración centralizada en application.properties, sin perfiles.
- Variables para el negocio, JWT, Stripe y Gemini.
- Actualización de secrets.example.properties.
- Creación de .cursorignore.
- Creación de sillon_db y asignación de permisos a sillon_app.

### Verificaciones realizadas
- Conexión a sillon_db con sillon_app por TCP.
- secrets.properties ignorado por Git.
- ./mvnw clean compile terminó con BUILD SUCCESS.
- Arranque correcto después de eliminar los perfiles.
- Commit 2256b91 subido al fork.
- Arranque correcto sin cargar secrets.properties, usando DB_PASSWORD
  y una ruta de importación opcional inexistente.
