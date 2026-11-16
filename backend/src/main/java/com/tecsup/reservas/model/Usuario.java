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

import java.time.LocalDate;

/** Estudiante (de la lista de estudiantes) o administrador. */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** Solo el administrador tiene contraseña (BCrypt). Los estudiantes entran con código o correo. */
    @Column(length = 100)
    private String claveHash;

    /** Si tiene fecha, no puede reservar hasta ese día (inclusive). */
    private LocalDate pausadoHasta;

    @Column(nullable = false)
    private int ausenciasSeguidas;

    @Column(nullable = false)
    private boolean activo = true;
}
