package com.example.multitools.tools.qrcode;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.core.Tool;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;

import java.io.IOException;

public class QRCodeReaderTool implements Tool {

    @Override
    public String getName() {
        return "Lecteur de QR Code";
    }

    @Override
    public String getDescription() {
        return "Lire un QR Code directement depuis une image sur son PC";
    }

    @Override
    public String getIcon() {
        return "▧";
    }

    @Override
    public void open() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/qrcode/qr-code-reader.fxml")
            );
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le lecteur de QR Code");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
