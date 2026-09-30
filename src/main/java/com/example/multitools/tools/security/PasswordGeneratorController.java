package com.example.multitools.tools.security;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.update.UpdateChecker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class PasswordGeneratorController {
    
    @FXML
    private TextField passwordField;
    
    @FXML
    private Label strengthLabel;
    
    @FXML
    private Label lengthLabel;
    
    @FXML
    private Slider lengthSlider;
    
    @FXML
    private Label lengthValueLabel;
    
    @FXML
    private CheckBox uppercaseCheck;
    
    @FXML
    private CheckBox lowercaseCheck;
    
    @FXML
    private CheckBox numbersCheck;
    
    @FXML
    private CheckBox symbolsCheck;
    
    @FXML
    private Button generateButton;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label versionLabel;

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    @FXML
    public void initialize() {
        versionLabel.setText("Version " + UpdateChecker.getCurrentVersion());
        
        // Écouteur pour le slider de longueur
        lengthSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            lengthValueLabel.setText(String.valueOf(newValue.intValue()));
        });
    }

    @FXML
    private void generatePassword() {
        int length = (int) lengthSlider.getValue();
        
        List<Character> characterPool = new ArrayList<>();
        
        if (uppercaseCheck.isSelected()) {
            for (char c : UPPERCASE.toCharArray()) {
                characterPool.add(c);
            }
        }
        
        if (lowercaseCheck.isSelected()) {
            for (char c : LOWERCASE.toCharArray()) {
                characterPool.add(c);
            }
        }
        
        if (numbersCheck.isSelected()) {
            for (char c : NUMBERS.toCharArray()) {
                characterPool.add(c);
            }
        }
        
        if (symbolsCheck.isSelected()) {
            for (char c : SYMBOLS.toCharArray()) {
                characterPool.add(c);
            }
        }
        
        if (characterPool.isEmpty()) {
            showAlert("Erreur", "Veuillez sélectionner au moins un type de caractère.");
            return;
        }
        
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characterPool.size());
            password.append(characterPool.get(index));
        }
        
        passwordField.setText(password.toString());
        lengthLabel.setText("Longueur: " + length);
        
        // Calculer la force du mot de passe
        String strength = calculateStrength(password.toString());
        strengthLabel.setText("Force: " + strength);
        
        // Couleur selon la force
        switch (strength.toLowerCase()) {
            case "très faible":
                strengthLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
                break;
            case "faible":
                strengthLabel.setStyle("-fx-text-fill: #f97316; -fx-font-size: 13px;");
                break;
            case "moyen":
                strengthLabel.setStyle("-fx-text-fill: #eab308; -fx-font-size: 13px;");
                break;
            case "fort":
                strengthLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 13px;");
                break;
            case "très fort":
                strengthLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 13px;");
                break;
            default:
                strengthLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        }
        
        updateStatus("Mot de passe généré avec succès");
    }

    private String calculateStrength(String password) {
        int length = password.length();
        boolean hasUpper = !password.equals(password.toLowerCase());
        boolean hasLower = !password.equals(password.toUpperCase());
        boolean hasNumber = password.matches(".*\\d.*");
        boolean hasSymbol = !password.matches("[A-Za-z0-9]*");
        
        int score = 0;
        
        if (length >= 8) score++;
        if (length >= 12) score++;
        if (length >= 16) score++;
        if (hasUpper) score++;
        if (hasLower) score++;
        if (hasNumber) score++;
        if (hasSymbol) score++;
        
        if (score <= 2) return "Très faible";
        if (score <= 3) return "Faible";
        if (score <= 4) return "Moyen";
        if (score <= 5) return "Fort";
        return "Très fort";
    }

    @FXML
    private void copyPassword() {
        String password = passwordField.getText();
        
        if (password == null || password.isEmpty()) {
            showAlert("Erreur", "Aucun mot de passe à copier.");
            return;
        }
        
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(password);
        clipboard.setContent(content);
        
        updateStatus("Mot de passe copié dans le presse-papiers");
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/security/security.fxml")
            );
            loader.setControllerFactory(param -> new SecurityController());
            
            Parent root = loader.load();
            
            NavigationManager.getInstance().navigateTo(root);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de revenir au menu sécurité: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateCheck() {
        updateStatus("Recherche de mises à jour...");
        
        Thread updateThread = new Thread(() -> {
            UpdateChecker checker = new UpdateChecker();
            UpdateChecker.UpdateInfo updateInfo = checker.checkForUpdates();
            
            javafx.application.Platform.runLater(() -> {
                if (updateInfo.hasUpdate) {
                    showUpdateDialog(updateInfo);
                } else {
                    showAlert("Mise à jour", "Vous utilisez déjà la dernière version de Multitools (" + 
                            UpdateChecker.getCurrentVersion() + ")");
                    updateStatus("Aucune mise à jour disponible");
                }
            });
        });
        
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private void showUpdateDialog(UpdateChecker.UpdateInfo updateInfo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Mise à jour disponible");
        alert.setHeaderText("Multitools " + updateInfo.latestVersion + " est disponible");
        
        String message = "Votre version: " + UpdateChecker.getCurrentVersion() + "\n" +
                        "Nouvelle version: " + updateInfo.latestVersion + "\n\n";
        
        if (updateInfo.releaseNotes != null && !updateInfo.releaseNotes.isEmpty()) {
            message += "Notes de version:\n" + updateInfo.releaseNotes + "\n\n";
        }
        
        message += "Voulez-vous télécharger et installer cette mise à jour maintenant ?";
        
        alert.setContentText(message);
        
        ButtonType installButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(installButton, cancelButton);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == installButton) {
                showAlert("Information", "Le téléchargement sera disponible dans une future version.");
            }
        });
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
