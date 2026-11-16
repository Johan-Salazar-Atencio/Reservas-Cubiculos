package com.tecsup.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Un cubículo físico y el estado de su sensor (ESP32 + PIR). */
@Entity
@Table(name = "cubiculos")
@Getter
@Setter
@NoArgsConstructor
public class Cubiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private int numero;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCubiculo estado = EstadoCubiculo.ACTIVO;

    @Column(length = 200)
    private String motivoMantenimiento;

    /** Última vez que el sensor dio señal de vida (latido o consulta de estado). */
    private LocalDateTime ultimoLatido;

    /** Última vez que el PIR detectó a alguien. */
    private LocalDateTime ultimaPresencia;
}
