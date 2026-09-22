package com.example.multitools.core;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavigationManager {
    
    private static NavigationManager instance;
    private Stage primaryStage;
    private Parent currentRoot;
    
    private NavigationManager() {
    }
    
    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }
    
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }
    
    public void navigateTo(Parent root) {
        if (primaryStage != null) {
            this.currentRoot = root;
            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, 950, 700);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
        }
    }
    
    public void navigateToTool(Tool tool) {
        tool.open();
    }
    
    public Parent getCurrentRoot() {
        return currentRoot;
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
