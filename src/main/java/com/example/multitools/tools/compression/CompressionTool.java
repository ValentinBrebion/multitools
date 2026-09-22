package com.example.multitools.tools.compression;

import com.example.multitools.core.Tool;
import com.example.multitools.core.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CompressionTool implements Tool {

    @Override
    public String getName() {
        return "Compression";
    }

    @Override
    public String getDescription() {
        return "Compresser et décompresser des fichiers";
    }

    @Override
    public String getIcon() {
        return "📦";
    }

    @Override
    public void open() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/compression/compress.fxml")
            );
            loader.setControllerFactory(param -> new CompressionController());
            
            Parent root = loader.load();
            
            Stage stage = NavigationManager.getInstance().getPrimaryStage();
            if (stage == null) {
                stage = new Stage();
            }
            
            Scene scene = new Scene(root, 950, 700);
            
            stage.setTitle("Compression - Multitools");
            stage.setScene(scene);
            
            stage.setMinWidth(850);
            stage.setMinHeight(600);
            stage.setResizable(true);
            
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
