package com.example.multitools;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.core.ToolManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        // Initialiser les gestionnaires
        NavigationManager.getInstance().setPrimaryStage(stage);
        ToolManager.getInstance();
        
        // Charger l'interface principale (menu de navigation)
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/main.fxml")
        );
        loader.setControllerFactory(param -> new MainController());
        
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 1100, 700);
        
        stage.setTitle("Multitools foundation");
        stage.setScene(scene);
        
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setResizable(true);
        
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
