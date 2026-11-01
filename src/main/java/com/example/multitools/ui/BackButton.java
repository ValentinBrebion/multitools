package com.example.multitools.ui;

import javafx.scene.control.Button;

public class BackButton extends Button {
    public BackButton() {
        super("← Retour");
        setStyle("-fx-background-color: transparent; "
                + "-fx-text-fill: #64748b; "
                + "-fx-font-size: 14px; "
                + "-fx-cursor: hand; "
                + "-fx-padding: 8 12 8 12;");
    }
}
