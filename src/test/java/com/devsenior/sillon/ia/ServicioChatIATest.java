
package com.devsenior.sillon.ia;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.ServicioService;
import com.devsenior.sillon.solicitud.ServicioSolicitudesContacto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioChatIATest {

    @Test
    void sinCreditosNoDebeConsultarLaIA() {

        SalonRepository salones = mock(SalonRepository.class);
        ServicioService catalogo = mock(ServicioService.class);
        ServicioCreditosIA creditos = mock(ServicioCreditosIA.class);
        BibliotecaPrompts prompts = mock(BibliotecaPrompts.class);
        AsistenteIA asistente = mock(AsistenteIA.class);
        LectorJsonIA lector = mock(LectorJsonIA.class);
        ServicioSolicitudesContacto contactos =
                mock(ServicioSolicitudesContacto.class);

        ServicioChatIA servicio = new ServicioChatIA(
                salones,
                catalogo,
                creditos,
                prompts,
                asistente,
                lector,
                contactos
        );

        Salon salon = new Salon();
        salon.setId(1L);

        when(salones.findBySlug("barberia-la-navaja"))
                .thenReturn(Optional.of(salon));

        doThrow(new PagoRequeridoException(
                "El salón no tiene créditos de IA disponibles"
        )).when(creditos).verificar(1L);

        SolicitudChatIA solicitud = new SolicitudChatIA(
                "Carlos Pérez",
                "3001234567",
                List.of(new SolicitudChatIA.Turno(
                        SolicitudChatIA.RolTurno.CLIENTE,
                        "¿Cuánto cuesta el corte clásico?"
                ))
        );

        assertThrows(
                PagoRequeridoException.class,
                () -> servicio.responder(
                        "barberia-la-navaja",
                        solicitud
                )
        );

        verifyNoInteractions(
                catalogo,
                prompts,
                asistente,
                lector,
                contactos
        );

        verify(creditos, never()).consumir(anyLong());
    }
}
