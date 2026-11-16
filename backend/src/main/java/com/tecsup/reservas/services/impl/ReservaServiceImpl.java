package com.tecsup.reservas.services.impl;

import com.tecsup.reservas.config.ReservasProperties;
import com.tecsup.reservas.dto.ReservaDtos.CrearReservaRequest;
import com.tecsup.reservas.dto.ReservaDtos.CuotaResponse;
import com.tecsup.reservas.dto.ReservaDtos.MisReservasResponse;
import com.tecsup.reservas.dto.ReservaDtos.PausaResponse;
import com.tecsup.reservas.dto.ReservaDtos.ReservaResponse;
import com.tecsup.reservas.exception.BusinessException;
import com.tecsup.reservas.exception.ResourceNotFoundException;
import com.tecsup.reservas.model.Cubiculo;
import com.tecsup.reservas.model.EstadoCubiculo;
import com.tecsup.reservas.model.EstadoReserva;
import com.tecsup.reservas.model.Reserva;
import com.tecsup.reservas.model.Rol;
import com.tecsup.reservas.model.TipoReserva;
import com.tecsup.reservas.model.Usuario;
import com.tecsup.reservas.repository.CubiculoRepository;
import com.tecsup.reservas.repository.ReservaRepository;
import com.tecsup.reservas.repository.UsuarioRepository;
import com.tecsup.reservas.services.Formato;
import com.tecsup.reservas.services.ProcesoTiempoService;
import com.tecsup.reservas.services.ReglasReserva;
import com.tecsup.reservas.services.ReservaMapper;
import com.tecsup.reservas.services.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservas;
    private final CubiculoRepository cubiculos;
    private final UsuarioRepository usuarios;
    private final ReglasReserva reglas;
    private final ReservaMapper mapper;
    private final ProcesoTiempoService proceso;
    private final ReservasProperties p;
    private final Clock clock;

    /**
     * Crea un bloque de N horas seguidas (N = quien reserva + sus integrantes). Es todo o nada: si una sola
     * persona o un solo turno falla, no se crea nada y nadie gasta su hora.
     */
    @Override
    @Transactional
    public ReservaResponse crear(Long creadorId, CrearReservaRequest req) {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();

        List<String> otros = req.integrantes() == null ? List.of() : req.integrantes().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList();
        int horas = 1 + otros.size();
        LocalTime horaInicio = reglas.leerHoraEnPunto(req.horaInicio());
        reglas.validarBloque(ahora, horaInicio, horas);

        Usuario creador = usuarios.findById(creadorId)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos tu cuenta."));
        if (creador.getRol() != Rol.ESTUDIANTE) {
            throw new BusinessException("SOLO_ESTUDIANTES", "Solo los estudiantes pueden reservar.");
        }

        Map<Long, Usuario> personas = new LinkedHashMap<>();
        personas.put(creador.getId(), creador);
        for (String identificador : otros) {
            Usuario u = usuarios.findByCodigoIgnoreCaseOrCorreoIgnoreCase(identificador, identificador)
                    .filter(x -> x.getRol() == Rol.ESTUDIANTE && x.isActivo())
                    .orElseThrow(() -> new BusinessException("ESTUDIANTE_NO_ENCONTRADO",
                            "No encontramos «" + identificador + "» en la lista de estudiantes."));
            if (personas.putIfAbsent(u.getId(), u) != null) {
                throw new BusinessException("INTEGRANTE_REPETIDO", u.getNombre() + " aparece más de una vez en el grupo.");
            }
        }

        // Se bloquean primero las personas y luego el cubículo, siempre en el mismo orden (evita bloqueos cruzados).
        usuarios.bloquearPorIds(personas.keySet());
        Cubiculo cubiculo = cubiculos.bloquearPorId(req.cubiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe ese cubículo."));
        if (cubiculo.getEstado() == EstadoCubiculo.MANTENIMIENTO) {
            throw new BusinessException("EN_MANTENIMIENTO", "El " + cubiculo.getNombre() + " está en mantenimiento.");
        }

        LocalDateTime inicio = hoy.atTime(horaInicio);
        LocalDateTime fin = inicio.plusHours(horas);

        for (Usuario u : personas.values()) {
            boolean yo = u.getId().equals(creador.getId());
            if (u.getPausadoHasta() != null && !hoy.isAfter(u.getPausadoHasta())) {
                throw new BusinessException("PAUSA_ACTIVA", (yo ? "Tienes tus" : u.getNombre() + " tiene sus")
                        + " reservas en pausa hasta el " + Formato.fechaLarga(u.getPausadoHasta()) + ".");
            }
            int usadas = reservas.findDeUsuario(u.getId(), hoy, EstadoReserva.CONSUMEN_HORA).size();
            if (usadas >= p.cuotaHorasDia()) {
                throw new BusinessException("CUOTA_AGOTADA",
                        yo ? "Ya usaste tu hora de hoy." : u.getNombre() + " ya usó su hora de hoy.");
            }
            for (Reserva otra : reservas.findDeUsuario(u.getId(), hoy, EstadoReserva.VIGENTES)) {
                if (otra.cruzaCon(inicio, fin)) {
                    throw new BusinessException("SE_CRUZA",
                            (yo ? "Ya tienes" : u.getNombre() + " ya tiene") + " otra reserva en ese horario.");
                }
            }
        }

        for (Reserva existente : reservas.findByCubiculoIdAndFechaAndEstadoIn(cubiculo.getId(), hoy, EstadoReserva.VIGENTES)) {
            if (existente.cruzaCon(inicio, fin)) {
                throw new BusinessException("TURNO_OCUPADO", "El " + cubiculo.getNombre() + " ya está reservado de "
                        + Formato.hhmm(existente.inicio()) + " a " + Formato.hhmm(existente.fin()) + ".");
            }
        }

        Reserva r = new Reserva();
        r.setCubiculo(cubiculo);
        r.setFecha(hoy);
        r.setHoraInicio(horaInicio);
        r.setHoras(horas);
        r.setTipo(horas == 1 ? TipoReserva.INDIVIDUAL : TipoReserva.GRUPO);
        r.setEstado(EstadoReserva.RESERVADA);
        r.setCreador(creador);
        r.getIntegrantes().addAll(personas.values());
        r.setCreadaEn(ahora);
        // Los N minutos para llegar cuentan desde el inicio del turno, o desde que se reserva si ya empezó.
        r.setCheckInLimite((inicio.isAfter(ahora) ? inicio : ahora).plusMinutes(p.minutosCheckin()));
        reservas.save(r);
        return mapper.aDto(r, creadorId, false, ahora);
    }

    @Override
    @Transactional
    public ReservaResponse cancelar(Long usuarioId, Long reservaId) {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        Reserva r = reservas.findById(reservaId).orElseThrow(() -> new ResourceNotFoundException("No existe esa reserva."));
        if (!r.getCreador().getId().equals(usuarioId)) {
            throw new BusinessException("NO_ES_QUIEN_RESERVO", "Solo quien creó la reserva puede cancelarla.");
        }
        if (r.getEstado() == EstadoReserva.EN_CURSO) {
            throw new BusinessException("NO_SE_PUEDE_CANCELAR", "Ya estás en el cubículo. Para salir usa «Terminé».");
        }
        if (r.getEstado() != EstadoReserva.RESERVADA) {
            throw new BusinessException("NO_SE_PUEDE_CANCELAR", "Esta reserva ya no se puede cancelar.");
        }
        boolean gratis = !ahora.isAfter(r.inicio().minusMinutes(p.minutosCancelacionGratis()));
        r.setEstado(gratis ? EstadoReserva.CANCELADA : EstadoReserva.CANCELADA_TARDE);
        r.setCanceladaEn(ahora);
        r.setFinRealEn(ahora);
        return mapper.aDto(r, usuarioId, false, ahora);
    }

    @Override
    @Transactional
    public ReservaResponse terminar(Long usuarioId, Long reservaId) {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        Reserva r = reservas.findById(reservaId).orElseThrow(() -> new ResourceNotFoundException("No existe esa reserva."));
        if (!r.incluyeA(usuarioId)) {
            throw new BusinessException("NO_ES_INTEGRANTE", "Esta reserva no es tuya.");
        }
        if (r.getEstado() != EstadoReserva.EN_CURSO) {
            throw new BusinessException("NO_SE_PUEDE_TERMINAR", "Solo se puede terminar una reserva que está en curso.");
        }
        r.setEstado(EstadoReserva.COMPLETADA);
        r.setFinRealEn(ahora);
        return mapper.aDto(r, usuarioId, false, ahora);
    }

    @Override
    @Transactional
    public MisReservasResponse mias(Long usuarioId) {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();
        Usuario u = usuarios.findById(usuarioId).orElseThrow(() -> new ResourceNotFoundException("No encontramos tu cuenta."));

        List<Reserva> deHoy = reservas.findDeUsuario(usuarioId, hoy, EnumSet.allOf(EstadoReserva.class)).stream()
                .sorted(Comparator.comparing(Reserva::inicio))
                .toList();
        int usadas = (int) deHoy.stream().filter(r -> EstadoReserva.CONSUMEN_HORA.contains(r.getEstado())).count();
        boolean enPausa = u.getPausadoHasta() != null && !hoy.isAfter(u.getPausadoHasta());

        List<ReservaResponse> hoyDto = deHoy.stream().map(r -> mapper.aDto(r, usuarioId, false, ahora)).toList();
        List<ReservaResponse> historial = reservas.historialDe(usuarioId, hoy, PageRequest.of(0, 15)).stream()
                .map(r -> mapper.aDto(r, usuarioId, false, ahora))
                .toList();
        return new MisReservasResponse(Formato.iso(ahora),
                new CuotaResponse(p.cuotaHorasDia(), usadas, Math.max(0, p.cuotaHorasDia() - usadas)),
                new PausaResponse(enPausa, enPausa ? u.getPausadoHasta() : null),
                u.getAusenciasSeguidas(), p.ausenciasParaPausa(), hoyDto, historial);
    }

    @Override
    @Transactional
    public List<ReservaResponse> listarAdmin(LocalDate fecha, Long cubiculoId, EstadoReserva estado, String texto) {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate dia = fecha != null ? fecha : ahora.toLocalDate();
        String q = texto == null ? "" : texto.trim().toLowerCase();
        return reservas.findByFechaOrderByHoraInicioAscCubiculoNumeroAsc(dia).stream()
                .filter(r -> cubiculoId == null || r.getCubiculo().getId().equals(cubiculoId))
                .filter(r -> estado == null || r.getEstado() == estado)
                .filter(r -> q.isEmpty() || coincide(r, q))
                .map(r -> mapper.aDto(r, null, true, ahora))
                .toList();
    }

    private boolean coincide(Reserva r, String q) {
        return r.getCreador().getCodigo().toLowerCase().contains(q)
                || r.getIntegrantes().stream().anyMatch(u -> u.getCodigo().toLowerCase().contains(q)
                || u.getNombre().toLowerCase().contains(q) || u.getCorreo().toLowerCase().contains(q));
    }
}
