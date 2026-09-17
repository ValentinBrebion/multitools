package com.example.javazip;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class JavaZipApp extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/main.fxml")
        );

        Parent root = loader.load();

        // Configuration de la fenêtre
        Scene scene = new Scene(root, 950, 700);

        stage.setTitle("JavaZip");
        stage.setScene(scene);

        stage.setMinWidth(850);
        stage.setMinHeight(600);
        stage.setResizable(true);

        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
