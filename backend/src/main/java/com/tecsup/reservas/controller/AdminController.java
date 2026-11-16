package com.tecsup.reservas.controller;

import com.tecsup.reservas.dto.AdminDtos.AccionResponse;
import com.tecsup.reservas.dto.AdminDtos.EstadisticasResponse;
import com.tecsup.reservas.dto.AdminDtos.MantenimientoRequest;
import com.tecsup.reservas.dto.AdminDtos.MotivoRequest;
import com.tecsup.reservas.dto.AdminDtos.OcuparRequest;
import com.tecsup.reservas.dto.AdminDtos.RegistroResponse;
import com.tecsup.reservas.dto.CubiculoDtos.PanelAdminResponse;
import com.tecsup.reservas.dto.ReservaDtos.ReservaResponse;
import com.tecsup.reservas.model.EstadoReserva;
import com.tecsup.reservas.security.UsuarioActual;
import com.tecsup.reservas.services.AdminService;
import com.tecsup.reservas.services.CubiculoService;
import com.tecsup.reservas.services.EstadisticaService;
import com.tecsup.reservas.services.RegistroService;
import com.tecsup.reservas.services.ReservaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Administrador")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CubiculoService cubiculos;
    private final AdminService admin;
    private final ReservaService reservas;
    private final RegistroService registro;
    private final EstadisticaService estadisticas;

    @GetMapping("/panel")
    public PanelAdminResponse panel() {
        return cubiculos.panelAdmin();
    }

    @PostMapping("/cubiculos/{id}/liberar")
    public AccionResponse liberar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody MotivoRequest request) {
        return admin.liberar(UsuarioActual.id(jwt), id, request.motivo());
    }

    @PostMapping("/cubiculos/{id}/ocupar")
    public AccionResponse ocupar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                 @Valid @RequestBody OcuparRequest request) {
        return admin.ocupar(UsuarioActual.id(jwt), id, request.horas(), request.motivo());
    }

    @PostMapping("/cubiculos/{id}/mantenimiento")
    public AccionResponse mantenimiento(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                        @Valid @RequestBody MantenimientoRequest request) {
        return admin.mantenimiento(UsuarioActual.id(jwt), id, request.activo(), request.motivo());
    }

    /** Reservas de un día (hoy por defecto), con los códigos completos. Todos los filtros son opcionales. */
    @GetMapping("/reservas")
    public List<ReservaResponse> reservas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long cubiculoId,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) String q) {
        return reservas.listarAdmin(fecha, cubiculoId, estado, q);
    }

    @GetMapping("/registro")
    public List<RegistroResponse> registro(@RequestParam(defaultValue = "50") int limite) {
        return registro.ultimos(limite);
    }

    @GetMapping("/estadisticas")
    public EstadisticasResponse estadisticas(@RequestParam(defaultValue = "7") int dias) {
        return estadisticas.resumen(dias);
    }
}
