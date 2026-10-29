package com.example.midterm_cinema.service;

import com.example.midterm_cinema.config.HibernateUtil;
import com.example.midterm_cinema.model.User;
import com.example.midterm_cinema.model.enums.UserRole;
import com.example.midterm_cinema.util.PasswordUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Locale;

public class AuthService {
    public User signIn(String username, String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Enter your password.");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            User user = findUser(session, normalizeUsername(username));
            transaction.commit();

            if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
                throw new IllegalArgumentException("Username or password is incorrect.");
            }
            return user;
        }
    }

    public User register(String username, String password) {
        return createAccount(null, username, password, UserRole.CUSTOMER, false);
    }

    public User createUser(User administrator, String username, String password, UserRole role) {
        return createAccount(administrator, username, password, role, true);
    }

    private User createAccount(
            User administrator,
            String username,
            String password,
            UserRole role,
            boolean adminAction) {
        if (role == null) {
            throw new IllegalArgumentException("Choose a user role.");
        }
        String normalizedUsername = normalizeUsername(username);
        if (!normalizedUsername.matches("[a-z0-9_]{3,30}")) {
            throw new IllegalArgumentException(
                    "Use 3–30 letters, numbers, or underscores for your username.");
        }
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new IllegalArgumentException("Use a password of 8–128 characters.");
        }

        String passwordHash = PasswordUtil.hash(password);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                if (adminAction) {
                    User account =
                            administrator == null
                                    ? null
                                    : session.get(User.class, administrator.getId());
                    if (account == null || account.getRole() != UserRole.ADMIN) {
                        throw new IllegalArgumentException("Administrator access required.");
                    }
                }
                if (findUser(session, normalizedUsername) != null) {
                    throw new IllegalArgumentException("That username is already taken.");
                }

                User user = new User(normalizedUsername, passwordHash, role);
                session.persist(user);
                transaction.commit();
                return user;
            } catch (RuntimeException error) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                if (error instanceof ConstraintViolationException) {
                    throw new IllegalArgumentException("That username is already taken.");
                }
                throw error;
            }
        }
    }

    private User findUser(Session session, String username) {
        return session.createQuery("from User where username = :username", User.class)
                .setParameter("username", username)
                .uniqueResult();
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
