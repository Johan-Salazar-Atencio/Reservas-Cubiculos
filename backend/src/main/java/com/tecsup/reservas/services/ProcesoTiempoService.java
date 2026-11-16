package com.tecsup.reservas.services;

/** Aplica lo que el paso del tiempo cambia: libera salas sin llegada y cierra reservas que terminaron. */
public interface ProcesoTiempoService {

    void procesar();
}
