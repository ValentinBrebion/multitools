package com.example.multitools.tools.image;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.core.Tool;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;

import java.io.IOException;

public class ImageConverterTool implements Tool {

    @Override
    public String getName() {
        return "Convertisseur d'image";
    }

    @Override
    public String getDescription() {
        return "Convertir une image en WebP, PNG, JPG ou ICO";
    }

    @Override
    public String getIcon() {
        return "🖼️";
    }

    @Override
    public void open() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/image/image-converter.fxml")
            );
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le convertisseur d'image");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
