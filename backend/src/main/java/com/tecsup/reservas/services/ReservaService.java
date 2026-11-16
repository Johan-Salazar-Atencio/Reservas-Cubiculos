package com.tecsup.reservas.services;

import com.tecsup.reservas.dto.ReservaDtos.CrearReservaRequest;
import com.tecsup.reservas.dto.ReservaDtos.MisReservasResponse;
import com.tecsup.reservas.dto.ReservaDtos.ReservaResponse;
import com.tecsup.reservas.model.EstadoReserva;

import java.time.LocalDate;
import java.util.List;

public interface ReservaService {

    ReservaResponse crear(Long creadorId, CrearReservaRequest request);

    ReservaResponse cancelar(Long usuarioId, Long reservaId);

    ReservaResponse terminar(Long usuarioId, Long reservaId);

    MisReservasResponse mias(Long usuarioId);

    /** Listado para el administrador (con códigos completos). Todos los filtros son opcionales. */
    List<ReservaResponse> listarAdmin(LocalDate fecha, Long cubiculoId, EstadoReserva estado, String texto);
}
