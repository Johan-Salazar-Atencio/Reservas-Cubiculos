package com.tecsup.reservas.controller;

import com.tecsup.reservas.dto.CubiculoDtos.DispositivoEstadoResponse;
import com.tecsup.reservas.services.SensorService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lo que llama la maqueta (ESP32) con la cabecera X-Sensor-Token. {n} es el número del cubículo (1 a 4). */
@Tag(name = "Maqueta / sensor")
@SecurityRequirement(name = "sensor")
@RestController
@RequestMapping("/api/sensores/{numero}")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensores;

    /** El PIR detectó a alguien. */
    @PostMapping("/presencia")
    public DispositivoEstadoResponse presencia(@PathVariable int numero) {
        return sensores.presencia(numero);
    }

    /** Señal de vida. */
    @PostMapping("/latido")
    public DispositivoEstadoResponse latido(@PathVariable int numero) {
        return sensores.latido(numero);
    }

    /** La maqueta consulta qué luz mostrar (también cuenta como señal de vida). */
    @GetMapping("/estado")
    public DispositivoEstadoResponse estado(@PathVariable int numero) {
        return sensores.estado(numero);
    }
}
