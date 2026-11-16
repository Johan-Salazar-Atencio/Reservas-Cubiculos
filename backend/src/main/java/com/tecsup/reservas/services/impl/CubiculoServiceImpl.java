package com.tecsup.reservas.services.impl;

import com.tecsup.reservas.config.RelojSimulado;
import com.tecsup.reservas.config.ReservasProperties;
import com.tecsup.reservas.dto.CubiculoDtos.AdminCubiculoResponse;
import com.tecsup.reservas.dto.CubiculoDtos.AgendaCubiculoResponse;
import com.tecsup.reservas.dto.CubiculoDtos.AgendaResponse;
import com.tecsup.reservas.dto.CubiculoDtos.CubiculoVivoResponse;
import com.tecsup.reservas.dto.CubiculoDtos.EnVivoResponse;
import com.tecsup.reservas.dto.CubiculoDtos.PanelAdminResponse;
import com.tecsup.reservas.dto.CubiculoDtos.SensorResponse;
import com.tecsup.reservas.dto.CubiculoDtos.SlotResponse;
import com.tecsup.reservas.dto.ReservaDtos.ReservaResponse;
import com.tecsup.reservas.model.Cubiculo;
import com.tecsup.reservas.model.EstadoCubiculo;
import com.tecsup.reservas.model.EstadoReserva;
import com.tecsup.reservas.model.EstadoSlot;
import com.tecsup.reservas.model.EstadoVisible;
import com.tecsup.reservas.model.Reserva;
import com.tecsup.reservas.model.TipoReserva;
import com.tecsup.reservas.repository.CubiculoRepository;
import com.tecsup.reservas.repository.ReservaRepository;
import com.tecsup.reservas.services.CalculadorEstado;
import com.tecsup.reservas.services.CalculadorEstado.Resultado;
import com.tecsup.reservas.services.CubiculoService;
import com.tecsup.reservas.services.Formato;
import com.tecsup.reservas.services.ProcesoTiempoService;
import com.tecsup.reservas.services.ReglasReserva;
import com.tecsup.reservas.services.ReservaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CubiculoServiceImpl implements CubiculoService {

    private final CubiculoRepository cubiculos;
    private final ReservaRepository reservas;
    private final CalculadorEstado calculador;
    private final ReglasReserva reglas;
    private final ReservaMapper mapper;
    private final ProcesoTiempoService proceso;
    private final ReservasProperties p;
    private final Clock clock;

    @Override
    @Transactional
    public EnVivoResponse enVivo(Long usuarioId) {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        Map<Long, List<Reserva>> vigentes = vigentesDeHoy(ahora.toLocalDate());
        List<CubiculoVivoResponse> lista = new ArrayList<>();
        for (Cubiculo c : cubiculos.findAllByOrderByNumeroAsc()) {
            Resultado res = calculador.calcular(c, ahora, vigentes.getOrDefault(c.getId(), List.of()));
            lista.add(vista(c, ahora, res, usuarioId, false));
        }
        return new EnVivoResponse(Formato.iso(ahora), clock instanceof RelojSimulado, reglas.estaAbierto(ahora),
                mensajeCierre(ahora), Formato.hhmm(p.horaApertura()), Formato.hhmm(p.horaCierre()),
                p.minutosCheckin(), lista);
    }

    @Override
    @Transactional
    public AgendaResponse agenda(Long usuarioId) {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();
        Map<Long, List<Reserva>> vigentes = vigentesDeHoy(hoy);
        List<LocalTime> turnos = reglas.turnos();
        List<AgendaCubiculoResponse> lista = new ArrayList<>();
        for (Cubiculo c : cubiculos.findAllByOrderByNumeroAsc()) {
            List<Reserva> deEste = vigentes.getOrDefault(c.getId(), List.of());
            List<SlotResponse> slots = new ArrayList<>();
            for (LocalTime hora : turnos) {
                slots.add(slot(c, hoy, hora, ahora, deEste, usuarioId));
            }
            lista.add(new AgendaCubiculoResponse(c.getId(), c.getNumero(), c.getNombre(), slots));
        }
        List<String> horas = turnos.stream().map(t -> Formato.hhmm(t)).toList();
        return new AgendaResponse(Formato.iso(ahora), reglas.estaAbierto(ahora), horas, lista);
    }

    @Override
    @Transactional
    public PanelAdminResponse panelAdmin() {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        Map<Long, List<Reserva>> vigentes = vigentesDeHoy(ahora.toLocalDate());
        List<AdminCubiculoResponse> lista = new ArrayList<>();
        for (Cubiculo c : cubiculos.findAllByOrderByNumeroAsc()) {
            List<Reserva> deEste = vigentes.getOrDefault(c.getId(), List.of());
            Resultado res = calculador.calcular(c, ahora, deEste);
            ReservaResponse actual = res.actual() == null ? null : mapper.aDto(res.actual(), null, true, ahora);
            List<ReservaResponse> proximas = deEste.stream()
                    .filter(r -> r.inicio().isAfter(ahora))
                    .sorted(Comparator.comparing(Reserva::inicio))
                    .map(r -> mapper.aDto(r, null, true, ahora))
                    .toList();
            lista.add(new AdminCubiculoResponse(vista(c, ahora, res, null, true), actual, proximas));
        }
        return new PanelAdminResponse(Formato.iso(ahora), clock instanceof RelojSimulado, lista);
    }

    private Map<Long, List<Reserva>> vigentesDeHoy(LocalDate hoy) {
        return reservas.findByFechaAndEstadoIn(hoy, EstadoReserva.VIGENTES).stream()
                .collect(Collectors.groupingBy(r -> r.getCubiculo().getId()));
    }

    private CubiculoVivoResponse vista(Cubiculo c, LocalDateTime ahora, Resultado res, Long usuarioId, boolean conSensor) {
        Reserva a = res.actual();
        String llegarAntesDe = null;
        Integer segundos = null;
        if (res.estado() == EstadoVisible.RESERVADO && a != null) {
            llegarAntesDe = Formato.hhmm(a.limiteCheckIn());
            segundos = (int) Math.max(0, Duration.between(ahora, a.limiteCheckIn()).getSeconds());
        }
        boolean deAdmin = a != null && a.getTipo() == TipoReserva.ADMIN;
        Integer personas = (a == null || deAdmin) ? null : a.getIntegrantes().size();
        Long miReserva = (a != null && a.incluyeA(usuarioId)) ? a.getId() : null;
        SensorResponse sensor = conSensor
                ? new SensorResponse(calculador.enLinea(c, ahora), Formato.iso(c.getUltimaPresencia()), Formato.iso(c.getUltimoLatido()))
                : null;
        String motivo = res.estado() == EstadoVisible.MANTENIMIENTO ? c.getMotivoMantenimiento() : null;
        return new CubiculoVivoResponse(c.getId(), c.getNumero(), c.getNombre(), res.estado(), res.luz(),
                Formato.hhmm(res.ocupadoHasta()), Formato.hhmm(res.libreDesde()), Formato.hhmm(res.proxima()),
                llegarAntesDe, segundos, res.minutosRestantes(), a == null ? null : a.getTipo(), personas,
                motivo, miReserva, sensor);
    }

    private SlotResponse slot(Cubiculo c, LocalDate hoy, LocalTime hora, LocalDateTime ahora,
                              List<Reserva> vigentes, Long usuarioId) {
        LocalDateTime ini = hoy.atTime(hora);
        LocalDateTime fin = ini.plusHours(1);
        EstadoSlot estado;
        boolean mia = false;
        if (!reglas.esDiaDeAtencion(hoy)) {
            estado = EstadoSlot.CERRADO;
        } else if (!fin.isAfter(ahora)) {
            estado = EstadoSlot.PASADO;
        } else if (c.getEstado() == EstadoCubiculo.MANTENIMIENTO) {
            estado = EstadoSlot.MANTENIMIENTO;
        } else {
            Reserva r = vigentes.stream().filter(x -> x.cruzaCon(ini, fin)).findFirst().orElse(null);
            if (r == null) {
                estado = EstadoSlot.LIBRE;
            } else {
                if (r.getTipo() == TipoReserva.ADMIN) {
                    estado = EstadoSlot.BLOQUEADO;
                } else if (r.getEstado() == EstadoReserva.EN_CURSO) {
                    estado = EstadoSlot.EN_CURSO;
                } else {
                    estado = EstadoSlot.RESERVADO;
                }
                mia = r.incluyeA(usuarioId);
            }
        }
        return new SlotResponse(Formato.hhmm(hora), estado, mia);
    }

    private String mensajeCierre(LocalDateTime ahora) {
        if (reglas.estaAbierto(ahora)) {
            return null;
        }
        LocalDate hoy = ahora.toLocalDate();
        String apertura = Formato.hhmm(p.horaApertura());
        if (!reglas.esDiaDeAtencion(hoy)) {
            return "Hoy no hay atención. Volvemos el " + Formato.dia(reglas.diaDeAtencionDespues(hoy, 1))
                    + " a las " + apertura + ".";
        }
        if (ahora.toLocalTime().isBefore(p.horaApertura())) {
            return "Aún no abrimos. La atención empieza a las " + apertura + ".";
        }
        LocalDate siguiente = reglas.diaDeAtencionDespues(hoy, 1);
        String cuando = siguiente.equals(hoy.plusDays(1)) ? "mañana" : "el " + Formato.dia(siguiente);
        return "La atención de hoy terminó. Volvemos " + cuando + " a las " + apertura + ".";
    }
}
