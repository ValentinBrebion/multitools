package com.example.multitools.tools.text;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TextController {
    
    @FXML
    private Label statusLabel;
    
    @FXML
    public void initialize() {
        statusLabel.setText("Outil de texte - À venir");
    }
}
