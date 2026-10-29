package com.example.midterm_cinema.service;

import com.example.midterm_cinema.config.HibernateUtil;
import com.example.midterm_cinema.model.Booking;
import com.example.midterm_cinema.model.Screening;
import com.example.midterm_cinema.model.Seat;
import com.example.midterm_cinema.model.User;
import com.example.midterm_cinema.model.enums.SeatType;
import com.example.midterm_cinema.model.enums.UserRole;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Booking rules and the Hibernate operations needed for a complete reservation. */
public class BookingService {
    public BigDecimal price(BigDecimal basePrice, SeatType type) {
        return basePrice.multiply(type.getMultiplier()).setScale(2, RoundingMode.HALF_UP);
    }

    public Set<Long> reservedSeats(Long screeningId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            Set<Long> reserved = findReservedSeats(session, screeningId);
            transaction.commit();
            return reserved;
        }
    }

    public Booking book(User currentUser, Long screeningId, List<Long> seatIds) {
        return createBooking(
                currentUser,
                currentUser == null ? null : currentUser.getId(),
                screeningId,
                seatIds,
                false);
    }

    public Booking bookForUser(
            User administrator, Long customerId, Long screeningId, List<Long> seatIds) {
        return createBooking(administrator, customerId, screeningId, seatIds, true);
    }

    private Booking createBooking(
            User actor,
            Long customerId,
            Long screeningId,
            List<Long> seatIds,
            boolean adminAssignment) {
        validateSelection(actor, seatIds);
        if (customerId == null) {
            throw new IllegalArgumentException("Choose a user for this ticket.");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                User account = session.get(User.class, actor.getId());
                if (account == null || (adminAssignment && account.getRole() != UserRole.ADMIN)) {
                    throw new IllegalArgumentException("Administrator access required.");
                }
                User customer = session.get(User.class, customerId);
                if (customer == null) {
                    throw new IllegalArgumentException("Please sign in again.");
                }

                String screeningQuery =
                        "from Screening s join fetch s.room join fetch s.movie where s.id = :id";
                Screening screening =
                        session.createQuery(screeningQuery, Screening.class)
                                .setParameter("id", screeningId)
                                .uniqueResult();
                if (screening == null || !screening.getStartsAt().isAfter(LocalDateTime.now())) {
                    throw new IllegalArgumentException("This screening is no longer available.");
                }

                String seatsQuery =
                        "from Seat s join fetch s.room where s.id in :ids order by s.rowLabel,"
                                + " s.seatNumber";
                List<Seat> seats =
                        session.createQuery(seatsQuery, Seat.class)
                                .setParameter("ids", seatIds)
                                .getResultList();
                validateSeats(seats, seatIds.size(), screening);

                Set<Long> reserved = findReservedSeats(session, screeningId);
                for (Long seatId : seatIds) {
                    if (reserved.contains(seatId)) {
                        throw seatConflict();
                    }
                }

                Booking booking = new Booking(customer, screening);
                for (Seat seat : seats) {
                    BigDecimal ticketPrice = price(screening.getBasePrice(), seat.getType());
                    booking.addSeat(seat, ticketPrice);
                }

                // The booking and every ticket are saved by the same transaction.
                session.persist(booking);
                transaction.commit();
                return booking;
            } catch (RuntimeException error) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                if (isSeatConflict(error)) {
                    throw seatConflict();
                }
                throw error;
            }
        }
    }

    public List<Booking> findMyBookings(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Please sign in to view your bookings.");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            // These relationships are loaded now so the UI can use them after session.close().
            String hql =
                    "select distinct b from Booking b "
                            + "join fetch b.screening s join fetch s.movie join fetch s.room "
                            + "join fetch b.seats bs join fetch bs.seat "
                            + "where b.user.id = :user order by b.bookedAt desc";
            List<Booking> bookings =
                    session.createQuery(hql, Booking.class)
                            .setParameter("user", user.getId())
                            .getResultList();
            transaction.commit();
            return bookings;
        }
    }

    private void validateSelection(User user, List<Long> seatIds) {
        if (user == null) {
            throw new IllegalArgumentException("Sign in before booking.");
        }
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one seat.");
        }
        Set<Long> uniqueIds = new HashSet<>();
        for (Long id : seatIds) {
            if (id == null || !uniqueIds.add(id)) {
                throw new IllegalArgumentException("Invalid seat selection.");
            }
        }
    }

    private void validateSeats(List<Seat> seats, int expectedCount, Screening screening) {
        if (seats.size() != expectedCount) {
            throw new IllegalArgumentException("Some selected seats do not exist.");
        }
        for (Seat seat : seats) {
            if (!seat.getRoom().getId().equals(screening.getRoom().getId())) {
                throw new IllegalArgumentException(
                        "Some seats do not belong to this screening's room.");
            }
        }
    }

    private Set<Long> findReservedSeats(Session session, Long screeningId) {
        String hql =
                "select ticket.seat.id from BookingSeat ticket where ticket.screening.id ="
                        + " :screening";
        List<Long> ids =
                session.createQuery(hql, Long.class)
                        .setParameter("screening", screeningId)
                        .getResultList();
        return new HashSet<>(ids);
    }

    private boolean isSeatConflict(Throwable error) {
        // A second customer may have booked the seat after our availability check.
        while (error != null) {
            if (error instanceof ConstraintViolationException) {
                ConstraintViolationException constraint = (ConstraintViolationException) error;
                if ("uq_screening_seat".equals(constraint.getConstraintName())) {
                    return true;
                }
            }
            error = error.getCause();
        }
        return false;
    }

    private IllegalArgumentException seatConflict() {
        return new IllegalArgumentException(
                "A selected seat was just booked. Please choose another seat.");
    }
}
