package com.tecsup.reservas.services.impl;

import com.tecsup.reservas.dto.AdminDtos.AusenciaResponse;
import com.tecsup.reservas.dto.AdminDtos.EstadisticasResponse;
import com.tecsup.reservas.dto.AdminDtos.HoraPicoResponse;
import com.tecsup.reservas.dto.AdminDtos.OcupacionResponse;
import com.tecsup.reservas.dto.AdminDtos.SensorEstadoResponse;
import com.tecsup.reservas.dto.AdminDtos.TotalesResponse;
import com.tecsup.reservas.model.Cubiculo;
import com.tecsup.reservas.model.EstadoReserva;
import com.tecsup.reservas.model.Reserva;
import com.tecsup.reservas.model.TipoReserva;
import com.tecsup.reservas.model.Usuario;
import com.tecsup.reservas.repository.CubiculoRepository;
import com.tecsup.reservas.repository.ReservaRepository;
import com.tecsup.reservas.services.CalculadorEstado;
import com.tecsup.reservas.services.EstadisticaService;
import com.tecsup.reservas.services.Formato;
import com.tecsup.reservas.services.ReglasReserva;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstadisticaServiceImpl implements EstadisticaService {

    private static final Set<EstadoReserva> OCUPAN =
            EnumSet.of(EstadoReserva.RESERVADA, EstadoReserva.EN_CURSO, EstadoReserva.COMPLETADA, EstadoReserva.AUSENTE);
    private static final Set<EstadoReserva> ATENDIDAS = EnumSet.of(EstadoReserva.EN_CURSO, EstadoReserva.COMPLETADA);
    private static final Set<EstadoReserva> CANCELADAS =
            EnumSet.of(EstadoReserva.CANCELADA, EstadoReserva.CANCELADA_TARDE, EstadoReserva.ANULADA_ADMIN);

    private final ReservaRepository reservas;
    private final CubiculoRepository cubiculos;
    private final ReglasReserva reglas;
    private final CalculadorEstado calculador;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public EstadisticasResponse resumen(int dias) {
        int n = Math.max(1, Math.min(dias, 90));
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate desde = ahora.toLocalDate().minusDays(n - 1L);
        List<Reserva> lista = reservas.findByFechaGreaterThanEqual(desde).stream()
                .filter(r -> r.getTipo() != TipoReserva.ADMIN)
                .toList();

        // Horas pico: cuántas reservas ocupan cada hora del día.
        Map<Integer, Integer> porHora = new TreeMap<>();
        for (LocalTime t : reglas.turnos()) {
            porHora.put(t.getHour(), 0);
        }
        for (Reserva r : lista) {
            if (!OCUPAN.contains(r.getEstado())) {
                continue;
            }
            for (int i = 0; i < r.getHoras(); i++) {
                porHora.merge(r.getHoraInicio().plusHours(i).getHour(), 1, Integer::sum);
            }
        }
        List<HoraPicoResponse> horasPico = porHora.entrySet().stream()
                .map(e -> new HoraPicoResponse(e.getKey(), e.getValue()))
                .toList();

        // Ausencias por persona (quien creó la reserva).
        Map<Usuario, Long> porPersona = lista.stream()
                .filter(r -> r.getEstado() == EstadoReserva.AUSENTE)
                .collect(Collectors.groupingBy(Reserva::getCreador, Collectors.counting()));
        List<AusenciaResponse> ausencias = porPersona.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Long.compare(b.getValue(), a.getValue());
                    return cmp != 0 ? cmp : a.getKey().getCodigo().compareTo(b.getKey().getCodigo());
                })
                .limit(5)
                .map(e -> new AusenciaResponse(e.getKey().getCodigo(), e.getKey().getNombre(), e.getValue()))
                .toList();

        List<OcupacionResponse> ocupacion = new ArrayList<>();
        List<SensorEstadoResponse> sensores = new ArrayList<>();
        for (Cubiculo c : cubiculos.findAllByOrderByNumeroAsc()) {
            List<Reserva> deEste = lista.stream().filter(r -> r.getCubiculo().getId().equals(c.getId())).toList();
            int reservadas = deEste.stream().filter(r -> OCUPAN.contains(r.getEstado())).mapToInt(Reserva::getHoras).sum();
            int ocupadas = deEste.stream().filter(r -> ATENDIDAS.contains(r.getEstado())).mapToInt(Reserva::getHoras).sum();
            int ausentes = (int) deEste.stream().filter(r -> r.getEstado() == EstadoReserva.AUSENTE).count();
            ocupacion.add(new OcupacionResponse(c.getNumero(), reservadas, ocupadas, ausentes));
            sensores.add(new SensorEstadoResponse(c.getNumero(), calculador.enLinea(c, ahora),
                    Formato.iso(c.getUltimoLatido()), Formato.iso(c.getUltimaPresencia())));
        }

        int atendidas = (int) lista.stream().filter(r -> ATENDIDAS.contains(r.getEstado())).count();
        int ausentes = (int) lista.stream().filter(r -> r.getEstado() == EstadoReserva.AUSENTE).count();
        int canceladas = (int) lista.stream().filter(r -> CANCELADAS.contains(r.getEstado())).count();
        int esperadas = atendidas + ausentes;
        int tasa = esperadas == 0 ? 0 : (int) Math.round(100.0 * ausentes / esperadas);
        return new EstadisticasResponse(Formato.iso(ahora), n,
                new TotalesResponse(lista.size(), atendidas, ausentes, canceladas, tasa),
                horasPico, ausencias, ocupacion, sensores);
    }
}
