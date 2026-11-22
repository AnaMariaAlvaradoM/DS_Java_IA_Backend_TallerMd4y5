
package com.devsenior.sillon.cliente;

import java.util.List;

import com.devsenior.sillon.compartido.RespuestaError;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
@Tag(
    name = "Clientes",
    description = "Consulta de clientes registrados en el salón"
)
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @Operation(
        summary = "Listar clientes del salón",
        description = "Permite al dueño consultar los clientes "
                + "registrados en su salón. "
                + "Cada cliente incluye su identificador, nombre y teléfono."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de clientes obtenida correctamente",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(
                    schema = @Schema(
                        implementation = ClienteResponse.class
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
    public List<ClienteResponse> listar(
            @AuthenticationPrincipal(expression = "salonId")
            Long salonId
    ) {
        return clienteService.listarPorSalon(salonId)
                .stream()
                .map(ClienteMapper::toResponse)
                .toList();
    }
}
