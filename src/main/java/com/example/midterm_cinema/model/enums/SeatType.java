package com.example.midterm_cinema.model.enums;

import java.math.BigDecimal;

public enum SeatType {
    NORMAL("1.00"),
    VIP("1.50");
    private final BigDecimal multiplier;

    SeatType(String multiplier) {
        this.multiplier = new BigDecimal(multiplier);
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }
}
