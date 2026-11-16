package com.tecsup.reservas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

/** Todas las clases piden un {@link Clock}; aquí se decide cuál: simulado (dev, test) o el real de Lima. */
@Configuration
public class RelojConfig {

    @Bean
    @Profile({"dev", "test"})
    public RelojSimulado relojSimulado(ReservasProperties p) {
        return new RelojSimulado(p.zona());
    }

    @Bean
    @Profile("!dev & !test")
    public Clock relojSistema(ReservasProperties p) {
        return Clock.system(p.zona());
    }
}
