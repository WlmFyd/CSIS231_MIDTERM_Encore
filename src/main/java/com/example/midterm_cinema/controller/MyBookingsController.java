package com.example.midterm_cinema.controller;

import com.example.midterm_cinema.model.Booking;
import com.example.midterm_cinema.service.BookingService;
import com.example.midterm_cinema.session.UserSession;
import com.example.midterm_cinema.util.Formats;
import com.example.midterm_cinema.util.FxTasks;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.util.List;

public class MyBookingsController {
    @FXML
    private ListView<Booking> bookingsList;
    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        bookingsList.setPlaceholder(
                new Label("Your next movie night is waiting. Browse movies to make a booking."));
        bookingsList.setCellFactory(
                list ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(Booking booking, boolean empty) {
                                super.updateItem(booking, empty);
                                if (empty || booking == null) {
                                    setGraphic(null);
                                    setText(null);
                                    return;
                                }
                                setGraphic(createBookingCard(booking));
                                setText(null);
                            }
                        });

        statusLabel.setText("Loading your bookings...");
        Task<List<Booking>> task =
                new Task<>() {
                    @Override
                    protected List<Booking> call() {
                        return new BookingService().findMyBookings(UserSession.getUser());
                    }
                };
        task.setOnSucceeded(
                event -> {
                    List<Booking> bookings = task.getValue();
                    bookingsList.getItems().setAll(bookings);
                    statusLabel.setText(bookings.size() + " confirmed bookings");
                });
        task.setOnFailed(event -> statusLabel.setText(FxTasks.message(task.getException())));
        FxTasks.start(task);
    }

    private VBox createBookingCard(Booking booking) {
        Label title = new Label(booking.getMovieTitle());
        title.getStyleClass().add("section-title");

        String screening =
                Formats.date(booking.getScreening().getStartsAt())
                        + "  •  "
                        + booking.getScreening().getRoom().getName();
        String seats =
                "Seats: "
                        + booking.getSeatLabels()
                        + "  •  "
                        + Formats.money(booking.getTotalPrice());
        Label details = new Label(screening + "\n" + seats);
        details.setWrapText(true);
        details.getStyleClass().add("muted");

        Label reference = new Label("Booking " + booking.getReference());
        reference.getStyleClass().add("eyebrow");
        VBox card = new VBox(10, title, details, reference);
        card.getStyleClass().add("panel");
        return card;
    }
}
