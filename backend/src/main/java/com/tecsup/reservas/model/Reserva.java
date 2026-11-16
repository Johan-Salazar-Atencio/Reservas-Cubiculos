package com.tecsup.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Un bloque de {@code horas} horas seguidas en un cubículo, el mismo día.
 * Una reserva individual tiene 1 integrante y dura 1 h; un relevo de N personas dura N horas.
 */
@Entity
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Evita que dos operaciones simultáneas (por ejemplo cancelar y detectar presencia) se pisen. */
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cubiculo_id")
    private Cubiculo cubiculo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private int horas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoReserva tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    /** Quien creó la reserva (en las ocupaciones manuales, el administrador). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creador_id")
    private Usuario creador;

    /** Todas las personas del bloque, incluido quien la creó. Cada una gasta su hora del día. */
    @ManyToMany
    @JoinTable(name = "reserva_integrantes",
            joinColumns = @JoinColumn(name = "reserva_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id"))
    private Set<Usuario> integrantes = new LinkedHashSet<>();

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    /** Hora límite para que el sensor confirme que llegó alguien. */
    private LocalDateTime checkInLimite;

    private LocalDateTime checkInEn;
    private LocalDateTime finRealEn;
    private LocalDateTime canceladaEn;

    @Column(length = 300)
    private String motivo;

    public LocalDateTime inicio() {
        return fecha.atTime(horaInicio);
    }

    public LocalDateTime fin() {
        return inicio().plusHours(horas);
    }

    /** El check-in vence a los N minutos o al terminar el bloque, lo que ocurra primero. */
    public LocalDateTime limiteCheckIn() {
        LocalDateTime fin = fin();
        return checkInLimite != null && checkInLimite.isBefore(fin) ? checkInLimite : fin;
    }

    public boolean incluyeA(Long usuarioId) {
        return usuarioId != null && integrantes.stream().anyMatch(u -> u.getId().equals(usuarioId));
    }

    public boolean cruzaCon(LocalDateTime desde, LocalDateTime hasta) {
        return inicio().isBefore(hasta) && fin().isAfter(desde);
    }
}
