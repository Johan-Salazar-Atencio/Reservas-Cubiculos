package com.tecsup.reservas.controller;

import com.tecsup.reservas.dto.ReservaDtos.CrearReservaRequest;
import com.tecsup.reservas.dto.ReservaDtos.MisReservasResponse;
import com.tecsup.reservas.dto.ReservaDtos.ReservaResponse;
import com.tecsup.reservas.security.UsuarioActual;
import com.tecsup.reservas.services.ReservaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reservas (estudiante)")
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservas;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CrearReservaRequest request) {
        return reservas.crear(UsuarioActual.id(jwt), request);
    }

    @GetMapping("/mias")
    public MisReservasResponse mias(@AuthenticationPrincipal Jwt jwt) {
        return reservas.mias(UsuarioActual.id(jwt));
    }

    @PostMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return reservas.cancelar(UsuarioActual.id(jwt), id);
    }

    @PostMapping("/{id}/terminar")
    public ReservaResponse terminar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return reservas.terminar(UsuarioActual.id(jwt), id);
    }
}
