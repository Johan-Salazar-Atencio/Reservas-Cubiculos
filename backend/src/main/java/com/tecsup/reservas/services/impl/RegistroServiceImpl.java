package com.tecsup.reservas.services.impl;

import com.tecsup.reservas.dto.AdminDtos.RegistroResponse;
import com.tecsup.reservas.model.AccionRegistro;
import com.tecsup.reservas.model.RegistroAdmin;
import com.tecsup.reservas.repository.RegistroAdminRepository;
import com.tecsup.reservas.services.Formato;
import com.tecsup.reservas.services.RegistroService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistroServiceImpl implements RegistroService {

    private final RegistroAdminRepository repositorio;
    private final Clock clock;

    @Override
    @Transactional
    public void registrar(String actor, AccionRegistro accion, Integer cubiculoNumero, String motivo, String detalle) {
        RegistroAdmin r = new RegistroAdmin();
        r.setMomento(LocalDateTime.now(clock));
        r.setActor(actor);
        r.setAccion(accion);
        r.setCubiculoNumero(cubiculoNumero);
        r.setMotivo(motivo);
        r.setDetalle(detalle);
        repositorio.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroResponse> ultimos(int limite) {
        int n = Math.max(1, Math.min(limite, 200));
        return repositorio.findAllByOrderByMomentoDescIdDesc(PageRequest.of(0, n)).stream()
                .map(r -> new RegistroResponse(r.getId(), Formato.iso(r.getMomento()), r.getActor(), r.getAccion(),
                        r.getCubiculoNumero(), r.getMotivo(), r.getDetalle()))
                .toList();
    }
}
