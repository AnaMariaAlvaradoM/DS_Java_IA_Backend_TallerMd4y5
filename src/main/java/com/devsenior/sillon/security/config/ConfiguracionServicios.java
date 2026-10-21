package com.devsenior.sillon.security.config;

import com.devsenior.sillon.salon.repository.SalonRepository;
import com.devsenior.sillon.salon.service.GeneradorSlugService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionServicios {

    @Bean
    public GeneradorSlugService generadorSlugService(SalonRepository salonRepository) {
        return new GeneradorSlugService(salonRepository);
    }
}
