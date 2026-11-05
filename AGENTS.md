# Instrucciones para trabajar en Sillón

## Fuentes de verdad

- Consultar el enunciado de la actividad y el contrato de la API
  antes de implementar una funcionalidad.
- Usar SPEC.md como resumen de las reglas de negocio.
- Si SPEC.md contradice el enunciado, corregir SPEC.md.
- No inventar endpoints, roles, estados ni restricciones.

## Tecnologías y estructura

- Usar Java 17, Spring Boot y Maven.
- Mantener el paquete base com.devsenior.sillon.
- No usar Lombok.
- Integrar la IA mediante HTTP con RestClient, sin SDK de IA.
- Separar controladores, servicios, repositorios, entidades y DTO.
- Mantener las reglas de negocio en la capa de servicios.
- Usar MySQL en local y PostgreSQL en producción.

## Seguridad

- Comprobar roles y pertenencia al salón en operaciones privadas.
- Devolver 404 al intentar acceder a recursos de otro salón.
- Guardar contraseñas de usuarios mediante BCrypt.
- No incluir contraseñas, tokens ni claves reales en Git.
- Mantener secrets.properties excluido del repositorio.
- Usar únicamente valores de ejemplo en las plantillas.
- No registrar credenciales en logs ni incluirlas en prompts.

## Verificación

- Respetar las rutas, campos y códigos HTTP del contrato.
- Añadir pruebas de reglas de negocio y casos de error.
- Probar aislamiento entre salones, cruces de citas,
  permisos, pagos repetidos y consumo de créditos.
- No presentar una comprobación como exitosa sin ejecutarla.
- Documentar las limitaciones y verificaciones pendientes.

## Entrega y uso de IA

- No modificar la carpeta .github.
- Registrar en BITACORA-IA.md el uso real de IA,
  las decisiones tomadas y las comprobaciones realizadas.
- No inventar evidencias, resultados de pruebas ni entradas
  de la bitácora.
- Revisar los cambios antes de confirmarlos en Git.