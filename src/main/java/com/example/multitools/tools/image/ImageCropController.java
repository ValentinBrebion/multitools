package com.example.multitools.tools.image;

import com.example.multitools.core.NavigationManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class ImageCropController {

    private static final double PREVIEW_MAX_WIDTH = 640;
    private static final double PREVIEW_MAX_HEIGHT = 360;
    private static final double HANDLE_SIZE = 14;
    private static final double HANDLE_HIT_RADIUS = 18;
    private static final double MIN_SELECTION_SIZE = 2;
    private static final int IDLE = -2;
    private static final int NEW_SELECTION = -1;
    private static final int MOVE_SELECTION = 8;

    @FXML
    private StackPane dropZone;

    @FXML
    private VBox emptyState;

    @FXML
    private Pane imageCanvas;

    @FXML
    private ImageView previewImageView;

    @FXML
    private Rectangle selectionRectangle;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private Label dimensionsLabel;

    @FXML
    private TextField destinationField;

    @FXML
    private Button cropButton;

    @FXML
    private Label statusLabel;

    private final Rectangle[] handles = new Rectangle[8];
    private Path selectedSource;
    private int sourceWidth;
    private int sourceHeight;
    private double imageWidth;
    private double imageHeight;
    private double selectionX;
    private double selectionY;
    private double selectionWidth;
    private double selectionHeight;
    private double pressX;
    private double pressY;
    private double startX;
    private double startY;
    private double startWidth;
    private double startHeight;
    private int activeHandle = IDLE;
    private int cropX;
    private int cropY;
    private int cropWidth;
    private int cropHeight;

    @FXML
    public void initialize() {
        dropZone.setOnMouseClicked(event -> {
            if (emptyState.isVisible()) {
                chooseSource();
            }
        });
        for (int index = 0; index < handles.length; index++) {
            Rectangle handle = new Rectangle(HANDLE_SIZE, HANDLE_SIZE);
            handle.setArcWidth(3);
            handle.setArcHeight(3);
            handle.setFill(javafx.scene.paint.Color.WHITE);
            handle.setStroke(javafx.scene.paint.Color.web("#087edb"));
            handle.setStrokeWidth(2);
            handle.setMouseTransparent(true);
            handles[index] = handle;
            imageCanvas.getChildren().add(handle);
        }

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
        imageCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::beginAdjustment);
        imageCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::adjustSelection);
        imageCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::finishAdjustment);
        setDropZoneStyle(false);
    }

    @FXML
    private void chooseSource() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner une image à recadrer");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            "Images prises en charge",
            "*.png", "*.PNG", "*.jpg", "*.JPG", "*.jpeg", "*.JPEG",
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
        String extension = getExtension(selectedSource);
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer l'image recadrée");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            "Image " + extension.toUpperCase(Locale.ROOT),
            "*." + extension
        ));
        Path current = readDestination();
        if (current != null) {
            Path parent = current.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                chooser.setInitialDirectory(parent.toFile());
            }
            chooser.setInitialFileName(current.getFileName().toString());
        } else {
            chooser.setInitialFileName(getCroppedFileName(selectedSource));
        }
        File file = chooser.showSaveDialog(getPrimaryStage());
        if (file == null) {
            return;
        }
        Path destination = file.toPath().toAbsolutePath().normalize();
        if (!getExtension(destination).equals(extension)) {
            destination = replaceExtension(destination, extension);
        }
        destinationField.setText(destination.toString());
        statusLabel.setText("Destination sélectionnée.");
    }

    @FXML
    private void crop() {
        if (selectedSource == null || cropWidth < 1 || cropHeight < 1) {
            showError("Sélectionnez une zone de l'image à conserver.");
            return;
        }
        Path destination = readDestination();
        if (destination == null) {
            showError("Choisissez un emplacement de destination.");
            return;
        }
        if (Files.exists(destination) && !confirmOverwrite(destination)) {
            return;
        }

        int x = cropX;
        int y = cropY;
        int width = cropWidth;
        int height = cropHeight;
        Path source = selectedSource;
        cropButton.setDisable(true);
        statusLabel.setText("Recadrage en cours…");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                new ImageCropService().crop(source, destination, x, y, width, height);
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            cropButton.setDisable(false);
            statusLabel.setText("Image recadrée : " + destination.getFileName());
        });
        task.setOnFailed(event -> {
            cropButton.setDisable(false);
            statusLabel.setText("Le recadrage a échoué.");
            Throwable error = task.getException();
            showError(error == null ? "Une erreur inconnue est survenue." : error.getMessage());
        });
        Thread worker = new Thread(task, "image-crop");
        worker.setDaemon(true);
        worker.start();
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

    private void beginAdjustment(MouseEvent event) {
        if (selectedSource == null) {
            return;
        }
        Point2D point = eventPoint(event);
        pressX = point.getX();
        pressY = point.getY();
        startX = selectionX;
        startY = selectionY;
        startWidth = selectionWidth;
        startHeight = selectionHeight;
        activeHandle = findHandle(pressX, pressY);
        if (activeHandle == IDLE) {
            activeHandle = isInsideSelection(pressX, pressY) ? MOVE_SELECTION : NEW_SELECTION;
        }
        if (activeHandle == NEW_SELECTION) {
            selectionX = pressX;
            selectionY = pressY;
            selectionWidth = 0;
            selectionHeight = 0;
        }
        updateSelectionDisplay();
        event.consume();
    }

    private void adjustSelection(MouseEvent event) {
        if (selectedSource == null || activeHandle == IDLE) {
            return;
        }
        Point2D point = eventPoint(event);
        double dx = point.getX() - pressX;
        double dy = point.getY() - pressY;

        if (activeHandle == MOVE_SELECTION) {
            selectionX = clamp(startX + dx, 0, imageWidth - startWidth);
            selectionY = clamp(startY + dy, 0, imageHeight - startHeight);
        } else {
            resizeSelection(activeHandle, point);
        }
        updateSelectionDisplay();
        event.consume();
    }

    private void finishAdjustment(MouseEvent event) {
        if (activeHandle == IDLE) {
            return;
        }
        Point2D point = eventPoint(event);
        double dx = point.getX() - pressX;
        double dy = point.getY() - pressY;
        if (activeHandle == MOVE_SELECTION) {
            selectionX = clamp(startX + dx, 0, imageWidth - startWidth);
            selectionY = clamp(startY + dy, 0, imageHeight - startHeight);
        } else {
            resizeSelection(activeHandle, point);
        }
        activeHandle = IDLE;
        updateSelectionDisplay();
        updateCropCoordinates();
        dimensionsLabel.setText("Zone conservée : " + cropWidth + " × " + cropHeight + " px");
        statusLabel.setText("Faites glisser les poignées bleues pour ajuster le cadre.");
        cropButton.setDisable(cropWidth < 1 || cropHeight < 1);
        event.consume();
    }

    private void resizeSelection(int handle, Point2D point) {
        double left = startX;
        double top = startY;
        double right = startX + startWidth;
        double bottom = startY + startHeight;
        switch (handle) {
            case 0 -> { left = clamp(point.getX(), 0, right - MIN_SELECTION_SIZE); top = clamp(point.getY(), 0, bottom - MIN_SELECTION_SIZE); }
            case 1 -> top = clamp(point.getY(), 0, bottom - MIN_SELECTION_SIZE);
            case 2 -> { right = clamp(point.getX(), left + MIN_SELECTION_SIZE, imageWidth); top = clamp(point.getY(), 0, bottom - MIN_SELECTION_SIZE); }
            case 3 -> right = clamp(point.getX(), left + MIN_SELECTION_SIZE, imageWidth);
            case 4 -> { right = clamp(point.getX(), left + MIN_SELECTION_SIZE, imageWidth); bottom = clamp(point.getY(), top + MIN_SELECTION_SIZE, imageHeight); }
            case 5 -> bottom = clamp(point.getY(), top + MIN_SELECTION_SIZE, imageHeight);
            case 6 -> { left = clamp(point.getX(), 0, right - MIN_SELECTION_SIZE); bottom = clamp(point.getY(), top + MIN_SELECTION_SIZE, imageHeight); }
            case 7 -> left = clamp(point.getX(), 0, right - MIN_SELECTION_SIZE);
            default -> { return; }
        }
        selectionX = left;
        selectionY = top;
        selectionWidth = right - left;
        selectionHeight = bottom - top;
    }

    private int findHandle(double x, double y) {
        for (int index = 0; index < handles.length; index++) {
            Point2D center = handleCenter(index);
            if (Math.abs(x - center.getX()) <= HANDLE_HIT_RADIUS
                && Math.abs(y - center.getY()) <= HANDLE_HIT_RADIUS) {
                return index;
            }
        }
        return NEW_SELECTION;
    }

    private Point2D handleCenter(int index) {
        return switch (index) {
            case 0 -> new Point2D(selectionX, selectionY);
            case 1 -> new Point2D(selectionX + selectionWidth / 2, selectionY);
            case 2 -> new Point2D(selectionX + selectionWidth, selectionY);
            case 3 -> new Point2D(selectionX + selectionWidth, selectionY + selectionHeight / 2);
            case 4 -> new Point2D(selectionX + selectionWidth, selectionY + selectionHeight);
            case 5 -> new Point2D(selectionX + selectionWidth / 2, selectionY + selectionHeight);
            case 6 -> new Point2D(selectionX, selectionY + selectionHeight);
            default -> new Point2D(selectionX, selectionY + selectionHeight / 2);
        };
    }

    private void updateSelectionDisplay() {
        selectionRectangle.setX(selectionX);
        selectionRectangle.setY(selectionY);
        selectionRectangle.setWidth(Math.max(0, selectionWidth));
        selectionRectangle.setHeight(Math.max(0, selectionHeight));
        for (int index = 0; index < handles.length; index++) {
            Point2D center = handleCenter(index);
            handles[index].setX(center.getX() - HANDLE_SIZE / 2);
            handles[index].setY(center.getY() - HANDLE_SIZE / 2);
        }
        updateCropCoordinates();
        cropButton.setDisable(cropWidth < 1 || cropHeight < 1);
        if (cropWidth > 0 && cropHeight > 0) {
            dimensionsLabel.setText("Zone conservée : " + cropWidth + " × " + cropHeight + " px");
        }
    }

    private void updateCropCoordinates() {
        if (imageWidth <= 0 || imageHeight <= 0 || selectionWidth <= 0 || selectionHeight <= 0) {
            cropX = 0;
            cropY = 0;
            cropWidth = 0;
            cropHeight = 0;
            return;
        }
        cropX = Math.max(0, Math.min(sourceWidth - 1, (int) Math.floor(selectionX * sourceWidth / imageWidth)));
        cropY = Math.max(0, Math.min(sourceHeight - 1, (int) Math.floor(selectionY * sourceHeight / imageHeight)));
        int endX = Math.max(cropX + 1, Math.min(sourceWidth,
            (int) Math.ceil((selectionX + selectionWidth) * sourceWidth / imageWidth)));
        int endY = Math.max(cropY + 1, Math.min(sourceHeight,
            (int) Math.ceil((selectionY + selectionHeight) * sourceHeight / imageHeight)));
        cropWidth = endX - cropX;
        cropHeight = endY - cropY;
    }

    private boolean isInsideSelection(double x, double y) {
        return x >= selectionX && x <= selectionX + selectionWidth
            && y >= selectionY && y <= selectionY + selectionHeight;
    }

    private Point2D clampToImage(double x, double y) {
        return new Point2D(clamp(x, 0, imageWidth), clamp(y, 0, imageHeight));
    }

    private Point2D eventPoint(MouseEvent event) {
        Point2D point = imageCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
        return clampToImage(point.getX(), point.getY());
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean handleDroppedFiles(List<File> files) {
        if (files.size() != 1) {
            showError("Déposez une seule image à la fois.");
            return false;
        }
        return setSource(files.get(0).toPath());
    }

    private boolean setSource(Path source) {
        if (!isSupportedImage(source)) {
            showError("Le fichier sélectionné n'a pas un format d'image pris en charge.");
            return false;
        }
        try {
            BufferedImage image = ImageIO.read(source.toFile());
            if (image == null) {
                showError("Le fichier sélectionné ne contient pas une image valide.");
                return false;
            }
            selectedSource = source.toAbsolutePath().normalize();
            sourceWidth = image.getWidth();
            sourceHeight = image.getHeight();
            double scale = Math.min(PREVIEW_MAX_WIDTH / sourceWidth, PREVIEW_MAX_HEIGHT / sourceHeight);
            imageWidth = Math.max(1, Math.round(sourceWidth * scale));
            imageHeight = Math.max(1, Math.round(sourceHeight * scale));
            imageCanvas.setPrefSize(imageWidth, imageHeight);
            imageCanvas.setMinSize(imageWidth, imageHeight);
            imageCanvas.setMaxSize(imageWidth, imageHeight);
            imageCanvas.setVisible(true);
            imageCanvas.setManaged(true);
            previewImageView.setX(0);
            previewImageView.setY(0);
            previewImageView.setFitWidth(imageWidth);
            previewImageView.setFitHeight(imageHeight);
            previewImageView.setImage(toPreviewImage(image, (int) imageWidth, (int) imageHeight));
            previewImageView.setVisible(true);
            selectionRectangle.setVisible(true);
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            selectedFileLabel.setText(selectedSource.getFileName().toString());
            selectedFileLabel.setVisible(true);
            selectedFileLabel.setManaged(true);
            selectionX = 0;
            selectionY = 0;
            selectionWidth = imageWidth;
            selectionHeight = imageHeight;
            updateSelectionDisplay();
            dimensionsLabel.setText("Image complète : " + sourceWidth + " × " + sourceHeight + " px");
            cropButton.setDisable(false);
            destinationField.setText(selectedSource.resolveSibling(getCroppedFileName(selectedSource)).toString());
            statusLabel.setText("Le cadre bleu démarre autour de l'image. Faites glisser ses poignées pour le régler.");
            return true;
        } catch (IOException e) {
            showError("Impossible de lire l'image : " + e.getMessage());
            return false;
        }
    }

    private WritableImage toPreviewImage(BufferedImage image, int width, int height) {
        BufferedImage preview = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = preview.createGraphics();
        try {
            graphics.setRenderingHint(
                java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.drawImage(image, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        int[] pixels = preview.getRGB(0, 0, width, height, null, 0, width);
        WritableImage writable = new WritableImage(width, height);
        writable.getPixelWriter().setPixels(
            0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width
        );
        return writable;
    }

    private boolean isSupportedImage(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String extension = getExtension(path);
        return extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg")
            || extension.equals("gif") || extension.equals("bmp") || extension.equals("webp");
    }

    private String getExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String getCroppedFileName(Path source) {
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        return base + "_recadree" + extension;
    }

    private Path replaceExtension(Path path, String extension) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return path.resolveSibling(base + "." + extension);
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
        alert.setTitle("Erreur de recadrage");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
