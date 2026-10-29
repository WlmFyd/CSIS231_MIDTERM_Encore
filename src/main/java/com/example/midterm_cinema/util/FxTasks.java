package com.example.midterm_cinema.util;

import javafx.concurrent.Task;

/** Small helper: run a JavaFX Task on a worker thread and format an error message. */
public final class FxTasks {
    private FxTasks() {}

    public static void start(Task<?> task) {
        Thread thread = new Thread(task, "encore-worker");
        thread.setDaemon(true);
        thread.start();
    }

    public static String message(Throwable error) {
        if (error instanceof IllegalArgumentException || error instanceof IllegalStateException) {
            return error.getMessage();
        }
        return "Could not complete this request. Check your database connection and try again.";
    }
}
