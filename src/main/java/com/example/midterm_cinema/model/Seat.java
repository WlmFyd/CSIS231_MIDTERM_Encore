package com.example.midterm_cinema.model;

import com.example.midterm_cinema.model.enums.SeatType;

import jakarta.persistence.*;

@Entity
@Table(
        name = "seats",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_room_seat",
                        columnNames = {"room_id", "row_label", "seat_number"}))
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "row_label", nullable = false, length = 3)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatType type;

    protected Seat() {}

    public Seat(Room room, String rowLabel, int seatNumber, SeatType type) {
        this.room = room;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public SeatType getType() {
        return type;
    }

    public String getLabel() {
        return rowLabel + seatNumber;
    }
}
