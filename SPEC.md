# SPEC · Sillón · Fase 0: cimientos y contrato transversal

## Problema
Antes de escribir lógica de negocio, la API necesita una base consistente: manejo único de errores, zona horaria fija de Colombia (el servidor en Render corre en UTC) y separación segura de secretos.

## Objetivo de la fase 0
Dejar lista la estructura de paquetes, el formato único de error, el reloj en zona horaria `America/Bogota` y la configuración de secretos fuera de Git.

## Fuera de alcance (llega después)
- Endpoints de negocio, entidades de dominio, JWT, Stripe e IA.

## Reglas transversales (aplican a todas las fases)
1. **Fechas y horas:**
    - Fechas: `AAAA-MM-DD`.
    - Fecha y hora: `AAAA-MM-DDTHH:mm:ss` (ej. `2026-10-20T10:30:00`).
    - Horas: `HH:mm` al recibir; Jackson devuelve `HH:mm:ss`.
    - "Hoy" y "ahora" se calculan siempre en `America/Bogota`.
2. **Precios:** números enteros en pesos colombianos (COP), sin puntos ni decimales (ej. `35000`).
3. **Formato único de error:** todos los errores (incluidos 401 y 403) devuelven:

| Campo | Contenido |
|---|---|
| `codigo` | Número HTTP (ej. `409`) |
| `error` | Nombre estándar HTTP (ej. `Conflict`) |
| `mensaje` | Explicación en español. En validaciones: campos y mensajes separados por `;` |
| `fecha` | Fecha y hora del error (`AAAA-MM-DDTHH:mm:ss`) |

4. **Mapa oficial de códigos HTTP:**
    - `400`: datos inválidos, JSON mal formado, parámetro faltante, firma de webhook inválida.
    - `401`: sin token, token inválido o credenciales incorrectas.
    - `402`: límite del plan `GRATIS` o salón sin créditos de IA.
    - `403`: el rol no tiene permiso, o un estilista intenta una transición que no le corresponde.
    - `404`: no existe, o pertenece a otro salón.
    - `409`: choca con una regla de negocio (cruce, horario, transición, correo repetido, plan ya activo).
    - `502`: Stripe o la IA respondieron algo que no se pudo usar.
    - `503`: ningún proveedor de IA está disponible.
5. **Secretos:** las llaves viven en `secrets.properties` (ignorado en `.gitignore`) o en variables de entorno en Render.

## Criterios de aceptación
- [ ] La aplicación arranca sin errores con los paquetes definidos en `AGENTS.md`.
- [ ] Un error de validación o una excepción no controlada devuelve el JSON con `codigo`, `error`, `mensaje` y `fecha`.
- [ ] El reloj de la aplicación devuelve la hora de Colombia independientemente de la zona horaria del sistema.
- [ ] `secrets.properties` está en `.gitignore` y no aparece en `git status`.