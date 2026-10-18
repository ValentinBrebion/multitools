package com.example.multitools.tools.divers;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.tools.converter.UnitConverterTool;
import com.example.multitools.update.UpdateChecker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

public class DiversController {

    @FXML
    private Label versionLabel;

    @FXML
    public void initialize() {
        versionLabel.setText("Version " + UpdateChecker.getCurrentVersion());
    }

    @FXML
    private void openConverterTool() {
        new UnitConverterTool().open();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            loader.setControllerFactory(type -> new com.example.multitools.MainController());
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de revenir au menu principal: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateCheck() {
        Alert searchingAlert = new Alert(Alert.AlertType.INFORMATION);
        searchingAlert.setTitle("Mise à jour");
        searchingAlert.setHeaderText(null);
        searchingAlert.setContentText("Recherche de mises à jour...");
        searchingAlert.show();

        Thread updateThread = new Thread(() -> {
            UpdateChecker checker = new UpdateChecker();
            UpdateChecker.UpdateInfo updateInfo = checker.checkForUpdates();
            javafx.application.Platform.runLater(() -> {
                searchingAlert.close();
                Alert resultAlert = new Alert(
                    updateInfo.hasUpdate ? Alert.AlertType.CONFIRMATION : Alert.AlertType.INFORMATION
                );
                resultAlert.setTitle("Mise à jour");
                resultAlert.setHeaderText(updateInfo.hasUpdate
                    ? "Multitools " + updateInfo.latestVersion + " est disponible"
                    : null);
                resultAlert.setContentText(updateInfo.hasUpdate
                    ? "Votre version: " + UpdateChecker.getCurrentVersion() + "\n"
                        + "Nouvelle version: " + updateInfo.latestVersion + "\n\n"
                        + "Le téléchargement sera disponible dans une future version."
                    : "Vous utilisez déjà la dernière version de Multitools ("
                        + UpdateChecker.getCurrentVersion() + ")");
                if (updateInfo.hasUpdate) {
                    resultAlert.getButtonTypes().setAll(
                        new javafx.scene.control.ButtonType(
                            "OK",
                            javafx.scene.control.ButtonBar.ButtonData.OK_DONE
                        ),
                        new javafx.scene.control.ButtonType(
                            "Annuler",
                            javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE
                        )
                    );
                }
                resultAlert.showAndWait();
            });
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
