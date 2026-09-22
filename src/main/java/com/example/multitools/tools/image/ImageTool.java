package com.example.multitools.tools.image;

import com.example.multitools.core.Tool;
import javafx.scene.control.Alert;

public class ImageTool implements Tool {

    @Override
    public String getName() {
        return "Image";
    }

    @Override
    public String getDescription() {
        return "Outils de manipulation d'images";
    }

    @Override
    public String getIcon() {
        return "🖼️";
    }

    @Override
    public void open() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Outil d'image");
        alert.setHeaderText(null);
        alert.setContentText("L'outil d'image sera disponible dans une future version.");
        alert.showAndWait();
    }
}
