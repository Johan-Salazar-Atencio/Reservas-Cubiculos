package com.tecsup.reservas.repository;

import com.tecsup.reservas.model.RegistroAdmin;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroAdminRepository extends JpaRepository<RegistroAdmin, Long> {

    List<RegistroAdmin> findAllByOrderByMomentoDescIdDesc(Pageable pageable);
}
