package com.tecsup.reservas.controller;

import com.tecsup.reservas.dto.AuthDtos.LoginRequest;
import com.tecsup.reservas.dto.AuthDtos.LoginResponse;
import com.tecsup.reservas.dto.AuthDtos.UsuarioResponse;
import com.tecsup.reservas.security.UsuarioActual;
import com.tecsup.reservas.services.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Acceso")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return auth.perfil(UsuarioActual.id(jwt));
    }
}
