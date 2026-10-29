package com.example.midterm_cinema.controller;

import com.example.midterm_cinema.model.*;
import com.example.midterm_cinema.model.enums.UserRole;
import com.example.midterm_cinema.service.AdminService;
import com.example.midterm_cinema.service.AuthService;
import com.example.midterm_cinema.service.BookingService;
import com.example.midterm_cinema.service.CinemaService;
import com.example.midterm_cinema.session.UserSession;
import com.example.midterm_cinema.util.Formats;
import com.example.midterm_cinema.util.FxTasks;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Connects the user's Touch Zone layout to small administrative operations. */
public class TouchZoneController {
    @FXML private VBox touchRoot;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalMoviesLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> userIdColumn;
    @FXML private TableColumn<User, String> userNameColumn;
    @FXML private TableView<Movie> moviesTable;
    @FXML private TableColumn<Movie, String> movieIdColumn;
    @FXML private TableColumn<Movie, String> movieNameColumn;
    @FXML private TableView<BookingSeat> ticketsTable;
    @FXML private TableColumn<BookingSeat, String> ticketColumn;
    @FXML private TableColumn<BookingSeat, String> ticketUserColumn;
    @FXML private TableColumn<BookingSeat, String> ticketMovieColumn;

    private final AdminService administration = new AdminService();
    private List<User> users = new ArrayList<>();
    private List<Movie> movies = new ArrayList<>();
    private List<BookingSeat> tickets = new ArrayList<>();
    private List<Room> rooms = new ArrayList<>();
    private int screeningRequest;
    private int seatRequest;

    @FXML
    private void initialize() {
        userIdColumn.setCellValueFactory(
                row -> new ReadOnlyStringWrapper(row.getValue().getId().toString()));
        userNameColumn.setCellValueFactory(
                row -> new ReadOnlyStringWrapper(row.getValue().getUsername()));
        movieIdColumn.setCellValueFactory(
                row -> new ReadOnlyStringWrapper(row.getValue().getId().toString()));
        movieNameColumn.setCellValueFactory(
                row -> new ReadOnlyStringWrapper(row.getValue().getTitle()));
        ticketColumn.setCellValueFactory(
                row ->
                        new ReadOnlyStringWrapper(
                                "#"
                                        + row.getValue().getId()
                                        + " • "
                                        + row.getValue().getSeat().getLabel()));
        ticketUserColumn.setCellValueFactory(
                row ->
                        new ReadOnlyStringWrapper(
                                row.getValue().getBooking().getUser().getId().toString()));
        ticketMovieColumn.setCellValueFactory(
                row ->
                        new ReadOnlyStringWrapper(
                                row.getValue().getScreening().getMovie().getTitle()));
        usersTable.setPlaceholder(new Label("No users."));
        moviesTable.setPlaceholder(new Label("No movies."));
        ticketsTable.setPlaceholder(new Label("No assigned tickets."));
        refresh("Ready. Select a row for edit/delete actions.");
    }

    private void refresh(String message) {
        User administrator = UserSession.getUser();
        touchRoot.setDisable(true);
        statusLabel.setText("Loading...");
        Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        users = administration.findUsers(administrator);
                        movies = administration.findMovies(administrator);
                        tickets = administration.findTickets(administrator);
                        rooms = administration.findRooms(administrator);
                        return null;
                    }
                };
        task.setOnSucceeded(event -> {
                    usersTable.getItems().setAll(users);
                    moviesTable.getItems().setAll(movies);
                    ticketsTable.getItems().setAll(tickets);
                    totalUsersLabel.setText(Integer.toString(users.size()));
                    totalMoviesLabel.setText(Integer.toString(movies.size()));
                    statusLabel.setText(message);
                    touchRoot.setDisable(false);
                });
        task.setOnFailed(event -> {
                    touchRoot.setDisable(false);
                    statusLabel.setText("Could not load Touch Zone.");
                    showError(task.getException());
                });
        FxTasks.start(task);
    }

    @FXML
    private void createUser() {
        User administrator = UserSession.getUser();
        GridPane form = form();
        TextField username = new TextField();
        username.setId("newUsername");
        PasswordField password = new PasswordField();
        password.setId("newPassword");
        ComboBox<UserRole> role = new ComboBox<>();
        role.setId("newUserRole");
        role.getItems().setAll(UserRole.values());
        role.setValue(UserRole.CUSTOMER);
        Label error = errorLabel();
        form.addRow(0, new Label("Username"), username);
        form.addRow(1, new Label("Password"), password);
        form.addRow(2, new Label("Role"), role);
        form.add(error, 0, 3, 2, 1);
        Dialog<ButtonType> dialog = dialog("Create user", form, "Create user");
        Button save = (Button) dialog.getDialogPane().lookupButton(ButtonType.APPLY);
        save.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    event.consume();
                    String name = username.getText();
                    String secret = password.getText();
                    UserRole selectedRole = role.getValue();
                    Task<Void> task =
                            new Task<>() {
                                @Override
                                protected Void call() {
                                    new AuthService()
                                            .createUser(administrator, name, secret, selectedRole);
                                    return null;
                                }
                            };
                    runDialogTask(dialog, task, error, "User created.");
                });
        dialog.showAndWait();
    }

    @FXML
    private void createMovie() {
        movieForm(null);
    }

    @FXML
    private void editMovie() {
        Movie selected = moviesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showSelectionMessage("Select a movie in the movie table first.");
            return;
        }
        movieForm(selected);
    }

    private void movieForm(Movie movie) {
        User administrator = UserSession.getUser();
        boolean creating = movie == null;
        GridPane form = form();
        TextField title = new TextField(creating ? "" : movie.getTitle());
        title.setId("movieTitle");
        TextField genre = new TextField(creating ? "" : movie.getGenre());
        genre.setId("movieGenre");
        TextField duration =
                new TextField(creating ? "120" : Integer.toString(movie.getDurationMinutes()));
        duration.setId("movieDuration");
        TextArea synopsis = new TextArea(creating ? "" : movie.getSynopsis());
        synopsis.setId("movieSynopsis");
        synopsis.setWrapText(true);
        synopsis.setPrefRowCount(3);
        form.addRow(0, new Label("Title"), title);
        form.addRow(1, new Label("Genre"), genre);
        form.addRow(2, new Label("Duration (minutes)"), duration);
        form.addRow(3, new Label("Synopsis"), synopsis);

        CheckBox schedule = new CheckBox("Schedule the first screening");
        schedule.setId("scheduleFirstScreening");
        ComboBox<Room> room = new ComboBox<>();
        room.setId("screeningRoom");
        room.getItems().setAll(rooms);
        room.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(Room value) {
                        return value == null ? "" : value.getName();
                    }

                    @Override
                    public Room fromString(String value) {
                        return null;
                    }
                });
        room.getSelectionModel().selectFirst();
        DatePicker date = new DatePicker(LocalDate.now().plusDays(1));
        date.setId("screeningDate");
        TextField time = new TextField("14:00");
        time.setId("screeningTime");
        TextField price = new TextField("10.00");
        price.setId("screeningPrice");
        if (creating) {
            form.add(schedule, 0, 4, 2, 1);
            form.addRow(5, new Label("Room"), room);
            form.addRow(6, new Label("Date (Beirut)"), date);
            form.addRow(7, new Label("Time (HH:mm)"), time);
            form.addRow(8, new Label("Base ticket price (USD)"), price);
            room.setDisable(true);
            date.setDisable(true);
            time.setDisable(true);
            price.setDisable(true);
            schedule.setOnAction(
                    event -> {
                        boolean disabled = !schedule.isSelected();
                        room.setDisable(disabled);
                        date.setDisable(disabled);
                        time.setDisable(disabled);
                        price.setDisable(disabled);
                    });
        }
        Label error = errorLabel();
        form.add(error, 0, creating ? 9 : 4, 2, 1);
        Dialog<ButtonType> dialog =
                dialog(
                        creating ? "Create movie" : "Edit movie",
                        form,
                        creating ? "Create movie" : "Save changes");
        Button save = (Button) dialog.getDialogPane().lookupButton(ButtonType.APPLY);
        save.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    event.consume();
                    try {
                        String name = title.getText();
                        String description = synopsis.getText();
                        String category = genre.getText();
                        int minutes = Integer.parseInt(duration.getText().trim());
                        Long roomId = null;
                        LocalDateTime startsAt = null;
                        BigDecimal basePrice = null;
                        if (creating && schedule.isSelected()) {
                            if (room.getValue() == null || date.getValue() == null)
                                throw new IllegalArgumentException("Choose a room and date.");
                            roomId = room.getValue().getId();
                            startsAt =
                                    LocalDateTime.of(
                                            date.getValue(),
                                            LocalTime.parse(time.getText().trim()));
                            basePrice = new BigDecimal(price.getText().trim());
                        }
                        Long selectedRoom = roomId;
                        LocalDateTime selectedTime = startsAt;
                        BigDecimal selectedPrice = basePrice;
                        Task<Void> task =
                                new Task<>() {
                                    @Override
                                    protected Void call() {
                                        if (creating)
                                            administration.createMovie(
                                                    administrator,
                                                    name,
                                                    description,
                                                    minutes,
                                                    category,
                                                    selectedRoom,
                                                    selectedTime,
                                                    selectedPrice);
                                        else
                                            administration.editMovie(
                                                    administrator,
                                                    movie.getId(),
                                                    name,
                                                    description,
                                                    minutes,
                                                    category);
                                        return null;
                                    }
                                };
                        runDialogTask(
                                dialog,
                                task,
                                error,
                                creating ? "Movie created." : "Movie updated.");
                    } catch (NumberFormatException invalidNumber) {
                        error.setText("Enter a whole-number duration and a numeric ticket price.");
                    } catch (java.time.format.DateTimeParseException invalidTime) {
                        error.setText("Enter the screening time as HH:mm, for example 18:30.");
                    } catch (IllegalArgumentException invalidInput) {
                        error.setText(invalidInput.getMessage());
                    }
                });
        dialog.showAndWait();
    }

    @FXML
    private void assignTicket() {
        if (users.isEmpty() || movies.isEmpty()) {
            showSelectionMessage("Create a user and a movie before assigning a ticket.");
            return;
        }
        User administrator = UserSession.getUser();
        GridPane form = form();
        ComboBox<User> user = new ComboBox<>();
        user.setId("ticketUser");
        user.getItems().setAll(users);
        user.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(User value) {
                        return value == null ? "" : value.getId() + " • " + value.getUsername();
                    }

                    @Override
                    public User fromString(String value) {
                        return null;
                    }
                });
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        user.setValue(selectedUser == null ? users.get(0) : selectedUser);
        ComboBox<Movie> movie = new ComboBox<>();
        movie.setId("ticketMovie");
        movie.getItems().setAll(movies);
        movie.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(Movie value) {
                        return value == null ? "" : value.getTitle();
                    }

                    @Override
                    public Movie fromString(String value) {
                        return null;
                    }
                });
        ComboBox<Screening> screening = new ComboBox<>();
        screening.setId("ticketScreening");
        screening.setPrefWidth(430);
        ComboBox<Seat> seat = new ComboBox<>();
        seat.setId("ticketSeat");
        BookingService bookings = new BookingService();
        seat.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(Seat value) {
                        if (value == null || screening.getValue() == null) return "";
                        BigDecimal price =
                                bookings.price(
                                        screening.getValue().getBasePrice(), value.getType());
                        return value.getLabel()
                                + " • "
                                + value.getType()
                                + " • "
                                + Formats.money(price);
                    }

                    @Override
                    public Seat fromString(String value) {
                        return null;
                    }
                });
        Label error = errorLabel();
        form.addRow(0, new Label("User"), user);
        form.addRow(1, new Label("Movie"), movie);
        form.addRow(2, new Label("Screening"), screening);
        form.addRow(3, new Label("Available seat"), seat);
        form.add(error, 0, 4, 2, 1);
        Dialog<ButtonType> dialog = dialog("Assign ticket", form, "Assign ticket");
        Button save = (Button) dialog.getDialogPane().lookupButton(ButtonType.APPLY);
        save.setDisable(true);
        movie.valueProperty()
                .addListener(
                        (observable, previous, selected) ->
                                loadScreenings(movie, screening, seat, save, error));
        screening
                .valueProperty()
                .addListener(
                        (observable, previous, selected) ->
                                loadSeats(screening, seat, save, error));
        seat.valueProperty()
                .addListener(
                        (observable, previous, selected) ->
                                save.setDisable(
                                        user.getValue() == null
                                                || screening.getValue() == null
                                                || selected == null));
        user.valueProperty()
                .addListener(
                        (observable, previous, selected) ->
                                save.setDisable(
                                        selected == null
                                                || screening.getValue() == null
                                                || seat.getValue() == null));
        Movie selectedMovie = moviesTable.getSelectionModel().getSelectedItem();
        movie.setValue(selectedMovie == null ? movies.get(0) : selectedMovie);
        save.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    event.consume();
                    if (user.getValue() == null
                            || screening.getValue() == null
                            || seat.getValue() == null) {
                        error.setText("Choose a user, screening, and available seat.");
                        return;
                    }
                    Long userId = user.getValue().getId();
                    Long screeningId = screening.getValue().getId();
                    List<Long> seatIds = new ArrayList<>();
                    seatIds.add(seat.getValue().getId());
                    Task<Void> task =
                            new Task<>() {
                                @Override
                                protected Void call() {
                                    bookings.bookForUser(
                                            administrator, userId, screeningId, seatIds);
                                    return null;
                                }
                            };
                    runDialogTask(dialog, task, error, "Ticket assigned.");
                });
        dialog.showAndWait();
    }

    private void loadScreenings(
            ComboBox<Movie> movie,
            ComboBox<Screening> screening,
            ComboBox<Seat> seat,
            Button save,
            Label error) {
        int request = ++screeningRequest;
        ++seatRequest;
        screening.getItems().clear();
        seat.getItems().clear();
        screening.setDisable(true);
        seat.setDisable(true);
        save.setDisable(true);
        Movie selected = movie.getValue();
        if (selected == null) return;
        error.setText("Loading screenings...");
        Task<List<Screening>> task =
                new Task<>() {
                    @Override
                    protected List<Screening> call() {
                        return new CinemaService().findUpcoming(selected.getId());
                    }
                };
        task.setOnSucceeded(
                event -> {
                    if (request != screeningRequest) return;
                    screening.getItems().setAll(task.getValue());
                    screening.setDisable(false);
                    if (task.getValue().isEmpty())
                        error.setText("This movie has no upcoming screenings.");
                    else {
                        error.setText("");
                        screening.getSelectionModel().selectFirst();
                    }
                });
        task.setOnFailed(
                event -> {
                    if (request == screeningRequest)
                        error.setText(FxTasks.message(task.getException()));
                });
        FxTasks.start(task);
    }

    private void loadSeats(
            ComboBox<Screening> screening, ComboBox<Seat> seat, Button save, Label error) {
        int request = ++seatRequest;
        seat.getItems().clear();
        seat.setDisable(true);
        save.setDisable(true);
        Screening selected = screening.getValue();
        if (selected == null) return;
        error.setText("Loading available seats...");
        Task<List<Seat>> task =
                new Task<>() {
                    @Override
                    protected List<Seat> call() {
                        List<Seat> available = new ArrayList<>();
                        Set<Long> reserved = new BookingService().reservedSeats(selected.getId());
                        for (Seat value :
                                new CinemaService().findSeats(selected.getRoom().getId())) {
                            if (!reserved.contains(value.getId())) available.add(value);
                        }
                        return available;
                    }
                };
        task.setOnSucceeded(
                event -> {
                    if (request != seatRequest) return;
                    seat.getItems().setAll(task.getValue());
                    seat.setDisable(false);
                    error.setText(task.getValue().isEmpty() ? "This screening is sold out." : "");
                    seat.getSelectionModel().selectFirst();
                });
        task.setOnFailed(
                event -> {
                    if (request == seatRequest) error.setText(FxTasks.message(task.getException()));
                });
        FxTasks.start(task);
    }

    @FXML
    private void deleteTicket() {
        BookingSeat selected = ticketsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showSelectionMessage("Select a ticket in the ticket table first.");
            return;
        }
        String detail =
                "Ticket #"
                        + selected.getId()
                        + " • "
                        + selected.getSeat().getLabel()
                        + "\n"
                        + selected.getScreening().getMovie().getTitle()
                        + "\n"
                        + Formats.date(selected.getScreening().getStartsAt())
                        + "\nFor "
                        + selected.getBooking().getUser().getUsername()
                        + "\nThis frees the seat and removes its value from the statistics.";
        if (!confirm("Delete this ticket?", detail)) return;
        User administrator = UserSession.getUser();
        Task<Void> task =
                new Task<>() {
                    @Override
                    protected Void call() {
                        administration.deleteTicket(administrator, selected.getId());
                        return null;
                    }
                };
        runOperation(task, "Ticket deleted.");
    }

    @FXML
    private void deleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showSelectionMessage("Select a user in the user table first.");
            return;
        }
        if (!confirm(
                "Delete this user?",
                selected.getUsername()
                        + " (#"
                        + selected.getId()
                        + ")\nUsers with bookings cannot be deleted.")) return;
        User administrator = UserSession.getUser();
        Task<Void> task =
                new Task<>() {
                    @Override
                    protected Void call() {
                        administration.deleteUser(administrator, selected.getId());
                        return null;
                    }
                };
        runOperation(task, "User deleted.");
    }

    @FXML
    private void deleteMovie() {
        Movie selected = moviesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showSelectionMessage("Select a movie in the movie table first.");
            return;
        }
        if (!confirm(
                "Delete this movie?",
                selected.getTitle()
                        + "\n"
                        + "Its unbooked screenings will also be deleted. Movies with bookings"
                        + " cannot be deleted.")) return;
        User administrator = UserSession.getUser();
        Task<Void> task =
                new Task<>() {
                    @Override
                    protected Void call() {
                        administration.deleteMovie(administrator, selected.getId());
                        return null;
                    }
                };
        runOperation(task, "Movie deleted.");
    }

    private void runOperation(Task<Void> task, String message) {
        touchRoot.setDisable(true);
        statusLabel.setText("Saving...");
        task.setOnSucceeded(event -> refresh(message));
        task.setOnFailed(
                event -> {
                    touchRoot.setDisable(false);
                    showError(task.getException());
                    refresh("Action was not completed.");
                });
        FxTasks.start(task);
    }

    private void runDialogTask(
            Dialog<ButtonType> dialog, Task<Void> task, Label error, String message) {
        dialog.getDialogPane().setDisable(true);
        error.setText("Saving...");
        task.setOnSucceeded(
                event -> {
                    dialog.getDialogPane().setDisable(false);
                    dialog.close();
                    refresh(message);
                });
        task.setOnFailed(
                event -> {
                    dialog.getDialogPane().setDisable(false);
                    error.setText(FxTasks.message(task.getException()));
                });
        FxTasks.start(task);
    }

    private GridPane form() {
        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(14);
        return form;
    }

    private Label errorLabel() {
        Label label = new Label();
        label.setId("formError");
        label.setWrapText(true);
        label.setMaxWidth(500);
        label.getStyleClass().add("status-text");
        return label;
    }

    private Dialog<ButtonType> dialog(String title, GridPane form, String action) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(usersTable.getScene().getWindow());
        dialog.setTitle(title);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.APPLY, ButtonType.CANCEL);
        dialog.getDialogPane()
                .getStylesheets()
                .add(
                        getClass()
                                .getResource("/com/example/midterm_cinema/css/theme.css")
                                .toExternalForm());
        dialog.getDialogPane().getStyleClass().add("app-root");
        Button save = (Button) dialog.getDialogPane().lookupButton(ButtonType.APPLY);
        save.setText(action);
        save.getStyleClass().add("primary-button");
        dialog.setOnCloseRequest(
                event -> {
                    if (dialog.getDialogPane().isDisabled()) event.consume();
                });
        return dialog;
    }

    private boolean confirm(String title, String detail) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(usersTable.getScene().getWindow());
        alert.setHeaderText(title);
        alert.setContentText(detail);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showSelectionMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(usersTable.getScene().getWindow());
        alert.setHeaderText("Choose a table row");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(Throwable error) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.initOwner(usersTable.getScene().getWindow());
        alert.setHeaderText("Action could not be completed");
        alert.setContentText(FxTasks.message(error));
        alert.showAndWait();
    }
}
