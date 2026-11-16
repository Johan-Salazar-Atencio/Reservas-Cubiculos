package com.tecsup.reservas.exception;

/** Una regla del negocio no se cumple (cuota agotada, turno ocupado, fuera de horario...). Responde 409. */
public class BusinessException extends RuntimeException {

    private final String codigo;

    public BusinessException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
