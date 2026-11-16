package com.tecsup.reservas.model;

/** Estado que ve la gente (y la maqueta) de un cubículo en este momento. */
public enum EstadoVisible {
    LIBRE,
    RESERVADO,        // reservado, todavía no llega nadie
    OCUPADO,          // el sensor confirmó que llegaron
    POR_TERMINAR,     // quedan pocos minutos (luz roja)
    OCUPADO_ADMIN,    // lo ocupó el administrador
    MANTENIMIENTO,
    CERRADO           // fuera de horario o día sin atención
}
