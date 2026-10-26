package com.example.multitools.tools.compression;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.update.UpdateChecker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    @FXML
    private Button addDirectoryButton;

    @FXML
    private ComboBox<String> outputFormatComboBox;

    @FXML
    private VBox compressionOptions;

    @FXML
    private Label formatsLabel;

    @FXML
    private Label pdfNoticeLabel;
    
    private List<Path> selectedFiles = new ArrayList<>();

    @FXML
    public void initialize() {
        versionLabel.setText("Version " + UpdateChecker.getCurrentVersion());

        compressButton.setDisable(true);
        outputFormatComboBox.getItems().setAll("ZIP", "PDF");
        outputFormatComboBox.setValue("ZIP");
        outputFormatComboBox.valueProperty().addListener(
                (observable, oldFormat, newFormat) -> handleOutputFormatChanged());

        // Configuration de la cell factory pour la ListView
        filesListView.setCellFactory(listView -> new FileListCell());

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
        List<Path> acceptedFiles = files.stream()
                .map(File::toPath)
                .filter(this::isAllowedForSelectedFormat)
                .toList();
        int rejectedCount = files.size() - acceptedFiles.size();

        addFilesToSelection(acceptedFiles);
        if (rejectedCount > 0) {
            updateStatus(rejectedCount + " fichier(s) ignoré(s) : seuls les fichiers .odt sont acceptés en PDF");
        } else if (!acceptedFiles.isEmpty()) {
            updateStatus(acceptedFiles.size() + " fichier(s) ajouté(s)");
        }
    }
    
    @FXML
    private void handleAddFiles() {
        FileChooser fileChooser = new FileChooser();
        boolean pdfMode = isPdfMode();
        fileChooser.setTitle(pdfMode
                ? "Sélectionner des fichiers ODT"
                : "Sélectionner des fichiers à compresser");
        if (pdfMode) {
            fileChooser.getExtensionFilters().setAll(
                    new FileChooser.ExtensionFilter("Documents OpenDocument (*.odt)", "*.odt"));
        }

        List<File> files = fileChooser.showOpenMultipleDialog(null);

        if (files != null && !files.isEmpty()) {
            List<Path> acceptedFiles = files.stream()
                    .map(File::toPath)
                    .filter(this::isAllowedForSelectedFormat)
                    .toList();
            addFilesToSelection(acceptedFiles);
            if (acceptedFiles.size() < files.size()) {
                updateStatus("Seuls les fichiers .odt sont acceptés en PDF");
            } else if (!acceptedFiles.isEmpty()) {
                updateStatus(acceptedFiles.size() + " fichier(s) ajouté(s)");
            }
        }
    }

    private void addFilesToSelection(List<Path> files) {
        selectedFiles.addAll(files);
        refreshFilesList();
    }

    private void refreshFilesList() {
        filesListView.getItems().setAll(selectedFiles.stream()
                .map(path -> java.nio.file.Files.isDirectory(path)
                        ? "[Dossier] " + path.getFileName()
                        : path.getFileName().toString())
                .toList());

        boolean hasFiles = !selectedFiles.isEmpty();
        emptyState.setVisible(!hasFiles);
        emptyState.setManaged(!hasFiles);
        filesListView.setVisible(hasFiles);
        filesListView.setManaged(hasFiles);
        compressButton.setDisable(!hasFiles);
    }

    private boolean isAllowedForSelectedFormat(Path path) {
        return !isPdfMode() || isOdtFile(path);
    }

    private boolean isOdtFile(Path path) {
        return !java.nio.file.Files.isDirectory(path)
                && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".odt");
    }

    private boolean isPdfMode() {
        return "PDF".equals(outputFormatComboBox.getValue());
    }

    private void handleOutputFormatChanged() {
        boolean pdfMode = isPdfMode();
        if (pdfMode) {
            selectedFiles.removeIf(path -> !isOdtFile(path));
        }

        compressionOptions.setVisible(!pdfMode);
        compressionOptions.setManaged(!pdfMode);
        addDirectoryButton.setDisable(pdfMode);
        formatsLabel.setText(pdfMode
                ? "Fichiers .odt uniquement"
                : "Tous les formats sont acceptés");
        pdfNoticeLabel.setText("Rendu texte et mise en page simple");
        pdfNoticeLabel.setVisible(pdfMode);
        pdfNoticeLabel.setManaged(pdfMode);
        compressButton.setText(pdfMode ? "Convertir en PDF" : "Compresser");
        refreshFilesList();
        compressionRatioLabel.setText("");
        updateStatus(pdfMode
                ? "Sélectionnez des fichiers ODT à convertir en PDF"
                : "Format ZIP sélectionné");
    }
    
    @FXML
    private void handleAddDirectory() {
        if (isPdfMode()) {
            updateStatus("En mode PDF, seuls les fichiers .odt peuvent être sélectionnés");
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Sélectionner un dossier à compresser");

        File directory = directoryChooser.showDialog(null);

        if (directory != null) {
            addFilesToSelection(List.of(directory.toPath()));

            updateStatus("Dossier ajouté : " + directory.getName());
        }
    }
    
    @FXML
    private void handleClearList() {
        selectedFiles.clear();
        refreshFilesList();

        compressionRatioLabel.setText("");

        updateStatus("Liste vidée");
    }
    
    @FXML
    private void handleCompress() {
        if (isPdfMode()) {
            handleConvertToPdf();
            return;
        }

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

    private void handleConvertToPdf() {
            if (selectedFiles.isEmpty()) {
                showAlert("Erreur", "Veuillez ajouter au moins un fichier ODT à convertir.");
                return;
            }

            List<Path> inputFiles = List.copyOf(selectedFiles);
            List<Path> outputFiles = new ArrayList<>();
            if (inputFiles.size() == 1) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Enregistrer le fichier PDF");
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
                String outputName = archiveNameField.getText().trim();
                if (outputName.isEmpty()) {
                    outputName = removeOdtExtension(inputFiles.get(0).getFileName().toString());
                }
                if (!outputName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                    outputName += ".pdf";
                }
                fileChooser.setInitialFileName(outputName);
                File outputFile = fileChooser.showSaveDialog(null);
                if (outputFile == null) {
                    return;
                }
                outputFiles.add(outputFile.toPath());
            } else {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Choisir le dossier de destination des PDF");
                File outputDirectory = directoryChooser.showDialog(null);
                if (outputDirectory == null) {
                    return;
                }
                for (Path inputFile : inputFiles) {
                    outputFiles.add(outputDirectory.toPath().resolve(
                            removeOdtExtension(inputFile.getFileName().toString()) + ".pdf"));
                }
                if (outputFiles.stream().distinct().count() != outputFiles.size()) {
                    showAlert("Noms de fichiers en double",
                            "Plusieurs documents ODT portent le même nom. Convertissez-les séparément pour éviter d'écraser un PDF.");
                    return;
                }
                List<Path> existingOutputs = outputFiles.stream()
                        .filter(java.nio.file.Files::exists)
                        .toList();
                if (!existingOutputs.isEmpty()) {
                    Alert overwriteAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    overwriteAlert.setTitle("Remplacer des fichiers ?");
                    overwriteAlert.setHeaderText("Certains PDF existent déjà");
                    overwriteAlert.setContentText("Voulez-vous remplacer les fichiers PDF existants ?");
                    if (overwriteAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                        return;
                    }
                }
            }

            compressButton.setDisable(true);
            Task<List<Path>> conversionTask = new Task<>() {
                @Override
                protected List<Path> call() throws Exception {
                    List<Path> convertedFiles = new ArrayList<>();
                    for (int index = 0; index < inputFiles.size(); index++) {
                        updateMessage("Conversion en cours (" + (index + 1) + "/" + inputFiles.size() + ")...");
                        OdtToPdfService.convert(inputFiles.get(index), outputFiles.get(index));
                        convertedFiles.add(outputFiles.get(index));
                    }
                    return convertedFiles;
                }
            };
            conversionTask.messageProperty().addListener(
                    (observable, oldMessage, newMessage) -> updateStatus(newMessage));
            conversionTask.setOnSucceeded(event -> {
                List<Path> convertedFiles = conversionTask.getValue();
                updateStatus("Conversion PDF terminée avec succès");
                compressionRatioLabel.setText(convertedFiles.size() + " fichier(s) PDF créé(s)");
                showAlert("Succès", convertedFiles.size() + " fichier(s) converti(s) en PDF.");
                compressButton.setDisable(selectedFiles.isEmpty());
            });
            conversionTask.setOnFailed(event -> {
                Throwable error = conversionTask.getException();
                updateStatus("Erreur lors de la conversion PDF");
                showAlert("Erreur de conversion",
                        error == null ? "La conversion ODT vers PDF a échoué." : error.getMessage());
                compressButton.setDisable(selectedFiles.isEmpty());
            });
            Thread conversionThread = new Thread(conversionTask, "odt-to-pdf-conversion");
            conversionThread.setDaemon(true);
            conversionThread.start();
        }

    private String removeOdtExtension(String fileName) {
        return fileName.substring(0, fileName.length() - ".odt".length());
    }
    
    @FXML
    private void handleRemoveSelected() {
        int selectedIndex = filesListView.getSelectionModel().getSelectedIndex();

        if (selectedIndex >= 0) {
            selectedFiles.remove(selectedIndex);
            refreshFilesList();

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

                Path currentExe = UpdateChecker.getCurrentExecutable();

                javafx.application.Platform.runLater(() -> {
                    progressStage.close();
                    updateStatus("Redémarrage pour terminer la mise à jour...");
                });

                UpdateChecker.installAndExit(currentExe, downloadedFile);

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

    // Classe interne pour les cellules personnalisées de la ListView
    private class FileListCell extends ListCell<String> {
        private final HBox content;
        private final Label iconLabel;
        private final Label nameLabel;
        private final Button deleteButton;

        public FileListCell() {
            super();

            iconLabel = new Label();
            iconLabel.setStyle("-fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 18px;");

            nameLabel = new Label();
            nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            deleteButton = new Button("✕");
            deleteButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #ef4444;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 4 8 4 8;" +
                "-fx-border-radius: 4;" +
                "-fx-background-radius: 4;"
            );
            deleteButton.setOnAction(event -> {
                int index = getIndex();
                if (index >= 0) {
                    selectedFiles.remove(index);
                    refreshFilesList();
                    updateStatus("Fichier supprimé");
                }
            });

            content = new HBox(10, iconLabel, nameLabel, spacer, deleteButton);
            content.setStyle("-fx-padding: 8 12 8 12;");
            content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item);
                iconLabel.setText(getFileIcon(item));
                setGraphic(content);
            }
        }

        private String getFileIcon(String fileName) {
            if (fileName.startsWith("[Dossier]")) {
                return "📁";
            }

            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".pdf")) return "📄";
            if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) return "📝";
            if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) return "📊";
            if (lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx")) return "📽";
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".gif") || lowerName.endsWith(".bmp")) return "🖼";
            if (lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".flac") || lowerName.endsWith(".ogg")) return "🎵";
            if (lowerName.endsWith(".mp4") || lowerName.endsWith(".avi") || lowerName.endsWith(".mkv") || lowerName.endsWith(".mov")) return "🎬";
            if (lowerName.endsWith(".zip") || lowerName.endsWith(".rar") || lowerName.endsWith(".7z") || lowerName.endsWith(".tar")) return "📦";
            if (lowerName.endsWith(".txt")) return "📃";
            if (lowerName.endsWith(".html") || lowerName.endsWith(".htm") || lowerName.endsWith(".css") || lowerName.endsWith(".js")) return "🌐";
            if (lowerName.endsWith(".java") || lowerName.endsWith(".class") || lowerName.endsWith(".jar")) return "☕";
            if (lowerName.endsWith(".py")) return "🐍";
            if (lowerName.endsWith(".cpp") || lowerName.endsWith(".c") || lowerName.endsWith(".h")) return "⚙";
            if (lowerName.endsWith(".exe") || lowerName.endsWith(".msi")) return "⚙";
            if (lowerName.endsWith(".xml") || lowerName.endsWith(".json")) return "📋";
            
            return "📄"; // Icône par défaut
        }
    }
}
