
package com.devsenior.sillon.servicio;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicios")
@Tag(
    name = "Servicios",
    description = "Gestión del catálogo de servicios del salón"
)
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @Operation(
        summary = "Crear servicio",
        description = "Permite al dueño registrar un servicio "
                + "con nombre, descripción, duración y precio."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Servicio creado correctamente",
            content = @Content(
                schema = @Schema(
                    implementation = ServicioResponse.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos del servicio inválidos",
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
            responseCode = "403",
            description = "El usuario no tiene permisos de dueño",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        )
    })
    @PostMapping
    public ResponseEntity<ServicioResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ServicioRequest request) {

        ServicioResponse respuesta = servicioService.crear(
                usuario.getSalonId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @Operation(
        summary = "Listar servicios",
        description = "Consulta los servicios del salón autenticado. "
                + "Disponible para dueños y estilistas."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de servicios obtenida correctamente",
            content = @Content(
                array = @ArraySchema(
                    schema = @Schema(
                        implementation = ServicioResponse.class
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
            description = "El usuario no tiene permisos",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<ServicioResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        List<ServicioResponse> servicios =
                servicioService.listar(usuario.getSalonId());

        return ResponseEntity.ok(servicios);
    }

    @Operation(
        summary = "Actualizar servicio",
        description = "Permite al dueño modificar un servicio "
                + "existente de su salón."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Servicio actualizado correctamente",
            content = @Content(
                schema = @Schema(
                    implementation = ServicioResponse.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos del servicio inválidos",
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
            description = "El servicio no existe o pertenece a otro salón",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ServicioResponse> actualizar(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id,
            @Valid @RequestBody ServicioRequest request) {

        ServicioResponse respuesta = servicioService.actualizar(
                usuario.getSalonId(),
                id,
                request
        );

        return ResponseEntity.ok(respuesta);
    }
}
