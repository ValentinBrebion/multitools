package com.example.multitools.tools.pdf;

import java.io.File;
import java.nio.file.Path;

import com.example.multitools.core.NavigationManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class PdfStampController {

    @FXML
    private TextField inputPathField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<String> stampChoice;
    @FXML
    private Button stampButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label statusLabel;

    private Path inputFile;

    @FXML
    private void initialize() {
        stampChoice.getItems().setAll("CONFIDENTIEL", "BROUILLON");
        stampChoice.getSelectionModel().selectFirst();
    }

    @FXML
    private void chooseInput() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner un fichier PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documents PDF", "*.pdf", "*.PDF"));
        File selected = chooser.showOpenDialog(NavigationManager.getInstance().getPrimaryStage());
        if (selected != null) {
            inputFile = selected.toPath();
            inputPathField.setText(inputFile.toString());
            statusLabel.setText("");
        }
    }

    @FXML
    private void stampPdf() {
        if (inputFile == null) {
            showError("Sélectionnez un fichier PDF à tamponner.");
            return;
        }

        String stampText = stampChoice.getValue();
        if (stampText == null) {
            showError("Choisissez un tampon.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le PDF tamponné");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Document PDF", "*.pdf"));
        chooser.setInitialFileName(inputFile.getFileName().toString()
                .replaceFirst("(?i)\\.pdf$", "") + "-" + stampText.toLowerCase() + ".pdf");
        File destination = chooser.showSaveDialog(NavigationManager.getInstance().getPrimaryStage());
        if (destination == null) {
            return;
        }

        Path output = destination.toPath();
        if (!output.getFileName().toString().toLowerCase().endsWith(".pdf")) {
            output = output.resolveSibling(output.getFileName() + ".pdf");
        }

        Path source = inputFile;
        Path target = output;
        String currentPassword = passwordField.getText();
        setBusy(true);
        statusLabel.setText("Ajout du tampon sur chaque page...");

        Task<Void> stampTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                new PdfStampService().stamp(source, target, currentPassword, stampText);
                return null;
            }
        };
        stampTask.setOnSucceeded(event -> {
            setBusy(false);
            passwordField.clear();
            statusLabel.setText("Tampon ajouté : " + target.getFileName());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Tampon PDF");
            alert.setHeaderText("Tampon ajouté");
            alert.setContentText("Le document a été enregistré ici :\n" + target);
            alert.showAndWait();
        });
        stampTask.setOnFailed(event -> {
            setBusy(false);
            Throwable error = stampTask.getException();
            showError("Impossible d'ajouter le tampon : "
                    + (error == null ? "erreur inconnue" : error.getMessage()));
        });
        Thread thread = new Thread(stampTask, "pdf-stamp");
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

    private void setBusy(boolean busy) {
        stampButton.setDisable(busy);
        inputPathField.setDisable(busy);
        passwordField.setDisable(busy);
        stampChoice.setDisable(busy);
        progressIndicator.setVisible(busy);
        progressIndicator.setManaged(busy);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("Tampon PDF");
        alert.setHeaderText("Une erreur est survenue");
        alert.showAndWait();
    }
}
