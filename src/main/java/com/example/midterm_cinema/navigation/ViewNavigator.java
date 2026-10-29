package com.example.midterm_cinema.navigation;

import com.example.midterm_cinema.controller.MovieDetailsController;
import com.example.midterm_cinema.controller.SeatSelectionController;
import com.example.midterm_cinema.model.Movie;
import com.example.midterm_cinema.model.Screening;
import com.example.midterm_cinema.model.User;
import com.example.midterm_cinema.model.enums.UserRole;
import com.example.midterm_cinema.session.UserSession;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/** Loads views into the main window's center. */
public final class ViewNavigator {
    private static StackPane container;
    private static Runnable refreshHeader;

    private ViewNavigator() {}

    public static void initialize(StackPane target, Runnable refresh) {
        container = target;
        refreshHeader = refresh;
    }

    private static FXMLLoader showView(String name) {
        try {
            FXMLLoader loader = createLoader(name);
            Parent view = loader.load();
            container.getChildren().setAll(view);
            refreshHeader.run();
            return loader;
        } catch (IOException error) {
            throw new IllegalStateException("Could not open " + name, error);
        }
    }

    private static FXMLLoader createLoader(String name) {
        return new FXMLLoader(
                ViewNavigator.class.getResource(
                        "/com/example/midterm_cinema/fxml/" + name + ".fxml"));
    }

    public static void browse() {
        showView("movie-browser");
    }

    public static void movie(Movie movie) {
        FXMLLoader loader = showView("movie-details");
        MovieDetailsController controller = loader.getController();
        controller.load(movie);
    }

    public static void seats(Screening screening) {
        FXMLLoader loader = showView("seat-selection");
        SeatSelectionController controller = loader.getController();
        controller.load(screening);
    }

    public static void bookings() {
        if (ensureSignedIn()) {
            showView("my-bookings");
        }
    }

    public static void dashboard() {
        showView("dashboard");
    }

    public static void touchZone() {
        User user = UserSession.getUser();
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Administrator access required.");
        }
        showView("touch-zone");
    }

    public static boolean ensureSignedIn() {
        if (UserSession.getUser() == null) {
            signIn();
        }
        return UserSession.getUser() != null;
    }

    public static void signIn() {
        try {
            FXMLLoader loader = createLoader("login");
            Scene scene = new Scene(loader.load(), 440, 440);
            Stage dialog = new Stage();
            dialog.initOwner(container.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Sign in • Encore");
            dialog.setResizable(false);
            dialog.setScene(scene);

            // Let an in-progress login finish before the dialog can be dismissed.
            dialog.setOnCloseRequest(
                    event -> {
                        Button loginButton = (Button) scene.lookup("#loginButton");
                        if (loginButton.isDisabled()) {
                            event.consume();
                        }
                    });
            dialog.showAndWait();
            refreshHeader.run();
        } catch (IOException error) {
            throw new IllegalStateException("Could not open sign in.", error);
        }
    }
}
