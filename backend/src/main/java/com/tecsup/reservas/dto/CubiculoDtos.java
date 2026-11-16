package com.tecsup.reservas.dto;

import com.tecsup.reservas.model.EstadoSlot;
import com.tecsup.reservas.model.EstadoVisible;
import com.tecsup.reservas.model.Luz;
import com.tecsup.reservas.model.TipoReserva;

import java.util.List;

/** Salidas de los cubículos: estado en vivo, agenda del día, panel del administrador y respuesta a la maqueta. */
public final class CubiculoDtos {

    private CubiculoDtos() {
    }

    public record SensorResponse(boolean enLinea, String ultimaPresencia, String ultimoLatido) {
    }

    /** Un cubículo "en vivo". {@code sensor} solo viene para el administrador. */
    public record CubiculoVivoResponse(
            Long id,
            int numero,
            String nombre,
            EstadoVisible estado,
            Luz luz,
            String ocupadoHasta,
            String libreDesde,
            String proximaReserva,
            String llegarAntesDe,
            Integer segundosParaLimite,
            Integer minutosRestantes,
            TipoReserva tipoReserva,
            Integer personas,
            String motivo,
            Long miReservaId,
            SensorResponse sensor) {
    }

    public record EnVivoResponse(
            String ahora,
            boolean simulado,
            boolean abierto,
            String mensajeCierre,
            String apertura,
            String cierre,
            int minutosCheckin,
            List<CubiculoVivoResponse> cubiculos) {
    }

    public record SlotResponse(String hora, EstadoSlot estado, boolean mia) {
    }

    public record AgendaCubiculoResponse(Long id, int numero, String nombre, List<SlotResponse> slots) {
    }

    public record AgendaResponse(String ahora, boolean abierto, List<String> horas, List<AgendaCubiculoResponse> cubiculos) {
    }

    public record AdminCubiculoResponse(
            CubiculoVivoResponse vivo,
            ReservaDtos.ReservaResponse reservaActual,
            List<ReservaDtos.ReservaResponse> proximas) {
    }

    public record PanelAdminResponse(String ahora, boolean simulado, List<AdminCubiculoResponse> cubiculos) {
    }

    /**
     * Lo que recibe la maqueta (ESP32). {@code checkInConfirmado} es true solo en la respuesta al evento de
     * presencia que confirmó la llegada: ahí la maqueta reproduce la bienvenida.
     */
    public record DispositivoEstadoResponse(
            int cubiculo,
            EstadoVisible estado,
            Luz luz,
            TipoReserva tipoReserva,
            Integer minutosRestantes,
            boolean checkInConfirmado,
            String ahora) {
    }
}
