
package com.devsenior.sillon.publico;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/publico/salones")
@Tag(
    name = "Catálogo público",
    description = "Consulta pública de información y catálogo de servicios de los salones, sin autenticación"
)
public class CatalogoPublicoController {

    private final CatalogoService catalogoService;

    public CatalogoPublicoController(
            CatalogoService catalogoService
    ) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/{slug}")
    public CatalogoPublicoResponse obtenerCatalogo(
            @PathVariable String slug
    ) {
        return catalogoService.obtenerCatalogo(slug);
    }
}
