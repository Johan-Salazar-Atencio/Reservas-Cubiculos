package com.tecsup.reservas.dto;

import com.tecsup.reservas.model.EstadoReserva;
import com.tecsup.reservas.model.TipoReserva;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** Entradas y salidas de las reservas. Las horas viajan como texto "HH:mm" (hora de Lima). */
public final class ReservaDtos {

    private ReservaDtos() {
    }

    /** {@code integrantes}: códigos o correos de las demás personas del relevo (sin contar a quien reserva). */
    public record CrearReservaRequest(
            @NotNull(message = "Elige un cubículo") Long cubiculoId,
            @NotBlank(message = "Elige la hora de inicio") String horaInicio,
            List<String> integrantes) {
    }

    /** {@code codigo} solo se llena cuando quien consulta puede verlo (el administrador o la propia persona). */
    public record PersonaResponse(String nombre, String codigo) {
    }

    public record ReservaResponse(
            Long id,
            Long cubiculoId,
            int cubiculoNumero,
            String cubiculoNombre,
            LocalDate fecha,
            String horaInicio,
            String horaFin,
            int horas,
            TipoReserva tipo,
            EstadoReserva estado,
            PersonaResponse creador,
            List<PersonaResponse> integrantes,
            boolean esCreador,
            boolean puedeCancelar,
            boolean puedeTerminar,
            boolean cancelacionGratis,
            String checkInLimite,
            Integer segundosParaLimite,
            String checkInEn,
            String motivo,
            boolean yaEmpezo) {
    }

    public record CuotaResponse(int horasPorDia, int usadas, int disponibles) {
    }

    public record PausaResponse(boolean activa, LocalDate hasta) {
    }

    public record MisReservasResponse(
            String ahora,
            CuotaResponse cuota,
            PausaResponse pausa,
            int ausenciasSeguidas,
            int ausenciasParaPausa,
            List<ReservaResponse> hoy,
            List<ReservaResponse> historial) {
    }
}
