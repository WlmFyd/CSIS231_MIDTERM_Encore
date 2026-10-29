package com.example.midterm_cinema.service;

import com.example.midterm_cinema.config.HibernateUtil;
import com.example.midterm_cinema.model.Booking;
import com.example.midterm_cinema.model.BookingSeat;
import com.example.midterm_cinema.model.Movie;
import com.example.midterm_cinema.model.Room;
import com.example.midterm_cinema.model.Screening;
import com.example.midterm_cinema.model.User;
import com.example.midterm_cinema.model.enums.UserRole;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Small administrative operations behind the Touch Zone screen. */
public class AdminService {
    public List<User> findUsers(User administrator) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            requireAdmin(session, administrator);
            List<User> users =
                    session.createQuery("from User order by username", User.class).getResultList();
            transaction.commit();
            return users;
        }
    }

    public List<Movie> findMovies(User administrator) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            requireAdmin(session, administrator);
            List<Movie> movies =
                    session.createQuery("from Movie order by title", Movie.class).getResultList();
            transaction.commit();
            return movies;
        }
    }

    public List<BookingSeat> findTickets(User administrator) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            requireAdmin(session, administrator);
            String hql =
                    "from BookingSeat ticket join fetch ticket.booking b join fetch b.user "
                            + "join fetch ticket.screening s join fetch s.movie join fetch s.room "
                            + "join fetch ticket.seat order by ticket.id desc";
            List<BookingSeat> tickets = session.createQuery(hql, BookingSeat.class).getResultList();
            transaction.commit();
            return tickets;
        }
    }

    public List<Room> findRooms(User administrator) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            requireAdmin(session, administrator);
            List<Room> rooms =
                    session.createQuery("from Room order by name", Room.class).getResultList();
            transaction.commit();
            return rooms;
        }
    }

    public Movie createMovie(
            User administrator,
            String title,
            String synopsis,
            int duration,
            String genre,
            Long roomId,
            LocalDateTime startsAt,
            BigDecimal basePrice) {
        Movie movie = new Movie(title, synopsis, duration, genre, null);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                requireAdmin(session, administrator);
                session.persist(movie);
                // Scheduling is optional, but lets a new movie be booked immediately.
                if (roomId != null || startsAt != null || basePrice != null) {
                    if (roomId == null
                            || startsAt == null
                            || !startsAt.isAfter(LocalDateTime.now())) {
                        throw new IllegalArgumentException(
                                "Choose a room and a future screening date/time.");
                    }
                    if (basePrice == null
                            || basePrice.signum() <= 0
                            || basePrice.scale() > 2
                            || basePrice.compareTo(new BigDecimal("99999999.99")) > 0) {
                        throw new IllegalArgumentException(
                                "Enter a positive ticket price with at most two decimal places.");
                    }
                    Room room = session.get(Room.class, roomId, LockMode.PESSIMISTIC_WRITE);
                    if (room == null)
                        throw new IllegalArgumentException("The selected room no longer exists.");
                    checkRoomAvailable(session, roomId, startsAt, duration, null);
                    session.persist(new Screening(movie, room, startsAt, basePrice));
                }
                transaction.commit();
                return movie;
            } catch (RuntimeException error) {
                if (transaction.isActive()) transaction.rollback();
                throw error;
            }
        }
    }

    public void editMovie(
            User administrator,
            Long movieId,
            String title,
            String synopsis,
            int duration,
            String genre) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                requireAdmin(session, administrator);
                Movie movie = session.get(Movie.class, movieId);
                if (movie == null)
                    throw new IllegalArgumentException("The selected movie no longer exists.");
                movie.updateDetails(title, synopsis, duration, genre);
                List<Screening> screenings =
                        session.createQuery(
                                        "from Screening s join fetch s.room where s.movie.id ="
                                            + " :movie and s.startsAt > :now order by s.room.id,"
                                            + " s.startsAt",
                                        Screening.class)
                                .setParameter("movie", movieId)
                                .setParameter("now", LocalDateTime.now())
                                .getResultList();
                for (Screening screening : screenings) {
                    session.lock(screening.getRoom(), LockMode.PESSIMISTIC_WRITE);
                    checkRoomAvailable(
                            session,
                            screening.getRoom().getId(),
                            screening.getStartsAt(),
                            duration,
                            screening.getId());
                }
                transaction.commit();
            } catch (RuntimeException error) {
                if (transaction.isActive()) transaction.rollback();
                throw error;
            }
        }
    }

    public void deleteUser(User administrator, Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                User actor = requireAdmin(session, administrator);
                if (actor.getId().equals(userId))
                    throw new IllegalArgumentException("You cannot delete your signed-in account.");
                User target = session.get(User.class, userId);
                if (target == null)
                    throw new IllegalArgumentException("The selected user no longer exists.");
                long bookings =
                        session.createQuery(
                                        "select count(b) from Booking b where b.user.id = :user",
                                        Long.class)
                                .setParameter("user", userId)
                                .getSingleResult();
                if (bookings > 0)
                    throw new IllegalArgumentException(
                            "This user has bookings. Remove their tickets before deleting the"
                                + " account.");
                session.remove(target);
                transaction.commit();
            } catch (RuntimeException error) {
                if (transaction.isActive()) transaction.rollback();
                if (error instanceof ConstraintViolationException)
                    throw new IllegalArgumentException(
                            "This user is referenced by a booking and cannot be deleted.");
                throw error;
            }
        }
    }

    public void deleteMovie(User administrator, Long movieId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                requireAdmin(session, administrator);
                Movie movie = session.get(Movie.class, movieId);
                if (movie == null)
                    throw new IllegalArgumentException("The selected movie no longer exists.");
                long bookings =
                        session.createQuery(
                                        "select count(b) from Booking b where b.screening.movie.id"
                                            + " = :movie",
                                        Long.class)
                                .setParameter("movie", movieId)
                                .getSingleResult();
                if (bookings > 0)
                    throw new IllegalArgumentException(
                            "This movie has bookings. Remove its tickets before deleting the"
                                + " movie.");
                List<Screening> screenings =
                        session.createQuery(
                                        "from Screening where movie.id = :movie", Screening.class)
                                .setParameter("movie", movieId)
                                .getResultList();
                for (Screening screening : screenings) session.remove(screening);
                session.remove(movie);
                transaction.commit();
            } catch (RuntimeException error) {
                if (transaction.isActive()) transaction.rollback();
                if (error instanceof ConstraintViolationException)
                    throw new IllegalArgumentException(
                            "This movie is referenced by a booking and cannot be deleted.");
                throw error;
            }
        }
    }

    public void deleteTicket(User administrator, Long ticketId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                requireAdmin(session, administrator);
                BookingSeat ticket = session.get(BookingSeat.class, ticketId);
                if (ticket == null)
                    throw new IllegalArgumentException("The selected ticket no longer exists.");
                Booking booking = ticket.getBooking();
                session.lock(booking, LockMode.PESSIMISTIC_WRITE);
                if (booking.getSeats().size() == 1) {
                    session.remove(booking); // Cascade removes the final ticket as well.
                } else {
                    booking.removeSeat(ticket); // orphanRemoval deletes this ticket only.
                }
                transaction.commit();
            } catch (RuntimeException error) {
                if (transaction.isActive()) transaction.rollback();
                throw error;
            }
        }
    }

    private void checkRoomAvailable(
            Session session, Long roomId, LocalDateTime start, int duration, Long ignoredId) {
        List<Screening> screenings =
                session.createQuery(
                                "from Screening s join fetch s.movie where s.room.id = :room and"
                                    + " s.startsAt < :end",
                                Screening.class)
                        .setParameter("room", roomId)
                        .setParameter("end", start.plusMinutes(duration))
                        .getResultList();
        for (Screening other : screenings) {
            if (ignoredId != null && ignoredId.equals(other.getId())) continue;
            LocalDateTime otherEnd =
                    other.getStartsAt().plusMinutes(other.getMovie().getDurationMinutes());
            if (otherEnd.isAfter(start))
                throw new IllegalArgumentException(
                        "This screening would overlap another screening in that room.");
        }
    }

    private User requireAdmin(Session session, User administrator) {
        User account =
                administrator == null ? null : session.get(User.class, administrator.getId());
        if (account == null || account.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Administrator access required.");
        }
        return account;
    }
}
