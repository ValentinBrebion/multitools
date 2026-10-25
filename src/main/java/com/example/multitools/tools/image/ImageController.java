package com.example.multitools.tools.image;

import com.example.multitools.core.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public class ImageController {

    @FXML
    private void openImageConverter() {
        new ImageConverterTool().open();
    }

    @FXML
    private void openImageCrop() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/image/image-crop.fxml")
            );
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            showNavigationError("Impossible d'ouvrir l'outil de recadrage d'image", e);
        }
    }

    @FXML
    private void openBackgroundRemoval() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/image/image-background-removal.fxml")
            );
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            showNavigationError("Impossible d'ouvrir l'outil de retrait du fond blanc", e);
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            loader.setControllerFactory(type -> new com.example.multitools.MainController());
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            showNavigationError("Impossible de revenir au menu principal", e);
        }
    }

    private void showNavigationError(String header, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(header);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
