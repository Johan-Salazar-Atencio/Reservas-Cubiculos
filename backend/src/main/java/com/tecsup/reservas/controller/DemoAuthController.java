package com.tecsup.reservas.controller;

import com.tecsup.reservas.dto.AuthDtos.CuentaDemo;
import com.tecsup.reservas.dto.AuthDtos.LoginResponse;
import com.tecsup.reservas.services.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Solo en dev: entrar con una cuenta de demostración con un clic (no existe en producción). */
@Tag(name = "Demostración (solo dev)")
@RestController
@Profile("dev")
@RequestMapping("/api/auth/demo")
@RequiredArgsConstructor
public class DemoAuthController {

    private final AuthService auth;

    @GetMapping
    public List<CuentaDemo> cuentas() {
        return auth.cuentasDemo();
    }

    @PostMapping("/{codigo}")
    public LoginResponse entrar(@PathVariable String codigo) {
        return auth.loginDemo(codigo);
    }
}
