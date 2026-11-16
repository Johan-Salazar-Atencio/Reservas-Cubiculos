package com.tecsup.reservas.model;

/** Color que debe mostrar la tira LED de la maqueta para cada estado. */
public enum Luz {
    APAGADA,   // libre o cerrado
    AMBAR,     // reservado, aún no llega
    AZUL,      // ocupado confirmado
    ROJO,      // quedan pocos minutos
    VIOLETA    // mantenimiento
}
