package com.example.multitools.tools.image;

import com.example.multitools.core.Tool;
import com.example.multitools.core.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;

import java.io.IOException;

public class ImageTool implements Tool {

    @Override
    public String getName() {
        return "Image";
    }

    @Override
    public String getDescription() {
        return "Outils de manipulation d'images";
    }

    @Override
    public String getIcon() {
        return "🖼️";
    }

    @Override
    public void open() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/image/image.fxml")
            );
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le module Image");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
