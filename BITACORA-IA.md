# BITACORA-IA.md — Registro de uso de IA en Sillón

Esta bitácora documenta **cómo se usó la IA** en el proyecto: para qué, qué se revisó y qué se rechazó o corrigió. Es evidencia de proceso y se revisa junto con el historial de commits.

## 1. Principios

- **Tú decides y respondes por el resultado; la IA ejecuta lo que ya decidiste.**
- Una entrada por cada uso importante, y **al menos una por cada fase en la que se haya usado IA**.
- Una bitácora **sin nada rechazado ni corregido es una alerta**: ninguna IA acierta siempre.
- Solo se registra lo que realmente ocurrió. No se inventan entradas.
- Nunca se pega una llave o secreto en un prompt.
- Un test rojo no se "arregla" con IA: primero se decide, contra el SPEC, si la causa está en el test o en el código. **Nunca se modifica un test para que pase.**

## 2. Cuándo se usa IA en este proyecto

| Momento | ¿IA? | Condición |
|---|---|---|
| Entender el negocio y redactar el SPEC | Sí, con límites | Chat como entrevistador: que pregunte lo que olvidé. Las reglas oficiales no se cambian |
| Decidir reglas, estados, permisos y planes | No | Ya están en el SPEC; mi trabajo es entenderlas y defenderlas |
| Crear el proyecto | No | Spring Initializr |
| Implementar una fase | Sí | Cursor en Plan → Agent con `SPEC.md` y `AGENTS.md`; yo apruebo el plan |
| Revisar el diff y verificar en Postman | No | Nada se acepta sin entenderlo y probarlo |
| Escribir los prompts del producto | Los escribo yo | La IA puede sugerir redacción, sin cambiar las reglas |
| Decidir los casos de prueba | No | Salen de las reglas de negocio |
| Escribir el código de los tests | Sí | Con la lista de casos que yo definí |
| Depurar un error | Sí, tras mi hipótesis | Primero mi hipótesis escrita; luego Ask/Debug |
| Stripe, Render, Supabase y llaves | No | Nunca pegar una llave en un prompt |
| README | Sí | Verificando que todo lo que diga sea cierto |
| Entender un fallo del calificador | Sí, tras leerlo | Primero leo el mensaje y ubico la regla de negocio |

## 3. Cómo llenar la tabla

| Columna | Qué escribir |
|---|---|
| **#** | Número consecutivo |
| **Fase** | Fase del SPEC (0–13) |
| **Herramienta y modo** | Ej.: Cursor · Plan, Cursor · Agent, Cursor · Ask, Chat del navegador |
| **Para qué la usé** | La tarea concreta, citando reglas del SPEC (R#) |
| **Qué revisé** | Qué comprobé: el plan contra el modelo, el diff línea a línea, filtros por salón, ejecución real de tests o Postman |
| **Qué rechacé o corregí** | Lo que la IA hizo mal o de más, y cómo lo resolví. Si no hubo nada, explica por qué (y desconfía) |

**Ejemplo de formato** (ficticio; no lo copies como si fuera tuyo):

| # | Fase | Herramienta y modo | Para qué la usé | Qué revisé | Qué rechacé o corregí |
|---|---|---|---|---|---|
| E1 | 4 | Cursor · Plan → Agent | Implementar agendar y cambiar estado (R8–R13) | El orden de validaciones contra SPEC §13, que el cruce usara solo `PENDIENTE`/`CONFIRMADA` y que cada consulta filtrara por salón | El agente usó `LocalDateTime.now()`: lo reemplacé por el `Clock`. También dejó una consulta de citas sin filtro por salón: la corregí |

## 4. Registro

| # | Fase | Herramienta y modo | Para qué la usé | Qué revisé | Qué rechacé o corregí |
|---|---|---|---|---|---|
| 1 | 0 | | | | |
| 2 | 1 | | | | |
| 3 | 2 | | | | |
| 4 | 3 | | | | |
| 5 | 4 | | | | |
| 6 | 5 | | | | |
| 7 | 6 | | | | |
| 8 | 7 | | | | |
| 9 | 8 | | | | |
| 10 | 9 | | | | |
| 11 | 10 | | | | |
| 12 | 11 | | | | |
| 13 | 12 | | | | |
| 14 | 13 | | | | |

> Agrega filas según avances: varias por fase si hubo varios usos importantes.

## 5. Depuración con hipótesis

Antes de pedir ayuda con un error, escribe tu hipótesis. Después registra el resultado.

| # | Fase | Síntoma (mensaje exacto) | Mi hipótesis | Qué propuso la IA | Causa real | Resolución |
|---|---|---|---|---|---|---|
| | | | | | | |

## 6. Decisiones técnicas relevantes

Decisiones que no están en el SPEC o que lo precisan (nunca contradicen reglas oficiales).

| Fecha | Fase | Decisión | Motivo | ¿Actualicé el SPEC? |
|---|---|---|---|---|
| | 6 | _Ej.: usar deserialización sin verificación de versión del evento de Stripe si el objeto viene vacío_ | _Versión de API de la cuenta distinta a la de la librería_ | |
| | | | | |

## 7. Fallos del calificador

| Fecha | Criterio | Mensaje del calificador | Regla de negocio afectada | Causa | Corrección | Nota antes → después |
|---|---|---|---|---|---|---|
| | | | | | | |

## 8. Rechazos y correcciones frecuentes (guía para revisar)

Cosas que suelen salir mal con un agente y conviene vigilar:

- Consultas sin filtro por salón, o `salonId` tomado de la URL o del cuerpo (R1).
- `now()` sin `Clock` (R15).
- Entidades JPA devueltas en respuestas.
- Reglas de negocio dentro de controladores.
- Lombok, Spring AI o dependencias no aprobadas.
- Llamada a la IA dentro de una transacción; crédito descontado antes de la respuesta o sin atomicidad (R19).
- Prompt con claves JSON distintas a `mensaje` o `respuesta`/`escalar`.
- Regla anti-promesas dejada solo en el prompt, sin validación en Java (R22).
- URL de Stripe o de la IA escrita fija en el código.
- Tests modificados para que pasen.
- Imports de `com.devsenior.atiende` o `com.fasterxml.jackson.databind`.
- Secretos o llaves de ejemplo reales en archivos versionados.

## 9. Pendientes

- [ ] 
- [ ] 

## 10. Verificación final de la bitácora

- [ ] Hay al menos una entrada por cada fase donde usé IA.
- [ ] Hay entradas con algo rechazado o corregido.
- [ ] Todo lo registrado ocurrió de verdad.
- [ ] No aparece ninguna llave, secreto ni dato personal real.
- [ ] Las entradas coinciden con el historial de commits.