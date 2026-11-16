package com.tecsup.reservas.dto;

import com.tecsup.reservas.model.Rol;
import jakarta.validation.constraints.NotBlank;

/** Entradas y salidas del inicio de sesión. */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Estudiante: solo identificador (código o correo). Administrador: identificador y clave. */
    public record LoginRequest(
            @NotBlank(message = "Escribe tu código o correo institucional") String identificador,
            String clave) {
    }

    public record UsuarioResponse(Long id, String codigo, String correo, String nombre, Rol rol) {
    }

    public record LoginResponse(String token, UsuarioResponse usuario) {
    }

    /** Cuenta de demostración que se muestra en la pantalla de ingreso (solo perfil dev). */
    public record CuentaDemo(String codigo, String nombre, Rol rol) {
    }
}
