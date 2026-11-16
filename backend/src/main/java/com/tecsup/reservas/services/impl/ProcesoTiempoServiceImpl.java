package com.tecsup.reservas.services.impl;

import com.tecsup.reservas.config.ReservasProperties;
import com.tecsup.reservas.model.AccionRegistro;
import com.tecsup.reservas.model.EstadoReserva;
import com.tecsup.reservas.model.Reserva;
import com.tecsup.reservas.model.Usuario;
import com.tecsup.reservas.repository.ReservaRepository;
import com.tecsup.reservas.services.Formato;
import com.tecsup.reservas.services.ProcesoTiempoService;
import com.tecsup.reservas.services.RegistroService;
import com.tecsup.reservas.services.ReglasReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Se ejecuta en una transacción propia (REQUIRES_NEW) dentro de un candado: así dos hilos no procesan la misma
 * reserva a la vez, aunque quien lo llame (una lectura, el planificador) esté dentro de otra transacción.
 */
@Service
public class ProcesoTiempoServiceImpl implements ProcesoTiempoService {

    private final ReservaRepository reservas;
    private final RegistroService registro;
    private final ReglasReserva reglas;
    private final ReservasProperties p;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final ReentrantLock candado = new ReentrantLock();

    public ProcesoTiempoServiceImpl(ReservaRepository reservas, RegistroService registro, ReglasReserva reglas,
                                    ReservasProperties p, Clock clock, PlatformTransactionManager transacciones) {
        this.reservas = reservas;
        this.registro = registro;
        this.reglas = reglas;
        this.p = p;
        this.clock = clock;
        this.tx = new TransactionTemplate(transacciones);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void procesar() {
        candado.lock();
        try {
            tx.executeWithoutResult(estado -> aplicar());
        } finally {
            candado.unlock();
        }
    }

    private void aplicar() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        for (Reserva r : reservas.findByEstadoIn(EstadoReserva.VIGENTES)) {
            if (r.getEstado() == EstadoReserva.RESERVADA) {
                if (!ahora.isBefore(r.limiteCheckIn())) {
                    marcarAusente(r, ahora);
                }
            } else if (!ahora.isBefore(r.fin())) {
                r.setEstado(EstadoReserva.COMPLETADA);
                r.setFinRealEn(r.fin());
            }
        }
    }

    /** Nadie llegó a tiempo: la sala queda libre y la ausencia se anota a quien reservó. */
    private void marcarAusente(Reserva r, LocalDateTime ahora) {
        r.setEstado(EstadoReserva.AUSENTE);
        r.setFinRealEn(ahora);
        Usuario creador = r.getCreador();
        creador.setAusenciasSeguidas(creador.getAusenciasSeguidas() + 1);
        registro.registrar("SISTEMA", AccionRegistro.LIBERACION_AUTOMATICA, r.getCubiculo().getNumero(),
                "No llegó nadie en " + p.minutosCheckin() + " min",
                "Reserva de " + creador.getCodigo() + " de " + Formato.hhmm(r.inicio()) + " a " + Formato.hhmm(r.fin()));
        if (creador.getAusenciasSeguidas() >= p.ausenciasParaPausa()) {
            LocalDate hasta = reglas.diaDeAtencionDespues(r.getFecha(), p.diasPausa());
            creador.setPausadoHasta(hasta);
            creador.setAusenciasSeguidas(0);
            registro.registrar("SISTEMA", AccionRegistro.PAUSA_AUTOMATICA, null,
                    p.ausenciasParaPausa() + " ausencias seguidas",
                    creador.getCodigo() + " no puede reservar hasta el " + Formato.fechaLarga(hasta));
        }
    }
}
