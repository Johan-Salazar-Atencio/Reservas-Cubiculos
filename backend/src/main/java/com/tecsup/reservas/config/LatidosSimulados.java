package com.tecsup.reservas.config;

import com.tecsup.reservas.services.SensorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Solo en dev: simula el latido de los sensores para que los cubículos aparezcan "en línea".
 * Cuando la maqueta real se conecte, es ella quien manda los latidos (GET /api/sensores/{n}/estado).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class LatidosSimulados {

    private final SensorService sensores;
    private final ReservasProperties p;
    private final Set<Integer> sinSenal = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelay = 5000, initialDelay = 3000)
    public void enviar() {
        for (int numero = 1; numero <= p.cubiculos(); numero++) {
            if (sinSenal.contains(numero)) {
                continue;
            }
            try {
                sensores.latido(numero);
            } catch (Exception e) {
                log.debug("Latido simulado del cubículo {} falló: {}", numero, e.getMessage());
            }
        }
    }

    public void sinSenal(int numero, boolean activo) {
        if (activo) {
            sinSenal.add(numero);
        } else {
            sinSenal.remove(numero);
        }
    }

    public boolean estaSinSenal(int numero) {
        return sinSenal.contains(numero);
    }
}
