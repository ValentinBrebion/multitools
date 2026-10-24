package com.example.multitools.tools.pdf;

import com.example.multitools.MainController;
import com.example.multitools.core.NavigationManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;

public class PdfController {

    @FXML
    private void openMergeTool() {
        navigateTo("/fxml/pdf/pdf-merge.fxml", "Impossible d'ouvrir le module de fusion PDF.");
    }

    @FXML
    private void openProtectionTool() {
        navigateTo("/fxml/pdf/pdf-protect.fxml", "Impossible d'ouvrir le module de protection PDF.");
    }

    @FXML
    private void handleBack() {
        navigateTo("/fxml/main.fxml", "Impossible de revenir au menu principal.");
    }

    private void navigateTo(String resource, String errorMessage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
            if (resource.equals("/fxml/main.fxml")) {
                loader.setControllerFactory(type -> new MainController());
            }
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(errorMessage + " " + e.getMessage());
            alert.showAndWait();
        }
    }
}
