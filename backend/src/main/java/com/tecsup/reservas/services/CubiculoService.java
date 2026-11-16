package com.tecsup.reservas.services;

import com.tecsup.reservas.dto.CubiculoDtos.AgendaResponse;
import com.tecsup.reservas.dto.CubiculoDtos.EnVivoResponse;
import com.tecsup.reservas.dto.CubiculoDtos.PanelAdminResponse;

public interface CubiculoService {

    /** Estado en vivo de los cubículos (lo que ve el estudiante). */
    EnVivoResponse enVivo(Long usuarioId);

    /** Agenda de hoy: cada cubículo con sus turnos de 1 hora. */
    AgendaResponse agenda(Long usuarioId);

    /** Lo que ve el administrador: quién ocupa cada cubículo, estado del sensor y próximas reservas. */
    PanelAdminResponse panelAdmin();
}
