
package com.devsenior.sillon.salon;

import com.devsenior.sillon.compartido.RespuestaError;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salon")
@Tag(
    name = "Salones",
    description = "Consulta y configuración del salón autenticado"
)
public class ControladorSalon {

    private final ServicioSalon servicioSalon;

    public ControladorSalon(ServicioSalon servicioSalon) {
        this.servicioSalon = servicioSalon;
    }

    @Operation(
        summary = "Consultar mi salón",
        description = "Permite al dueño consultar los datos, "
                + "el plan, los créditos y la configuración de su salón."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Datos del salón obtenidos correctamente",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaSalon.class
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
    public ResponseEntity<RespuestaSalon> consultar(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        RespuestaSalon respuesta =
                servicioSalon.consultar(usuario.getSalonId());

        return ResponseEntity.ok(respuesta);
    }

    @Operation(
        summary = "Actualizar configuración del salón",
        description = "Permite al dueño modificar el horario "
                + "de apertura, cierre, domingos e intervalo de agenda."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Configuración actualizada correctamente",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaSalon.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de configuración inválidos",
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
            responseCode = "409",
            description = "El horario de apertura y cierre incumple las reglas del salón",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        )
    })
    @PutMapping("/configuracion")
    public ResponseEntity<RespuestaSalon> actualizarConfiguracion(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody SolicitudConfiguracionSalon solicitud) {

        RespuestaSalon respuesta =
                servicioSalon.actualizarConfiguracion(
                        usuario.getSalonId(),
                        solicitud
                );

        return ResponseEntity.ok(respuesta);
    }
}
