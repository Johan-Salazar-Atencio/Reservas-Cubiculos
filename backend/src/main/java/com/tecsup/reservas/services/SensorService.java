package com.tecsup.reservas.services;

import com.tecsup.reservas.dto.CubiculoDtos.DispositivoEstadoResponse;

/** Lo que usa la maqueta (ESP32 + PIR). Toda llamada cuenta como señal de vida del sensor. */
public interface SensorService {

    /** El PIR detectó a alguien: si hay una reserva esperando, se confirma la llegada. */
    DispositivoEstadoResponse presencia(int numero);

    /** Solo señal de vida. */
    DispositivoEstadoResponse latido(int numero);

    /** La maqueta pregunta qué luz mostrar. */
    DispositivoEstadoResponse estado(int numero);
}
