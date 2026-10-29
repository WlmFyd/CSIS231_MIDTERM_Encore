package com.example.midterm_cinema.session;

import com.example.midterm_cinema.model.User;

public final class UserSession {
    private static User currentUser;

    private UserSession() {}

    public static User getUser() {
        return currentUser;
    }

    public static void signIn(User user) {
        currentUser = user;
    }

    public static void signOut() {
        currentUser = null;
    }
}
