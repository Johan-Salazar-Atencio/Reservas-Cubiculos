package com.tecsup.reservas.exception;

/** No existe lo que se pidió (cubículo, reserva, estudiante). Responde 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}
