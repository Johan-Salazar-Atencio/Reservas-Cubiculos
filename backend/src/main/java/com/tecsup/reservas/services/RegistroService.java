package com.tecsup.reservas.services;

import com.tecsup.reservas.dto.AdminDtos.RegistroResponse;
import com.tecsup.reservas.model.AccionRegistro;

import java.util.List;

/** Registro de auditoría: quién hizo qué, cuándo y por qué. */
public interface RegistroService {

    void registrar(String actor, AccionRegistro accion, Integer cubiculoNumero, String motivo, String detalle);

    List<RegistroResponse> ultimos(int limite);
}
