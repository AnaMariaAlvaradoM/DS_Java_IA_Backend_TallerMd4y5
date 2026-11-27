package com.devsenior.sillon.security;

import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.service.SalonService;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.servicio.ServicioService;
import com.devsenior.sillon.servicio.dto.ServicioResponse;
import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import com.devsenior.sillon.usuario.repository.UsuarioRepository;
import com.devsenior.sillon.security.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        com.devsenior.sillon.salon.controller.SalonController.class,
        com.devsenior.sillon.servicio.ServicioController.class
})
@Import({SecurityConfig.class, JwtAuthFilter.class, UsuarioDetailsService.class, JwtService.class})
class EndpointsAutenticadosMvcTest {

    private static final Long SALON_ID = 55L;
    private static final String EMAIL = "duena@salon.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private SalonService salonService;

    @MockitoBean
    private ServicioService servicioService;

    private String token;

    @BeforeEach
    void prepararTokenAutenticado() {
        Salon salon = new Salon();
        salon.setId(SALON_ID);

        Usuario usuario = new Usuario();
        usuario.setId(91L);
        usuario.setEmail(EMAIL);
        usuario.setContrasena("hash");
        usuario.setRol(Rol.DUENO);
        usuario.setActivo(true);
        usuario.setSalon(salon);

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        token = jwtService.generarToken(UsuarioPrincipal.build(usuario));
    }

    @Test
    void obtenerSalonConJwtDevuelve200() throws Exception {
        when(salonService.obtenerSalonPorId(SALON_ID))
                .thenReturn(new SalonResponse(SALON_ID, "Salón Centro", "salon-centro",
                        "3001234567", "Calle 1", Plan.GRATIS, 10, null));

        mockMvc.perform(get("/api/salon")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(salonService).obtenerSalonPorId(SALON_ID);
    }

    @Test
    void crearServicioConJwtDevuelve201() throws Exception {
        when(servicioService.crearServicio(any(), eq(SALON_ID)))
                .thenReturn(new ServicioResponse(77L, "Corte", "Corte clásico", 30, 25000, true));

        mockMvc.perform(post("/api/servicios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Corte",
                                  "descripcion": "Corte clásico",
                                  "duracionMinutos": 30,
                                  "precio": 25000,
                                  "activo": true
                                }
                                """))
                .andExpect(status().isCreated());

        verify(servicioService).crearServicio(any(), eq(SALON_ID));
    }
}
