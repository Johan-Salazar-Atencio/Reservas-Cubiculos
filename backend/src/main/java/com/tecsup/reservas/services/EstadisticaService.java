package com.tecsup.reservas.services;

import com.tecsup.reservas.dto.AdminDtos.EstadisticasResponse;

public interface EstadisticaService {

    /** Resumen de los últimos {@code dias} días (incluido hoy): horas pico, ausencias, ocupación y sensores. */
    EstadisticasResponse resumen(int dias);
}
