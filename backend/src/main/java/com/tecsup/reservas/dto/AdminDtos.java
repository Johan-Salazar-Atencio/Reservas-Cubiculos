package com.tecsup.reservas.dto;

import com.tecsup.reservas.model.AccionRegistro;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Entradas y salidas del panel del administrador. */
public final class AdminDtos {

    private AdminDtos() {
    }

    public record MotivoRequest(
            @NotBlank(message = "Escribe el motivo")
            @Size(min = 5, max = 250, message = "El motivo debe tener entre 5 y 250 caracteres") String motivo) {
    }

    public record OcuparRequest(
            @Min(value = 1, message = "Mínimo 1 hora") @Max(value = 4, message = "Máximo 4 horas") int horas,
            @NotBlank(message = "Escribe el motivo")
            @Size(min = 5, max = 250, message = "El motivo debe tener entre 5 y 250 caracteres") String motivo) {
    }

    public record MantenimientoRequest(
            boolean activo,
            @NotBlank(message = "Escribe el motivo")
            @Size(min = 5, max = 250, message = "El motivo debe tener entre 5 y 250 caracteres") String motivo) {
    }

    public record AccionResponse(String mensaje) {
    }

    public record RegistroResponse(Long id, String momento, String actor, AccionRegistro accion,
                                   Integer cubiculo, String motivo, String detalle) {
    }

    public record HoraPicoResponse(int hora, int reservas) {
    }

    public record AusenciaResponse(String codigo, String nombre, long ausencias) {
    }

    public record OcupacionResponse(int cubiculo, int horasReservadas, int horasOcupadas, int ausencias) {
    }

    public record SensorEstadoResponse(int cubiculo, boolean enLinea, String ultimoLatido, String ultimaPresencia) {
    }

    public record TotalesResponse(int reservas, int completadas, int ausentes, int canceladas, int tasaAusenciaPorc) {
    }

    public record EstadisticasResponse(
            String ahora,
            int dias,
            TotalesResponse totales,
            List<HoraPicoResponse> horasPico,
            List<AusenciaResponse> ausencias,
            List<OcupacionResponse> ocupacion,
            List<SensorEstadoResponse> sensores) {
    }
}
