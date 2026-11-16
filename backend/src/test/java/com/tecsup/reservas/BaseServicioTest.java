package com.tecsup.reservas;

import com.tecsup.reservas.config.RelojSimulado;
import com.tecsup.reservas.model.Cubiculo;
import com.tecsup.reservas.model.Rol;
import com.tecsup.reservas.model.Usuario;
import com.tecsup.reservas.repository.CubiculoRepository;
import com.tecsup.reservas.repository.RegistroAdminRepository;
import com.tecsup.reservas.repository.ReservaRepository;
import com.tecsup.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Base de las pruebas: vacía la base, crea 4 cubículos, un administrador y 6 estudiantes (E1 a E6) y congela el
 * reloj un lunes a las 09:00 (la atención normal es de lunes a viernes, de 07:00 a 19:00).
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseServicioTest {

    protected static final LocalDate LUNES = LocalDate.of(2026, 11, 9);
    protected static final String CLAVE_ADMIN = "clave-de-pruebas";

    @Autowired protected RelojSimulado reloj;
    @Autowired protected UsuarioRepository usuarios;
    @Autowired protected CubiculoRepository cubiculos;
    @Autowired protected ReservaRepository reservas;
    @Autowired protected RegistroAdminRepository registro;
    @Autowired protected PasswordEncoder encoder;

    @BeforeEach
    void sembrar() {
        reservas.deleteAll();
        registro.deleteAll();
        usuarios.deleteAll();
        cubiculos.deleteAll();
        for (int i = 1; i <= 4; i++) {
            Cubiculo c = new Cubiculo();
            c.setNumero(i);
            c.setNombre("Cubículo " + i);
            cubiculos.save(c);
        }
        crearUsuario("ADMIN", "admin@tecsup.edu.pe", "Administración", Rol.ADMIN, encoder.encode(CLAVE_ADMIN));
        for (int i = 1; i <= 6; i++) {
            crearUsuario("E" + i, "e" + i + "@tecsup.edu.pe", "Estudiante " + i, Rol.ESTUDIANTE, null);
        }
        a(9, 0);
    }

    protected Usuario crearUsuario(String codigo, String correo, String nombre, Rol rol, String claveHash) {
        Usuario u = new Usuario();
        u.setCodigo(codigo);
        u.setCorreo(correo);
        u.setNombre(nombre);
        u.setRol(rol);
        u.setClaveHash(claveHash);
        return usuarios.save(u);
    }

    /** Congela el reloj en el lunes de las pruebas a esa hora. */
    protected void a(int hora, int minuto) {
        reloj.congelarEn(LUNES.atTime(hora, minuto));
    }

    protected void avanzar(int minutos) {
        reloj.avanzar(Duration.ofMinutes(minutos));
    }

    protected Usuario est(int n) {
        return usuarios.findByCodigoIgnoreCaseOrCorreoIgnoreCase("E" + n, "E" + n).orElseThrow();
    }

    protected Usuario admin() {
        return usuarios.findByCodigoIgnoreCaseOrCorreoIgnoreCase("ADMIN", "ADMIN").orElseThrow();
    }

    protected Cubiculo cub(int n) {
        return cubiculos.findByNumero(n).orElseThrow();
    }
}
