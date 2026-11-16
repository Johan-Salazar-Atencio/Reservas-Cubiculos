package com.tecsup.reservas.dto;

import java.util.Map;

/** Formato único de error de la API. */
public record ErrorResponse(String codigo, String mensaje, Map<String, String> campos) {
}
