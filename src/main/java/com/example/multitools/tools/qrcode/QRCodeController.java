package com.example.multitools.tools.qrcode;

import com.example.multitools.core.NavigationManager;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class QRCodeController {

    @FXML
    private TextArea contentArea;

    @FXML
    private ComboBox<Integer> sizeComboBox;

    @FXML
    private ComboBox<ErrorCorrectionLevel> correctionComboBox;

    @FXML
    private ImageView qrImageView;

    @FXML
    private Label statusLabel;

    @FXML
    private javafx.scene.control.Button saveButton;

    private final QRCodeGenerator generator = new QRCodeGenerator();
    private BitMatrix generatedMatrix;

    @FXML
    public void initialize() {
        sizeComboBox.getItems().setAll(256, 384, 512, 768);
        sizeComboBox.setValue(384);
        correctionComboBox.getItems().setAll(
            ErrorCorrectionLevel.L,
            ErrorCorrectionLevel.M,
            ErrorCorrectionLevel.Q,
            ErrorCorrectionLevel.H
        );
        correctionComboBox.setValue(ErrorCorrectionLevel.M);
        correctionComboBox.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ErrorCorrectionLevel level, boolean empty) {
                super.updateItem(level, empty);
                setText(empty || level == null ? null : correctionLabel(level));
            }
        });
        correctionComboBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ErrorCorrectionLevel level, boolean empty) {
                super.updateItem(level, empty);
                setText(empty || level == null ? null : correctionLabel(level));
            }
        });

        contentArea.textProperty().addListener((observable, oldValue, newValue) -> invalidateGeneratedCode());
        sizeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> invalidateGeneratedCode());
        correctionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> invalidateGeneratedCode());
    }

    @FXML
    private void handleGenerate() {
        try {
            generatedMatrix = generator.generate(
                contentArea.getText(),
                sizeComboBox.getValue(),
                correctionComboBox.getValue()
            );
            qrImageView.setImage(toWritableImage(generatedMatrix));
            saveButton.setDisable(false);
            statusLabel.setText("QR code généré — " + contentArea.getText().length() + " caractères.");
            statusLabel.setStyle("-fx-text-fill: #15803d;");
        } catch (IllegalArgumentException e) {
            generatedMatrix = null;
            qrImageView.setImage(null);
            saveButton.setDisable(true);
            statusLabel.setText(e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #b91c1c;");
        }
    }

    @FXML
    private void handleSave() {
        if (generatedMatrix == null) {
            statusLabel.setText("Générez d'abord un QR code.");
            statusLabel.setStyle("-fx-text-fill: #b91c1c;");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le QR code");
        fileChooser.setInitialFileName("qrcode.png");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image PNG", "*.png")
        );
        File file = fileChooser.showSaveDialog(NavigationManager.getInstance().getPrimaryStage());
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }

        try {
            if (!ImageIO.write(generator.toImage(generatedMatrix), "PNG", file)) {
                throw new IOException("Aucun encodeur PNG n'est disponible.");
            }
            statusLabel.setText("Image enregistrée : " + file.getAbsolutePath());
            statusLabel.setStyle("-fx-text-fill: #15803d;");
        } catch (IOException e) {
            showError("Impossible d'enregistrer le QR code : " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            loader.setControllerFactory(type -> new com.example.multitools.MainController());
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (IOException e) {
            showError("Impossible de revenir au menu principal : " + e.getMessage());
        }
    }

    private WritableImage toWritableImage(BitMatrix matrix) {
        WritableImage image = new WritableImage(matrix.getWidth(), matrix.getHeight());
        PixelWriter pixelWriter = image.getPixelWriter();
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                pixelWriter.setArgb(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return image;
    }

    private void invalidateGeneratedCode() {
        if (generatedMatrix != null) {
            generatedMatrix = null;
            qrImageView.setImage(null);
            saveButton.setDisable(true);
            statusLabel.setText("Les paramètres ont changé. Générez un nouveau QR code.");
            statusLabel.setStyle("-fx-text-fill: #64748b;");
        }
    }

    private String correctionLabel(ErrorCorrectionLevel level) {
        return switch (level) {
            case L -> "L — faible (7 %)";
            case M -> "M — moyen (15 %)";
            case Q -> "Q — élevé (25 %)";
            case H -> "H — très élevé (30 %)";
        };
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
