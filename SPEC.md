# Contrato de Sillón

## Salón, identidad y catálogo

La barbería es la unidad de aislamiento. El JWT identifica el correo y el salón. La API comprueba que el usuario siga activo y aplica su rol actual. La creación del dueño y del salón comparte una transacción. El email se normaliza a minúsculas y es único en la plataforma. El nombre se convierte en un slug sin tildes. Ante una coincidencia se agrega un sufijo numérico.

Un salón nuevo comienza en GRATIS con diez créditos y atención de lunes a sábado, de 08:00 a 19:00, cada 30 minutos. El intervalo configurable va de 15 a 60 minutos. La apertura tiene que ser anterior al cierre. Un servicio dura de 15 a 480 minutos y cuesta un número entero positivo de pesos colombianos. Se puede desactivar y deja de aparecer en el catálogo público.

Al incorporar un estilista se comprueba primero el límite del plan, después la unicidad del correo y por último la pertenencia de los servicios. Así, un salón que ya alcanzó el límite recibe el mismo 402 aunque cambie los datos de la persona. El plan GRATIS admite dos estilistas. PRO permite ampliar el equipo. Cada estilista ofrece al menos un servicio del propio salón.

## Agenda

La fecha y la hora del negocio corresponden a Bogotá. Se comprueba el salón, el servicio activo y el estilista que lo ofrece. Java suma la duración al inicio, valida el horario y comprueba la intersección con citas PENDIENTES o CONFIRMADAS del mismo estilista. Dos intervalos se cruzan si el primero empieza antes de que termine el segundo y acaba después de su inicio. Las citas consecutivas pueden compartir el instante de fin e inicio. La disponibilidad recorre franjas desde la apertura y descarta las anteriores al momento de la consulta.

Al reservar se busca el cliente por salón y teléfono normalizado. Una reserva pública nace PENDIENTE y EN_LINEA. Una cita creada por el dueño nace CONFIRMADA y SALON. El precio y la hora de fin quedan guardados en la cita. Un cambio posterior al servicio conserva lo pactado.

| Estado inicial | Destino permitido | Situación |
|---|---|---|
| PENDIENTE | CONFIRMADA | El salón acepta la reserva |
| PENDIENTE | CANCELADA | El cliente desiste antes de confirmar |
| CONFIRMADA | COMPLETADA | Terminó la atención |
| CONFIRMADA | CANCELADA | Se canceló el turno |
| CONFIRMADA | NO_ASISTIO | El cliente no llegó |

Los tres últimos destinos de una cita confirmada son finales. El estilista solo puede completar sus citas o registrar una inasistencia. Consultar una cita ajena devuelve 404. Las reservas concurrentes se serializan sobre el salón antes de leer las ocupaciones.

## Pagos

El servidor elige el precio y el modo de Checkout. El plan PRO se ofrece como suscripción mensual y el paquete como pago único. La URL de retorno informa al navegador. Solo el webhook firmado cambia el saldo. Un pago confirmado de PRO suma 300 créditos y guarda el identificador de suscripción. El paquete suma 200. Cancelar la suscripción devuelve el plan a GRATIS y conserva el saldo. Cada evento se identifica por su ID y cada operación por su sesión de Checkout o su suscripción cancelada. La transacción guarda el registro y el efecto económico juntos.

## IA y atención humana

La generación se ejecuta fuera de transacciones de base de datos. La API comprueba el saldo, llama al proveedor y consume un crédito con una actualización condicional cuando llega la respuesta. Una respuesta de modelo que contiene JSON inválido ya incurrió en consumo. Los fallos de conexión de todos los proveedores devuelven 503. Una respuesta incompleta o inválida devuelve 502.

El recordatorio sirve para citas pendientes o confirmadas. Java arma los seis datos verificados y una persona revisa el borrador antes de enviarlo. El chatbot usa el catálogo activo y recibe el historial en cada petición. Las reservas, cambios, reclamos o conversaciones que requieren una persona generan una solicitud. Una oferta de descuentos, cupones, servicios gratis, promociones o reembolsos en el texto del modelo se sustituye por una respuesta fija y se registra con el motivo REGLA_DE_SEGURIDAD. La bandeja del dueño permite marcar cada solicitud atendida una vez.

## Contrato HTTP

| Acceso | Método y ruta | Uso |
|---|---|---|
| Público | `POST /api/auth/registro`, `POST /api/auth/login` | Registrar salón y dueño, obtener JWT |
| Dueño | `GET /api/salon`, `PUT /api/salon/configuracion` | Consultar el plan, créditos y horario |
| Dueño o estilista | `GET /api/servicios` | Consultar servicios del propio salón |
| Dueño | `POST /api/servicios`, `PUT /api/servicios/{id}` | Crear, editar o desactivar un servicio |
| Dueño | `GET /api/estilistas`, `POST /api/estilistas`, `GET /api/clientes` | Administrar el equipo y consultar clientes |
| Dueño | `POST /api/citas`, `POST /api/citas/{id}/recordatorio` | Agendar o preparar un borrador para WhatsApp |
| Dueño o estilista | `GET /api/citas?fecha=AAAA-MM-DD`, `PATCH /api/citas/{id}/estado` | Revisar la agenda o cambiar el estado según el rol |
| Público | `GET /api/publico/salones/{slug}`, `GET /api/publico/salones/{slug}/disponibilidad` | Consultar catálogo o franjas por fecha, servicio y estilista |
| Público | `POST /api/publico/salones/{slug}/reservas`, `POST /api/publico/salones/{slug}/chat` | Reservar en línea o consultar al asistente |
| Dueño | `GET /api/solicitudes`, `PATCH /api/solicitudes/{id}/atendida` | Gestionar consultas derivadas por el chatbot |
| Dueño | `POST /api/pagos/suscripcion`, `POST /api/pagos/creditos` | Crear una sesión de Stripe Checkout |
| Stripe | `POST /api/pagos/webhook` | Recibir el evento con su firma |
| Público | `GET /api/pagos/resultado?estado=exito` | Mostrar el regreso desde Checkout |
| Administrador | `GET /api/admin/salones` | Listar todos los salones |

Los errores incluyen `codigo`, `error`, `mensaje` y `fecha`. Los DTO y sus restricciones están disponibles en Swagger. Las validaciones de entrada responden 400, las credenciales inválidas 401 y los permisos insuficientes 403. Las reglas incompatibles responden 409 y los límites del plan o crédito 402. Las operaciones de creación de recursos responden 201.

El alcance comprende el backend y las pruebas. Quedan fuera el envío automático de WhatsApp o correo, reportes, tareas programadas, una interfaz gráfica y cobros del salón a sus clientes.
