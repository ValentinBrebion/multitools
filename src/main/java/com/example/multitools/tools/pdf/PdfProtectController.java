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
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class PdfProtectController {

    @FXML
    private TextField inputPathField;
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button protectButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label statusLabel;

    private Path inputFile;

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
    private void protectPdf() {
        if (inputFile == null) {
            showError("Sélectionnez un fichier PDF à protéger.");
            return;
        }

        String password = passwordField.getText();
        if (password == null || password.isBlank()) {
            showError("Saisissez le nouveau mot de passe.");
            return;
        }
        if (!password.equals(confirmPasswordField.getText())) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le PDF protégé");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Document PDF", "*.pdf"));
        chooser.setInitialFileName(inputFile.getFileName().toString()
                .replaceFirst("(?i)\\.pdf$", "") + "-protege.pdf");
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
        String sourcePassword = currentPasswordField.getText();
        setBusy(true);
        statusLabel.setText("Protection du document PDF...");

        Task<Void> protectTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                new PdfProtectionService().protect(source, target, sourcePassword, password);
                return null;
            }
        };
        protectTask.setOnSucceeded(event -> {
            setBusy(false);
            statusLabel.setText("PDF protégé : " + target.getFileName());
            currentPasswordField.clear();
            passwordField.clear();
            confirmPasswordField.clear();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Protection PDF");
            alert.setHeaderText("Protection terminée");
            alert.setContentText("Le PDF protégé a été enregistré ici :\n" + target
                    + "\n\nConservez le mot de passe : il sera demandé à l'ouverture.");
            alert.showAndWait();
        });
        protectTask.setOnFailed(event -> {
            setBusy(false);
            Throwable error = protectTask.getException();
            showError("Impossible de protéger le PDF : "
                    + (error == null ? "erreur inconnue" : error.getMessage()));
        });
        Thread thread = new Thread(protectTask, "pdf-protect");
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
        protectButton.setDisable(busy);
        progressIndicator.setVisible(busy);
        progressIndicator.setManaged(busy);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("Protection PDF");
        alert.setHeaderText("Une erreur est survenue");
        alert.showAndWait();
    }
}
