package com.tecsup.reservas.services;

import com.tecsup.reservas.config.ReservasProperties;
import com.tecsup.reservas.model.Cubiculo;
import com.tecsup.reservas.model.EstadoCubiculo;
import com.tecsup.reservas.model.EstadoReserva;
import com.tecsup.reservas.model.EstadoVisible;
import com.tecsup.reservas.model.Luz;
import com.tecsup.reservas.model.Reserva;
import com.tecsup.reservas.model.TipoReserva;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Calcula, a partir de las reservas vigentes, el estado que ve la gente de un cubículo y la luz que debe
 * mostrar la maqueta. El estado nunca se guarda: se deduce del horario.
 */
@Component
@RequiredArgsConstructor
public class CalculadorEstado {

    private final ReservasProperties p;
    private final ReglasReserva reglas;

    public record Resultado(EstadoVisible estado, Luz luz, Reserva actual, LocalDateTime ocupadoHasta,
                            LocalDateTime libreDesde, LocalDateTime proxima, Integer minutosRestantes) {
    }

    public Resultado calcular(Cubiculo c, LocalDateTime ahora, List<Reserva> vigentes) {
        if (c.getEstado() == EstadoCubiculo.MANTENIMIENTO) {
            return new Resultado(EstadoVisible.MANTENIMIENTO, Luz.VIOLETA, null, null, null, null, null);
        }
        if (!reglas.estaAbierto(ahora)) {
            return new Resultado(EstadoVisible.CERRADO, Luz.APAGADA, null, null, null, null, null);
        }

        Reserva actual = null;
        LocalDateTime proxima = null;
        for (Reserva r : vigentes) {
            if (!ahora.isBefore(r.inicio()) && ahora.isBefore(r.fin())) {
                actual = r;
            } else if (r.inicio().isAfter(ahora) && (proxima == null || r.inicio().isBefore(proxima))) {
                proxima = r.inicio();
            }
        }
        if (actual == null) {
            return new Resultado(EstadoVisible.LIBRE, Luz.APAGADA, null, null, null, proxima, null);
        }

        // A qué hora queda libre de verdad: se siguen las reservas que empiezan justo cuando termina esta.
        LocalDateTime fin = actual.fin();
        LocalDateTime libre = fin;
        boolean avanza = true;
        for (int guarda = 0; avanza && guarda < 30; guarda++) {
            avanza = false;
            for (Reserva r : vigentes) {
                if (r.inicio().equals(libre)) {
                    libre = r.fin();
                    avanza = true;
                    break;
                }
            }
        }
        LocalDateTime cierre = ahora.toLocalDate().atTime(p.horaCierre());
        LocalDateTime libreDesde = libre.isBefore(cierre) ? libre : null;

        long segundos = Math.max(0, Duration.between(ahora, fin).getSeconds());
        int minutos = (int) Math.ceil(segundos / 60.0);

        EstadoVisible estado;
        Luz luz;
        if (actual.getTipo() == TipoReserva.ADMIN) {
            estado = EstadoVisible.OCUPADO_ADMIN;
            luz = Luz.AZUL;
        } else if (actual.getEstado() == EstadoReserva.RESERVADA) {
            estado = EstadoVisible.RESERVADO;
            luz = Luz.AMBAR;
        } else if (segundos <= p.minutosAvisoFin() * 60L) {
            estado = EstadoVisible.POR_TERMINAR;
            luz = Luz.ROJO;
        } else {
            estado = EstadoVisible.OCUPADO;
            luz = Luz.AZUL;
        }
        return new Resultado(estado, luz, actual, fin, libreDesde, proxima, minutos);
    }

    /** El sensor está "en línea" si dio señal de vida hace menos de N segundos. */
    public boolean enLinea(Cubiculo c, LocalDateTime ahora) {
        return c.getUltimoLatido() != null
                && !c.getUltimoLatido().plusSeconds(p.segundosSinSenal()).isBefore(ahora);
    }
}
