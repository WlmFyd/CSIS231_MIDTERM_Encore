package com.example.midterm_cinema.service;

import com.example.midterm_cinema.config.HibernateUtil;
import com.example.midterm_cinema.model.Booking;
import com.example.midterm_cinema.model.Movie;
import com.example.midterm_cinema.model.MovieStatistics;
import com.example.midterm_cinema.model.Screening;
import com.example.midterm_cinema.model.Seat;
import com.example.midterm_cinema.model.User;
import com.example.midterm_cinema.model.enums.UserRole;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Movie browsing, screening lookup, and the small administrative report. */
public class CinemaService {
    public List<Movie> findAllMovies() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            List<Movie> movies =
                    session.createQuery("from Movie order by title", Movie.class).getResultList();
            transaction.commit();
            return movies;
        }
    }

    public List<Movie> filterMovies(List<Movie> movies, String search, String genre) {
        String query = "";
        if (search != null) {
            query = search.trim().toLowerCase(Locale.ROOT);
        }

        List<Movie> results = new ArrayList<>();
        for (Movie movie : movies) {
            boolean titleMatches = movie.getTitle().toLowerCase(Locale.ROOT).contains(query);
            boolean genreMatches =
                    genre == null || genre.equals("All genres") || genre.equals(movie.getGenre());
            if (titleMatches && genreMatches) {
                results.add(movie);
            }
        }
        return results;
    }

    public List<Screening> findUpcoming(Long movieId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            String hql =
                    "from Screening s join fetch s.movie join fetch s.room "
                            + "where s.movie.id = :movie and s.startsAt > :now order by s.startsAt";
            List<Screening> screenings =
                    session.createQuery(hql, Screening.class)
                            .setParameter("movie", movieId)
                            .setParameter("now", LocalDateTime.now())
                            .getResultList();
            transaction.commit();
            return screenings;
        }
    }

    public List<Seat> findSeats(Long roomId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            String hql =
                    "from Seat s join fetch s.room where s.room.id = :room "
                            + "order by s.rowLabel, s.seatNumber";
            List<Seat> seats =
                    session.createQuery(hql, Seat.class)
                            .setParameter("room", roomId)
                            .getResultList();
            transaction.commit();
            return seats;
        }
    }

    public List<MovieStatistics> findMovieStatistics(User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Administrator access required.");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            User account = session.get(User.class, currentUser.getId());
            if (account == null || account.getRole() != UserRole.ADMIN) {
                transaction.rollback();
                throw new IllegalArgumentException("Administrator access required.");
            }

            List<Movie> movies =
                    session.createQuery("from Movie order by title", Movie.class).getResultList();
            // Fetch the tickets in the same query, before the session is closed.
            String hql =
                    "select distinct b from Booking b "
                            + "join fetch b.screening s join fetch s.movie "
                            + "join fetch b.seats";
            List<Booking> bookings = session.createQuery(hql, Booking.class).getResultList();

            List<MovieStatistics> results = new ArrayList<>();
            for (Movie movie : movies) {
                MovieStatistics row = new MovieStatistics(movie.getTitle());
                for (Booking booking : bookings) {
                    Long bookedMovieId = booking.getScreening().getMovie().getId();
                    if (movie.getId().equals(bookedMovieId)) {
                        row.addBooking(booking);
                    }
                }
                results.add(row);
            }

            transaction.commit();
            return results;
        }
    }
}
