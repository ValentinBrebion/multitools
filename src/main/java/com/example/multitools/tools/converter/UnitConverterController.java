package com.example.multitools.tools.converter;

import com.example.multitools.core.NavigationManager;
import com.example.multitools.update.UpdateChecker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class UnitConverterController {

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private ComboBox<String> fromUnitComboBox;

    @FXML
    private ComboBox<String> toUnitComboBox;

    @FXML
    private TextField inputValue;

    @FXML
    private TextField outputValue;

    @FXML
    private Label resultLabel;

    @FXML
    private Label versionLabel;

    private Map<String, Map<String, Double>> conversionRates;

    @FXML
    public void initialize() {
        initializeConversionRates();
        initializeCategories();
        setupListeners();
        versionLabel.setText("Version " + UpdateChecker.getCurrentVersion());
    }

    private void initializeConversionRates() {
        conversionRates = new HashMap<>();

        // Longueur (base: mètre)
        Map<String, Double> length = new HashMap<>();
        length.put("Mètre", 1.0);
        length.put("Kilomètre", 1000.0);
        length.put("Centimètre", 0.01);
        length.put("Millimètre", 0.001);
        length.put("Pied", 0.3048);
        length.put("Pouce", 0.0254);
        length.put("Mile", 1609.34);
        conversionRates.put("Longueur", length);

        // Poids (base: kilogramme)
        Map<String, Double> weight = new HashMap<>();
        weight.put("Kilogramme", 1.0);
        weight.put("Gramme", 0.001);
        weight.put("Milligramme", 0.000001);
        weight.put("Tonne", 1000.0);
        weight.put("Livre", 0.453592);
        weight.put("Once", 0.0283495);
        conversionRates.put("Poids", weight);

        // Température (conversion spéciale)
        Map<String, Double> temperature = new HashMap<>();
        temperature.put("Celsius", 0.0);
        temperature.put("Fahrenheit", 0.0);
        temperature.put("Kelvin", 0.0);
        conversionRates.put("Température", temperature);

        // Volume (base: litre)
        Map<String, Double> volume = new HashMap<>();
        volume.put("Litre", 1.0);
        volume.put("Millilitre", 0.001);
        volume.put("Mètre cube", 1000.0);
        volume.put("Gallon US", 3.78541);
        volume.put("Pinte US", 0.473176);
        conversionRates.put("Volume", volume);

        // Vitesse (base: m/s)
        Map<String, Double> speed = new HashMap<>();
        speed.put("Mètre/seconde", 1.0);
        speed.put("Kilomètre/heure", 0.277778);
        speed.put("Mille/heure", 0.44704);
        speed.put("Nœud", 0.514444);
        conversionRates.put("Vitesse", speed);

        // Temps (base: seconde)
        Map<String, Double> time = new HashMap<>();
        time.put("Seconde", 1.0);
        time.put("Minute", 60.0);
        time.put("Heure", 3600.0);
        time.put("Jour", 86400.0);
        time.put("Semaine", 604800.0);
        conversionRates.put("Temps", time);
    }

    private void initializeCategories() {
        categoryComboBox.getItems().addAll(conversionRates.keySet());
        categoryComboBox.setValue("Longueur");
        updateUnits();
    }

    private void setupListeners() {
        categoryComboBox.setOnAction(e -> updateUnits());
        fromUnitComboBox.setOnAction(e -> convert());
        toUnitComboBox.setOnAction(e -> convert());
        inputValue.textProperty().addListener((obs, oldVal, newVal) -> convert());
    }

    private void updateUnits() {
        String category = categoryComboBox.getValue();
        if (category != null && conversionRates.containsKey(category)) {
            Map<String, Double> units = conversionRates.get(category);
            fromUnitComboBox.getItems().clear();
            toUnitComboBox.getItems().clear();
            fromUnitComboBox.getItems().addAll(units.keySet());
            toUnitComboBox.getItems().addAll(units.keySet());

            if (!units.isEmpty()) {
                fromUnitComboBox.setValue(units.keySet().iterator().next());
                toUnitComboBox.setValue(units.keySet().iterator().next());
            }
        }
        convert();
    }

    @FXML
    private void convert() {
        String category = categoryComboBox.getValue();
        String fromUnit = fromUnitComboBox.getValue();
        String toUnit = toUnitComboBox.getValue();
        String inputText = inputValue.getText();

        if (category == null || fromUnit == null || toUnit == null || inputText.isEmpty()) {
            outputValue.setText("");
            return;
        }

        try {
            double value = Double.parseDouble(inputText);
            double result;

            if (category.equals("Température")) {
                result = convertTemperature(value, fromUnit, toUnit);
            } else {
                result = convertStandard(value, category, fromUnit, toUnit);
            }

            outputValue.setText(String.format("%.4f", result));
            resultLabel.setText(value + " " + fromUnit + " = " + String.format("%.4f", result) + " " + toUnit);
        } catch (NumberFormatException e) {
            outputValue.setText("Erreur");
            resultLabel.setText("Veuillez entrer un nombre valide");
        }
    }

    private double convertStandard(double value, String category, String fromUnit, String toUnit) {
        Map<String, Double> rates = conversionRates.get(category);
        double fromRate = rates.get(fromUnit);
        double toRate = rates.get(toUnit);

        // Convertir vers l'unité de base, puis vers l'unité cible
        double baseValue = value * fromRate;
        return baseValue / toRate;
    }

    private double convertTemperature(double value, String fromUnit, String toUnit) {
        if (fromUnit.equals(toUnit)) {
            return value;
        }

        // Convertir vers Celsius d'abord
        double celsius;
        switch (fromUnit) {
            case "Celsius":
                celsius = value;
                break;
            case "Fahrenheit":
                celsius = (value - 32) * 5 / 9;
                break;
            case "Kelvin":
                celsius = value - 273.15;
                break;
            default:
                return value;
        }

        // Convertir de Celsius vers l'unité cible
        switch (toUnit) {
            case "Celsius":
                return celsius;
            case "Fahrenheit":
                return celsius * 9 / 5 + 32;
            case "Kelvin":
                return celsius + 273.15;
            default:
                return celsius;
        }
    }

    @FXML
    private void swapUnits() {
        String temp = fromUnitComboBox.getValue();
        fromUnitComboBox.setValue(toUnitComboBox.getValue());
        toUnitComboBox.setValue(temp);
        convert();
    }

    @FXML
    private void clearFields() {
        inputValue.setText("");
        outputValue.setText("");
        resultLabel.setText("");
    }

    @FXML
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Aide - Convertisseur d'unités");
        alert.setHeaderText("Comment utiliser le convertisseur");
        alert.setContentText(
            "1. Sélectionnez une catégorie (Longueur, Poids, Température, etc.)\n" +
            "2. Choisissez l'unité de départ et l'unité cible\n" +
            "3. Entrez la valeur à convertir\n" +
            "4. Le résultat s'affiche automatiquement\n\n" +
            "Categories disponibles:\n" +
            "- Longueur: mètre, kilomètre, centimètre, millimètre, pied, pouce, mile\n" +
            "- Poids: kilogramme, gramme, milligramme, tonne, livre, once\n" +
            "- Température: Celsius, Fahrenheit, Kelvin\n" +
            "- Volume: litre, millilitre, mètre cube, gallon, pinte\n" +
            "- Vitesse: m/s, km/h, mph, nœud\n" +
            "- Temps: seconde, minute, heure, jour, semaine"
        );
        alert.showAndWait();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/main.fxml")
            );
            loader.setControllerFactory(param -> new com.example.multitools.MainController());

            Parent root = loader.load();

            NavigationManager.getInstance().navigateTo(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdateCheck() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mise à jour");
        alert.setHeaderText(null);
        alert.setContentText("Recherche de mises à jour...");

        Thread updateThread = new Thread(() -> {
            UpdateChecker checker = new UpdateChecker();
            UpdateChecker.UpdateInfo updateInfo = checker.checkForUpdates();

            javafx.application.Platform.runLater(() -> {
                if (updateInfo.hasUpdate) {
                    alert.close();
                    showUpdateDialog(updateInfo);
                } else {
                    alert.close();
                    Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                    infoAlert.setTitle("Mise à jour");
                    infoAlert.setHeaderText(null);
                    infoAlert.setContentText("Vous utilisez déjà la dernière version de Multitools (" +
                            UpdateChecker.getCurrentVersion() + ")");
                    infoAlert.showAndWait();
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

        javafx.scene.control.ButtonType installButton = new javafx.scene.control.ButtonType("OK", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType cancelButton = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(installButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == installButton) {
                startUpdateProcess(updateInfo);
            }
        });
    }

    private void startUpdateProcess(UpdateChecker.UpdateInfo updateInfo) {
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Téléchargement de la mise à jour");
        progressAlert.setHeaderText("Téléchargement en cours...");
        progressAlert.setContentText("Veuillez patienter pendant le téléchargement de la mise à jour.");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);

        VBox content = new VBox(10, new Label("Progression:"), progressBar);
        progressAlert.getDialogPane().setContent(content);
        progressAlert.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        progressAlert.show();

        Thread downloadThread = new Thread(() -> {
            try {
                UpdateChecker checker = new UpdateChecker();
                java.nio.file.Path downloadedFile = checker.downloadUpdate(updateInfo.downloadUrl, progress -> {
                    javafx.application.Platform.runLater(() -> {
                        if (progress >= 0) {
                            progressBar.setProgress(progress);
                        } else {
                            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                        }
                    });
                });

                javafx.application.Platform.runLater(() -> {
                    progressAlert.close();
                    Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                    infoAlert.setTitle("Téléchargement terminé");
                    infoAlert.setHeaderText(null);
                    infoAlert.setContentText("Le fichier a été téléchargé avec succès. L'application va maintenant s'installer et redémarrer.");
                    infoAlert.showAndWait();

                    Thread installThread = new Thread(() -> {
                        try {
                            String currentExePath = UpdateChecker.getCurrentExecutablePath();
                            java.nio.file.Path currentExe = java.nio.file.Path.of(currentExePath);
                            java.nio.file.Path scriptPath = checker.createUpdateScript(currentExe, downloadedFile);
                            UpdateChecker.runUpdateScriptAndExit(scriptPath);
                        } catch (Exception e) {
                            e.printStackTrace();
                            javafx.application.Platform.runLater(() -> {
                                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                                errorAlert.setTitle("Erreur");
                                errorAlert.setHeaderText(null);
                                errorAlert.setContentText("Erreur lors de l'installation: " + e.getMessage());
                                errorAlert.showAndWait();
                            });
                        }
                    });
                    installThread.setDaemon(true);
                    installThread.start();
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    progressAlert.close();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Erreur");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("Erreur lors du téléchargement: " + e.getMessage());
                    errorAlert.showAndWait();
                });
            }
        });
        downloadThread.setDaemon(true);
        downloadThread.start();
    }
}
