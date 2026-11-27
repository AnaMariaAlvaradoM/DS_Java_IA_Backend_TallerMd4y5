package com.devsenior.sillon.servicio;

import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.devsenior.sillon.servicio.dto.ServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioServiceTest {

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private SalonRepository salonRepository;

    @Test
    void crearServicioPermiteDescripcionOmitida() {
        Long salonId = 12L;
        Salon salon = new Salon();
        salon.setId(salonId);
        when(salonRepository.findById(salonId)).thenReturn(Optional.of(salon));

        ServicioService servicioService = new ServicioService(servicioRepository, salonRepository);
        ServicioRequest request = new ServicioRequest("Arreglo de barba", null, 30, 20000, true);

        ServicioResponse response = servicioService.crearServicio(request, salonId);

        ArgumentCaptor<Servicio> servicioCaptor = ArgumentCaptor.forClass(Servicio.class);
        verify(servicioRepository).save(servicioCaptor.capture());
        assertNull(servicioCaptor.getValue().getDescripcion());
        assertSame(salon, servicioCaptor.getValue().getSalon());
        assertNull(response.descripcion());
    }
}
