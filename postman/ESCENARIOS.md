# Escenarios de Postman

Fuente: PDF, páginas 30–33, escenarios 1–52. Son 55 peticiones porque los escenarios 10, 12 y 37 usan dos peticiones. Cada petición tiene pruebas post-response; todos los rechazos comprueban además el formato único de error.

| Escenario | Carpeta | Petición |
|---|---|---|
| 01 | 1 | Registrar salón A |
| 02 | 1 | Correo ya registrado |
| 03 | 1 | Registro inválido |
| 04 | 1 | Login dueño A |
| 05 | 1 | Contraseña incorrecta |
| 06 | 1 | Mi salón sin token |
| 07 | 1 | Mi salón GRATIS |
| 08 | 1 | Configurar horario |
| 09 | 1 | Apertura posterior al cierre |
| 10 | 2 | Crear Corte clásico |
| 10 | 2 | Crear Arreglo de barba |
| 11 | 2 | Servicio de cinco minutos y precio negativo |
| 12 | 2 | Crear Mateo con corte y barba |
| 12 | 2 | Crear Sara con corte |
| 13 | 2 | Tercer estilista en GRATIS |
| 14 | 2 | Listar dos estilistas |
| 15 | 3 | Catálogo público |
| 16 | 3 | Disponibilidad de Mateo |
| 17 | 3 | Reserva pública Mateo 10:00 |
| 18 | 3 | Disponibilidad después de reservar |
| 19 | 3 | Cruce con Mateo 10:30 |
| 20 | 3 | Cita Sara 10:30 |
| 21 | 3 | Sara no ofrece barba |
| 22 | 3 | Corte termina después del cierre |
| 23 | 3 | Cita en el pasado |
| 24 | 3 | Cita en domingo |
| 25 | 3 | Agenda del dueño |
| 26 | 3 | Confirmar reserva |
| 27 | 3 | No volver a PENDIENTE |
| 28 | 3 | Login Mateo |
| 29 | 3 | Agenda de Mateo |
| 30 | 3 | Mateo no crea servicios |
| 31 | 3 | Mateo no toca cita de Sara |
| 32 | 3 | Mateo no cancela su cita |
| 33 | 3 | Mateo completa su cita |
| 34 | 3 | Clientes del salón |
| 35 | 4 | Registrar salón B |
| 36 | 4 | B no cambia cita de A |
| 37 | 4 | Servicios de B vacíos |
| 37 | 4 | Agenda de B vacía |
| 38 | 4 | Dueño A no entra a administración |
| 39 | 5 | Recordatorio de Sara |
| 40 | 5 | Sin recordatorio de COMPLETADA |
| 41 | 5 | Chat precio del corte |
| 42 | 5 | Chat quiere reservar |
| 43 | 5 | Intento de cupón SILLA50 |
| 44 | 5 | Chat sin mensajes |
| 45 | 5 | Chat de salón inexistente |
| 46 | 5 | Solicitudes del dueño |
| 47 | 6 | Crear pago PRO |
| 48 | 6 | Crear pago de créditos |
| 49 | 6 | Webhook sin firma |
| 50 | 6 | Estilista no paga |
| 51 | 7 | Mi salón después del pago manual |
| 52 | 7 | Crear tercer estilista en PRO |

Las carpetas 1–6 se ejecutan de arriba a abajo sin copiar tokens. Cada ejecución reinicia sus variables y crea otros salones. La carpeta 7 debe conservar las variables de la misma ejecución y ejecutarse después del pago manual; no se incluye en el recorrido automático inicial.

La carpeta 5 usa IA real configurada. Una respuesta externa fallida deja la prueba en rojo: no se acepta como resultado correcto ni se sustituye por una respuesta simulada en esta colección.

Resultados del 2026-10-27: carpetas 1–6 aprobadas en local y producción, con 107 aserciones sin fallos en cada entorno. Carpeta 7 aprobada en producción después del pago manual, con cuatro aserciones sin fallos; no se ejecutó en local. Una copia temporal con código HTTP esperado incorrecto falló, como exige el PDF.

El historial de fallos, evidencia y pendientes se registra en [VERIFICACION.md](../VERIFICACION.md). PR y calificador oficial siguen pendientes por instrucción del estudiante; estos resultados no constituyen una nota académica.
