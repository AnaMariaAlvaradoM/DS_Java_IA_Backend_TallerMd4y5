# Bitácora de IA · Sillón

| # | Fase | Herramienta y modo | Para qué la usé | Qué revisé | Qué rechacé o corregí |
|---|------|--------------------|-----------------|------------|-----------------------|
| 1 | 1    | Cloude developer   | Explicar el slug| El código  | Lo acepte pero revise bien |
| 2 | 3    | Antigravity Agent  | Generación de entidades, DTOs y validación de orden para catálogo y equipo | Estructura de ManyToMany, validaciones de límite de plan (402), correo (409) y servicios (404), y métodos de búsqueda por salón | Se corrigió el orden de validación para asegurar fail-fast en el límite del plan antes de verificar correo o servicios, y se garantizó la conversión a DTOs dentro de métodos transaccionales sin exponer entidades JPA |
| 3 | 5    | Antigravity Agent  | Implementación de catálogo público, disponibilidad y reservas en línea sin token | Aislamiento público en DTOs (sin credenciales), cálculo de franjas horarias con ReglasAgenda, y formateo de horario en español de Colombia | Se verificó que EstilistaPublicoRespuesta solo exponga id, nombre y servicioIds. Se normalizaron los espacios en el formateo de a. m. / p. m. ("8:00 a. m." / "7:00 p. m.") para cumplir exactamente con la SPEC |
