package com.tecsup.reservas.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** Formatos de fecha y hora que usa la API (hora de Lima, sin zona). */
public final class Formato {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Locale ES = Locale.forLanguageTag("es-PE");

    private Formato() {
    }

    public static String hhmm(LocalDateTime t) {
        return t == null ? null : t.format(HHMM);
    }

    public static String hhmm(LocalTime t) {
        return t == null ? null : t.format(HHMM);
    }

    public static String iso(LocalDateTime t) {
        return t == null ? null : t.format(ISO);
    }

    public static String dia(LocalDate d) {
        return d.getDayOfWeek().getDisplayName(TextStyle.FULL, ES);
    }

    public static String fechaLarga(LocalDate d) {
        return dia(d) + " " + d.getDayOfMonth() + " de " + d.getMonth().getDisplayName(TextStyle.FULL, ES);
    }
}
