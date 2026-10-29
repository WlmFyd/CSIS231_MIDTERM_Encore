package com.example.midterm_cinema;

import com.example.midterm_cinema.config.HibernateUtil;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class EncoreApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Beirut"));
        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource("/com/example/midterm_cinema/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 1240, 820);
        stage.setTitle("Encore");
        stage.getIcons().add(new Image(getClass().getResource("/com/example/midterm_cinema/images/encore-logo.png").toExternalForm()));
        stage.setMinWidth(1050);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        HibernateUtil.shutdown();
    }
}
