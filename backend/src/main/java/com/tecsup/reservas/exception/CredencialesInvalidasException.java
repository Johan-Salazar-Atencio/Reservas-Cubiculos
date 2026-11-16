package com.tecsup.reservas.exception;

/** El código, correo o contraseña no sirven. Responde 401. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
