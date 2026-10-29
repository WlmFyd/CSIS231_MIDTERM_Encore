package com.example.midterm_cinema.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Formats {
    private Formats() {}

    public static String money(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String date(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • HH:mm"));
    }
}
