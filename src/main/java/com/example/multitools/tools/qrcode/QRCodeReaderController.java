package com.example.multitools.tools.qrcode;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.tools.divers.DiversController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class QRCodeReaderController {

    @FXML
    private Label imageNameLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private TextArea resultArea;

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image contenant un QR Code");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(
                "Images",
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"
            )
        );
        Window window = imageNameLabel.getScene().getWindow();
        File imageFile = fileChooser.showOpenDialog(window);
        if (imageFile == null) {
            return;
        }

        imageNameLabel.setText(imageFile.getName());
        resultArea.clear();
        statusLabel.setText("");
        decodeImage(imageFile);
    }

    private void decodeImage(File imageFile) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                throw new IOException("Le fichier sélectionné n'est pas une image lisible.");
            }

            QRCodeDecoder.decode(image).ifPresentOrElse(
                decodedText -> {
                    resultArea.setText(decodedText);
                    statusLabel.setText("QR Code lu avec succès.");
                },
                () -> statusLabel.setText("Aucun QR Code détecté dans cette image.")
            );
        } catch (IOException e) {
            showError("Impossible de lire l'image sélectionnée: " + e.getMessage());
        } catch (NoClassDefFoundError e) {
            showError(
                "Les dépendances QR Code sont absentes de cette installation. "
                    + "Recompilez ou réinstallez l'application."
            );
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/divers/divers.fxml")
            );
            loader.setControllerFactory(type -> new DiversController());
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (IOException e) {
            showError("Impossible de revenir au menu Divers: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
