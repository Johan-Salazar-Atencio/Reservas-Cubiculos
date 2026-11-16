package com.tecsup.reservas.services.impl;

import com.tecsup.reservas.dto.AuthDtos.CuentaDemo;
import com.tecsup.reservas.dto.AuthDtos.LoginRequest;
import com.tecsup.reservas.dto.AuthDtos.LoginResponse;
import com.tecsup.reservas.dto.AuthDtos.UsuarioResponse;
import com.tecsup.reservas.exception.CredencialesInvalidasException;
import com.tecsup.reservas.exception.ResourceNotFoundException;
import com.tecsup.reservas.model.Rol;
import com.tecsup.reservas.model.Usuario;
import com.tecsup.reservas.repository.UsuarioRepository;
import com.tecsup.reservas.security.JwtService;
import com.tecsup.reservas.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    /**
     * Estudiante: basta el código o el correo (debe estar en la lista de estudiantes).
     * Administrador: además necesita su contraseña, entre por donde entre.
     */
    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String identificador = request.identificador().trim();
        Usuario u = usuarios.findByCodigoIgnoreCaseOrCorreoIgnoreCase(identificador, identificador)
                .orElseThrow(() -> new CredencialesInvalidasException(
                        "No encontramos ese código o correo en la lista de estudiantes."));
        if (!u.isActivo()) {
            throw new CredencialesInvalidasException("Esta cuenta está desactivada.");
        }
        if (u.getRol() == Rol.ADMIN) {
            boolean claveOk = request.clave() != null && u.getClaveHash() != null
                    && encoder.matches(request.clave(), u.getClaveHash());
            if (!claveOk) {
                throw new CredencialesInvalidasException("Usuario o contraseña incorrectos.");
            }
        }
        return respuesta(u);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse perfil(Long usuarioId) {
        return aDto(usuarios.findById(usuarioId).orElseThrow(() -> new ResourceNotFoundException("No encontramos tu cuenta.")));
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse loginDemo(String codigo) {
        Usuario u = usuarios.findByCodigoIgnoreCaseOrCorreoIgnoreCase(codigo, codigo)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta de demostración " + codigo + "."));
        return respuesta(u);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaDemo> cuentasDemo() {
        return usuarios.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getRol).thenComparing(Usuario::getCodigo))
                .map(u -> new CuentaDemo(u.getCodigo(), u.getNombre(), u.getRol()))
                .toList();
    }

    private LoginResponse respuesta(Usuario u) {
        return new LoginResponse(jwt.emitir(u), aDto(u));
    }

    private UsuarioResponse aDto(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getCodigo(), u.getCorreo(), u.getNombre(), u.getRol());
    }
}
