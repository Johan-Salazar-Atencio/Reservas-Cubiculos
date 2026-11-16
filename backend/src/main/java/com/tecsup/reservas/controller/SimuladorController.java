package com.tecsup.reservas.controller;

import com.tecsup.reservas.config.DatosDemo;
import com.tecsup.reservas.config.LatidosSimulados;
import com.tecsup.reservas.config.RelojSimulado;
import com.tecsup.reservas.dto.AdminDtos.AccionResponse;
import com.tecsup.reservas.dto.CubiculoDtos.DispositivoEstadoResponse;
import com.tecsup.reservas.dto.SimuladorDtos.AvanzarRelojRequest;
import com.tecsup.reservas.dto.SimuladorDtos.IrAHoraRequest;
import com.tecsup.reservas.dto.SimuladorDtos.RelojResponse;
import com.tecsup.reservas.dto.SimuladorDtos.SinSenalRequest;
import com.tecsup.reservas.exception.BusinessException;
import com.tecsup.reservas.services.Formato;
import com.tecsup.reservas.services.ProcesoTiempoService;
import com.tecsup.reservas.services.SensorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Solo en dev (y solo para el administrador): reloj y sensores virtuales para probar la página sin la maqueta.
 * Llaman a los mismos servicios que usará el ESP32 real.
 */
@Tag(name = "Simulador (solo dev)")
@RestController
@Profile("dev")
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class SimuladorController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm");

    private final RelojSimulado reloj;
    private final ProcesoTiempoService proceso;
    private final SensorService sensores;
    private final DatosDemo datos;
    private final LatidosSimulados latidos;

    @GetMapping("/reloj")
    public RelojResponse reloj() {
        return respuesta();
    }

    @PostMapping("/reloj/avanzar")
    public RelojResponse avanzar(@Valid @RequestBody AvanzarRelojRequest request) {
        reloj.avanzar(Duration.ofMinutes(request.minutos()));
        proceso.procesar();
        return respuesta();
    }

    @PostMapping("/reloj/ir")
    public RelojResponse ir(@Valid @RequestBody IrAHoraRequest request) {
        LocalTime hora;
        try {
            hora = LocalTime.parse(request.hora().trim(), HORA);
        } catch (DateTimeParseException e) {
            throw new BusinessException("HORA_INVALIDA", "La hora debe tener el formato HH:mm, por ejemplo 11:00.");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime destino = ahora.toLocalDate().atTime(hora);
        if (destino.isBefore(ahora)) {
            throw new BusinessException("SOLO_ADELANTE", "Solo se puede avanzar. Usa «Restablecer demo» para volver al inicio.");
        }
        reloj.irA(destino);
        proceso.procesar();
        return respuesta();
    }

    /** Borra todo y vuelve a crear los datos de demostración con el reloj a las 10:00. */
    @PostMapping("/reloj/restablecer")
    public RelojResponse restablecer() {
        datos.reiniciar();
        return respuesta();
    }

    @PostMapping("/sensores/{numero}/presencia")
    public DispositivoEstadoResponse presencia(@PathVariable int numero) {
        return sensores.presencia(numero);
    }

    @PostMapping("/sensores/{numero}/latido")
    public DispositivoEstadoResponse latido(@PathVariable int numero) {
        return sensores.latido(numero);
    }

    /** Deja de enviar latidos de ese sensor para ver cómo se muestra "sin señal". */
    @PostMapping("/sensores/{numero}/sin-senal")
    public AccionResponse sinSenal(@PathVariable int numero, @RequestBody SinSenalRequest request) {
        latidos.sinSenal(numero, request.activo());
        return new AccionResponse(request.activo()
                ? "El sensor del cubículo " + numero + " dejó de dar señal."
                : "El sensor del cubículo " + numero + " volvió a dar señal.");
    }

    private RelojResponse respuesta() {
        return new RelojResponse(Formato.iso(LocalDateTime.now(reloj)), true);
    }
}
