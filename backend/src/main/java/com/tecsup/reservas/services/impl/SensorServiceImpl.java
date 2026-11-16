package com.tecsup.reservas.services.impl;

import com.tecsup.reservas.dto.CubiculoDtos.DispositivoEstadoResponse;
import com.tecsup.reservas.exception.ResourceNotFoundException;
import com.tecsup.reservas.model.Cubiculo;
import com.tecsup.reservas.model.EstadoReserva;
import com.tecsup.reservas.model.Reserva;
import com.tecsup.reservas.repository.CubiculoRepository;
import com.tecsup.reservas.repository.ReservaRepository;
import com.tecsup.reservas.services.CalculadorEstado;
import com.tecsup.reservas.services.CalculadorEstado.Resultado;
import com.tecsup.reservas.services.Formato;
import com.tecsup.reservas.services.ProcesoTiempoService;
import com.tecsup.reservas.services.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorServiceImpl implements SensorService {

    private final CubiculoRepository cubiculos;
    private final ReservaRepository reservas;
    private final CalculadorEstado calculador;
    private final ProcesoTiempoService proceso;
    private final Clock clock;

    /**
     * Confirma la llegada si hay una reserva esperando y el PIR detecta a alguien entre el inicio del turno y el
     * límite de los 10 minutos. Una detección antes del inicio no cuenta (podría ser quien sale del turno anterior).
     */
    @Override
    @Transactional
    public DispositivoEstadoResponse presencia(int numero) {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        Cubiculo c = bloquear(numero);
        c.setUltimaPresencia(ahora);
        c.setUltimoLatido(ahora);
        List<Reserva> vigentes = vigentes(c, ahora);
        boolean confirmado = false;
        for (Reserva r : vigentes) {
            if (r.getEstado() == EstadoReserva.RESERVADA && !ahora.isBefore(r.inicio()) && ahora.isBefore(r.limiteCheckIn())) {
                r.setEstado(EstadoReserva.EN_CURSO);
                r.setCheckInEn(ahora);
                r.getCreador().setAusenciasSeguidas(0);
                confirmado = true;
            }
        }
        return respuesta(c, ahora, vigentes, confirmado);
    }

    @Override
    @Transactional
    public DispositivoEstadoResponse latido(int numero) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        Cubiculo c = bloquear(numero);
        c.setUltimoLatido(ahora);
        return respuesta(c, ahora, vigentes(c, ahora), false);
    }

    @Override
    @Transactional
    public DispositivoEstadoResponse estado(int numero) {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        Cubiculo c = bloquear(numero);
        c.setUltimoLatido(ahora);
        return respuesta(c, ahora, vigentes(c, ahora), false);
    }

    private Cubiculo bloquear(int numero) {
        return cubiculos.bloquearPorNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el cubículo " + numero + "."));
    }

    private List<Reserva> vigentes(Cubiculo c, LocalDateTime ahora) {
        return reservas.findByCubiculoIdAndFechaAndEstadoIn(c.getId(), ahora.toLocalDate(), EstadoReserva.VIGENTES);
    }

    private DispositivoEstadoResponse respuesta(Cubiculo c, LocalDateTime ahora, List<Reserva> vigentes, boolean confirmado) {
        Resultado res = calculador.calcular(c, ahora, vigentes);
        return new DispositivoEstadoResponse(c.getNumero(), res.estado(), res.luz(),
                res.actual() == null ? null : res.actual().getTipo(), res.minutosRestantes(), confirmado,
                Formato.iso(ahora));
    }
}
