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

/** Registro de auditoría: quién hizo qué, cuándo y por qué. */
@Entity
@Table(name = "registro_admin")
@Getter
@Setter
@NoArgsConstructor
public class RegistroAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime momento;

    /** Código de quien lo hizo, o SISTEMA. */
    @Column(nullable = false, length = 40)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccionRegistro accion;

    private Integer cubiculoNumero;

    @Column(length = 300)
    private String motivo;

    @Column(length = 400)
    private String detalle;
}
