package com.example.multitools.tools.pdf;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.example.multitools.core.NavigationManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

public class PdfMergeController {

    @FXML
    private ListView<Path> fileList;
    @FXML
    private Button addButton;
    @FXML
    private Button removeButton;
    @FXML
    private Button upButton;
    @FXML
    private Button downButton;
    @FXML
    private Button mergeButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        fileList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Path path, boolean empty) {
                super.updateItem(path, empty);
                setText(empty || path == null ? null : path.getFileName().toString());
            }
        });
        fileList.getSelectionModel().selectedIndexProperty().addListener(
                (observable, oldIndex, newIndex) -> updateSelectionButtons());
        updateSelectionButtons();
    }

    @FXML
    private void addFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner des fichiers PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documents PDF", "*.pdf", "*.PDF"));
        List<File> selectedFiles = chooser.showOpenMultipleDialog(
                NavigationManager.getInstance().getPrimaryStage());
        if (selectedFiles != null) {
            selectedFiles.stream()
                    .map(File::toPath)
                    .filter(path -> !fileList.getItems().contains(path))
                    .forEach(fileList.getItems()::add);
        }
    }

    @FXML
    private void removeSelected() {
        int selectedIndex = fileList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            fileList.getItems().remove(selectedIndex);
        }
    }

    @FXML
    private void moveUp() {
        moveSelected(-1);
    }

    @FXML
    private void moveDown() {
        moveSelected(1);
    }

    @FXML
    private void mergeFiles() {
        if (fileList.getItems().size() < 2) {
            showError("Sélectionnez au moins deux fichiers PDF.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le PDF fusionné");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Document PDF", "*.pdf"));
        chooser.setInitialFileName("documents-fusionnes.pdf");
        File destination = chooser.showSaveDialog(NavigationManager.getInstance().getPrimaryStage());
        if (destination == null) {
            return;
        }

        Path output = destination.toPath();
        if (!output.getFileName().toString().toLowerCase().endsWith(".pdf")) {
            output = output.resolveSibling(output.getFileName() + ".pdf");
        }
        Path finalOutput = output;
        List<Path> inputs = fileList.getItems().stream().collect(Collectors.toList());
        setBusy(true);
        statusLabel.setText("Fusion des fichiers PDF...");

        Task<Void> mergeTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                new PdfMergeService().merge(inputs, finalOutput);
                return null;
            }
        };
        mergeTask.setOnSucceeded(event -> {
            setBusy(false);
            statusLabel.setText("Fusion terminée : " + finalOutput.getFileName());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Fusion PDF");
            alert.setHeaderText("Fusion terminée");
            alert.setContentText("Le fichier a été enregistré ici :\n" + finalOutput);
            alert.showAndWait();
        });
        mergeTask.setOnFailed(event -> {
            setBusy(false);
            Throwable error = mergeTask.getException();
            showError("Impossible de fusionner les PDF : "
                    + (error == null ? "erreur inconnue" : error.getMessage()));
        });
        Thread thread = new Thread(mergeTask, "pdf-merge");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pdf/pdf.fxml"));
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            showError("Impossible de revenir au menu PDF : " + e.getMessage());
        }
    }

    private void moveSelected(int offset) {
        int index = fileList.getSelectionModel().getSelectedIndex();
        int targetIndex = index + offset;
        if (index < 0 || targetIndex < 0 || targetIndex >= fileList.getItems().size()) {
            return;
        }
        Path selected = fileList.getItems().remove(index);
        fileList.getItems().add(targetIndex, selected);
        fileList.getSelectionModel().select(targetIndex);
    }

    private void updateSelectionButtons() {
        int index = fileList.getSelectionModel().getSelectedIndex();
        int size = fileList.getItems().size();
        removeButton.setDisable(index < 0);
        upButton.setDisable(index <= 0);
        downButton.setDisable(index < 0 || index >= size - 1);
    }

    private void setBusy(boolean busy) {
        addButton.setDisable(busy);
        removeButton.setDisable(busy);
        upButton.setDisable(busy);
        downButton.setDisable(busy);
        mergeButton.setDisable(busy);
        fileList.setDisable(busy);
        progressIndicator.setVisible(busy);
        progressIndicator.setManaged(busy);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Fusion PDF");
        alert.setHeaderText("Une erreur est survenue");
        alert.showAndWait();
    }
}
