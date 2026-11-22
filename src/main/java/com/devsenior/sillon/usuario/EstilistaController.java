
package com.devsenior.sillon.usuario;

import java.util.List;

import com.devsenior.sillon.compartido.RespuestaError;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estilistas")
@Tag(
    name = "Estilistas",
    description = "Registro y consulta de estilistas del salón"
)
public class EstilistaController {

    private final EstilistaService estilistaService;

    public EstilistaController(EstilistaService estilistaService) {
        this.estilistaService = estilistaService;
    }

    @Operation(
        summary = "Registrar estilista",
        description = "Permite al dueño registrar un estilista "
                + "y asociarlo con servicios de su salón. "
                + "El plan GRATIS permite máximo dos estilistas."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Estilista registrado correctamente",
            content = @Content(
                schema = @Schema(
                    implementation = EstilistaResponse.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos del estilista inválidos",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
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
            responseCode = "402",
            description = "El salón alcanzó el límite de estilistas del plan GRATIS",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "El usuario no tiene permisos de dueño",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Uno de los servicios no existe o pertenece a otro salón",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "El correo electrónico ya está registrado",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        )
    })
    @PostMapping
    public ResponseEntity<EstilistaResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody EstilistaRequest request) {

        EstilistaResponse respuesta = estilistaService.crear(
                usuario.getSalonId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @Operation(
        summary = "Listar estilistas",
        description = "Consulta los estilistas registrados "
                + "en el salón autenticado."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de estilistas obtenida correctamente",
            content = @Content(
                array = @ArraySchema(
                    schema = @Schema(
                        implementation = EstilistaResponse.class
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
            description = "El usuario no tiene permisos de dueño",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<EstilistaResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        List<EstilistaResponse> estilistas =
                estilistaService.listar(usuario.getSalonId());

        return ResponseEntity.ok(estilistas);
    }
}
