package com.example.midterm_cinema.controller;

import com.example.midterm_cinema.model.Movie;
import com.example.midterm_cinema.service.CinemaService;
import com.example.midterm_cinema.service.DemoDataService;
import com.example.midterm_cinema.util.FxTasks;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MovieBrowserController {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> genreFilter;
    @FXML
    private TilePane movieGrid;
    @FXML
    private Label statusLabel;
    @FXML
    private Button reloadButton;

    private final CinemaService cinema = new CinemaService();
    private List<Movie> allMovies = new ArrayList<>();

    @FXML
    private void initialize() {
        genreFilter.getItems().add("All genres");
        genreFilter.getSelectionModel().selectFirst();
        searchField.textProperty().addListener((observable, oldValue, newValue) -> renderMovies());
        genreFilter.setOnAction(event -> renderMovies());
        reload();
    }

    @FXML
    private void reload() {
        statusLabel.setText("Loading movies...");
        reloadButton.setDisable(true);
        movieGrid.getChildren().clear();

        // call() runs on a worker thread. The two event handlers run on the JavaFX thread.
        Task<List<Movie>> task =
                new Task<>() {
                    @Override
                    protected List<Movie> call() {
                        new DemoDataService().initialize();
                        return cinema.findAllMovies();
                    }
                };

        task.setOnSucceeded(
                event -> {
                    allMovies = task.getValue();
                    fillGenres();
                    renderMovies();
                    reloadButton.setDisable(false);
                });
        task.setOnFailed(
                event -> {
                    statusLabel.setText(FxTasks.message(task.getException()));
                    task.getException().printStackTrace();
                    reloadButton.setDisable(false);
                });
        FxTasks.start(task);
    }

    private void fillGenres() {
        String previousGenre = genreFilter.getValue();
        List<String> genres = new ArrayList<>();
        for (Movie movie : allMovies) {
            if (!genres.contains(movie.getGenre())) {
                genres.add(movie.getGenre());
            }
        }
        Collections.sort(genres);

        genreFilter.getItems().setAll("All genres");
        genreFilter.getItems().addAll(genres);
        if (genreFilter.getItems().contains(previousGenre)) {
            genreFilter.setValue(previousGenre);
        } else {
            genreFilter.getSelectionModel().selectFirst();
        }
    }

    private void renderMovies() {
        movieGrid.getChildren().clear();
        List<Movie> results =
                cinema.filterMovies(allMovies, searchField.getText(), genreFilter.getValue());
        for (Movie movie : results) {
            movieGrid.getChildren().add(createMovieCard(movie));
        }
        if (results.isEmpty()) {
            statusLabel.setText("No movies match your search.");
        } else {
            statusLabel.setText(results.size() + " movies • choose your next story");
        }
    }

    private Node createMovieCard(Movie movie) {
        try {
            String path = "/com/example/midterm_cinema/fxml/movie-card.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Node card = loader.load();
            MovieCardController controller = loader.getController();
            controller.setMovie(movie);
            return card;
        } catch (IOException error) {
            throw new IllegalStateException("Could not load a movie card.", error);
        }
    }
}
