package com.tecsup.reservas.model;

/** Acciones que quedan en el registro (quién, cuándo y por qué). SISTEMA = hecho por la propia página. */
public enum AccionRegistro {
    LIBERAR,
    OCUPAR,
    MANTENIMIENTO_ON,
    MANTENIMIENTO_OFF,
    LIBERACION_AUTOMATICA,
    PAUSA_AUTOMATICA
}
