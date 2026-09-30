package com.example.multitools.tools.security;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.update.UpdateChecker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

public class SecurityController {
    
    @FXML
    private Label versionLabel;

    @FXML
    public void initialize() {
        versionLabel.setText("Version " + UpdateChecker.getCurrentVersion());
    }

    @FXML
    private void openPasswordGenerator() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/security/password-generator.fxml")
            );
            loader.setControllerFactory(param -> new PasswordGeneratorController());
            
            Parent root = loader.load();
            
            NavigationManager.getInstance().navigateTo(root);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le générateur de mot de passe: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/main.fxml")
            );
            loader.setControllerFactory(param -> new com.example.multitools.MainController());
            
            Parent root = loader.load();
            
            NavigationManager.getInstance().navigateTo(root);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de revenir au menu principal: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateCheck() {
        showAlert("Mise à jour", "Recherche de mises à jour...");
        
        Thread updateThread = new Thread(() -> {
            UpdateChecker checker = new UpdateChecker();
            UpdateChecker.UpdateInfo updateInfo = checker.checkForUpdates();
            
            javafx.application.Platform.runLater(() -> {
                if (updateInfo.hasUpdate) {
                    showUpdateDialog(updateInfo);
                } else {
                    showAlert("Mise à jour", "Vous utilisez déjà la dernière version de Multitools (" + 
                            UpdateChecker.getCurrentVersion() + ")");
                }
            });
        });
        
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private void showUpdateDialog(UpdateChecker.UpdateInfo updateInfo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Mise à jour disponible");
        alert.setHeaderText("Multitools " + updateInfo.latestVersion + " est disponible");
        
        String message = "Votre version: " + UpdateChecker.getCurrentVersion() + "\n" +
                        "Nouvelle version: " + updateInfo.latestVersion + "\n\n";
        
        if (updateInfo.releaseNotes != null && !updateInfo.releaseNotes.isEmpty()) {
            message += "Notes de version:\n" + updateInfo.releaseNotes + "\n\n";
        }
        
        message += "Voulez-vous télécharger et installer cette mise à jour maintenant ?";
        
        alert.setContentText(message);
        
        javafx.scene.control.ButtonType installButton = new javafx.scene.control.ButtonType("OK", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType cancelButton = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(installButton, cancelButton);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == installButton) {
                showAlert("Information", "Le téléchargement sera disponible dans une future version.");
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
