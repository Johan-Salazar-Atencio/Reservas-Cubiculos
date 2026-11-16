package com.tecsup.reservas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Entradas y salidas del simulador (solo perfil dev): reloj y sensores virtuales. */
public final class SimuladorDtos {

    private SimuladorDtos() {
    }

    public record RelojResponse(String ahora, boolean simulado) {
    }

    public record AvanzarRelojRequest(
            @Min(value = 1, message = "Mínimo 1 minuto") @Max(value = 600, message = "Máximo 600 minutos") int minutos) {
    }

    public record IrAHoraRequest(@NotBlank(message = "Escribe la hora, por ejemplo 11:00") String hora) {
    }

    public record SinSenalRequest(boolean activo) {
    }
}
