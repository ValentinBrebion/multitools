package com.example.multitools.tools.image;

import com.example.multitools.core.NavigationManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class ImageBackgroundRemovalController {

    private static final int PREVIEW_MAX_WIDTH = 400;
    private static final int PREVIEW_MAX_HEIGHT = 220;
    private static final int BACKGROUND_COLOR_TOLERANCE = 12;

    @FXML
    private StackPane dropZone;
    @FXML
    private VBox emptyState;
    @FXML
    private Label selectedFileLabel;
    @FXML
    private Canvas previewCheckerboard;
    @FXML
    private ImageView previewImageView;
    @FXML
    private Label previewPlaceholder;
    @FXML
    private TextField destinationField;
    @FXML
    private Button removeButton;
    @FXML
    private Label statusLabel;

    private Path selectedSource;
    private BufferedImage sourcePreview;

    @FXML
    public void initialize() {
        drawPreviewCheckerboard();
        dropZone.setOnMouseClicked(event -> {
            if (emptyState.isVisible()) {
                chooseSource();
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
                setDropZoneStyle(true);
            }
            event.consume();
        });
        dropZone.setOnDragExited(event -> {
            setDropZoneStyle(false);
            event.consume();
        });
        dropZone.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean accepted = dragboard.hasFiles() && handleDroppedFiles(dragboard.getFiles());
            event.setDropCompleted(accepted);
            event.consume();
        });
        setDropZoneStyle(false);
    }

    @FXML
    private void chooseSource() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner une image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            "Images", "*.png", "*.PNG", "*.jpg", "*.JPG", "*.jpeg", "*.JPEG",
            "*.gif", "*.GIF", "*.bmp", "*.BMP", "*.webp", "*.WEBP"
        ));
        File file = chooser.showOpenDialog(getPrimaryStage());
        if (file != null) {
            setSource(file.toPath());
        }
    }

    @FXML
    private void chooseDestination() {
        if (selectedSource == null) {
            showError("Sélectionnez d'abord une image.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer l'image avec fond transparent");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image PNG", "*.png", "*.PNG")
        );
        Path currentDestination = readDestination();
        if (currentDestination != null) {
            Path parent = currentDestination.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                chooser.setInitialDirectory(parent.toFile());
            }
            chooser.setInitialFileName(currentDestination.getFileName().toString());
        } else {
            chooser.setInitialFileName(getDestinationName(selectedSource));
        }
        File file = chooser.showSaveDialog(getPrimaryStage());
        if (file != null) {
            Path destination = file.toPath().toAbsolutePath().normalize();
            if (!destination.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
                destination = destination.resolveSibling(destination.getFileName() + ".png");
            }
            destinationField.setText(destination.toString());
            statusLabel.setText("Destination sélectionnée.");
        }
    }

    @FXML
    private void removeBackground() {
        Path source = selectedSource;
        Path destination = readDestination();
        if (source == null || destination == null) {
            showError("Sélectionnez une image et un emplacement de destination.");
            return;
        }
        if (Files.exists(destination) && !confirmOverwrite(destination)) {
            return;
        }

        removeButton.setDisable(true);
        statusLabel.setText("Retrait du fond blanc en cours…");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                new ImageBackgroundRemovalService()
                    .removeBackground(source, destination, BACKGROUND_COLOR_TOLERANCE);
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            removeButton.setDisable(false);
            statusLabel.setText("Image enregistrée : " + destination.getFileName());
            updatePreview();
        });
        task.setOnFailed(event -> {
            removeButton.setDisable(false);
            statusLabel.setText("Le retrait du fond a échoué.");
            Throwable error = task.getException();
            showError(error == null ? "Une erreur inconnue est survenue." : error.getMessage());
        });
        Thread thread = new Thread(task, "image-background-removal");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/image/image.fxml"));
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            showError("Impossible de revenir au menu Image : " + e.getMessage());
        }
    }

    private boolean handleDroppedFiles(List<File> files) {
        if (files.size() != 1) {
            showError("Déposez une seule image à la fois.");
            return false;
        }
        Path path = files.get(0).toPath();
        if (!isSupportedImage(path)) {
            showError("Le fichier sélectionné n'est pas une image prise en charge.");
            return false;
        }
        setSource(path);
        return true;
    }

    private void setSource(Path source) {
        selectedSource = source.toAbsolutePath().normalize();
        Path previewSource = selectedSource;
        sourcePreview = null;
        previewImageView.setImage(null);
        previewPlaceholder.setText("Chargement de l'aperçu…");
        previewPlaceholder.setVisible(true);
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        selectedFileLabel.setText(selectedSource.getFileName().toString());
        selectedFileLabel.setVisible(true);
        selectedFileLabel.setManaged(true);
        destinationField.setText(selectedSource.resolveSibling(getDestinationName(selectedSource)).toString());
        removeButton.setDisable(true);
        statusLabel.setText("Chargement de l'image pour l'aperçu…");

        Task<BufferedImage> previewTask = new Task<>() {
            @Override
            protected BufferedImage call() throws Exception {
                BufferedImage image = ImageIO.read(previewSource.toFile());
                if (image == null) {
                    throw new IOException("Le fichier sélectionné ne contient pas une image valide.");
                }
                BufferedImage result = new ImageBackgroundRemovalService()
                    .removeBackground(image, BACKGROUND_COLOR_TOLERANCE);
                double scale = Math.min(
                    (double) PREVIEW_MAX_WIDTH / result.getWidth(),
                    (double) PREVIEW_MAX_HEIGHT / result.getHeight()
                );
                int width = Math.max(1, (int) Math.round(result.getWidth() * scale));
                int height = Math.max(1, (int) Math.round(result.getHeight() * scale));
                BufferedImage preview = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = preview.createGraphics();
                try {
                    graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC
                    );
                    graphics.drawImage(result, 0, 0, width, height, null);
                } finally {
                    graphics.dispose();
                }
                return preview;
            }
        };
        previewTask.setOnSucceeded(event -> {
            if (!previewSource.equals(selectedSource)) {
                return;
            }
            sourcePreview = previewTask.getValue();
            removeButton.setDisable(false);
            statusLabel.setText("Arrière-plan retiré dans l'aperçu. Enregistrez le PNG pour conserver le résultat.");
            updatePreview();
        });
        previewTask.setOnFailed(event -> {
            if (!previewSource.equals(selectedSource)) {
                return;
            }
            previewPlaceholder.setText("Impossible de prévisualiser cette image.");
            removeButton.setDisable(true);
            Throwable error = previewTask.getException();
            showError("Impossible de charger l'aperçu : "
                + (error == null ? "erreur inconnue" : error.getMessage()));
        });
        Thread thread = new Thread(previewTask, "image-background-preview");
        thread.setDaemon(true);
        thread.start();
    }

    private void updatePreview() {
        if (sourcePreview == null) {
            return;
        }
        int width = sourcePreview.getWidth();
        int height = sourcePreview.getHeight();
        int[] pixels = sourcePreview.getRGB(0, 0, width, height, null, 0, width);
        WritableImage image = new WritableImage(width, height);
        image.getPixelWriter().setPixels(
            0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width
        );
        previewImageView.setImage(image);
        previewPlaceholder.setVisible(false);
    }

    private void drawPreviewCheckerboard() {
        var graphics = previewCheckerboard.getGraphicsContext2D();
        int tileSize = 16;
        for (int y = 0; y < PREVIEW_MAX_HEIGHT; y += tileSize) {
            for (int x = 0; x < PREVIEW_MAX_WIDTH; x += tileSize) {
                boolean light = ((x / tileSize) + (y / tileSize)) % 2 == 0;
                graphics.setFill(light
                    ? javafx.scene.paint.Color.WHITE
                    : javafx.scene.paint.Color.web("#e2e8f0"));
                graphics.fillRect(x, y, tileSize, tileSize);
            }
        }
    }

    private boolean isSupportedImage(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
            || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp");
    }

    private String getDestinationName(Path source) {
        String fileName = source.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
        return baseName + "_sans_fond.png";
    }

    private Path readDestination() {
        String value = destinationField.getText();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Path.of(value).toAbsolutePath().normalize();
        } catch (RuntimeException e) {
            showError("Le chemin de destination est invalide.");
            return null;
        }
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

    private void setDropZoneStyle(boolean active) {
        dropZone.setStyle(active
            ? "-fx-background-color: #eef7ff; -fx-border-color: #087edb; -fx-border-radius: 10; -fx-background-radius: 10;"
            : "-fx-background-color: white; -fx-border-color: #cbd5df; -fx-border-radius: 10; -fx-background-radius: 10;");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de retrait du fond");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
