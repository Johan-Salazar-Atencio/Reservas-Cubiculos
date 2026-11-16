package com.tecsup.reservas.controller;

import com.tecsup.reservas.dto.CubiculoDtos.AgendaResponse;
import com.tecsup.reservas.dto.CubiculoDtos.EnVivoResponse;
import com.tecsup.reservas.security.UsuarioActual;
import com.tecsup.reservas.services.CubiculoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cubículos")
@RestController
@RequestMapping("/api/cubiculos")
@RequiredArgsConstructor
public class CubiculoController {

    private final CubiculoService cubiculos;

    /** Estado en vivo de los 4 cubículos. */
    @GetMapping
    public EnVivoResponse enVivo(@AuthenticationPrincipal Jwt jwt) {
        return cubiculos.enVivo(UsuarioActual.id(jwt));
    }

    /** Turnos de hoy de cada cubículo. */
    @GetMapping("/agenda")
    public AgendaResponse agenda(@AuthenticationPrincipal Jwt jwt) {
        return cubiculos.agenda(UsuarioActual.id(jwt));
    }
}
