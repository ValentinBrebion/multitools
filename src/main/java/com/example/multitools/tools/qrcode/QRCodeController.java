package com.example.multitools.tools.qrcode;

import com.example.multitools.core.NavigationManager;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
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
    private ComboBox<String> paletteComboBox;

    @FXML
    private ColorPicker foregroundColorPicker;

    @FXML
    private ColorPicker backgroundColorPicker;

    @FXML
    private ColorPicker gradientColorPicker;

    @FXML
    private CheckBox gradientCheckBox;

    @FXML
    private ComboBox<QRCodeGenerator.ModuleShape> shapeComboBox;

    @FXML
    private Slider logoSizeSlider;

    @FXML
    private Slider logoMarginSlider;

    @FXML
    private Label logoSizeValueLabel;

    @FXML
    private Label logoMarginValueLabel;

    @FXML
    private CheckBox removeLogoBackgroundCheckBox;

    @FXML
    private ImageView qrImageView;

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<String> outputFormatComboBox;

    @FXML
    private javafx.scene.control.Button saveButton;

    @FXML
    private javafx.scene.control.Button removeLogoButton;

    @FXML
    private Label logoNameLabel;

    private final QRCodeGenerator generator = new QRCodeGenerator();
    private BitMatrix generatedMatrix;
    private BufferedImage generatedImage;
    private BufferedImage logoImage;
    private boolean applyingPalette;

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

        paletteComboBox.getItems().setAll("Moderne", "Sombre", "Pastel", "Personnalisée");
        paletteComboBox.setValue("Moderne");
        shapeComboBox.getItems().setAll(QRCodeGenerator.ModuleShape.values());
        shapeComboBox.setValue(QRCodeGenerator.ModuleShape.SQUARE);
        outputFormatComboBox.getItems().setAll("PNG", "JPG (JPEG)", "ICO");
        outputFormatComboBox.setValue("PNG");
        outputFormatComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                saveButton.setText("Enregistrer en " + outputExtension(newValue).toUpperCase(java.util.Locale.ROOT));
            }
        });
        applyPalette("Moderne");

        contentArea.textProperty().addListener((observable, oldValue, newValue) -> invalidateGeneratedCode());
        sizeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> invalidateGeneratedCode());
        correctionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> invalidateGeneratedCode());
        paletteComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !applyingPalette) {
                applyPalette(newValue);
            }
        });
        foregroundColorPicker.valueProperty().addListener((observable, oldValue, newValue) -> customPaletteSelected());
        backgroundColorPicker.valueProperty().addListener((observable, oldValue, newValue) -> customPaletteSelected());
        gradientColorPicker.valueProperty().addListener((observable, oldValue, newValue) -> customPaletteSelected());
        gradientCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> invalidateGeneratedCode());
        shapeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> invalidateGeneratedCode());
        logoSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            logoSizeValueLabel.setText(Math.round(newValue.doubleValue()) + " %");
            invalidateGeneratedCode();
        });
        logoMarginSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            logoMarginValueLabel.setText(Math.round(newValue.doubleValue()) + " %");
            invalidateGeneratedCode();
        });
        removeLogoBackgroundCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> invalidateGeneratedCode());
    }

    @FXML
    private void handleGenerate() {
        try {
            generatedMatrix = generator.generate(
                contentArea.getText(),
                sizeComboBox.getValue(),
                correctionComboBox.getValue()
            );
            generatedImage = generator.toImage(
                generatedMatrix,
                toAwtColor(foregroundColorPicker.getValue()),
                toAwtColor(backgroundColorPicker.getValue()),
                toAwtColor(gradientColorPicker.getValue()),
                gradientCheckBox.isSelected(),
                shapeComboBox.getValue(),
                logoImage,
                (int) Math.round(logoSizeSlider.getValue()),
                (int) Math.round(logoMarginSlider.getValue()),
                removeLogoBackgroundCheckBox.isSelected()
            );
            qrImageView.setImage(toWritableImage(generatedImage));
            saveButton.setDisable(false);
            statusLabel.setText("QR code généré — " + contentArea.getText().length() + " caractères.");
            statusLabel.setStyle("-fx-text-fill: #15803d;");
        } catch (IllegalArgumentException e) {
            generatedMatrix = null;
            generatedImage = null;
            qrImageView.setImage(null);
            saveButton.setDisable(true);
            statusLabel.setText(e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #b91c1c;");
        }
    }

    @FXML
    private void handleSave() {
        if (generatedImage == null) {
            statusLabel.setText("Générez d'abord un QR code.");
            statusLabel.setStyle("-fx-text-fill: #b91c1c;");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        String selectedFormat = outputFormatComboBox.getValue();
        String extension = outputExtension(selectedFormat);
        fileChooser.setTitle("Enregistrer le QR code");
        fileChooser.setInitialFileName("qrcode." + extension);
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(formatDescription(selectedFormat), "*." + extension)
        );
        File file = fileChooser.showSaveDialog(NavigationManager.getInstance().getPrimaryStage());
        if (file == null) {
            return;
        }
        file = withExtension(file, extension);

        try {
            QRCodeImageExporter.save(generatedImage, selectedFormat, file);
            statusLabel.setText("Image enregistrée : " + file.getAbsolutePath());
            statusLabel.setStyle("-fx-text-fill: #15803d;");
        } catch (IOException e) {
            showError("Impossible d'enregistrer le QR code : " + e.getMessage());
        }
    }

    private String outputExtension(String format) {
        return switch (format) {
            case "JPG (JPEG)" -> "jpg";
            case "ICO" -> "ico";
            default -> "png";
        };
    }

    private String formatDescription(String format) {
        return switch (format) {
            case "JPG (JPEG)" -> "Image JPEG";
            case "ICO" -> "Icône Windows";
            default -> "Image PNG";
        };
    }

    private File withExtension(File file, String extension) {
        String name = file.getName();
        String lowerName = name.toLowerCase(java.util.Locale.ROOT);
        for (String knownExtension : new String[]{".png", ".jpg", ".jpeg", ".ico"}) {
            if (lowerName.endsWith(knownExtension)) {
                name = name.substring(0, name.length() - knownExtension.length());
                break;
            }
        }
        if (name.isBlank()) {
            name = "qrcode";
        }
        return new File(file.getParentFile(), name + "." + extension);
    }

    @FXML
    private void handleImportLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importer un logo");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images PNG ou SVG", "*.png", "*.svg")
        );
        File file = fileChooser.showOpenDialog(NavigationManager.getInstance().getPrimaryStage());
        if (file == null) {
            return;
        }
        try {
            logoImage = QRCodeLogoLoader.load(file);
            logoNameLabel.setText(file.getName());
            removeLogoButton.setDisable(false);
            correctionComboBox.setValue(ErrorCorrectionLevel.H);
            invalidateGeneratedCode();
        } catch (IOException e) {
            showError("Impossible d'importer le logo : " + e.getMessage());
        }
    }

    @FXML
    private void handleRemoveLogo() {
        logoImage = null;
        logoNameLabel.setText("Aucun logo sélectionné");
        removeLogoButton.setDisable(true);
        invalidateGeneratedCode();
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

    private WritableImage toWritableImage(BufferedImage source) {
        WritableImage image = new WritableImage(source.getWidth(), source.getHeight());
        PixelWriter pixelWriter = image.getPixelWriter();
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                pixelWriter.setArgb(x, y, source.getRGB(x, y));
            }
        }
        return image;
    }

    private void invalidateGeneratedCode() {
        if (generatedMatrix != null) {
            generatedMatrix = null;
            generatedImage = null;
            qrImageView.setImage(null);
            saveButton.setDisable(true);
            statusLabel.setText("Les paramètres ont changé. Générez un nouveau QR code.");
            statusLabel.setStyle("-fx-text-fill: #64748b;");
        }
    }

    private void customPaletteSelected() {
        if (!applyingPalette && paletteComboBox.getValue() != null) {
            applyingPalette = true;
            paletteComboBox.setValue("Personnalisée");
            applyingPalette = false;
            invalidateGeneratedCode();
        }
    }

    private void applyPalette(String palette) {
        applyingPalette = true;
        switch (palette) {
            case "Sombre" -> {
                foregroundColorPicker.setValue(javafx.scene.paint.Color.web("#f8fafc"));
                backgroundColorPicker.setValue(javafx.scene.paint.Color.web("#172033"));
                gradientColorPicker.setValue(javafx.scene.paint.Color.web("#38bdf8"));
            }
            case "Pastel" -> {
                foregroundColorPicker.setValue(javafx.scene.paint.Color.web("#8b5cf6"));
                backgroundColorPicker.setValue(javafx.scene.paint.Color.web("#fff7ed"));
                gradientColorPicker.setValue(javafx.scene.paint.Color.web("#ec4899"));
            }
            case "Personnalisée" -> {
                // Keep the colors selected by the user.
            }
            default -> {
                foregroundColorPicker.setValue(javafx.scene.paint.Color.web("#087edb"));
                backgroundColorPicker.setValue(javafx.scene.paint.Color.WHITE);
                gradientColorPicker.setValue(javafx.scene.paint.Color.web("#7c3aed"));
            }
        }
        applyingPalette = false;
        invalidateGeneratedCode();
    }

    private Color toAwtColor(javafx.scene.paint.Color color) {
        return new Color(
            (float) color.getRed(),
            (float) color.getGreen(),
            (float) color.getBlue(),
            (float) color.getOpacity()
        );
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
