package com.tecsup.reservas.controller;

import com.tecsup.reservas.config.RelojSimulado;
import com.tecsup.reservas.dto.SimuladorDtos.RelojResponse;
import com.tecsup.reservas.services.Formato;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;

/** La hora del servidor (hora de Lima). La barra superior de la página la muestra en todas las pantallas. */
@Tag(name = "Reloj")
@RestController
@RequestMapping("/api/reloj")
@RequiredArgsConstructor
public class RelojController {

    private final Clock clock;

    @GetMapping
    public RelojResponse reloj() {
        return new RelojResponse(Formato.iso(LocalDateTime.now(clock)), clock instanceof RelojSimulado);
    }
}
