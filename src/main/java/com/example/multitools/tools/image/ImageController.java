package com.example.multitools.tools.image;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ImageController {
    
    @FXML
    private Label statusLabel;
    
    @FXML
    public void initialize() {
        statusLabel.setText("Outil d'image - À venir");
    }
}
