package com.tecsup.reservas.repository;

import com.tecsup.reservas.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busca por código o por correo, sin distinguir mayúsculas. */
    Optional<Usuario> findByCodigoIgnoreCaseOrCorreoIgnoreCase(String codigo, String correo);

    /** Bloquea las filas (en orden de id) para que dos reservas simultáneas no gasten la misma hora. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id in :ids order by u.id")
    List<Usuario> bloquearPorIds(@Param("ids") Collection<Long> ids);
}
