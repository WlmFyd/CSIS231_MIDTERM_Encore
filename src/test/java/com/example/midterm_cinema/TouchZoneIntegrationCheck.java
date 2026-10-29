package com.example.midterm_cinema;

import com.example.midterm_cinema.config.HibernateUtil;
import com.example.midterm_cinema.model.*;
import com.example.midterm_cinema.model.enums.*;
import com.example.midterm_cinema.service.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Real database check; cleanup is restricted to the IDs created by this check. */
public class TouchZoneIntegrationCheck {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
        }
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Beirut"));
        AdminService administration = new AdminService();
        AuthService accounts = new AuthService();
        CinemaService cinema = new CinemaService();
        BookingService bookings = new BookingService();
        List<Long> userIds = new ArrayList<>();
        List<Long> movieIds = new ArrayList<>();
        User administrator = null;
        try {
            new DemoDataService().initialize();
            administrator = accounts.signIn("admin", "EncoreAdmin!26");
            User admin = administrator;
            String suffix = Long.toString(System.currentTimeMillis(), 36);
            int initialUsers = administration.findUsers(admin).size();
            int initialMovies = administration.findMovies(admin).size();
            User customer =
                    accounts.createUser(
                            admin, "tz_" + suffix, "TouchZoneCheck!26", UserRole.CUSTOMER);
            userIds.add(customer.getId());
            User extraAdmin =
                    accounts.createUser(
                            admin, "tza_" + suffix, "TouchZoneCheck!26", UserRole.ADMIN);
            userIds.add(extraAdmin.getId());
            require(administration.findUsers(admin).size() == initialUsers + 2, "User counter");
            require(
                    accounts.signIn(customer.getUsername(), "TouchZoneCheck!26")
                            .getId()
                            .equals(customer.getId()),
                    "Created user cannot log in");
            require(extraAdmin.getRole() == UserRole.ADMIN, "Admin-selected role not saved");
            rejects(
                    () ->
                            accounts.createUser(
                                    customer, "bad_" + suffix, "TouchZoneCheck!26", UserRole.ADMIN),
                    "Customer created an admin");
            rejects(
                    () ->
                            accounts.createUser(
                                    admin,
                                    customer.getUsername(),
                                    "TouchZoneCheck!26",
                                    UserRole.CUSTOMER),
                    "Duplicate user accepted");
            rejects(() -> administration.findUsers(customer), "Customer read user list");
            rejects(() -> administration.findMovies(null), "Guest read admin movie list");
            rejects(() -> administration.findTickets(customer), "Customer read all tickets");
            rejects(() -> administration.findRooms(customer), "Customer read admin room list");
            rejects(
                    () -> administration.deleteUser(admin, admin.getId()),
                    "Self deletion accepted");

            Room room = administration.findRooms(admin).get(0);
            LocalDateTime start =
                    LocalDateTime.now()
                            .plusWeeks(3)
                            .withHour(10)
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);
            Movie movie =
                    administration.createMovie(
                            admin,
                            "Touch check " + suffix,
                            "A test synopsis",
                            90,
                            "Test",
                            room.getId(),
                            start,
                            new BigDecimal("12.34"));
            movieIds.add(movie.getId());
            Movie next =
                    administration.createMovie(
                            admin,
                            "Next touch check " + suffix,
                            "Another test synopsis",
                            90,
                            "Test",
                            room.getId(),
                            start.plusHours(2),
                            new BigDecimal("10.00"));
            movieIds.add(next.getId());
            require(administration.findMovies(admin).size() == initialMovies + 2, "Movie counter");
            rejects(
                    () ->
                            administration.createMovie(
                                    customer,
                                    "Unauthorized",
                                    "Synopsis",
                                    90,
                                    "Test",
                                    null,
                                    null,
                                    null),
                    "Customer created movie");
            rejects(
                    () ->
                            administration.createMovie(
                                    admin,
                                    "Overlap",
                                    "Synopsis",
                                    90,
                                    "Test",
                                    room.getId(),
                                    start.plusMinutes(30),
                                    new BigDecimal("10.00")),
                    "Room overlap accepted");
            rejects(
                    () ->
                            administration.createMovie(
                                    admin,
                                    "Past",
                                    "Synopsis",
                                    90,
                                    "Test",
                                    room.getId(),
                                    LocalDateTime.now().minusDays(1),
                                    new BigDecimal("10.00")),
                    "Past screening accepted");
            rejects(
                    () ->
                            administration.createMovie(
                                    admin,
                                    "Bad price",
                                    "Synopsis",
                                    90,
                                    "Test",
                                    room.getId(),
                                    start.plusDays(1),
                                    new BigDecimal("1.001")),
                    "Fractional-cent base price accepted");
            require(
                    administration.findMovies(admin).size() == initialMovies + 2,
                    "Failed creation left a partial movie");

            administration.editMovie(
                    admin, movie.getId(), "Updated " + suffix, "Updated synopsis", 95, "Edited");
            Movie reloaded = null;
            for (Movie value : administration.findMovies(admin))
                if (value.getId().equals(movie.getId())) reloaded = value;
            require(
                    reloaded != null
                            && reloaded.getTitle().equals("Updated " + suffix)
                            && reloaded.getDurationMinutes() == 95,
                    "Movie edit did not persist");
            rejects(
                    () ->
                            administration.editMovie(
                                    admin,
                                    movie.getId(),
                                    "Invalid extension",
                                    "Synopsis",
                                    130,
                                    "Edited"),
                    "Edited duration overlaps next screening");
            for (Movie value : administration.findMovies(admin))
                if (value.getId().equals(movie.getId()))
                    require(value.getDurationMinutes() == 95, "Failed edit not rolled back");

            Screening screening = cinema.findUpcoming(movie.getId()).get(0);
            Seat normal = null;
            Seat vip = null;
            for (Seat value : cinema.findSeats(room.getId())) {
                if (normal == null && value.getType() == SeatType.NORMAL) normal = value;
                if (vip == null && value.getType() == SeatType.VIP) vip = value;
            }
            require(normal != null && vip != null, "Seat types missing");
            List<Long> seats = List.of(normal.getId(), vip.getId());
            rejects(
                    () ->
                            bookings.bookForUser(
                                    customer, customer.getId(), screening.getId(), seats),
                    "Customer assigned admin ticket");
            Booking booking =
                    bookings.bookForUser(admin, customer.getId(), screening.getId(), seats);
            require(
                    booking.getTotalPrice().compareTo(new BigDecimal("30.85")) == 0,
                    "Admin assignment pricing");
            require(
                    bookings.findMyBookings(customer).size() == 1,
                    "Assigned ticket missing from customer history");
            rejects(
                    () -> administration.deleteUser(admin, customer.getId()),
                    "User with bookings deleted");
            rejects(
                    () -> administration.deleteMovie(admin, movie.getId()),
                    "Movie with bookings deleted");
            rejects(
                    () -> administration.deleteTicket(customer, booking.getSeats().get(0).getId()),
                    "Customer deleted admin ticket");

            BookingSeat normalTicket = null;
            BookingSeat vipTicket = null;
            for (BookingSeat ticket : administration.findTickets(admin)) {
                if (!ticket.getBooking().getUser().getId().equals(customer.getId())) continue;
                if (ticket.getSeat().getType() == SeatType.NORMAL) normalTicket = ticket;
                else vipTicket = ticket;
            }
            require(normalTicket != null && vipTicket != null, "Ticket table missing assignment");
            administration.deleteTicket(admin, normalTicket.getId());
            List<Booking> remaining = bookings.findMyBookings(customer);
            require(
                    remaining.size() == 1 && remaining.get(0).getSeats().size() == 1,
                    "Deleting one ticket removed other tickets");
            require(
                    remaining.get(0).getTotalPrice().compareTo(new BigDecimal("18.51")) == 0,
                    "Partial deletion total");
            require(
                    !bookings.reservedSeats(screening.getId()).contains(normal.getId()),
                    "Deleted normal seat remains reserved");
            for (MovieStatistics row : cinema.findMovieStatistics(admin))
                if (row.getTitle().equals("Updated " + suffix)) {
                    require(
                            row.getBookings() == 1
                                    && row.getTickets() == 1
                                    && row.getRevenue().compareTo(new BigDecimal("18.51")) == 0,
                            "Statistics after deleting one ticket");
                }
            administration.deleteTicket(admin, vipTicket.getId());
            require(
                    bookings.findMyBookings(customer).isEmpty(),
                    "Final ticket left an empty booking");
            require(
                    !bookings.reservedSeats(screening.getId()).contains(vip.getId()),
                    "Deleted VIP seat remains reserved");
            administration.deleteMovie(admin, movie.getId());
            movieIds.remove(movie.getId());
            require(
                    cinema.findUpcoming(movie.getId()).isEmpty(),
                    "Deleted movie's screenings retained");
            administration.deleteUser(admin, customer.getId());
            userIds.remove(customer.getId());
            require(
                    administration.findUsers(admin).size() == initialUsers + 1,
                    "User deletion counter");
            System.out.println(
                    "PASS: admin authorization, account creation/roles, counters, movie"
                        + " creation/edit/deletion, optional screening, overlap checks, ticket"
                        + " assignment, partial/final ticket deletion, freed seats, history,"
                        + " statistics, and protected deletions.");
        } finally {
            if (administrator != null) {
                for (Long userId : userIds) {
                    for (BookingSeat ticket : administration.findTickets(administrator)) {
                        if (ticket.getBooking().getUser().getId().equals(userId))
                            administration.deleteTicket(administrator, ticket.getId());
                    }
                    administration.deleteUser(administrator, userId);
                }
                for (Long movieId : movieIds) administration.deleteMovie(administrator, movieId);
                System.out.println("CLEANUP_OK: only temporary Touch Zone records removed.");
            }
            HibernateUtil.shutdown();
        }
    }
}
