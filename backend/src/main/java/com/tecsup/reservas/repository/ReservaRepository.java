package com.tecsup.reservas.repository;

import com.tecsup.reservas.model.EstadoReserva;
import com.tecsup.reservas.model.Reserva;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByEstadoIn(Collection<EstadoReserva> estados);

    List<Reserva> findByFechaAndEstadoIn(LocalDate fecha, Collection<EstadoReserva> estados);

    List<Reserva> findByCubiculoIdAndFechaAndEstadoIn(Long cubiculoId, LocalDate fecha, Collection<EstadoReserva> estados);

    List<Reserva> findByFechaOrderByHoraInicioAscCubiculoNumeroAsc(LocalDate fecha);

    List<Reserva> findByFechaGreaterThanEqual(LocalDate desde);

    /** Reservas en las que participa una persona un día dado. */
    @Query("select r from Reserva r join r.integrantes i "
            + "where i.id = :usuarioId and r.fecha = :fecha and r.estado in :estados")
    List<Reserva> findDeUsuario(@Param("usuarioId") Long usuarioId,
                                @Param("fecha") LocalDate fecha,
                                @Param("estados") Collection<EstadoReserva> estados);

    @Query("select r from Reserva r join r.integrantes i "
            + "where i.id = :usuarioId and r.fecha < :hoy order by r.fecha desc, r.horaInicio desc")
    List<Reserva> historialDe(@Param("usuarioId") Long usuarioId, @Param("hoy") LocalDate hoy, Pageable pageable);
}
