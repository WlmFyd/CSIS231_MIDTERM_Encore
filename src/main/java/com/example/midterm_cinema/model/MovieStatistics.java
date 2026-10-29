package com.example.midterm_cinema.model;

import java.math.BigDecimal;

public class MovieStatistics {
    private final String title;
    private long bookings;
    private long tickets;
    private BigDecimal revenue;

    public MovieStatistics(String title) {
        this.title = title;
        this.revenue = BigDecimal.ZERO;
    }

    public void addBooking(Booking booking) {
        bookings++;
        tickets += booking.getSeats().size();
        revenue = revenue.add(booking.getTotalPrice());
    }

    public String getTitle() {
        return title;
    }

    public long getBookings() {
        return bookings;
    }

    public long getTickets() {
        return tickets;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
