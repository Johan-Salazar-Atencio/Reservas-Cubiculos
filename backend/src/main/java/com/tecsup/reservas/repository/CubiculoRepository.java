package com.tecsup.reservas.repository;

import com.tecsup.reservas.model.Cubiculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CubiculoRepository extends JpaRepository<Cubiculo, Long> {

    List<Cubiculo> findAllByOrderByNumeroAsc();

    Optional<Cubiculo> findByNumero(int numero);

    /** Bloquea el cubículo mientras se crea una reserva: así dos personas no toman el mismo turno. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cubiculo c where c.id = :id")
    Optional<Cubiculo> bloquearPorId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cubiculo c where c.numero = :numero")
    Optional<Cubiculo> bloquearPorNumero(@Param("numero") int numero);
}
