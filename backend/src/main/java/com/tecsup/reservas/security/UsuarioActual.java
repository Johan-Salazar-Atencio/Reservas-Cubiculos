package com.tecsup.reservas.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Saca el id del usuario del JWT que llegó en la petición. */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public static String codigo(Jwt jwt) {
        return jwt.getClaimAsString("codigo");
    }
}
