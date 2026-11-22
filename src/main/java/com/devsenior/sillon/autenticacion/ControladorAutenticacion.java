
package com.devsenior.sillon.autenticacion;

import com.devsenior.sillon.compartido.RespuestaError;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(
    name = "Autenticación",
    description = "Registro de salones e inicio de sesión"
)
public class ControladorAutenticacion {

    private final ServicioAutenticacion servicioAutenticacion;

    public ControladorAutenticacion(
            ServicioAutenticacion servicioAutenticacion) {
        this.servicioAutenticacion = servicioAutenticacion;
    }

    @Operation(
        summary = "Registrar salón y dueño",
        description = "Crea un salón en plan GRATIS, registra "
                + "su dueño y devuelve un token JWT."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Salón y dueño registrados correctamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = RespuestaAutenticacion.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de registro inválidos",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "El correo ya está registrado",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        )
    })
    @PostMapping("/registro")
    public ResponseEntity<RespuestaAutenticacion> registrar(
            @Valid @RequestBody SolicitudRegistro solicitud) {

        RespuestaAutenticacion respuesta =
                servicioAutenticacion.registrar(solicitud);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @Operation(
        summary = "Iniciar sesión",
        description = "Valida las credenciales de un usuario "
                + "y devuelve un token JWT."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Inicio de sesión correcto",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = RespuestaAutenticacion.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de la solicitud inválidos",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciales incorrectas",
            content = @Content(
                schema = @Schema(
                    implementation = RespuestaError.class
                )
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<RespuestaAutenticacion> login(
            @Valid @RequestBody SolicitudLogin solicitud) {

        RespuestaAutenticacion respuesta =
                servicioAutenticacion.login(solicitud);

        return ResponseEntity.ok(respuesta);
    }
}
