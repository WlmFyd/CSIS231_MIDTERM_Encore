package com.example.midterm_cinema.controller;

import com.example.midterm_cinema.model.User;
import com.example.midterm_cinema.service.AuthService;
import com.example.midterm_cinema.session.UserSession;
import com.example.midterm_cinema.util.FxTasks;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;

    @FXML
    private void signIn() {
        authenticate(false);
    }

    @FXML
    private void register() {
        authenticate(true);
    }

    private void authenticate(boolean createAccount) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        loginButton.setDisable(true);
        registerButton.setDisable(true);
        statusLabel.setText(createAccount ? "Creating your account..." : "Signing in...");

        Task<User> task =
                new Task<>() {
                    @Override
                    protected User call() {
                        AuthService auth = new AuthService();
                        if (createAccount) {
                            return auth.register(username, password);
                        }
                        return auth.signIn(username, password);
                    }
                };
        task.setOnSucceeded(
                event -> {
                    UserSession.signIn(task.getValue());
                    passwordField.clear();
                    Stage window = (Stage) loginButton.getScene().getWindow();
                    window.close();
                });
        task.setOnFailed(
                event -> {
                    statusLabel.setText(FxTasks.message(task.getException()));
                    loginButton.setDisable(false);
                    registerButton.setDisable(false);
                });
        FxTasks.start(task);
    }
}
