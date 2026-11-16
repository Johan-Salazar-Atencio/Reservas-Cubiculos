package com.tecsup.reservas.services;

import com.tecsup.reservas.config.ReservasProperties;
import com.tecsup.reservas.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Reglas de calendario: horario, día de atención y validación de un bloque de turnos. */
@Component
@RequiredArgsConstructor
public class ReglasReserva {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm");

    private final ReservasProperties p;

    public boolean esDiaDeAtencion(LocalDate fecha) {
        return p.diasAtencion().contains(fecha.getDayOfWeek());
    }

    public boolean estaAbierto(LocalDateTime ahora) {
        LocalTime h = ahora.toLocalTime();
        return esDiaDeAtencion(ahora.toLocalDate()) && !h.isBefore(p.horaApertura()) && h.isBefore(p.horaCierre());
    }

    /** Los turnos del día: 07:00, 08:00 ... 18:00. */
    public List<LocalTime> turnos() {
        List<LocalTime> lista = new ArrayList<>();
        for (LocalTime t = p.horaApertura(); t.isBefore(p.horaCierre()); t = t.plusHours(1)) {
            lista.add(t);
        }
        return lista;
    }

    /** El día de atención que queda {@code cantidad} días de atención después de {@code desde}. */
    public LocalDate diaDeAtencionDespues(LocalDate desde, int cantidad) {
        LocalDate dia = desde;
        int faltan = Math.max(1, cantidad);
        for (int guarda = 0; faltan > 0 && guarda < 60; guarda++) {
            dia = dia.plusDays(1);
            if (esDiaDeAtencion(dia)) {
                faltan--;
            }
        }
        return dia;
    }

    public LocalTime leerHoraEnPunto(String texto) {
        LocalTime hora;
        try {
            hora = LocalTime.parse(texto.trim(), HORA);
        } catch (DateTimeParseException e) {
            throw new BusinessException("HORA_INVALIDA", "La hora debe tener el formato HH:mm, por ejemplo 11:00.");
        }
        if (hora.getMinute() != 0) {
            throw new BusinessException("HORA_INVALIDA", "Los turnos empiezan en hora en punto, por ejemplo 11:00.");
        }
        return hora;
    }

    /** Un bloque de {@code horas} turnos seguidos, solo para hoy y dentro del horario. */
    public void validarBloque(LocalDateTime ahora, LocalTime horaInicio, int horas) {
        LocalDate hoy = ahora.toLocalDate();
        if (!esDiaDeAtencion(hoy)) {
            throw new BusinessException("DIA_SIN_ATENCION", "Hoy no hay atención de cubículos.");
        }
        if (horaInicio.isBefore(p.horaApertura()) || !horaInicio.isBefore(p.horaCierre())) {
            throw new BusinessException("FUERA_DE_HORARIO", "Los turnos van de "
                    + Formato.hhmm(p.horaApertura()) + " a " + Formato.hhmm(p.horaCierre()) + ".");
        }
        LocalDateTime inicio = hoy.atTime(horaInicio);
        LocalDateTime fin = inicio.plusHours(horas);
        if (fin.isAfter(hoy.atTime(p.horaCierre()))) {
            throw new BusinessException("BLOQUE_PASA_DEL_CIERRE", "Un bloque de " + horas
                    + (horas == 1 ? " hora" : " horas") + " desde las " + Formato.hhmm(horaInicio)
                    + " termina después de las " + Formato.hhmm(p.horaCierre()) + ".");
        }
        if (!inicio.plusHours(1).isAfter(ahora)) {
            throw new BusinessException("TURNO_PASADO", "Ese turno ya terminó. Elige el turno actual o uno más tarde.");
        }
    }
}
