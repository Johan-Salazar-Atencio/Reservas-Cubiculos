package com.tecsup.reservas.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Reloj que se puede adelantar (perfil dev) o congelar (pruebas). Sirve para probar la regla de los
 * 10 minutos sin esperar y sin la maqueta. En producción se usa el reloj real de Lima.
 */
public class RelojSimulado extends Clock {

    private final ZoneId zona;
    private volatile Duration desfase = Duration.ZERO;
    private volatile Instant congelado;

    public RelojSimulado(ZoneId zona) {
        this.zona = zona;
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId otraZona) {
        throw new UnsupportedOperationException("El reloj simulado siempre usa " + zona);
    }

    @Override
    public Instant instant() {
        Instant fijo = congelado;
        return fijo != null ? fijo : Instant.now().plus(desfase);
    }

    /** Salta a ese momento y sigue corriendo en tiempo real desde ahí. */
    public synchronized void irA(LocalDateTime momento) {
        congelado = null;
        desfase = Duration.between(Instant.now(), momento.atZone(zona).toInstant());
    }

    /** Detiene el reloj en ese momento (para pruebas automáticas). */
    public synchronized void congelarEn(LocalDateTime momento) {
        congelado = momento.atZone(zona).toInstant();
    }

    public synchronized void avanzar(Duration duracion) {
        if (congelado != null) {
            congelado = congelado.plus(duracion);
        } else {
            desfase = desfase.plus(duracion);
        }
    }
}
