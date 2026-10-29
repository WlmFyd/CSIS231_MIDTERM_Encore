package com.example.midterm_cinema.controller;

import com.example.midterm_cinema.model.Movie;
import com.example.midterm_cinema.model.Screening;
import com.example.midterm_cinema.navigation.ViewNavigator;
import com.example.midterm_cinema.service.CinemaService;
import com.example.midterm_cinema.util.FxTasks;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.util.List;

public class MovieDetailsController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label metadataLabel;
    @FXML
    private Label synopsisLabel;
    @FXML
    private ComboBox<Screening> screeningChoice;
    @FXML
    private Label statusLabel;
    @FXML
    private Button chooseSeatsButton;

    public void load(Movie movie) {
        titleLabel.setText(movie.getTitle());
        metadataLabel.setText(movie.getGenre() + "  •  " + movie.getDurationMinutes() + " min");
        synopsisLabel.setText(movie.getSynopsis());
        statusLabel.setText("Loading available screenings...");
        chooseSeatsButton.setDisable(true);

        Task<List<Screening>> task =
                new Task<>() {
                    @Override
                    protected List<Screening> call() {
                        return new CinemaService().findUpcoming(movie.getId());
                    }
                };
        task.setOnSucceeded(
                event -> {
                    List<Screening> screenings = task.getValue();
                    screeningChoice.getItems().setAll(screenings);
                    screeningChoice.getSelectionModel().selectFirst();
                    chooseSeatsButton.setDisable(screenings.isEmpty());
                    if (screenings.isEmpty()) {
                        statusLabel.setText("No upcoming screenings are scheduled for this movie.");
                    } else {
                        statusLabel.setText(
                                "All screening times are in Beirut time. Sign in is required to"
                                        + " book.");
                    }
                });
        task.setOnFailed(event -> statusLabel.setText(FxTasks.message(task.getException())));
        FxTasks.start(task);
    }

    @FXML
    private void back() {
        ViewNavigator.browse();
    }

    @FXML
    private void chooseSeats() {
        Screening screening = screeningChoice.getValue();
        if (screening != null && ViewNavigator.ensureSignedIn()) {
            ViewNavigator.seats(screening);
        }
    }
}
