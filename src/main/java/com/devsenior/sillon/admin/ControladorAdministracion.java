
package com.devsenior.sillon.admin;

import java.util.List;

import com.devsenior.sillon.compartido.RespuestaError;
import com.devsenior.sillon.salon.RespuestaSalon;
import com.devsenior.sillon.salon.SalonRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(
    name = "9 Administración",
    description = "Administración general de los salones de la plataforma"
)
public class ControladorAdministracion {

    private final SalonRepository salonRepository;

    public ControladorAdministracion(
            SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    @Operation(
        summary = "Listar todos los salones",
        description = "Permite al administrador consultar "
                + "todos los salones registrados en Sillón. "
                + "Requiere el rol ADMIN."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de salones obtenida correctamente",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(
                    schema = @Schema(
                        implementation = RespuestaSalon.class
                    )
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token JWT ausente o inválido",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Se requiere el rol ADMIN",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        )
    })
    @GetMapping("/salones")
    @Transactional(readOnly = true)
    public List<RespuestaSalon> listarSalones() {

        return salonRepository.findAll()
                .stream()
                .map(RespuestaSalon::desde)
                .toList();
    }
}
