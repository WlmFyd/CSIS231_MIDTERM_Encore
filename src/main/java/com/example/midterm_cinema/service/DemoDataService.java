package com.example.midterm_cinema.service;

import com.example.midterm_cinema.config.HibernateUtil;
import com.example.midterm_cinema.model.*;
import com.example.midterm_cinema.model.enums.*;
import com.example.midterm_cinema.util.PasswordUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Inserts starter content only where it is missing; existing records are retained. */
public class DemoDataService {
    public void initialize() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                // Once rooms are set up, preserve intentional admin edits/deletions.
                long roomCount = session.createQuery("select count(r) from Room r", Long.class).getSingleResult();
                if (roomCount > 0) {
                    transaction.commit();
                    return;
                }
                if (findUser(session, "admin") == null)
                    session.persist(
                            new User("admin", PasswordUtil.hash("EncoreAdmin!26"), UserRole.ADMIN));
                if (findUser(session, "demo") == null)
                    session.persist(
                            new User(
                                    "demo", PasswordUtil.hash("EncoreDemo!26"), UserRole.CUSTOMER));
                if (session.createQuery("select count(m) from Movie m", Long.class)
                                .getSingleResult()
                        == 0) {
                    session.persist(
                            new Movie(
                                    "Beyond the Blue",
                                    "A marine biologist follows a mysterious signal into an"
                                            + " unexplored ocean trench, where an extraordinary"
                                            + " discovery challenges everything she knows.",
                                    118,
                                    "Adventure",
                                    null));
                    session.persist(
                            new Movie(
                                    "Midnight in Motion",
                                    "An overnight train connects five strangers. As the city"
                                        + " disappears behind them, a missing passenger turns their"
                                        + " quiet journey into a race against time.",
                                    106,
                                    "Thriller",
                                    null));
                    session.persist(
                            new Movie(
                                    "The Last Broadcast",
                                    "When a forgotten radio station starts receiving messages from"
                                        + " tomorrow, its only presenter must decide which future"
                                        + " is worth saving.",
                                    124,
                                    "Sci-Fi",
                                    null));
                    session.persist(
                            new Movie(
                                    "A Little More Time",
                                    "Two childhood friends reunite to restore their neighborhood"
                                        + " cinema, discovering that some stories deserve a second"
                                        + " showing.",
                                    98,
                                    "Romance",
                                    null));
                    session.persist(
                            new Movie(
                                    "Paper Planets",
                                    "A young inventor's paper creations spring to life, carrying"
                                        + " her family on an unexpected adventure through worlds"
                                        + " built from imagination.",
                                    92,
                                    "Animation",
                                    null));
                    session.persist(
                            new Movie(
                                    "The Quiet Hours",
                                    "A chef returns home to care for his father and discovers a box"
                                        + " of unfinished letters that changes his understanding of"
                                        + " their family.",
                                    112,
                                    "Drama",
                                    null));
                }
                List<Room> rooms =
                        session.createQuery("from Room order by id", Room.class).getResultList();
                if (rooms.isEmpty()) {
                    for (int number = 1; number <= 2; number++) {
                        Room room = new Room("Screen " + number);
                        session.persist(room);
                        rooms.add(room);
                        for (int row = 0; row < 6; row++)
                            for (int seat = 1; seat <= 10; seat++) {
                                session.persist(
                                        new Seat(
                                                room,
                                                String.valueOf((char) ('A' + row)),
                                                seat,
                                                row >= 4 ? SeatType.VIP : SeatType.NORMAL));
                            }
                    }
                }
                if (session.createQuery("select count(s) from Screening s", Long.class)
                                .getSingleResult()
                        == 0) {
                    List<Movie> movies =
                            session.createQuery("from Movie order by id", Movie.class)
                                    .getResultList();
                    // Tomorrow onward, in the application's Beirut timezone. Three hours per slot
                    // avoids overlap.
                    for (int day = 1; day <= 3; day++)
                        for (int index = 0; index < movies.size(); index++) {
                            int roomIndex = index % rooms.size();
                            int slot = index / rooms.size();
                            session.persist(
                                    new Screening(
                                            movies.get(index),
                                            rooms.get(roomIndex),
                                            LocalDate.now()
                                                    .plusDays(day)
                                                    .atTime(14, 0)
                                                    .plusHours(slot * 3L),
                                            new BigDecimal("10.00")));
                        }
                }
                transaction.commit();
            } catch (RuntimeException error) {
                if (transaction.isActive()) transaction.rollback();
                throw error;
            }
        }
    }

    private User findUser(Session session, String username) {
        return session.createQuery("from User where username = :username", User.class)
                .setParameter("username", username)
                .uniqueResult();
    }
}
