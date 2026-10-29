package com.example.midterm_cinema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> seats = new ArrayList<>();

    protected Booking() {}

    public Booking(User user, Screening screening) {
        this.user = user;
        this.screening = screening;
        this.bookedAt = LocalDateTime.now();
        this.reference = UUID.randomUUID().toString();
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public User getUser() {
        return user;
    }

    public Screening getScreening() {
        return screening;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public List<BookingSeat> getSeats() {
        return new ArrayList<>(seats);
    }

    public void addSeat(Seat seat, BigDecimal price) {
        seats.add(new BookingSeat(this, screening, seat, price));
    }

    public void removeSeat(BookingSeat ticket) {
        seats.remove(ticket);
    }

    public String getMovieTitle() {
        return screening.getMovie().getTitle();
    }

    public BigDecimal getTotalPrice() {
        BigDecimal total = BigDecimal.ZERO;
        for (BookingSeat ticket : seats) {
            total = total.add(ticket.getPrice());
        }
        return total;
    }

    public String getSeatLabels() {
        List<String> labels = new ArrayList<>();
        for (BookingSeat ticket : seats) {
            labels.add(ticket.getSeat().getLabel());
        }
        return String.join(", ", labels);
    }
}
