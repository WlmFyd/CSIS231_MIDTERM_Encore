package com.example.midterm_cinema.controller;

import com.example.midterm_cinema.model.Movie;
import com.example.midterm_cinema.navigation.ViewNavigator;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Locale;

/** Fills one reusable FXML card with a movie from the database. */
public class MovieCardController {
    @FXML
    private VBox poster;
    @FXML
    private Label genreLabel;
    @FXML
    private Label posterTitle;
    @FXML
    private Label titleLabel;
    @FXML
    private Label metadataLabel;

    private Movie movie;

    public void setMovie(Movie movie) {
        this.movie = movie;
        genreLabel.setText(movie.getGenre().toUpperCase(Locale.ROOT));
        posterTitle.setText(movie.getTitle());
        titleLabel.setText(movie.getTitle());
        metadataLabel.setText(movie.getGenre() + "  •  " + movie.getDurationMinutes() + " min");
        poster.getStyleClass().remove("poster-0");
        int color = (int) (movie.getId() % 6);
        poster.getStyleClass().add("poster-" + color);
    }

    @FXML
    private void showScreenings() {
        if (movie != null) {
            ViewNavigator.movie(movie);
        }
    }
}
