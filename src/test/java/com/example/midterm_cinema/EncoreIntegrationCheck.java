package com.example.midterm_cinema;

import com.example.midterm_cinema.config.HibernateUtil;
import com.example.midterm_cinema.model.*;
import com.example.midterm_cinema.model.enums.*;
import com.example.midterm_cinema.service.*;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Real PostgreSQL integration check. Deletes only the temporary account and its bookings. */
public class EncoreIntegrationCheck {
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void rejects(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
        }
    }

    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Beirut"));
        User temporaryUser = null;
        try {
            new DemoDataService().initialize();
            AuthService auth = new AuthService();
            BookingService service = new BookingService();
            List<Movie> movies = new CinemaService().findAllMovies();
            require(movies.size() >= 6, "Expected demo movies");
            require(
                    new CinemaService().filterMovies(movies, "  beyond  ", "All genres").size()
                            == 1,
                    "Search filtering");
            for (Movie result : new CinemaService().filterMovies(movies, "", "Animation")) {
                require(result.getGenre().equals("Animation"), "Genre filtering");
            }
            User admin = auth.signIn("admin", "EncoreAdmin!26");
            require(admin.getRole() == UserRole.ADMIN, "Admin login");
            rejects(() -> auth.signIn("admin", "wrong-password"), "Wrong password accepted");
            String username = "qa_" + Long.toString(System.currentTimeMillis(), 36);
            temporaryUser = auth.register(username, "CheckPassword!26");
            User user = temporaryUser;
            require(user.getRole() == UserRole.CUSTOMER, "Registration role");
            require(
                    !user.getPasswordHash().contains("CheckPassword"),
                    "Password stored in plaintext");
            require(
                    auth.signIn(username.toUpperCase(Locale.ROOT), "CheckPassword!26")
                            .getId()
                            .equals(user.getId()),
                    "Username normalization");
            rejects(
                    () -> auth.register(username, "CheckPassword!26"),
                    "Duplicate username accepted");
            rejects(
                    () -> new CinemaService().findMovieStatistics(user),
                    "Customer accessed dashboard");
            List<MovieStatistics> before = new CinemaService().findMovieStatistics(admin);
            CinemaService screeningRepo = new CinemaService();
            Screening screening = firstScreening(movies, screeningRepo);
            List<Seat> seats = screeningRepo.findSeats(screening.getRoom().getId());
            Set<Long> reserved = service.reservedSeats(screening.getId());
            Seat normal = availableSeat(seats, SeatType.NORMAL, reserved, null);
            Seat vip = availableSeat(seats, SeatType.VIP, reserved, null);
            Seat concurrentSeat = availableSeat(seats, SeatType.NORMAL, reserved, normal.getId());
            require(
                    service.price(new BigDecimal("9.99"), SeatType.VIP)
                            .equals(new BigDecimal("14.99")),
                    "Money rounding");
            rejects(
                    () -> service.book(null, screening.getId(), List.of(normal.getId())),
                    "Guest booking accepted");
            rejects(
                    () -> service.book(user, screening.getId(), List.of()),
                    "Empty booking accepted");
            rejects(
                    () ->
                            service.book(
                                    user,
                                    screening.getId(),
                                    List.of(normal.getId(), normal.getId())),
                    "Repeated seat accepted");
            Seat otherRoomSeat;
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                otherRoomSeat =
                        session.createQuery(
                                        "from Seat s join fetch s.room where s.room.id <> :room",
                                        Seat.class)
                                .setParameter("room", screening.getRoom().getId())
                                .setMaxResults(1)
                                .uniqueResult();
            }
            require(otherRoomSeat != null, "Second room missing");
            Seat wrongSeat = otherRoomSeat;
            rejects(
                    () -> service.book(user, screening.getId(), List.of(wrongSeat.getId())),
                    "Seat from another room accepted");
            Booking booking =
                    service.book(user, screening.getId(), List.of(normal.getId(), vip.getId()));
            BigDecimal expected =
                    service.price(screening.getBasePrice(), SeatType.NORMAL)
                            .add(service.price(screening.getBasePrice(), SeatType.VIP));
            require(booking.getTotalPrice().compareTo(expected) == 0, "Mixed seat total");
            require(service.findMyBookings(user).size() == 1, "Booking did not persist");
            rejects(
                    () -> service.book(user, screening.getId(), List.of(normal.getId())),
                    "Duplicate booking accepted");
            // Bypass the service once to prove PostgreSQL itself enforces uniqueness.
            boolean constrained = false;
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Transaction transaction = session.beginTransaction();
                try {
                    Booking duplicate =
                            new Booking(
                                    session.get(User.class, user.getId()),
                                    session.get(Screening.class, screening.getId()));
                    duplicate.addSeat(
                            session.get(Seat.class, normal.getId()), new BigDecimal("10.00"));
                    session.persist(duplicate);
                    transaction.commit();
                } catch (org.hibernate.exception.ConstraintViolationException expectedConstraint) {
                    if (transaction.isActive()) transaction.rollback();
                    constrained =
                            "uq_screening_seat".equals(expectedConstraint.getConstraintName());
                }
            }
            require(constrained, "Database uniqueness constraint missing");
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger successes = new AtomicInteger();
            AtomicInteger conflicts = new AtomicInteger();
            Callable<Void> contender =
                    () -> {
                        start.await();
                        try {
                            service.book(user, screening.getId(), List.of(concurrentSeat.getId()));
                            successes.incrementAndGet();
                        } catch (IllegalArgumentException conflict) {
                            conflicts.incrementAndGet();
                        }
                        return null;
                    };
            try {
                Future<Void> first = executor.submit(contender);
                Future<Void> second = executor.submit(contender);
                start.countDown();
                first.get(20, TimeUnit.SECONDS);
                second.get(20, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }
            require(
                    successes.get() == 1 && conflicts.get() == 1,
                    "Concurrent double booking was not prevented");
            List<MovieStatistics> after = new CinemaService().findMovieStatistics(admin);
            require(
                    totalBookings(after) == totalBookings(before) + 2,
                    "Booking count / orphan booking rollback");
            require(totalTickets(after) == totalTickets(before) + 3, "Ticket count");
            BigDecimal revenueIncrease =
                    expected.add(service.price(screening.getBasePrice(), SeatType.NORMAL));
            require(
                    totalRevenue(after).subtract(totalRevenue(before)).compareTo(revenueIncrease)
                            == 0,
                    "Revenue aggregation");
            require(
                    service.findMyBookings(user).size() == 2,
                    "Booking history contains wrong number of bookings");
            require(
                    service.reservedSeats(screening.getId())
                            .containsAll(
                                    List.of(normal.getId(), vip.getId(), concurrentSeat.getId())),
                    "Availability not persisted");
            require(after.size() == movies.size(), "Dashboard omits movies with zero sales");
            // The same physical seat remains available for a different screening.
            Screening another = anotherScreening(screening, screeningRepo);
            require(
                    !service.reservedSeats(another.getId()).contains(normal.getId()),
                    "Availability incorrectly attached to room seat instead of screening");
            System.out.println(
                    "PASS: search, roles, password hashing, pricing, validation, persistence, DB"
                        + " constraint, concurrent booking, screening-specific availability,"
                        + " history, and statistics.");
        } finally {
            if (temporaryUser != null) {
                try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                    Transaction transaction = session.beginTransaction();
                    for (Booking booking :
                            session.createQuery("from Booking where user.id = :user", Booking.class)
                                    .setParameter("user", temporaryUser.getId())
                                    .getResultList()) session.remove(booking);
                    session.remove(session.get(User.class, temporaryUser.getId()));
                    transaction.commit();
                    System.out.println("CLEANUP_OK: temporary account and test bookings removed.");
                }
            }
            HibernateUtil.shutdown();
        }
    }

    private static long totalBookings(List<MovieStatistics> rows) {
        long total = 0;
        for (MovieStatistics row : rows) {
            total += row.getBookings();
        }
        return total;
    }

    private static long totalTickets(List<MovieStatistics> rows) {
        long total = 0;
        for (MovieStatistics row : rows) {
            total += row.getTickets();
        }
        return total;
    }

    private static BigDecimal totalRevenue(List<MovieStatistics> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (MovieStatistics row : rows) {
            total = total.add(row.getRevenue());
        }
        return total;
    }

    private static Screening firstScreening(List<Movie> movies, CinemaService cinema) {
        for (Movie movie : movies) {
            List<Screening> screenings = cinema.findUpcoming(movie.getId());
            if (!screenings.isEmpty()) {
                return screenings.get(0);
            }
        }
        throw new AssertionError("No upcoming screening available for the check.");
    }

    private static Seat availableSeat(
            List<Seat> seats, SeatType type, Set<Long> reserved, Long excluded) {
        for (Seat seat : seats) {
            if (seat.getType() == type
                    && !reserved.contains(seat.getId())
                    && !seat.getId().equals(excluded)) {
                return seat;
            }
        }
        throw new AssertionError("No available seat for the check.");
    }

    private static Screening anotherScreening(Screening original, CinemaService cinema) {
        for (Screening screening : cinema.findUpcoming(original.getMovie().getId())) {
            if (!screening.getId().equals(original.getId())
                    && screening.getRoom().getId().equals(original.getRoom().getId())) {
                return screening;
            }
        }
        throw new AssertionError("A second screening is needed for the check.");
    }
}
