package com.tecsup.reservas.model;

import java.util.EnumSet;

/**
 * Ciclo de vida de una reserva:
 * RESERVADA -> (el sensor detecta a alguien) -> EN_CURSO -> COMPLETADA
 * RESERVADA -> (nadie llega en 10 min) -> AUSENTE
 * RESERVADA -> CANCELADA (a tiempo, devuelve la hora) o CANCELADA_TARDE (gasta la hora)
 * cualquiera vigente -> ANULADA_ADMIN (el administrador la libera, devuelve la hora)
 */
public enum EstadoReserva {
    RESERVADA,
    EN_CURSO,
    COMPLETADA,
    AUSENTE,
    CANCELADA,
    CANCELADA_TARDE,
    ANULADA_ADMIN;

    /** Reservas que todavía ocupan el cubículo. */
    public static final EnumSet<EstadoReserva> VIGENTES = EnumSet.of(RESERVADA, EN_CURSO);

    /** Reservas que gastan la hora del día de cada integrante. */
    public static final EnumSet<EstadoReserva> CONSUMEN_HORA =
            EnumSet.of(RESERVADA, EN_CURSO, COMPLETADA, AUSENTE, CANCELADA_TARDE);
}
