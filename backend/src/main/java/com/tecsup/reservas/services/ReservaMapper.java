package com.tecsup.reservas.services;

import com.tecsup.reservas.config.ReservasProperties;
import com.tecsup.reservas.dto.ReservaDtos.PersonaResponse;
import com.tecsup.reservas.dto.ReservaDtos.ReservaResponse;
import com.tecsup.reservas.model.Cubiculo;
import com.tecsup.reservas.model.EstadoReserva;
import com.tecsup.reservas.model.Reserva;
import com.tecsup.reservas.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** Convierte una reserva en su respuesta JSON. Los códigos solo se muestran si quien consulta puede verlos. */
@Component
@RequiredArgsConstructor
public class ReservaMapper {

    private final ReservasProperties p;

    public ReservaResponse aDto(Reserva r, Long usuarioId, boolean verCodigos, LocalDateTime ahora) {
        boolean esCreador = usuarioId != null && r.getCreador().getId().equals(usuarioId);
        boolean puedeCancelar = esCreador && r.getEstado() == EstadoReserva.RESERVADA;
        boolean puedeTerminar = r.incluyeA(usuarioId) && r.getEstado() == EstadoReserva.EN_CURSO;
        boolean gratis = puedeCancelar && !ahora.isAfter(r.inicio().minusMinutes(p.minutosCancelacionGratis()));
        Integer segundos = null;
        if (r.getEstado() == EstadoReserva.RESERVADA) {
            segundos = (int) Math.max(0, Duration.between(ahora, r.limiteCheckIn()).getSeconds());
        }
        List<PersonaResponse> integrantes = r.getIntegrantes().stream()
                .map(u -> persona(u, usuarioId, verCodigos))
                .toList();
        Cubiculo c = r.getCubiculo();
        return new ReservaResponse(r.getId(), c.getId(), c.getNumero(), c.getNombre(), r.getFecha(),
                Formato.hhmm(r.getHoraInicio()), Formato.hhmm(r.fin()), r.getHoras(), r.getTipo(), r.getEstado(),
                persona(r.getCreador(), usuarioId, verCodigos), integrantes,
                esCreador, puedeCancelar, puedeTerminar, gratis,
                Formato.hhmm(r.limiteCheckIn()), segundos, Formato.hhmm(r.getCheckInEn()), r.getMotivo(),
                !ahora.isBefore(r.inicio()));
    }

    private PersonaResponse persona(Usuario u, Long usuarioId, boolean verCodigos) {
        boolean visible = verCodigos || u.getId().equals(usuarioId);
        return new PersonaResponse(u.getNombre(), visible ? u.getCodigo() : null);
    }
}
