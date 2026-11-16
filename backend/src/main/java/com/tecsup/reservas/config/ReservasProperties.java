package com.tecsup.reservas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/** Reglas del negocio y secretos, leídos de application*.yml bajo el prefijo "reservas". */
@ConfigurationProperties(prefix = "reservas")
public record ReservasProperties(
        String zonaHoraria,
        String apertura,
        String cierre,
        List<DayOfWeek> diasAtencion,
        int cuotaHorasDia,
        int minutosCheckin,
        int minutosAvisoFin,
        int minutosCancelacionGratis,
        int ausenciasParaPausa,
        int diasPausa,
        int cubiculos,
        int segundosSinSenal,
        List<String> corsOrigenes,
        String sensorToken,
        String jwtSecreto,
        int jwtHoras,
        Demo demo) {

    public record Demo(boolean habilitado, String horaInicio, String claveAdmin) {
        public LocalTime hora() {
            return LocalTime.parse(horaInicio);
        }
    }

    public LocalTime horaApertura() {
        return LocalTime.parse(apertura);
    }

    public LocalTime horaCierre() {
        return LocalTime.parse(cierre);
    }

    public ZoneId zona() {
        return ZoneId.of(zonaHoraria);
    }
}
