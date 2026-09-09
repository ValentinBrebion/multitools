package com.example.javazip.controller;

import com.example.javazip.service.ZipService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MainController {
    
    @FXML
    private ListView<String> filesListView;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label compressionRatioLabel;
    
    private List<Path> selectedFiles = new ArrayList<>();
    
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
            updateStatus("Dossier ajouté: " + directory.getName());
        }
    }
    
    @FXML
    private void handleClearList() {
        selectedFiles.clear();
        filesListView.getItems().clear();
        compressionRatioLabel.setText("");
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
            updateStatus("Élément supprimé");
        }
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
