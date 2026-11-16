package com.tecsup.reservas.services;

import com.tecsup.reservas.dto.AdminDtos.AccionResponse;

/** Acciones manuales del administrador. Todas piden un motivo y quedan en el registro. */
public interface AdminService {

    AccionResponse liberar(Long adminId, Long cubiculoId, String motivo);

    AccionResponse ocupar(Long adminId, Long cubiculoId, int horas, String motivo);

    AccionResponse mantenimiento(Long adminId, Long cubiculoId, boolean activo, String motivo);
}
