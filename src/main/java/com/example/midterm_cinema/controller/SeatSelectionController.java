package com.example.midterm_cinema.controller;

import com.example.midterm_cinema.model.Booking;
import com.example.midterm_cinema.model.Screening;
import com.example.midterm_cinema.model.Seat;
import com.example.midterm_cinema.model.User;
import com.example.midterm_cinema.model.enums.SeatType;
import com.example.midterm_cinema.navigation.ViewNavigator;
import com.example.midterm_cinema.service.BookingService;
import com.example.midterm_cinema.service.CinemaService;
import com.example.midterm_cinema.session.UserSession;
import com.example.midterm_cinema.util.Formats;
import com.example.midterm_cinema.util.FxTasks;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeatSelectionController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label screeningLabel;
    @FXML
    private Label pricesLabel;
    @FXML
    private GridPane seatGrid;
    @FXML
    private ListView<String> selectedSeats;
    @FXML
    private Label totalLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button bookButton;

    private final BookingService bookings = new BookingService();
    private final List<Seat> selection = new ArrayList<>();
    private List<Seat> roomSeats = new ArrayList<>();
    private Set<Long> reservedIds = new HashSet<>();
    private Screening screening;
    private boolean saving;

    public void load(Screening screening) {
        this.screening = screening;
        titleLabel.setText(screening.getMovie().getTitle());
        screeningLabel.setText(
                Formats.date(screening.getStartsAt()) + "  •  " + screening.getRoom().getName());
        BigDecimal vipPrice = bookings.price(screening.getBasePrice(), SeatType.VIP);
        pricesLabel.setText(
                "Normal "
                        + Formats.money(screening.getBasePrice())
                        + "   /   VIP "
                        + Formats.money(vipPrice));
        refreshAvailability();
    }

    private void refreshAvailability() {
        selection.clear();
        selectedSeats.getItems().clear();
        totalLabel.setText(Formats.money(BigDecimal.ZERO));
        bookButton.setDisable(true);
        seatGrid.setDisable(true);
        statusLabel.setText("Loading seat availability...");

        Task<Void> task =
                new Task<>() {
                    @Override
                    protected Void call() {
                        // The UI reads these fields only after this task has succeeded.
                        roomSeats = new CinemaService().findSeats(screening.getRoom().getId());
                        reservedIds = bookings.reservedSeats(screening.getId());
                        return null;
                    }
                };
        task.setOnSucceeded(
                event -> {
                    drawSeats();
                    seatGrid.setDisable(false);
                    statusLabel.setText("Your seats are confirmed when you complete the booking.");
                });
        task.setOnFailed(event -> statusLabel.setText(FxTasks.message(task.getException())));
        FxTasks.start(task);
    }

    private void drawSeats() {
        seatGrid.getChildren().clear();
        Region aisle = new Region();
        aisle.setMinWidth(18);
        seatGrid.add(aisle, 6, 0);

        String previousRow = "";
        int rowIndex = -1;
        for (Seat seat : roomSeats) {
            if (!seat.getRowLabel().equals(previousRow)) {
                previousRow = seat.getRowLabel();
                rowIndex++;
                Label rowLabel = new Label(previousRow);
                rowLabel.getStyleClass().add("muted");
                seatGrid.add(rowLabel, 0, rowIndex);
            }

            ToggleButton button = createSeatButton(seat);
            int column = seat.getSeatNumber();
            if (column > 5) {
                column++; // Leave column 6 empty for the aisle.
            }
            seatGrid.add(button, column, rowIndex);
        }
    }

    private ToggleButton createSeatButton(Seat seat) {
        ToggleButton button = new ToggleButton(seat.getLabel());
        button.getStyleClass().add("seat");
        if (seat.getType() == SeatType.VIP) {
            button.getStyleClass().add("seat-vip");
        } else {
            button.getStyleClass().add("seat-normal");
        }
        button.setAccessibleText(seat.getLabel() + " " + seat.getType());

        if (reservedIds.contains(seat.getId())) {
            button.setDisable(true);
            button.getStyleClass().add("seat-booked");
            button.setTooltip(new Tooltip("Already booked"));
        } else {
            BigDecimal price = bookings.price(screening.getBasePrice(), seat.getType());
            button.setTooltip(new Tooltip(seat.getType() + " • " + Formats.money(price)));
        }

        button.setOnAction(
                event -> {
                    if (button.isSelected()) {
                        selection.add(seat);
                    } else {
                        selection.remove(seat);
                    }
                    updateSummary();
                });
        return button;
    }

    private void updateSummary() {
        selectedSeats.getItems().clear();
        BigDecimal total = BigDecimal.ZERO;
        for (Seat seat : selection) {
            BigDecimal price = bookings.price(screening.getBasePrice(), seat.getType());
            selectedSeats
                    .getItems()
                    .add(
                            seat.getLabel()
                                    + "  •  "
                                    + seat.getType()
                                    + "  •  "
                                    + Formats.money(price));
            total = total.add(price);
        }
        totalLabel.setText(Formats.money(total));
        bookButton.setDisable(selection.isEmpty() || saving);
    }

    @FXML
    private void back() {
        if (!saving) {
            ViewNavigator.movie(screening.getMovie());
        }
    }

    @FXML
    private void book() {
        if (saving || selection.isEmpty() || !ViewNavigator.ensureSignedIn()) {
            return;
        }
        saving = true;
        seatGrid.setDisable(true);
        bookButton.setDisable(true);
        statusLabel.setText("Confirming your booking...");

        Window owner = bookButton.getScene().getWindow();
        User user = UserSession.getUser();
        List<Long> seatIds = new ArrayList<>();
        for (Seat seat : selection) {
            seatIds.add(seat.getId());
        }

        Task<Booking> task =
                new Task<>() {
                    @Override
                    protected Booking call() {
                        return bookings.book(user, screening.getId(), seatIds);
                    }
                };
        task.setOnSucceeded(
                event -> {
                    saving = false;
                    showConfirmation(task.getValue(), owner);
                    ViewNavigator.bookings();
                });
        task.setOnFailed(
                event -> {
                    saving = false;
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.initOwner(owner);
                    alert.setHeaderText("Booking could not be completed");
                    alert.setContentText(FxTasks.message(task.getException()));
                    alert.showAndWait();
                    refreshAvailability();
                });
        FxTasks.start(task);
    }

    private void showConfirmation(Booking booking, Window owner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle("Booking confirmed • Encore");
        alert.setHeaderText("Enjoy the show!");
        alert.setContentText(
                booking.getMovieTitle()
                        + "\n"
                        + Formats.date(booking.getScreening().getStartsAt())
                        + "\nSeats: "
                        + booking.getSeatLabels()
                        + "\nTotal: "
                        + Formats.money(booking.getTotalPrice())
                        + "\nBooking: "
                        + booking.getReference());
        alert.showAndWait();
    }
}
