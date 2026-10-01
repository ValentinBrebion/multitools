package com.example.multitools.tools.converter;

import com.example.multitools.core.Tool;
import com.example.multitools.core.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class UnitConverterTool implements Tool {

    @Override
    public String getName() {
        return "Convertisseur";
    }

    @Override
    public String getDescription() {
        return "Convertir des unités de mesure";
    }

    @Override
    public String getIcon() {
        return "🔄";
    }

    @Override
    public void open() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/converter/unit-converter.fxml")
            );
            loader.setControllerFactory(param -> new UnitConverterController());

            Parent root = loader.load();

            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
