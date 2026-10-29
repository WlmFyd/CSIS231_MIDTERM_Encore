package com.example.midterm_cinema.controller;

import com.example.midterm_cinema.model.MovieStatistics;
import com.example.midterm_cinema.model.User;
import com.example.midterm_cinema.service.CinemaService;
import com.example.midterm_cinema.session.UserSession;
import com.example.midterm_cinema.util.Formats;
import com.example.midterm_cinema.util.FxTasks;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.util.List;

public class DashboardController {
    @FXML
    private Label bookingsLabel;
    @FXML
    private Label ticketsLabel;
    @FXML
    private Label revenueLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TableView<MovieStatistics> statisticsTable;
    @FXML
    private TableColumn<MovieStatistics, String> movieColumn;
    @FXML
    private TableColumn<MovieStatistics, String> bookingsColumn;
    @FXML
    private TableColumn<MovieStatistics, String> ticketsColumn;
    @FXML
    private TableColumn<MovieStatistics, String> revenueColumn;
    @FXML
    private Button refreshButton;

    @FXML
    private void initialize() {
        // Each lambda tells the table which value to display in that column.
        movieColumn.setCellValueFactory(
                row -> new ReadOnlyStringWrapper(row.getValue().getTitle()));
        bookingsColumn.setCellValueFactory(
                row -> new ReadOnlyStringWrapper(Long.toString(row.getValue().getBookings())));
        ticketsColumn.setCellValueFactory(
                row -> new ReadOnlyStringWrapper(Long.toString(row.getValue().getTickets())));
        revenueColumn.setCellValueFactory(
                row -> new ReadOnlyStringWrapper(Formats.money(row.getValue().getRevenue())));
        refresh();
    }

    @FXML
    private void refresh() {
        User user = UserSession.getUser();
        refreshButton.setDisable(true);
        statusLabel.setText("Loading statistics...");

        Task<List<MovieStatistics>> task =
                new Task<>() {
                    @Override
                    protected List<MovieStatistics> call() {
                        return new CinemaService().findMovieStatistics(user);
                    }
                };
        task.setOnSucceeded(
                event -> {
                    List<MovieStatistics> rows = task.getValue();
                    statisticsTable.getItems().setAll(rows);
                    showTotals(rows);
                    statusLabel.setText("All-time confirmed bookings • prices in USD");
                    refreshButton.setDisable(false);
                });
        task.setOnFailed(
                event -> {
                    statusLabel.setText(FxTasks.message(task.getException()));
                    refreshButton.setDisable(false);
                });
        FxTasks.start(task);
    }

    private void showTotals(List<MovieStatistics> rows) {
        long bookings = 0;
        long tickets = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        for (MovieStatistics row : rows) {
            bookings += row.getBookings();
            tickets += row.getTickets();
            revenue = revenue.add(row.getRevenue());
        }
        bookingsLabel.setText(Long.toString(bookings));
        ticketsLabel.setText(Long.toString(tickets));
        revenueLabel.setText(Formats.money(revenue));
    }
}
