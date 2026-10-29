package com.example.midterm_cinema.config;

import com.example.midterm_cinema.model.*;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Owns the single, application-wide SessionFactory. Sessions remain short-lived. */
public final class HibernateUtil {
    private static SessionFactory sessionFactory;

    private HibernateUtil() {}

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            Properties credentials = new Properties();
            Path path = Path.of("config", "database.properties");
            try (InputStream input = Files.newInputStream(path)) {
                credentials.load(input);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Create config/database.properties using the example file, then enter your"
                                + " database credentials.",
                        e);
            }
            for (String key :
                    new String[] {
                        "hibernate.connection.url",
                        "hibernate.connection.username",
                        "hibernate.connection.password"
                    }) {
                if (credentials.getProperty(key) == null
                        || credentials.getProperty(key).isBlank()) {
                    throw new IllegalStateException("Missing database setting: " + key);
                }
            }
            Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
            configuration.addProperties(credentials);
            configuration.addAnnotatedClass(Movie.class);
            configuration.addAnnotatedClass(User.class);
            configuration.addAnnotatedClass(Room.class);
            configuration.addAnnotatedClass(Seat.class);
            configuration.addAnnotatedClass(Screening.class);
            configuration.addAnnotatedClass(Booking.class);
            configuration.addAnnotatedClass(BookingSeat.class);
            sessionFactory = configuration.buildSessionFactory();
        }
        return sessionFactory;
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}
