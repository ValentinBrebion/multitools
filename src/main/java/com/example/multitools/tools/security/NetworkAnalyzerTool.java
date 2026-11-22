package com.example.multitools.tools.security;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.core.Tool;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;

import java.io.IOException;

public class NetworkAnalyzerTool implements Tool {

    @Override
    public String getName() {
        return "Analyseur réseau";
    }

    @Override
    public String getDescription() {
        return "Observer simplement les paquets réseau de cet appareil";
    }

    @Override
    public String getIcon() {
        return "Wi-Fi";
    }

    @Override
    public void open() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/security/network-analyzer.fxml")
            );
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir l'analyseur réseau");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
