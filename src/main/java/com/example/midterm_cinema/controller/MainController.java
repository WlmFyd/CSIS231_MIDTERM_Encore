package com.example.midterm_cinema.controller;

import com.example.midterm_cinema.model.User;
import com.example.midterm_cinema.model.enums.UserRole;
import com.example.midterm_cinema.navigation.ViewNavigator;
import com.example.midterm_cinema.session.UserSession;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainController {
    @FXML
    private StackPane viewContainer;
    @FXML
    private Button dashboardButton;
    @FXML
    private Button touchButton;
    @FXML
    private Button accountButton;
    @FXML
    private Label accountLabel;

    @FXML
    private void initialize() {
        ViewNavigator.initialize(viewContainer, () -> refreshHeader());
        ViewNavigator.browse();
    }

    private void refreshHeader() {
        User user = UserSession.getUser();
        accountLabel.setText(user == null ? "Guest" : user.getUsername());
        accountButton.setText(user == null ? "Sign in" : "Sign out");
        boolean admin = user != null && user.getRole() == UserRole.ADMIN;
        dashboardButton.setVisible(admin);
        dashboardButton.setManaged(admin);
        touchButton.setVisible(admin);
        touchButton.setManaged(admin);
    }

    @FXML
    private void browse() {
        ViewNavigator.browse();
    }

    @FXML
    private void bookings() {
        ViewNavigator.bookings();
    }

    @FXML
    private void dashboard() {
        ViewNavigator.dashboard();
    }

    @FXML
    private void touchZone() {
        ViewNavigator.touchZone();
    }

    @FXML
    private void account() {
        if (UserSession.getUser() == null) ViewNavigator.signIn();
        else {
            UserSession.signOut();
            ViewNavigator.browse();
        }
        refreshHeader();
    }
}
