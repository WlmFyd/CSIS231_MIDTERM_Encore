package com.example.midterm_cinema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "booking_seats",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_screening_seat",
                        columnNames = {"screening_id", "seat_id"}))
public class BookingSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    protected BookingSeat() {}

    BookingSeat(Booking booking, Screening screening, Seat seat, BigDecimal price) {
        this.booking = booking;
        this.screening = screening;
        this.seat = seat;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public Screening getScreening() {
        return screening;
    }

    public Seat getSeat() {
        return seat;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
