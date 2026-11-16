package com.tecsup.reservas.config;

import com.tecsup.reservas.services.ProcesoTiempoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada pocos segundos libera las salas donde no llegó nadie y cierra las reservas que terminaron. */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class TareasProgramadas {

    private final ProcesoTiempoService proceso;

    @Scheduled(fixedDelay = 2000, initialDelay = 5000)
    public void procesarTiempos() {
        try {
            proceso.procesar();
        } catch (Exception e) {
            log.warn("No se pudieron procesar los tiempos: {}", e.getMessage());
        }
    }
}
