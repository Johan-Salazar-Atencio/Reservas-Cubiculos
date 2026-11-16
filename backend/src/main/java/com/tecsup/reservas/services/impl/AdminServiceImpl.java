package com.tecsup.reservas.services.impl;

import com.tecsup.reservas.config.ReservasProperties;
import com.tecsup.reservas.dto.AdminDtos.AccionResponse;
import com.tecsup.reservas.exception.BusinessException;
import com.tecsup.reservas.exception.ResourceNotFoundException;
import com.tecsup.reservas.model.AccionRegistro;
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
import com.tecsup.reservas.services.AdminService;
import com.tecsup.reservas.services.Formato;
import com.tecsup.reservas.services.ProcesoTiempoService;
import com.tecsup.reservas.services.ReglasReserva;
import com.tecsup.reservas.services.RegistroService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final CubiculoRepository cubiculos;
    private final ReservaRepository reservas;
    private final UsuarioRepository usuarios;
    private final RegistroService registro;
    private final ProcesoTiempoService proceso;
    private final ReglasReserva reglas;
    private final ReservasProperties p;
    private final Clock clock;

    /** Anula la reserva u ocupación que hay en este momento. La hora se devuelve y no cuenta como ausencia. */
    @Override
    @Transactional
    public AccionResponse liberar(Long adminId, Long cubiculoId, String motivo) {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        Usuario admin = admin(adminId);
        Cubiculo c = cubiculo(cubiculoId);
        Reserva actual = reservas.findByCubiculoIdAndFechaAndEstadoIn(c.getId(), ahora.toLocalDate(), EstadoReserva.VIGENTES)
                .stream()
                .filter(r -> !ahora.isBefore(r.inicio()) && ahora.isBefore(r.fin()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("YA_ESTA_LIBRE", "El " + c.getNombre() + " ya está libre."));
        actual.setEstado(EstadoReserva.ANULADA_ADMIN);
        actual.setFinRealEn(ahora);
        actual.setMotivo(motivo);
        String que = actual.getTipo() == TipoReserva.ADMIN
                ? "una ocupación del administrador"
                : "la reserva de " + actual.getCreador().getCodigo();
        registro.registrar(admin.getCodigo(), AccionRegistro.LIBERAR, c.getNumero(), motivo,
                "Se anuló " + que + " (" + Formato.hhmm(actual.inicio()) + " a " + Formato.hhmm(actual.fin()) + ")");
        return new AccionResponse(c.getNombre() + " liberado.");
    }

    /** Marca el cubículo como ocupado desde el turno actual durante N horas (visitas, eventos, uso interno). */
    @Override
    @Transactional
    public AccionResponse ocupar(Long adminId, Long cubiculoId, int horas, String motivo) {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        Usuario admin = admin(adminId);
        Cubiculo c = cubiculo(cubiculoId);
        if (c.getEstado() == EstadoCubiculo.MANTENIMIENTO) {
            throw new BusinessException("EN_MANTENIMIENTO", "El " + c.getNombre() + " está en mantenimiento. Reactívalo primero.");
        }
        if (!reglas.estaAbierto(ahora)) {
            throw new BusinessException("FUERA_DE_HORARIO", "Solo se puede ocupar un cubículo dentro del horario de atención.");
        }
        LocalDate hoy = ahora.toLocalDate();
        LocalDateTime inicio = hoy.atTime(ahora.getHour(), 0);
        LocalDateTime fin = inicio.plusHours(horas);
        if (fin.isAfter(hoy.atTime(p.horaCierre()))) {
            throw new BusinessException("BLOQUE_PASA_DEL_CIERRE",
                    "El bloque termina después de las " + Formato.hhmm(p.horaCierre()) + ". Elige menos horas.");
        }
        boolean choca = reservas.findByCubiculoIdAndFechaAndEstadoIn(c.getId(), hoy, EstadoReserva.VIGENTES).stream()
                .anyMatch(r -> r.cruzaCon(inicio, fin));
        if (choca) {
            throw new BusinessException("TURNO_OCUPADO",
                    "Hay una reserva en ese horario. Libera el cubículo primero o elige menos horas.");
        }
        Reserva r = new Reserva();
        r.setCubiculo(c);
        r.setFecha(hoy);
        r.setHoraInicio(inicio.toLocalTime());
        r.setHoras(horas);
        r.setTipo(TipoReserva.ADMIN);
        r.setEstado(EstadoReserva.EN_CURSO);
        r.setCreador(admin);
        r.setCreadaEn(ahora);
        r.setCheckInEn(ahora);
        r.setMotivo(motivo);
        reservas.save(r);
        registro.registrar(admin.getCodigo(), AccionRegistro.OCUPAR, c.getNumero(), motivo,
                "Ocupado de " + Formato.hhmm(inicio) + " a " + Formato.hhmm(fin));
        return new AccionResponse(c.getNombre() + " ocupado hasta las " + Formato.hhmm(fin) + ".");
    }

    /** Activa o termina el mantenimiento. Al activarlo se anulan las reservas de hoy de ese cubículo. */
    @Override
    @Transactional
    public AccionResponse mantenimiento(Long adminId, Long cubiculoId, boolean activo, String motivo) {
        proceso.procesar();
        LocalDateTime ahora = LocalDateTime.now(clock);
        Usuario admin = admin(adminId);
        Cubiculo c = cubiculo(cubiculoId);
        if (activo) {
            if (c.getEstado() == EstadoCubiculo.MANTENIMIENTO) {
                throw new BusinessException("YA_EN_MANTENIMIENTO", "El " + c.getNombre() + " ya está en mantenimiento.");
            }
            List<Reserva> afectadas = reservas.findByCubiculoIdAndFechaAndEstadoIn(c.getId(), ahora.toLocalDate(), EstadoReserva.VIGENTES);
            for (Reserva r : afectadas) {
                r.setEstado(EstadoReserva.ANULADA_ADMIN);
                r.setFinRealEn(ahora);
                r.setMotivo("Mantenimiento: " + motivo);
            }
            c.setEstado(EstadoCubiculo.MANTENIMIENTO);
            c.setMotivoMantenimiento(motivo);
            registro.registrar(admin.getCodigo(), AccionRegistro.MANTENIMIENTO_ON, c.getNumero(), motivo,
                    afectadas.size() + (afectadas.size() == 1 ? " reserva anulada" : " reservas anuladas"));
            return new AccionResponse(c.getNombre() + " en mantenimiento.");
        }
        if (c.getEstado() != EstadoCubiculo.MANTENIMIENTO) {
            throw new BusinessException("NO_ESTA_EN_MANTENIMIENTO", "El " + c.getNombre() + " no está en mantenimiento.");
        }
        c.setEstado(EstadoCubiculo.ACTIVO);
        c.setMotivoMantenimiento(null);
        registro.registrar(admin.getCodigo(), AccionRegistro.MANTENIMIENTO_OFF, c.getNumero(), motivo, null);
        return new AccionResponse(c.getNombre() + " disponible otra vez.");
    }

    private Usuario admin(Long adminId) {
        return usuarios.findById(adminId)
                .filter(u -> u.getRol() == Rol.ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos la cuenta del administrador."));
    }

    private Cubiculo cubiculo(Long id) {
        return cubiculos.bloquearPorId(id).orElseThrow(() -> new ResourceNotFoundException("No existe ese cubículo."));
    }
}
