# Postman de Sillón

Colección v2.1 basada en `SPEC.md` y en P.1-P.3 del PDF oficial (páginas 30-32).
Incluye **62 peticiones reales**, cada una con su script Post-response y dos
pruebas: estado HTTP y contrato/regla del escenario. Las carpetas 1-6 contienen
60 peticiones; las dos de la carpeta 7 requieren pagos de prueba manuales.

## Archivos e importación

- [Colección](sillon.postman_collection.json).
- [Entorno local](local.postman_environment.json): `baseUrl=http://localhost:8080`.
- [Entorno de producción](produccion.postman_environment.json): `baseUrl` vacío
  deliberadamente. Introduzca la URL **real** de Render, sin barra final.
  No se presupone que el despliegue ya exista.

Importe los tres JSON en Postman y seleccione el entorno. Cada entorno contiene
únicamente `baseUrl`, como exige el PDF. Los scripts fallan explícitamente si
falta esa URL; no se sustituye producción por localhost.

## Antes de ejecutar

1. Use una instancia de pruebas de la API con su base de datos disponible.
   La colección **crea datos persistentes**: dos salones, cuentas, servicios,
   citas y solicitudes. No borra datos porque la API no ofrece esas operaciones.
2. Configure en el **servidor**, no en Postman, un proveedor de IA operativo o
   simulado, y Stripe exclusivamente en **modo prueba**, con sus precios y URL
   pública configurados. La carpeta 5 consume créditos de IA y la 6 crea sesiones
   Checkout. No ejecutar contra Stripe en modo real.
3. Las credenciales y los tokens del servidor nunca se incluyen en esta entrega.
   La primera petición genera una contraseña aleatoria para cuentas desechables
   y correos `@example.test`; no es una contraseña personal o preexistente.

## Runner: carpetas 1 a 6

Ejecute las carpetas en orden y con una iteración. No copie tokens ni IDs:
los Post-response los guardan en variables de colección.

| Carpeta | Peticiones | Evidencia |
|---|---:|---|
| 1. Registro, login y configuración | 9 | Registro, duplicados, validación, JWT y horario |
| 2. Catálogo y equipo | 8 | Servicios, actualización, estilistas y límite GRATIS |
| 3. Agenda: reservas, cruces y estados | 20 | Disponibilidad, reservas, cruces, fechas, estados y permisos |
| 4. Aislamiento entre salones y roles | 5 | Salón B, listas aisladas y acceso restringido |
| 5. IA: recordatorio y chatbot | 11 | Factura, créditos, escalamiento, anti-promesas y atención |
| 6. Pagos con Stripe (modo prueba) | 7 | Checkout, firma ausente, permisos y resultado informativo |
| 7. Después de pagar (manual) | 2 | Plan PRO, créditos y tercer estilista |
| **Total** | **62** | **62 scripts Post-response; 124 pruebas declaradas** |

La primera petición:

- Genera un sufijo con fecha/hora y UUID para nombres de salón y correos únicos.
- Limpia las variables de una ejecución anterior.
- Calcula las fechas en `America/Bogota`: un día de lunes a sábado, al menos dos
  días adelante; el próximo domingo; y ayer para rechazar citas pasadas.
- Prepara `emailA`, `emailB`, `emailMateo`, `emailSara` y `passwordPrueba`.

Las siguientes peticiones capturan `tokenA`, `tokenB`, `tokenMateo`, IDs y slugs
de salones, servicios, estilistas, citas, solicitudes y sesiones de pago.
El token del dueño A sigue siendo la autenticación heredada: las peticiones del
salón B o de Mateo usan explícitamente su propio token; las públicas usan No Auth.

Una nueva ejecución completa crea datos nuevos. Para ejecutar una petición
aislada, primero complete sus pasos previos. Las pruebas de IA comprueban reglas,
no una redacción idéntica del modelo; una respuesta inválida no se acepta como
200 ni se oculta con una prueba permisiva.

## Newman, si está instalado

Desde la raíz del repositorio, con Newman 6.2 o posterior:

```powershell
newman run .\postman\sillon.postman_collection.json -e .\postman\local.postman_environment.json --timeout-request 180000 --bail
```

Para Render, complete primero `baseUrl` en el entorno de producción y cambie
`local.postman_environment.json` por `produccion.postman_environment.json`.
El timeout contempla el arranque en frío de Render. Un fallo detiene la ejecución
por `--bail`; no use `--suppress-exit-code` para ocultarlo.

La carpeta 7 se omite de forma explícita mientras `ejecutarManual=false`, su valor
inicial. Eso **no acredita** sus pruebas. En la creación de esta entrega no había
Newman instalado ni una API escuchando en el puerto local 8080: la validación
estática de JSON y scripts no sustituye una ejecución contra el backend.

## Carpeta 7: solo después de pagos de prueba

1. Termine las carpetas 1-6 sin fallos.
2. Abra las URLs capturadas en `urlPagoPro` y `urlPagoCreditos`.
3. Complete **ambos** Checkout en modo prueba con la tarjeta pública de pruebas
   `4242 4242 4242 4242`, fecha futura y CVC de prueba. Nunca use una tarjeta real.
4. Espere los webhooks legítimos firmados. Ni abrir una URL de resultado ni crear
   una sesión aplica beneficios.
5. En las variables de colección, cambie `ejecutarManual` a `true` y ejecute
   **solo la carpeta 7** en Postman. No vuelva a ejecutar la carpeta 1, pues inicia
   otra ejecución y restablece esa variable.

Se comprueba PRO, más de 300 créditos y la creación del tercer estilista.
No se firman eventos ni se guardan secretos de webhook en Postman. La creación del
tercer estilista debe ejecutarse una vez por ejecución completa: repetirla con
el mismo correo produce el conflicto legítimo de duplicado.

## Cobertura y pendientes

Los escenarios P.3 se implementan en su orden relativo, salvo la excepción
administrativa indicada abajo. Los escenarios que crean dos recursos o consultan
dos listas se separan en peticiones HTTP reales. Se añaden actualización de
servicio, atención de solicitudes con repetición/aislamiento y resultado de pago.

- **Escenario 38 del PDF pendiente:** `GET /api/admin/salones` figura en el
  contrato, pero el código actual no tiene un controlador para esa operación,
  solo una restricción de seguridad para `/api/admin/**`. Obtener 403 por ese
  filtro no demostraría que existe el endpoint. No se incluye esa petición ni se
  modifica el backend; en su lugar hay una comprobación adicional de rol sobre
  `GET /api/salon` con el token de Mateo. No se afirma que sustituya el escenario.
- Falta ejecutar las carpetas 1-6 con Postman/Newman contra local y contra la URL
  real de Render, y comprobar la carpeta 7 después de los pagos de prueba.
- La URL real de Render sigue pendiente: el entorno de producción se entrega sin
  una dirección inventada. Esta tarea no modifica el README raíz ni el despliegue.
- Antes de guardar o exportar otra vez, vacíe contraseñas y tokens generados.
  No publique JWT, URLs de sesiones ya creadas, entornos personales ni reportes
  que incluyan cabeceras de autenticación.
