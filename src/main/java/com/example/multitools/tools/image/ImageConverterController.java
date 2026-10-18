package com.example.multitools.tools.image;

import com.example.multitools.core.NavigationManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class ImageConverterController {

    @FXML
    private StackPane dropZone;

    @FXML
    private VBox emptyState;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private ComboBox<ImageOutputFormat> formatComboBox;

    @FXML
    private TextField destinationField;

    @FXML
    private Button convertButton;

    @FXML
    private Label statusLabel;

    private Path selectedSource;

    @FXML
    public void initialize() {
        formatComboBox.getItems().setAll(ImageOutputFormat.values());
        formatComboBox.setValue(ImageOutputFormat.WEBP);
        formatComboBox.valueProperty().addListener(
            (observable, previous, selected) -> updateDestinationExtension()
        );

        dropZone.setOnMouseClicked(event -> chooseSource());
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        dropZone.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                dropZone.setStyle("-fx-background-color: #eef7ff; -fx-border-color: #087edb;");
            }
            event.consume();
        });
        dropZone.setOnDragExited(event -> {
            setDropZoneStyle(false);
            event.consume();
        });
        dropZone.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean accepted = false;
            if (dragboard.hasFiles()) {
                accepted = handleDroppedFiles(dragboard.getFiles());
            }
            event.setDropCompleted(accepted);
            event.consume();
        });
        setDropZoneStyle(false);
    }

    @FXML
    private void chooseSource() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner une image");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(
                "Images",
                "*.png", "*.PNG", "*.jpg", "*.JPG", "*.jpeg", "*.JPEG",
                "*.gif", "*.GIF", "*.bmp", "*.BMP", "*.wbmp", "*.WBMP", "*.webp", "*.WEBP"
            )
        );
        File file = chooser.showOpenDialog(getPrimaryStage());
        if (file == null) {
            return;
        }
        setSource(file.toPath());
    }

    @FXML
    private void chooseDestination() {
        Path currentDestination = readPath(destinationField);
        FileChooser chooser = new FileChooser();
        ImageOutputFormat format = formatComboBox.getValue();
        chooser.setTitle("Enregistrer l'image " + format.getLabel());
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(
                "Image " + format.getLabel(),
                "*." + format.getExtension()
            )
        );
        if (currentDestination != null) {
            Path parent = currentDestination.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                chooser.setInitialDirectory(parent.toFile());
            }
            chooser.setInitialFileName(currentDestination.getFileName().toString());
        } else {
            chooser.setInitialFileName("image." + format.getExtension());
        }

        File file = chooser.showSaveDialog(getPrimaryStage());
        if (file == null) {
            return;
        }

        Path destination = file.toPath().toAbsolutePath().normalize();
        if (!hasExtension(destination, "." + format.getExtension())) {
            destination = replaceExtension(destination, format);
        }
        destinationField.setText(destination.toString());
        statusLabel.setText("Destination sélectionnée.");
    }

    @FXML
    private void convert() {
        Path source = selectedSource;
        Path destination = readPath(destinationField);
        ImageOutputFormat selectedFormat = formatComboBox.getValue();
        if (source == null || destination == null) {
            showError("Sélectionnez une image et un emplacement de destination.");
            return;
        }
        if (selectedFormat == null) {
            showError("Sélectionnez un format de sortie.");
            return;
        }
        if (Files.exists(destination) && !confirmOverwrite(destination)) {
            return;
        }

        convertButton.setDisable(true);
        statusLabel.setText("Conversion en cours…");
        Task<Void> conversion = new Task<>() {
            @Override
            protected Void call() throws Exception {
                new ImageConversionService().convert(source, destination, selectedFormat);
                return null;
            }
        };
        conversion.setOnSucceeded(event -> {
            convertButton.setDisable(false);
            statusLabel.setText("Conversion terminée : " + destination.getFileName());
        });
        conversion.setOnFailed(event -> {
            convertButton.setDisable(false);
            Throwable error = conversion.getException();
            statusLabel.setText("La conversion a échoué.");
            showError(error == null ? "Une erreur inconnue est survenue." : error.getMessage());
        });
        Thread conversionThread = new Thread(conversion, "image-conversion");
        conversionThread.setDaemon(true);
        conversionThread.start();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/image/image.fxml"));
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de revenir au menu Image");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private Path readPath(TextField field) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Path.of(value).toAbsolutePath().normalize();
        } catch (RuntimeException e) {
            showError("Le chemin de fichier saisi est invalide.");
            return null;
        }
    }

    private boolean handleDroppedFiles(List<File> files) {
        if (files.size() != 1) {
            showError("Déposez une seule image à la fois.");
            return false;
        }
        File file = files.get(0);
        if (!isSupportedImage(file.toPath())) {
            showError("Le fichier déposé n'a pas un format d'image pris en charge.");
            return false;
        }
        setSource(file.toPath());
        return true;
    }

    private void setSource(Path source) {
        selectedSource = source.toAbsolutePath().normalize();
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        selectedFileLabel.setText(selectedSource.getFileName().toString());
        selectedFileLabel.setVisible(true);
        selectedFileLabel.setManaged(true);
        Path destination = replaceExtension(selectedSource, formatComboBox.getValue());
        if (hasExtension(selectedSource, "." + formatComboBox.getValue().getExtension())) {
            String fileName = destination.getFileName().toString();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            destination = destination.resolveSibling(baseName + "-converted."
                + formatComboBox.getValue().getExtension());
        }
        destinationField.setText(destination.toString());
        statusLabel.setText("Image sélectionnée. Choisissez le format de sortie puis lancez la conversion.");
    }

    private void updateDestinationExtension() {
        if (formatComboBox == null || destinationField == null) {
            return;
        }
        Path current = readPathWithoutAlert(destinationField);
        if (current == null) {
            return;
        }
        destinationField.setText(replaceExtension(current, formatComboBox.getValue()).toString());
    }

    private Path replaceExtension(Path path, ImageOutputFormat format) {
        String fileName = path.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        return path.resolveSibling(baseName + "." + format.getExtension());
    }

    private boolean isSupportedImage(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
            || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".wbmp")
            || name.endsWith(".webp");
    }

    private boolean hasExtension(Path path, String extension) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension);
    }

    private Path readPathWithoutAlert(TextField field) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Path.of(value).toAbsolutePath().normalize();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void setDropZoneStyle(boolean active) {
        dropZone.setStyle(active
            ? "-fx-background-color: #eef7ff; -fx-border-color: #087edb; -fx-border-radius: 10; -fx-background-radius: 10;"
            : "-fx-background-color: white; -fx-border-color: #cbd5df; -fx-border-radius: 10; -fx-background-radius: 10;");
    }

    private boolean confirmOverwrite(Path destination) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer le remplacement");
        alert.setHeaderText("Le fichier existe déjà");
        alert.setContentText("Voulez-vous remplacer " + destination.getFileName() + " ?");
        ButtonType replace = new ButtonType("Remplacer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(replace, cancel);
        return alert.showAndWait().filter(replace::equals).isPresent();
    }

    private Stage getPrimaryStage() {
        return NavigationManager.getInstance().getPrimaryStage();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de conversion");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
