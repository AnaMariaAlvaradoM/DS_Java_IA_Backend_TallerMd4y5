package com.devsenior.sillon.compartido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.devsenior.sillon.compartido.excepcion.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.excepcion.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.excepcion.ErrorDePasarelaException;
import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;

class ManejadorGlobalErroresTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 20, 10, 30, 0);
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private ManejadorGlobalErrores manejador;

    @BeforeEach
    void configurar() {
        Clock relojFijo = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        manejador = new ManejadorGlobalErrores(relojFijo);
    }

    @Test
    void cadaExcepcionPropiaProduceSuCodigoYNombreEstandar() {
        verificar(manejador.manejarNoEncontrado(new RecursoNoEncontradoException("x")), 404, "Not Found");
        verificar(manejador.manejarReglaDeNegocio(new ReglaDeNegocioException("x")), 409, "Conflict");
        verificar(manejador.manejarPagoRequerido(new PagoRequeridoException("x")), 402, "Payment Required");
        verificar(manejador.manejarAccionNoPermitida(new AccionNoPermitidaException("x")), 403, "Forbidden");
        verificar(manejador.manejarCredencialesInvalidas(new CredencialesInvalidasException("x")), 401,
                "Unauthorized");
        verificar(manejador.manejarFirmaInvalida(new FirmaInvalidaException("x")), 400, "Bad Request");
        verificar(manejador.manejarErrorDePasarela(new ErrorDePasarelaException("x")), 502, "Bad Gateway");
    }

    @Test
    void elMensajeDeNegocioYLaFechaDelRelojViajanEnLaRespuesta() {
        ResponseEntity<RespuestaError> respuesta = manejador
                .manejarReglaDeNegocio(new ReglaDeNegocioException("El correo ya existe"));

        assertThat(respuesta.getBody().mensaje()).isEqualTo("El correo ya existe");
        assertThat(respuesta.getBody().fecha()).isEqualTo(AHORA);
    }

    @Test
    void laValidacionNombraLosCamposSeparadosPorPuntoYComa() {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "peticion");
        resultado.addError(new FieldError("peticion", "email", "formato inválido"));
        resultado.addError(new FieldError("peticion", "password", "debe tener entre 8 y 72 caracteres"));
        MethodArgumentNotValidException excepcion = mock(MethodArgumentNotValidException.class);
        when(excepcion.getBindingResult()).thenReturn(resultado);

        ResponseEntity<RespuestaError> respuesta = manejador.manejarValidacion(excepcion);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        assertThat(respuesta.getBody().mensaje())
                .isEqualTo("email: formato inválido; password: debe tener entre 8 y 72 caracteres");
    }

    @Test
    void laValidacionConUnSoloCampoNoLlevaPuntoYComa() {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "peticion");
        resultado.addError(new FieldError("peticion", "nombre", "es obligatorio"));
        MethodArgumentNotValidException excepcion = mock(MethodArgumentNotValidException.class);
        when(excepcion.getBindingResult()).thenReturn(resultado);

        ResponseEntity<RespuestaError> respuesta = manejador.manejarValidacion(excepcion);

        assertThat(respuesta.getBody().mensaje()).isEqualTo("nombre: es obligatorio");
    }

    private void verificar(ResponseEntity<RespuestaError> respuesta, int codigoEsperado, String errorEsperado) {
        assertThat(respuesta.getStatusCode().value()).isEqualTo(codigoEsperado);
        RespuestaError cuerpo = respuesta.getBody();
        assertThat(cuerpo.codigo()).isEqualTo(codigoEsperado);
        assertThat(cuerpo.error()).isEqualTo(errorEsperado);
        assertThat(cuerpo.fecha()).isEqualTo(AHORA);
    }
}
