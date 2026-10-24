package com.example.multitools;

import java.io.IOException;
import java.nio.file.Path;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.core.ToolManager;
import com.example.multitools.tools.compression.CompressionTool;
import com.example.multitools.tools.image.ImageTool;
import com.example.multitools.tools.qrcode.QRCodeTool;
import com.example.multitools.tools.text.TextTool;
import com.example.multitools.update.UpdateChecker;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

public class MainController {
    
    @FXML
    private TextField searchField;

    @FXML
    private Label versionLabel;
    
    @FXML
    public void initialize() {
        versionLabel.setText("Version " + UpdateChecker.getCurrentVersion());
        
        // Initialiser les gestionnaires
        NavigationManager.getInstance();
        ToolManager.getInstance();
    }
    
    @FXML
    private void openCompressionTool() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/compression/compress.fxml")
            );
            loader.setControllerFactory(param -> new com.example.multitools.tools.compression.CompressionController());
            
            Parent root = loader.load();
            
            // Naviguer vers l'outil de compression dans la même fenêtre
            NavigationManager.getInstance().navigateTo(root);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir l'outil de compression: " + e.getMessage());
        }
    }
    
    @FXML
    private void openImageTool() {
        ImageTool imageTool = new ImageTool();
        imageTool.open();
    }
    
    @FXML
    private void openTextTool() {
        TextTool textTool = new TextTool();
        textTool.open();
    }
    
    @FXML
    private void openSecurityTool() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/security/security.fxml")
            );
            loader.setControllerFactory(param -> new com.example.multitools.tools.security.SecurityController());

            Parent root = loader.load();

            NavigationManager.getInstance().navigateTo(root);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir l'outil de sécurité: " + e.getMessage());
        }
    }

    @FXML
    private void openQRCodeTool() {
        new QRCodeTool().open();
    }

    @FXML
    private void openDiversTool() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/divers/divers.fxml")
            );
            loader.setControllerFactory(param -> new com.example.multitools.tools.divers.DiversController());

            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le module Divers: " + e.getMessage());
        }
    }

    @FXML
    private void openPdfTool() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/pdf/pdf.fxml")
            );
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le menu PDF: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleUpdateCheck() {
        showAlert("Mise à jour", "Recherche de mises à jour...");
        
        Thread updateThread = new Thread(() -> {
            com.example.multitools.update.UpdateChecker checker = new com.example.multitools.update.UpdateChecker();
            com.example.multitools.update.UpdateChecker.UpdateInfo updateInfo = checker.checkForUpdates();
            
            javafx.application.Platform.runLater(() -> {
                if (updateInfo.hasUpdate) {
                    showUpdateDialog(updateInfo);
                } else {
                    showAlert("Mise à jour", "Vous utilisez déjà la dernière version de Multitools (" + 
                            com.example.multitools.update.UpdateChecker.getCurrentVersion() + ")");
                }
            });
        });
        
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    private void showUpdateDialog(com.example.multitools.update.UpdateChecker.UpdateInfo updateInfo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Mise à jour disponible");
        alert.setHeaderText("Multitools " + updateInfo.latestVersion + " est disponible");

        String message = "Votre version: " + com.example.multitools.update.UpdateChecker.getCurrentVersion() + "\n" +
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
                startUpdateProcess(updateInfo);
            }
        });
    }

    private void startUpdateProcess(UpdateChecker.UpdateInfo info) {
        // On vérifie tout de suite qu'on peut se mettre à jour (évite de télécharger en mode dev)
        final Path currentExe;
        try {
            currentExe = UpdateChecker.getCurrentExecutable();
        } catch (IllegalStateException e) {
            showAlert("Mise à jour", e.getMessage());
            return;
        }

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Mise à jour");
        progressAlert.setHeaderText("Téléchargement de Multitools " + info.latestVersion + "...");
        progressAlert.getDialogPane().setContent(new VBox(10, new Label("Progression :"), progressBar));
        progressAlert.initModality(Modality.APPLICATION_MODAL);
        progressAlert.show();

        Task<Path> download = new Task<>() {
            @Override
            protected Path call() throws Exception {
                // updateProgress est thread-safe ; une valeur négative = barre indéterminée
                return new UpdateChecker().downloadUpdate(info.downloadUrl,
                        p -> updateProgress(p < 0 ? -1 : p, 1));
            }
        };
        progressBar.progressProperty().bind(download.progressProperty());

        download.setOnSucceeded(e -> {
            progressAlert.close();
            try {
                UpdateChecker.installAndExit(currentExe, download.getValue());
            } catch (IOException ex) {
                showAlert("Erreur", "Impossible de lancer l'installation : " + ex.getMessage());
            }
        });
        download.setOnFailed(e -> {
            progressAlert.close();
            showAlert("Erreur", "Erreur lors du téléchargement : " + download.getException().getMessage());
        });

        Thread thread = new Thread(download, "multitools-update-download");
        thread.setDaemon(true);
        thread.start();
    }

    
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
