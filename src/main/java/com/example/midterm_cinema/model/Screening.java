package com.example.midterm_cinema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "screenings")
public class Screening {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    protected Screening() {}

    public Screening(Movie movie, Room room, LocalDateTime startsAt, BigDecimal basePrice) {
        if (basePrice == null || basePrice.signum() <= 0)
            throw new IllegalArgumentException("Ticket price must be positive.");
        this.movie = movie;
        this.room = room;
        this.startsAt = startsAt;
        this.basePrice = basePrice;
    }

    public Long getId() {
        return id;
    }

    public Movie getMovie() {
        return movie;
    }

    public Room getRoom() {
        return room;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    @Override
    public String toString() {
        return startsAt.format(DateTimeFormatter.ofPattern("EEE, dd MMM • HH:mm"))
                + "  |  "
                + room.getName()
                + "  |  $"
                + basePrice;
    }
}
