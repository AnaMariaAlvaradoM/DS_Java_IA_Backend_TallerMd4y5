# Reglas para el agente · Sillón

- Paquete base com.devsenior.sillon, organizado por dominios (auth, salon, usuario, servicio, cliente, cita, publico, pago, chat, ia, compartido, seguridad).
- Toda consulta se filtra por el salón del usuario autenticado. Si el dato es de otro salón, responder 404.
- "Hoy" y "ahora" se calculan con el bean Clock de la zona America/Bogota, nunca con la hora del servidor.
- Los prompts viven en archivos dentro de src/main/resources/prompts.
- Java calcula y valida; la IA solo redacta.
- Nunca modificar un test para que pase.
- No agregar dependencias sin preguntar.