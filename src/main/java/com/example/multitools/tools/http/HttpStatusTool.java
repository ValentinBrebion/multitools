package com.example.multitools.tools.http;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.core.Tool;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;

import java.io.IOException;

public class HttpStatusTool implements Tool {

    @Override
    public String getName() {
        return "Codes HTTP";
    }

    @Override
    public String getDescription() {
        return "Rechercher la signification et les causes des codes HTTP";
    }

    @Override
    public String getIcon() {
        return "HTTP";
    }

    @Override
    public void open() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/http/http-status.fxml")
            );
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir l'explorateur de codes HTTP");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
