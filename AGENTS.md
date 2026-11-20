# Trabajo sobre Sillón

Usa Java 17 y el paquete com.devsenior.sillon. Organiza los cambios dentro del dominio de negocio que corresponda. Contrasta cada cambio con SPEC.md y con el DTO de entrada o salida documentado en Swagger. Consulta antes de incorporar otra dependencia de producción.

Obtén el salón del usuario autenticado para los datos privados. Las rutas públicas resuelven el salón por slug. Comprueba a qué salón pertenecen los identificadores antes de relacionar entidades. Usar un ID ajeno debe producir el mismo 404 que uno desconocido.

Inyecta Clock con la zona del negocio. Mantén el cálculo de horarios, precios y créditos en Java. Guarda los prompts en recursos y usa proveedores intercambiables con tiempos máximos de espera. Nunca incluyas secretos en prompts, archivos versionados, pruebas o logs.

Ejecuta el wrapper de Maven y la colección Postman tras cambiar la API. Investiga cada test rojo contra la regla que protege. Conserva las pruebas oficiales de .github y registra en la bitácora el uso real de IA y las correcciones verificadas.
