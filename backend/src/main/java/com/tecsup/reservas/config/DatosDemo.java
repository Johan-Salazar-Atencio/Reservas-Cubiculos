package com.tecsup.reservas.config;

import com.tecsup.reservas.model.AccionRegistro;
import com.tecsup.reservas.model.Cubiculo;
import com.tecsup.reservas.model.EstadoReserva;
import com.tecsup.reservas.model.RegistroAdmin;
import com.tecsup.reservas.model.Reserva;
import com.tecsup.reservas.model.Rol;
import com.tecsup.reservas.model.TipoReserva;
import com.tecsup.reservas.model.Usuario;
import com.tecsup.reservas.repository.CubiculoRepository;
import com.tecsup.reservas.repository.RegistroAdminRepository;
import com.tecsup.reservas.repository.ReservaRepository;
import com.tecsup.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Solo en dev: crea los 4 cubículos, un administrador, 12 estudiantes de prueba y algo de historial para
 * que las estadísticas no estén vacías. Todos los nombres llevan "Demo" porque no son personas reales.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DatosDemo implements ApplicationRunner {

    private static final String[] NOMBRES = {"Ana", "Bruno", "Carla", "Diego", "Elena", "Felipe",
            "Gabriela", "Hugo", "Ivana", "Jorge", "Karen", "Luis"};

    private final UsuarioRepository usuarios;
    private final CubiculoRepository cubiculos;
    private final ReservaRepository reservas;
    private final RegistroAdminRepository registro;
    private final PasswordEncoder encoder;
    private final RelojSimulado reloj;
    private final ReservasProperties p;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        sembrar();
    }

    /** Borra todo y vuelve a crear los datos de demostración (botón "Restablecer demo" del simulador). */
    @Transactional
    public void reiniciar() {
        // Hibernate ejecuta los DELETE después de los INSERT; sin estos flush, los códigos únicos (DEMO01...) chocarían.
        reservas.deleteAll();
        reservas.flush();
        registro.deleteAllInBatch();
        usuarios.deleteAllInBatch();
        cubiculos.deleteAllInBatch();
        sembrar();
    }

    private void sembrar() {
        LocalDate hoy = LocalDate.now(p.zona());
        reloj.irA(hoy.atTime(p.demo().hora()));
        LocalDateTime ahora = LocalDateTime.now(reloj);

        List<Cubiculo> lista = new ArrayList<>();
        for (int i = 1; i <= p.cubiculos(); i++) {
            Cubiculo c = new Cubiculo();
            c.setNumero(i);
            c.setNombre("Cubículo " + i);
            lista.add(cubiculos.save(c));
        }

        crearUsuario("ADMIN", "admin@tecsup.edu.pe", "Administración", Rol.ADMIN, encoder.encode(p.demo().claveAdmin()));
        List<Usuario> est = new ArrayList<>();
        for (int i = 0; i < NOMBRES.length; i++) {
            est.add(crearUsuario(String.format("DEMO%02d", i + 1), String.format("demo%02d@tecsup.edu.pe", i + 1),
                    NOMBRES[i] + " Demo", Rol.ESTUDIANTE, null));
        }
        est.get(11).setAusenciasSeguidas(1);
        usuarios.save(est.get(11));

        // Hoy: un cubículo ocupado, un relevo de 2 horas para más tarde y una reserva de la tarde.
        crearReserva(lista.get(1), hoy, LocalTime.of(10, 0), List.of(est.get(2)),
                EstadoReserva.EN_CURSO, hoy.atTime(8, 30), ahora);
        crearReserva(lista.get(2), hoy, LocalTime.of(11, 0), List.of(est.get(3), est.get(4)),
                EstadoReserva.RESERVADA, hoy.atTime(9, 0), null);
        crearReserva(lista.get(3), hoy, LocalTime.of(14, 0), List.of(est.get(5)),
                EstadoReserva.RESERVADA, hoy.atTime(9, 15), null);

        sembrarHistorial(hoy, lista, est);
    }

    private void sembrarHistorial(LocalDate hoy, List<Cubiculo> lista, List<Usuario> est) {
        Random azar = new Random(2026);
        for (int d = 1; d <= 6; d++) {
            LocalDate dia = hoy.minusDays(d);
            Set<String> turnos = new HashSet<>();
            Set<Long> personas = new HashSet<>();
            int cantidad = 5 + azar.nextInt(4);
            for (int k = 0; k < cantidad; k++) {
                Cubiculo c = lista.get(azar.nextInt(lista.size()));
                int hora = azar.nextInt(100) < 75 ? 9 + azar.nextInt(7) : 7 + azar.nextInt(11);
                Usuario u = est.get(azar.nextInt(est.size()));
                if (!turnos.add(c.getNumero() + "-" + hora) || !personas.add(u.getId())) {
                    continue;
                }
                int dado = azar.nextInt(100);
                EstadoReserva estado = dado < 78 ? EstadoReserva.COMPLETADA
                        : dado < 92 ? EstadoReserva.AUSENTE : EstadoReserva.CANCELADA;
                LocalDateTime inicio = dia.atTime(hora, 0);
                Reserva r = crearReserva(c, dia, LocalTime.of(hora, 0), List.of(u), estado, inicio.minusHours(2),
                        estado == EstadoReserva.COMPLETADA ? inicio.plusMinutes(2 + azar.nextInt(6)) : null);
                if (estado == EstadoReserva.AUSENTE) {
                    anotar("SISTEMA", AccionRegistro.LIBERACION_AUTOMATICA, c.getNumero(),
                            "No llegó nadie en " + p.minutosCheckin() + " min",
                            "Reserva de " + u.getCodigo() + " a las " + hora + ":00", r.inicio().plusMinutes(p.minutosCheckin()));
                }
            }
        }
        LocalDateTime ayer = hoy.minusDays(1).atTime(15, 20);
        anotar("ADMIN", AccionRegistro.MANTENIMIENTO_ON, 4, "Cambio de la tira LED", "0 reservas anuladas", ayer);
        anotar("ADMIN", AccionRegistro.MANTENIMIENTO_OFF, 4, "Tira LED cambiada", null, ayer.plusHours(1));
        anotar("ADMIN", AccionRegistro.LIBERAR, 2, "La persona se retiró y olvidó pulsar Terminé", null, ayer.plusHours(2));
    }

    private Usuario crearUsuario(String codigo, String correo, String nombre, Rol rol, String claveHash) {
        Usuario u = new Usuario();
        u.setCodigo(codigo);
        u.setCorreo(correo);
        u.setNombre(nombre);
        u.setRol(rol);
        u.setClaveHash(claveHash);
        return usuarios.save(u);
    }

    private Reserva crearReserva(Cubiculo c, LocalDate fecha, LocalTime hora, List<Usuario> personas,
                                 EstadoReserva estado, LocalDateTime creada, LocalDateTime checkIn) {
        Reserva r = new Reserva();
        r.setCubiculo(c);
        r.setFecha(fecha);
        r.setHoraInicio(hora);
        r.setHoras(personas.size());
        r.setTipo(personas.size() == 1 ? TipoReserva.INDIVIDUAL : TipoReserva.GRUPO);
        r.setEstado(estado);
        r.setCreador(personas.get(0));
        r.getIntegrantes().addAll(personas);
        r.setCreadaEn(creada);
        LocalDateTime base = r.inicio().isAfter(creada) ? r.inicio() : creada;
        r.setCheckInLimite(base.plusMinutes(p.minutosCheckin()));
        r.setCheckInEn(checkIn);
        if (estado == EstadoReserva.COMPLETADA) {
            r.setFinRealEn(r.fin());
        } else if (estado == EstadoReserva.AUSENTE) {
            r.setFinRealEn(r.inicio().plusMinutes(p.minutosCheckin()));
        } else if (estado == EstadoReserva.CANCELADA) {
            r.setCanceladaEn(creada.plusHours(1));
        }
        return reservas.save(r);
    }

    private void anotar(String actor, AccionRegistro accion, Integer cubiculo, String motivo, String detalle, LocalDateTime momento) {
        RegistroAdmin r = new RegistroAdmin();
        r.setMomento(momento);
        r.setActor(actor);
        r.setAccion(accion);
        r.setCubiculoNumero(cubiculo);
        r.setMotivo(motivo);
        r.setDetalle(detalle);
        registro.save(r);
    }
}
