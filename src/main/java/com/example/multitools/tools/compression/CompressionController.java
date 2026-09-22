package com.example.multitools.tools.compression;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.update.UpdateChecker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CompressionController {
    
    @FXML
    private ListView<String> filesListView;

    @FXML
    private VBox emptyState;

    @FXML
    private StackPane dropZone;

    @FXML
    private Label statusLabel;

    @FXML
    private Label compressionRatioLabel;

    @FXML
    private Label versionLabel;

    @FXML
    private TextField archiveNameField;
    
    @FXML
    private Button compressButton;
    
    private List<Path> selectedFiles = new ArrayList<>();

    @FXML
    public void initialize() {
        versionLabel.setText("Version " + UpdateChecker.getCurrentVersion());

        compressButton.setDisable(true);

        dropZone.setOnMouseClicked(event -> {
            if (event.getTarget() != filesListView) {
                handleAddFiles();
            }
        });

        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                dropZone.setStyle(
                    "-fx-background-color: #eef7ff;" +
                    "-fx-border-color: #087edb;" +
                    "-fx-border-radius: 10;" +
                    "-fx-background-radius: 10;"
                );
            }
            event.consume();
        });

        dropZone.setOnDragExited(event -> {
            dropZone.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #cbd5df;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
            );
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();

            if (db.hasFiles()) {
                addDroppedFiles(db.getFiles());
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }

            event.consume();
        });
    }

    private void addDroppedFiles(List<File> files) {
        for (File file : files) {
            selectedFiles.add(file.toPath());
            filesListView.getItems().add(file.getName());
        }

        if (!files.isEmpty()) {
            emptyState.setVisible(false);
            emptyState.setManaged(false);

            filesListView.setVisible(true);
            filesListView.setManaged(true);

            updateStatus(files.size() + " fichier(s) ajouté(s)");
        }
    }
    
    @FXML
    private void handleAddFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner des fichiers à compresser");

        List<File> files = fileChooser.showOpenMultipleDialog(null);

        if (files != null) {
            for (File file : files) {
                selectedFiles.add(file.toPath());
                filesListView.getItems().add(file.getName());
            }

            // Cacher le message d'accueil
            emptyState.setVisible(false);
            emptyState.setManaged(false);

            // Afficher la liste
            filesListView.setVisible(true);
            filesListView.setManaged(true);

            compressButton.setDisable(false);

            updateStatus(files.size() + " fichier(s) ajouté(s)");
        }
    }
    
    @FXML
    private void handleAddDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Sélectionner un dossier à compresser");

        File directory = directoryChooser.showDialog(null);

        if (directory != null) {
            selectedFiles.add(directory.toPath());
            filesListView.getItems().add("[Dossier] " + directory.getName());

            emptyState.setVisible(false);
            emptyState.setManaged(false);

            filesListView.setVisible(true);
            filesListView.setManaged(true);

            compressButton.setDisable(false);

            updateStatus("Dossier ajouté : " + directory.getName());
        }
    }
    
    @FXML
    private void handleClearList() {
        selectedFiles.clear();
        filesListView.getItems().clear();

        emptyState.setVisible(true);
        emptyState.setManaged(true);

        filesListView.setVisible(false);
        filesListView.setManaged(false);

        compressionRatioLabel.setText("");

        compressButton.setDisable(true);

        updateStatus("Liste vidée");
    }
    
    @FXML
    private void handleCompress() {
        if (selectedFiles.isEmpty()) {
            showAlert("Erreur", "Veuillez ajouter des fichiers ou dossiers à compresser.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier ZIP");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichier ZIP", "*.zip")
        );
        String archiveName = archiveNameField.getText().trim();

        if (archiveName.isEmpty()) {
        archiveName = "archive";
        }

        if (!archiveName.toLowerCase().endsWith(".zip")) {
            archiveName += ".zip";
        }

        fileChooser.setInitialFileName(archiveName);
        File zipFile = fileChooser.showSaveDialog(null);
        
        if (zipFile != null) {
            try {
                updateStatus("Compression en cours...");
                ZipService.compressFiles(selectedFiles, zipFile.toPath());
                
                long ratio = ZipService.calculateCompressionRatio(selectedFiles, zipFile.toPath());
                compressionRatioLabel.setText("Taux de compression: " + ratio + "%");
                
                updateStatus("Compression terminée avec succès!");
                showAlert("Succès", "Fichiers compressés avec succès!\nEmplacement: " + zipFile.getAbsolutePath());
                
            } catch (Exception e) {
                updateStatus("Erreur lors de la compression");
                showAlert("Erreur", "Erreur lors de la compression: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void handleRemoveSelected() {
        int selectedIndex = filesListView.getSelectionModel().getSelectedIndex();

        if (selectedIndex >= 0) {
            selectedFiles.remove(selectedIndex);
            filesListView.getItems().remove(selectedIndex);

            if (filesListView.getItems().isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);

                filesListView.setVisible(false);
                filesListView.setManaged(false);
            }

            updateStatus("Élément supprimé");
        }
    }
    
    @FXML
    private void handleUpdateCheck() {
        updateStatus("Recherche de mises à jour...");
        
        Thread updateThread = new Thread(() -> {
            UpdateChecker checker = new UpdateChecker();
            UpdateChecker.UpdateInfo updateInfo = checker.checkForUpdates();
            
            javafx.application.Platform.runLater(() -> {
                if (updateInfo.hasUpdate) {
                    showUpdateDialog(updateInfo);
                } else {
                    showAlert("Mise à jour", "Vous utilisez déjà la dernière version de Multitools (" + 
                            UpdateChecker.getCurrentVersion() + ")");
                    updateStatus("Aucune mise à jour disponible");
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
        
        ButtonType installButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(installButton, cancelButton);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == installButton) {
                installUpdate(updateInfo);
            }
        });
    }

    /**
     * Télécharge le nouvel exécutable avec une popup de progression, puis
     * déclenche le remplacement + relance de l'application.
     */
    private void installUpdate(UpdateChecker.UpdateInfo updateInfo) {
        if (updateInfo.downloadUrl == null) {
            showAlert("Erreur", "Aucun lien de téléchargement disponible pour cette version.");
            return;
        }

        // Popup de progression
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("Téléchargement de la mise à jour");
        progressStage.setResizable(false);

        Label progressLabel = new Label("Téléchargement en cours...");
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(320);

        VBox progressBox = new VBox(12, progressLabel, progressBar);
        progressBox.setPadding(new Insets(20));

        progressStage.setScene(new Scene(progressBox));
        progressStage.show();

        updateStatus("Téléchargement de la mise à jour...");

        Thread downloadThread = new Thread(() -> {
            UpdateChecker checker = new UpdateChecker();

            try {
                Path downloadedFile = checker.downloadUpdate(updateInfo.downloadUrl, progress -> {
                    javafx.application.Platform.runLater(() -> {
                        if (progress < 0) {
                            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                        } else {
                            progressBar.setProgress(progress);
                            progressLabel.setText("Téléchargement en cours... " + (int) (progress * 100) + "%");
                        }
                    });
                });

                Path currentExe = UpdateChecker.getCurrentExecutablePath();
                Path updateScript = checker.createUpdateScript(currentExe, downloadedFile);

                javafx.application.Platform.runLater(() -> {
                    progressStage.close();
                    updateStatus("Redémarrage pour terminer la mise à jour...");
                });

                // Laisse le temps à l'UI de se fermer proprement avant de tuer le process
                Thread.sleep(300);

                UpdateChecker.runUpdateScriptAndExit(updateScript);

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    progressStage.close();
                    showAlert("Erreur", "Erreur lors du téléchargement de la mise à jour: " + e.getMessage());
                    updateStatus("Erreur lors de la mise à jour");
                });
            }
        });

        downloadThread.setDaemon(true);
        downloadThread.start();
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    @FXML
    private void handleBack() {
        try {
            // Revenir au menu principal
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/main.fxml")
            );
            loader.setControllerFactory(param -> new com.example.multitools.MainController());
            
            Parent root = loader.load();
            
            // Naviguer vers le menu principal
            NavigationManager.getInstance().navigateTo(root);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de revenir au menu principal: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
