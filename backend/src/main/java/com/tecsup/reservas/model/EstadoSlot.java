package com.tecsup.reservas.model;

/** Estado de un turno de 1 hora en la agenda del día. */
public enum EstadoSlot {
    PASADO,
    LIBRE,
    RESERVADO,
    EN_CURSO,
    BLOQUEADO,
    MANTENIMIENTO,
    CERRADO
}
