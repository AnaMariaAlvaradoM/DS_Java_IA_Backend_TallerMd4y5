
package com.devsenior.sillon.compartido;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.models.tags.Tag;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EtiquetasOpenApi {

    @Bean
    public OpenApiCustomizer organizarEtiquetas() {
        return api -> {

            if (api.getPaths() == null) {
                return;
            }

            Set<String> presentes = new HashSet<>();

            api.getPaths().forEach((ruta, pathItem) -> {

                String etiqueta = obtenerEtiqueta(ruta);

                if (etiqueta != null) {
                    pathItem.readOperations().forEach(
                        operacion -> operacion.setTags(
                            List.of(etiqueta)
                        )
                    );

                    presentes.add(etiqueta);
                }
            });

            List<String> orden = List.of(
                "0 Público",
                "1 Autenticación",
                "2 Mi salón",
                "3 Servicios",
                "4 Estilistas",
                "5 Clientes",
                "6 Agenda",
                "7 Pagos",
                "8 Solicitudes",
                "9 Administración"
            );

            api.setTags(
                orden.stream()
                    .filter(presentes::contains)
                    .map(nombre -> new Tag().name(nombre))
                    .toList()
            );
        };
    }

    private String obtenerEtiqueta(String ruta) {

        if (ruta.startsWith("/api/publico/")) {
            return "0 Público";
        }

        if (ruta.startsWith("/api/auth/")) {
            return "1 Autenticación";
        }

        if (ruta.equals("/api/salon")
                || ruta.startsWith("/api/salon/")) {
            return "2 Mi salón";
        }

        if (ruta.startsWith("/api/servicios")) {
            return "3 Servicios";
        }

        if (ruta.startsWith("/api/estilistas")) {
            return "4 Estilistas";
        }

        if (ruta.startsWith("/api/clientes")) {
            return "5 Clientes";
        }

        if (ruta.startsWith("/api/citas")) {
            return "6 Agenda";
        }

        if (ruta.startsWith("/api/pagos/")) {
            return "7 Pagos";
        }

        if (ruta.startsWith("/api/solicitudes")) {
            return "8 Solicitudes";
        }

        if (ruta.startsWith("/api/admin/")) {
            return "9 Administración";
        }

        return null;
    }
}
