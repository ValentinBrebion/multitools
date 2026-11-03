package com.example.multitools.tools.image;

import com.example.multitools.core.NavigationManager;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

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

public class ImageColorExtractionController {

    private static final int PREVIEW_MAX_WIDTH = 640;
    private static final int PREVIEW_MAX_HEIGHT = 320;
    private static final int DEFAULT_COLOR_COUNT = 6;

    @FXML
    private StackPane dropZone;
    @FXML
    private VBox emptyState;
    @FXML
    private ImageView imagePreview;
    @FXML
    private Canvas previewCheckerboard;
    @FXML
    private Label selectedFileLabel;
    @FXML
    private Slider colorCountSlider;
    @FXML
    private Label colorCountLabel;
    @FXML
    private FlowPane paletteFlow;
    @FXML
    private Button copyAllButton;
    @FXML
    private Label statusLabel;

    private Path selectedSource;
    private PauseTransition refreshDelay;

    @FXML
    public void initialize() {
        drawCheckerboard();
        refreshDelay = new PauseTransition(Duration.millis(250));
        refreshDelay.setOnFinished(event -> extractPalette());
        colorCountSlider.setValue(DEFAULT_COLOR_COUNT);
        updateColorCountLabel();
        colorCountSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateColorCountLabel();
            if (selectedSource != null) {
                refreshDelay.playFromStart();
            }
        });
        dropZone.setCursor(javafx.scene.Cursor.HAND);
        dropZone.setOnMouseClicked(event -> {
            if (emptyState.isVisible()) {
                chooseImage();
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
    private void chooseImage() {
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
    private void copyAllColors() {
        String palette = paletteFlow.getChildren().stream()
            .filter(ColorCard.class::isInstance)
            .map(ColorCard.class::cast)
            .map(card -> card.color().hex())
            .reduce((first, second) -> first + System.lineSeparator() + second)
            .orElse("");
        if (palette.isEmpty()) {
            return;
        }
        copyToClipboard(palette);
        statusLabel.setText("Palette copiée dans le presse-papiers.");
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/image/image.fxml"));
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            showError("Impossible de revenir au menu Image : " + rootMessage(e));
        }
    }

    private boolean handleDroppedFiles(List<File> files) {
        if (files.size() != 1) {
            showError("Déposez une seule image à la fois.");
            return false;
        }
        Path source = files.get(0).toPath();
        if (!isSupportedImage(source)) {
            showError("Le fichier sélectionné n'est pas une image prise en charge.");
            return false;
        }
        setSource(source);
        return true;
    }

    private void setSource(Path source) {
        refreshDelay.stop();
        selectedSource = source.toAbsolutePath().normalize();
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        imagePreview.setImage(null);
        imagePreview.setVisible(true);
        selectedFileLabel.setText(selectedSource.getFileName().toString());
        selectedFileLabel.setVisible(true);
        paletteFlow.getChildren().clear();
        copyAllButton.setDisable(true);
        statusLabel.setText("Analyse de l'image…");
        extractPalette();
    }

    private void extractPalette() {
        Path source = selectedSource;
        if (source == null) {
            return;
        }
        int requestedCount = getSelectedColorCount();
        statusLabel.setText("Extraction des couleurs…");
        paletteFlow.getChildren().clear();
        copyAllButton.setDisable(true);

        Task<ExtractionResult> task = new Task<>() {
            @Override
            protected ExtractionResult call() throws Exception {
                BufferedImage input = ImageIO.read(source.toFile());
                if (input == null) {
                    throw new IOException("Le fichier ne contient pas une image prise en charge.");
                }
                List<ImageColorExtractionService.ExtractedColor> colors =
                    new ImageColorExtractionService().extractColors(input, requestedCount);
                return new ExtractionResult(createThumbnail(input), colors);
            }
        };
        task.setOnSucceeded(event -> {
            if (!source.equals(selectedSource) || requestedCount != getSelectedColorCount()) {
                return;
            }
            ExtractionResult result = task.getValue();
            imagePreview.setImage(toFxImage(result.thumbnail()));
            displayPalette(result.colors());
            if (result.colors().isEmpty()) {
                statusLabel.setText("Aucune couleur opaque n'a été trouvée dans cette image.");
                return;
            }
            copyAllButton.setDisable(false);
            statusLabel.setText(result.colors().size() + " couleurs extraites. Cliquez sur une couleur pour copier son code HEX.");
        });
        task.setOnFailed(event -> {
            if (!source.equals(selectedSource) || requestedCount != getSelectedColorCount()) {
                return;
            }
            statusLabel.setText("L'extraction des couleurs a échoué.");
            Throwable error = task.getException();
            showError(error == null ? "Une erreur inconnue est survenue." : rootMessage(error));
        });
        Thread thread = new Thread(task, "image-color-extraction");
        thread.setDaemon(true);
        thread.start();
    }

    private BufferedImage createThumbnail(BufferedImage input) {
        double scale = Math.min(
            (double) PREVIEW_MAX_WIDTH / input.getWidth(),
            (double) PREVIEW_MAX_HEIGHT / input.getHeight()
        );
        int width = Math.max(1, (int) Math.round(input.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(input.getHeight() * scale));
        BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = thumbnail.createGraphics();
        try {
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.drawImage(input, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return thumbnail;
    }

    private WritableImage toFxImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int[] pixels = bufferedImage.getRGB(0, 0, width, height, null, 0, width);
        WritableImage image = new WritableImage(width, height);
        image.getPixelWriter().setPixels(
            0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width
        );
        return image;
    }

    private void displayPalette(List<ImageColorExtractionService.ExtractedColor> colors) {
        paletteFlow.getChildren().clear();
        for (ImageColorExtractionService.ExtractedColor color : colors) {
            ColorCard card = new ColorCard(color);
            card.setOnMouseClicked(event -> {
                copyToClipboard(color.hex());
                statusLabel.setText(color.hex() + " copié dans le presse-papiers.");
            });
            Tooltip.install(card, new Tooltip("Cliquer pour copier " + color.hex()));
            paletteFlow.getChildren().add(card);
        }
    }

    private void drawCheckerboard() {
        var graphics = previewCheckerboard.getGraphicsContext2D();
        int tileSize = 16;
        for (int y = 0; y < (int) previewCheckerboard.getHeight(); y += tileSize) {
            for (int x = 0; x < (int) previewCheckerboard.getWidth(); x += tileSize) {
                graphics.setFill(((x / tileSize) + (y / tileSize)) % 2 == 0
                    ? Color.WHITE
                    : Color.web("#e2e8f0"));
                graphics.fillRect(x, y, tileSize, tileSize);
            }
        }
    }

    private int getSelectedColorCount() {
        return (int) Math.round(colorCountSlider.getValue());
    }

    private void updateColorCountLabel() {
        colorCountLabel.setText(getSelectedColorCount() + " couleurs");
    }

    private boolean isSupportedImage(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
            || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp");
    }

    private void copyToClipboard(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private Stage getPrimaryStage() {
        return NavigationManager.getInstance().getPrimaryStage();
    }

    private void setDropZoneStyle(boolean active) {
        dropZone.setStyle(active
            ? "-fx-background-color: #eef7ff; -fx-border-color: #087edb; -fx-border-radius: 10; -fx-background-radius: 10;"
            : "-fx-background-color: white; -fx-border-color: #cbd5df; -fx-border-radius: 10; -fx-background-radius: 10;");
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null || cause.getMessage().isBlank()
            ? cause.getClass().getSimpleName()
            : cause.getMessage();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Extraction des couleurs");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static final class ColorCard extends VBox {

        private final ImageColorExtractionService.ExtractedColor color;

        private ColorCard(ImageColorExtractionService.ExtractedColor color) {
            this.color = color;
            setAlignment(javafx.geometry.Pos.TOP_LEFT);
            setSpacing(7);
            setPrefWidth(136);
            setStyle("-fx-padding: 8; -fx-background-color: white; -fx-background-radius: 8;"
                + "-fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-cursor: hand;");
            Rectangle swatch = new Rectangle(118, 54, Color.rgb(color.red(), color.green(), color.blue()));
            swatch.setArcWidth(8);
            swatch.setArcHeight(8);
            Label hexLabel = new Label(color.hex());
            hexLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
            Label rgbLabel = new Label(color.rgb());
            rgbLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            getChildren().addAll(swatch, hexLabel, rgbLabel);
        }

        private ImageColorExtractionService.ExtractedColor color() {
            return color;
        }
    }

    private record ExtractionResult(
        BufferedImage thumbnail,
        List<ImageColorExtractionService.ExtractedColor> colors
    ) {
    }
}
