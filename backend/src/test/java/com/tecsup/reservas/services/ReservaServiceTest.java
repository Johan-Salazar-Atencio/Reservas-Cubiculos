package com.tecsup.reservas.services;

import com.tecsup.reservas.BaseServicioTest;
import com.tecsup.reservas.dto.CubiculoDtos.CubiculoVivoResponse;
import com.tecsup.reservas.dto.CubiculoDtos.DispositivoEstadoResponse;
import com.tecsup.reservas.dto.ReservaDtos.CrearReservaRequest;
import com.tecsup.reservas.dto.ReservaDtos.ReservaResponse;
import com.tecsup.reservas.exception.BusinessException;
import com.tecsup.reservas.model.AccionRegistro;
import com.tecsup.reservas.model.EstadoReserva;
import com.tecsup.reservas.model.EstadoVisible;
import com.tecsup.reservas.model.Luz;
import com.tecsup.reservas.model.Reserva;
import com.tecsup.reservas.model.TipoReserva;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/** Reglas del negocio con el reloj congelado: cuota, solapes, relevos, check-in de 10 minutos y sanciones. */
class ReservaServiceTest extends BaseServicioTest {

    @Autowired ReservaService servicio;
    @Autowired SensorService sensores;
    @Autowired AdminService admin;
    @Autowired CubiculoService vista;
    @Autowired ProcesoTiempoService proceso;

    private ReservaResponse reservar(int estudiante, int cubiculo, String hora, String... otros) {
        return servicio.crear(est(estudiante).getId(), new CrearReservaRequest(cub(cubiculo).getId(), hora, List.of(otros)));
    }

    private String codigoDelError(ThrowingCallable accion) {
        Throwable t = catchThrowable(accion);
        assertThat(t).isInstanceOf(BusinessException.class);
        return ((BusinessException) t).getCodigo();
    }

    private CubiculoVivoResponse enVivo(int numero) {
        return vista.enVivo(null).cubiculos().stream().filter(c -> c.numero() == numero).findFirst().orElseThrow();
    }

    private Reserva reserva(Long id) {
        return reservas.findById(id).orElseThrow();
    }

    // ---------- horario y turnos ----------

    @Test
    void reservaIndividualValida() {
        ReservaResponse r = reservar(1, 1, "10:00");
        assertThat(r.estado()).isEqualTo(EstadoReserva.RESERVADA);
        assertThat(r.tipo()).isEqualTo(TipoReserva.INDIVIDUAL);
        assertThat(r.horaInicio()).isEqualTo("10:00");
        assertThat(r.horaFin()).isEqualTo("11:00");
        assertThat(r.horas()).isEqualTo(1);
        assertThat(r.esCreador()).isTrue();
    }

    @Test
    void rechazaHorasInvalidasYTurnosPasados() {
        assertThat(codigoDelError(() -> reservar(1, 1, "10:30"))).isEqualTo("HORA_INVALIDA");
        assertThat(codigoDelError(() -> reservar(1, 1, "abc"))).isEqualTo("HORA_INVALIDA");
        assertThat(codigoDelError(() -> reservar(1, 1, "06:00"))).isEqualTo("FUERA_DE_HORARIO");
        assertThat(codigoDelError(() -> reservar(1, 1, "19:00"))).isEqualTo("FUERA_DE_HORARIO");
        // un relevo de 2 desde las 18:00 terminaría a las 20:00
        assertThat(codigoDelError(() -> reservar(1, 1, "18:00", "E2"))).isEqualTo("BLOQUE_PASA_DEL_CIERRE");
        a(11, 30);
        assertThat(codigoDelError(() -> reservar(1, 1, "10:00"))).isEqualTo("TURNO_PASADO");
    }

    @Test
    void sePuedeReservarElTurnoEnCursoYLosDiezMinutosCuentanDesdeQueSeReserva() {
        a(10, 30);
        ReservaResponse r = reservar(1, 1, "10:00");
        assertThat(r.checkInLimite()).isEqualTo("10:40");
        assertThat(r.segundosParaLimite()).isEqualTo(600);
    }

    @Test
    void sinAtencionLosDomingosNiFueraDeHorario() {
        reloj.congelarEn(LocalDate.of(2026, 11, 8).atTime(10, 0));
        assertThat(codigoDelError(() -> reservar(1, 1, "11:00"))).isEqualTo("DIA_SIN_ATENCION");
        assertThat(vista.enVivo(null).abierto()).isFalse();

        a(20, 0);
        assertThat(vista.enVivo(null).abierto()).isFalse();
        assertThat(enVivo(1).estado()).isEqualTo(EstadoVisible.CERRADO);
    }

    // ---------- cuota y cancelaciones ----------

    @Test
    void soloUnaHoraPorDia() {
        reservar(1, 1, "10:00");
        assertThat(codigoDelError(() -> reservar(1, 2, "11:00"))).isEqualTo("CUOTA_AGOTADA");
    }

    @Test
    void cancelarConAnticipacionDevuelveLaHora() {
        ReservaResponse r = reservar(1, 1, "10:00");
        ReservaResponse cancelada = servicio.cancelar(est(1).getId(), r.id());
        assertThat(cancelada.estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(reservar(1, 2, "11:00").estado()).isEqualTo(EstadoReserva.RESERVADA);
    }

    @Test
    void cancelarTardeGastaLaHoraPeroNoEsAusencia() {
        ReservaResponse r = reservar(1, 1, "10:00");
        a(9, 50);
        ReservaResponse cancelada = servicio.cancelar(est(1).getId(), r.id());
        assertThat(cancelada.estado()).isEqualTo(EstadoReserva.CANCELADA_TARDE);
        assertThat(codigoDelError(() -> reservar(1, 2, "11:00"))).isEqualTo("CUOTA_AGOTADA");
        assertThat(est(1).getAusenciasSeguidas()).isZero();
    }

    @Test
    void soloQuienReservoPuedeCancelar() {
        ReservaResponse r = reservar(1, 1, "10:00");
        assertThat(codigoDelError(() -> servicio.cancelar(est(2).getId(), r.id()))).isEqualTo("NO_ES_QUIEN_RESERVO");
    }

    // ---------- solapes ----------

    @Test
    void noSePuedeReservarUnTurnoOcupado() {
        reservar(1, 1, "10:00");
        assertThat(codigoDelError(() -> reservar(2, 1, "10:00"))).isEqualTo("TURNO_OCUPADO");
        assertThat(reservar(2, 2, "10:00").estado()).isEqualTo(EstadoReserva.RESERVADA); // otro cubículo, sí
        assertThat(reservar(3, 1, "11:00").estado()).isEqualTo(EstadoReserva.RESERVADA); // otro turno, sí
    }

    // ---------- relevos ----------

    @Test
    void relevoReservaUnBloqueDeNHorasYCadaPersonaGastaSuHora() {
        ReservaResponse r = reservar(1, 1, "10:00", "E2", "e3@tecsup.edu.pe");
        assertThat(r.tipo()).isEqualTo(TipoReserva.GRUPO);
        assertThat(r.horas()).isEqualTo(3);
        assertThat(r.horaFin()).isEqualTo("13:00");
        assertThat(r.integrantes()).hasSize(3);
        assertThat(codigoDelError(() -> reservar(2, 2, "10:00"))).isEqualTo("CUOTA_AGOTADA");
        assertThat(codigoDelError(() -> reservar(3, 2, "10:00"))).isEqualTo("CUOTA_AGOTADA");
    }

    @Test
    void relevoEsTodoONada() {
        reservar(4, 1, "11:00"); // el segundo turno del bloque ya está ocupado
        assertThat(codigoDelError(() -> reservar(1, 1, "10:00", "E2"))).isEqualTo("TURNO_OCUPADO");
        // nadie del grupo gastó su hora
        assertThat(reservar(1, 2, "10:00").estado()).isEqualTo(EstadoReserva.RESERVADA);
        assertThat(reservar(2, 3, "10:00").estado()).isEqualTo(EstadoReserva.RESERVADA);
    }

    @Test
    void relevoRechazaPersonasRepetidasInexistentesOSinHora() {
        assertThat(codigoDelError(() -> reservar(1, 1, "10:00", "E1"))).isEqualTo("INTEGRANTE_REPETIDO");
        assertThat(codigoDelError(() -> reservar(1, 1, "10:00", "NOEXISTE"))).isEqualTo("ESTUDIANTE_NO_ENCONTRADO");
        assertThat(codigoDelError(() -> reservar(1, 1, "10:00", "ADMIN"))).isEqualTo("ESTUDIANTE_NO_ENCONTRADO");
        reservar(2, 2, "10:00");
        assertThat(codigoDelError(() -> reservar(1, 1, "10:00", "E2"))).isEqualTo("CUOTA_AGOTADA");
    }

    // ---------- llegada (sensor) y los 10 minutos ----------

    @Test
    void laPresenciaDentroDeLosDiezMinutosConfirmaLaLlegada() {
        ReservaResponse r = reservar(1, 1, "10:00");
        a(10, 3);
        DispositivoEstadoResponse resp = sensores.presencia(1);
        assertThat(resp.checkInConfirmado()).isTrue();
        assertThat(resp.estado()).isEqualTo(EstadoVisible.OCUPADO);
        assertThat(resp.luz()).isEqualTo(Luz.AZUL);
        assertThat(resp.tipoReserva()).isEqualTo(TipoReserva.INDIVIDUAL);
        assertThat(reserva(r.id()).getEstado()).isEqualTo(EstadoReserva.EN_CURSO);

        // con la reserva confirmada, la sala sigue ocupada aunque el PIR ya no vea movimiento
        a(10, 40);
        assertThat(enVivo(1).estado()).isEqualTo(EstadoVisible.OCUPADO);
        assertThat(sensores.presencia(1).checkInConfirmado()).isFalse();
    }

    @Test
    void laPresenciaAntesDelInicioNoCuenta() {
        ReservaResponse r = reservar(1, 1, "10:00");
        a(9, 55);
        assertThat(sensores.presencia(1).checkInConfirmado()).isFalse();
        assertThat(reserva(r.id()).getEstado()).isEqualTo(EstadoReserva.RESERVADA);
    }

    @Test
    void sinPresenciaLaSalaSeLiberaALosDiezMinutos() {
        ReservaResponse r = reservar(1, 1, "10:00");
        a(10, 5);
        CubiculoVivoResponse esperando = enVivo(1);
        assertThat(esperando.estado()).isEqualTo(EstadoVisible.RESERVADO);
        assertThat(esperando.luz()).isEqualTo(Luz.AMBAR);
        assertThat(esperando.llegarAntesDe()).isEqualTo("10:10");

        a(10, 10);
        proceso.procesar();
        assertThat(reserva(r.id()).getEstado()).isEqualTo(EstadoReserva.AUSENTE);
        assertThat(enVivo(1).estado()).isEqualTo(EstadoVisible.LIBRE);
        assertThat(est(1).getAusenciasSeguidas()).isEqualTo(1);
        assertThat(codigoDelError(() -> reservar(1, 2, "11:00"))).isEqualTo("CUOTA_AGOTADA");
        assertThat(registro.findAll()).anyMatch(x -> x.getAccion() == AccionRegistro.LIBERACION_AUTOMATICA);
    }

    @Test
    void siNadieLlegaSeLiberaElBloqueEnteroYSoloSeAnotaAQuienReservo() {
        ReservaResponse r = reservar(1, 1, "10:00", "E2");
        a(10, 10);
        proceso.procesar();
        assertThat(reserva(r.id()).getEstado()).isEqualTo(EstadoReserva.AUSENTE);
        assertThat(enVivo(1).estado()).isEqualTo(EstadoVisible.LIBRE);
        assertThat(est(1).getAusenciasSeguidas()).isEqualTo(1);
        assertThat(est(2).getAusenciasSeguidas()).isZero();
        // el segundo turno del bloque también quedó libre
        assertThat(reservar(3, 1, "11:00").estado()).isEqualTo(EstadoReserva.RESERVADA);
    }

    @Test
    void dosAusenciasSeguidasActivanLaPausa() {
        reservar(1, 1, "10:00");
        a(10, 10);
        proceso.procesar();
        assertThat(est(1).getAusenciasSeguidas()).isEqualTo(1);

        reloj.congelarEn(LUNES.plusDays(1).atTime(9, 0)); // martes
        reservar(1, 1, "10:00");
        reloj.congelarEn(LUNES.plusDays(1).atTime(10, 10));
        proceso.procesar();
        assertThat(est(1).getPausadoHasta()).isEqualTo(LUNES.plusDays(2)); // miércoles
        assertThat(est(1).getAusenciasSeguidas()).isZero();
        assertThat(registro.findAll()).anyMatch(x -> x.getAccion() == AccionRegistro.PAUSA_AUTOMATICA);

        reloj.congelarEn(LUNES.plusDays(2).atTime(9, 0));
        assertThat(codigoDelError(() -> reservar(1, 1, "10:00"))).isEqualTo("PAUSA_ACTIVA");
        reloj.congelarEn(LUNES.plusDays(3).atTime(9, 0));
        assertThat(reservar(1, 1, "10:00").estado()).isEqualTo(EstadoReserva.RESERVADA);
    }

    @Test
    void llegarATiempoReiniciaElContadorDeAusencias() {
        reservar(1, 1, "10:00");
        a(10, 10);
        proceso.procesar();
        assertThat(est(1).getAusenciasSeguidas()).isEqualTo(1);

        reloj.congelarEn(LUNES.plusDays(1).atTime(9, 0));
        reservar(1, 1, "10:00");
        reloj.congelarEn(LUNES.plusDays(1).atTime(10, 2));
        sensores.presencia(1);
        assertThat(est(1).getAusenciasSeguidas()).isZero();
    }

    // ---------- terminar ----------

    @Test
    void terminarLiberaLoQueQuedaDelBloque() {
        ReservaResponse r = reservar(1, 1, "10:00", "E2");
        assertThat(codigoDelError(() -> servicio.terminar(est(2).getId(), r.id()))).isEqualTo("NO_SE_PUEDE_TERMINAR");
        a(10, 2);
        sensores.presencia(1);
        a(10, 30);
        ReservaResponse fin = servicio.terminar(est(2).getId(), r.id()); // cualquiera del grupo puede terminar
        assertThat(fin.estado()).isEqualTo(EstadoReserva.COMPLETADA);
        assertThat(enVivo(1).estado()).isEqualTo(EstadoVisible.LIBRE);
        assertThat(reservar(3, 1, "11:00").estado()).isEqualTo(EstadoReserva.RESERVADA);
        assertThat(codigoDelError(() -> reservar(1, 2, "11:00"))).isEqualTo("CUOTA_AGOTADA"); // la hora ya se usó
    }

    // ---------- colores de la maqueta ----------

    @Test
    void elEstadoVisibleYLaLuzCambianConElTiempo() {
        reservar(1, 1, "10:00");
        CubiculoVivoResponse libre = enVivo(1);
        assertThat(libre.estado()).isEqualTo(EstadoVisible.LIBRE);
        assertThat(libre.luz()).isEqualTo(Luz.APAGADA);
        assertThat(libre.proximaReserva()).isEqualTo("10:00");

        a(10, 0);
        assertThat(enVivo(1).luz()).isEqualTo(Luz.AMBAR);
        a(10, 2);
        sensores.presencia(1);
        assertThat(enVivo(1).luz()).isEqualTo(Luz.AZUL);
        a(10, 54);
        assertThat(enVivo(1).estado()).isEqualTo(EstadoVisible.OCUPADO);
        a(10, 55);
        CubiculoVivoResponse porTerminar = enVivo(1);
        assertThat(porTerminar.estado()).isEqualTo(EstadoVisible.POR_TERMINAR);
        assertThat(porTerminar.luz()).isEqualTo(Luz.ROJO);
        assertThat(porTerminar.minutosRestantes()).isEqualTo(5);
        a(11, 0);
        assertThat(enVivo(1).estado()).isEqualTo(EstadoVisible.LIBRE);
        assertThat(reservas.findAll()).allMatch(x -> x.getEstado() == EstadoReserva.COMPLETADA);
    }

    // ---------- administrador ----------

    @Test
    void elAdministradorPuedeLiberarYOcuparConMotivo() {
        ReservaResponse r = reservar(1, 1, "10:00");
        a(10, 2);
        sensores.presencia(1);
        admin.liberar(admin().getId(), cub(1).getId(), "La persona se retiró y olvidó terminar");
        assertThat(reserva(r.id()).getEstado()).isEqualTo(EstadoReserva.ANULADA_ADMIN);
        assertThat(enVivo(1).estado()).isEqualTo(EstadoVisible.LIBRE);
        assertThat(codigoDelError(() -> admin.liberar(admin().getId(), cub(1).getId(), "Ya estaba libre"))).isEqualTo("YA_ESTA_LIBRE");
        assertThat(reservar(1, 2, "11:00").estado()).isEqualTo(EstadoReserva.RESERVADA); // la hora se devolvió

        admin.ocupar(admin().getId(), cub(3).getId(), 2, "Visita de la dirección");
        CubiculoVivoResponse ocupado = enVivo(3);
        assertThat(ocupado.estado()).isEqualTo(EstadoVisible.OCUPADO_ADMIN);
        assertThat(ocupado.ocupadoHasta()).isEqualTo("12:00");
        assertThat(codigoDelError(() -> reservar(2, 3, "11:00"))).isEqualTo("TURNO_OCUPADO");

        assertThat(registro.findAll()).extracting(x -> x.getAccion())
                .contains(AccionRegistro.LIBERAR, AccionRegistro.OCUPAR);
    }

    @Test
    void elMantenimientoAnulaLasReservasDeHoyYBloqueaLasNuevas() {
        ReservaResponse r = reservar(1, 1, "10:00");
        admin.mantenimiento(admin().getId(), cub(1).getId(), true, "Cambio de la tira LED");
        assertThat(reserva(r.id()).getEstado()).isEqualTo(EstadoReserva.ANULADA_ADMIN);
        CubiculoVivoResponse c = enVivo(1);
        assertThat(c.estado()).isEqualTo(EstadoVisible.MANTENIMIENTO);
        assertThat(c.luz()).isEqualTo(Luz.VIOLETA);
        assertThat(codigoDelError(() -> reservar(2, 1, "12:00"))).isEqualTo("EN_MANTENIMIENTO");
        assertThat(reservar(1, 2, "10:00").estado()).isEqualTo(EstadoReserva.RESERVADA); // su hora se devolvió

        admin.mantenimiento(admin().getId(), cub(1).getId(), false, "Ya quedó listo");
        assertThat(reservar(3, 1, "12:00").estado()).isEqualTo(EstadoReserva.RESERVADA);
    }

    // ---------- concurrencia ----------

    private String intentar(CountDownLatch salida, Long usuarioId, Long cubiculoId, String hora) {
        try {
            salida.await();
            servicio.crear(usuarioId, new CrearReservaRequest(cubiculoId, hora, List.of()));
            return "OK";
        } catch (BusinessException e) {
            return e.getCodigo();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "INTERRUMPIDO";
        }
    }

    @Test
    void dosPersonasPidiendoElMismoTurnoAlMismoTiempoSoloUnaLoConsigue() throws Exception {
        Long u1 = est(1).getId();
        Long u2 = est(2).getId();
        Long c = cub(1).getId();
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> a = pool.submit(() -> intentar(salida, u1, c, "10:00"));
            Future<String> b = pool.submit(() -> intentar(salida, u2, c, "10:00"));
            salida.countDown();
            assertThat(List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("OK", "TURNO_OCUPADO");
        } finally {
            pool.shutdownNow();
        }
        assertThat(reservas.findAll()).hasSize(1);
    }

    @Test
    void unaPersonaNoPuedeGastarDosHorasPidiendoDosCubiculosAlMismoTiempo() throws Exception {
        Long u1 = est(1).getId();
        Long c1 = cub(1).getId();
        Long c2 = cub(2).getId();
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> a = pool.submit(() -> intentar(salida, u1, c1, "10:00"));
            Future<String> b = pool.submit(() -> intentar(salida, u1, c2, "11:00"));
            salida.countDown();
            assertThat(List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("OK", "CUOTA_AGOTADA");
        } finally {
            pool.shutdownNow();
        }
        assertThat(reservas.findAll()).hasSize(1);
    }
}
