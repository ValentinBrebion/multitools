package com.example.multitools;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.core.ToolManager;
import com.example.multitools.tools.compression.CompressionTool;
import com.example.multitools.tools.converter.UnitConverterTool;
import com.example.multitools.tools.image.ImageTool;
import com.example.multitools.tools.text.TextTool;
import com.example.multitools.update.UpdateChecker;
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
    private void openConverterTool() {
        UnitConverterTool converterTool = new UnitConverterTool();
        converterTool.open();
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

    private void startUpdateProcess(com.example.multitools.update.UpdateChecker.UpdateInfo updateInfo) {
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Téléchargement de la mise à jour");
        progressAlert.setHeaderText("Téléchargement en cours...");
        progressAlert.setContentText("Veuillez patienter pendant le téléchargement de la mise à jour.");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);

        VBox content = new VBox(10, new Label("Progression:"), progressBar);
        progressAlert.getDialogPane().setContent(content);
        progressAlert.initModality(Modality.APPLICATION_MODAL);
        progressAlert.show();

        Thread downloadThread = new Thread(() -> {
            try {
                com.example.multitools.update.UpdateChecker checker = new com.example.multitools.update.UpdateChecker();
                java.nio.file.Path downloadedFile = checker.downloadUpdate(updateInfo.downloadUrl, progress -> {
                    javafx.application.Platform.runLater(() -> {
                        if (progress >= 0) {
                            progressBar.setProgress(progress);
                        } else {
                            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                        }
                    });
                });

                javafx.application.Platform.runLater(() -> {
                    progressAlert.close();
                    showAlert("Téléchargement terminé", "Le fichier a été téléchargé avec succès. L'application va maintenant s'installer et redémarrer.");

                    Thread installThread = new Thread(() -> {
                        try {
                            String currentExePath = com.example.multitools.update.UpdateChecker.getCurrentExecutablePath();
                            java.nio.file.Path currentExe = java.nio.file.Path.of(currentExePath);
                            java.nio.file.Path scriptPath = checker.createUpdateScript(currentExe, downloadedFile);
                            com.example.multitools.update.UpdateChecker.runUpdateScriptAndExit(scriptPath);
                        } catch (Exception e) {
                            e.printStackTrace();
                            javafx.application.Platform.runLater(() -> {
                                showAlert("Erreur", "Erreur lors de l'installation: " + e.getMessage());
                            });
                        }
                    });
                    installThread.setDaemon(true);
                    installThread.start();
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    progressAlert.close();
                    showAlert("Erreur", "Erreur lors du téléchargement: " + e.getMessage());
                });
            }
        });
        downloadThread.setDaemon(true);
        downloadThread.start();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
