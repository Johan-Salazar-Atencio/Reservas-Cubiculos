package com.tecsup.reservas.services;

import com.tecsup.reservas.dto.AuthDtos.CuentaDemo;
import com.tecsup.reservas.dto.AuthDtos.LoginRequest;
import com.tecsup.reservas.dto.AuthDtos.LoginResponse;
import com.tecsup.reservas.dto.AuthDtos.UsuarioResponse;

import java.util.List;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UsuarioResponse perfil(Long usuarioId);

    /** Solo dev: entrar con una cuenta de demostración sin contraseña. */
    LoginResponse loginDemo(String codigo);

    List<CuentaDemo> cuentasDemo();
}
