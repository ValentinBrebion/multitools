package com.example.multitools.tools.text;

import com.example.multitools.core.Tool;
import javafx.scene.control.Alert;

public class TextTool implements Tool {

    @Override
    public String getName() {
        return "Texte";
    }

    @Override
    public String getDescription() {
        return "Outils de manipulation de texte";
    }

    @Override
    public String getIcon() {
        return "📝";
    }

    @Override
    public void open() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Outil de texte");
        alert.setHeaderText(null);
        alert.setContentText("L'outil de texte sera disponible dans une future version.");
        alert.showAndWait();
    }
}
