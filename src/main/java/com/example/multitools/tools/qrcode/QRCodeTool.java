package com.example.multitools.tools.qrcode;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.core.Tool;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;

import java.io.IOException;

public class QRCodeTool implements Tool {

    @Override
    public String getName() {
        return "Générateur de QR code";
    }

    @Override
    public String getDescription() {
        return "Créer et exporter des QR codes";
    }

    @Override
    public String getIcon() {
        return "▦";
    }

    @Override
    public void open() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/qrcode/qrcode.fxml")
            );
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le générateur de QR code");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
