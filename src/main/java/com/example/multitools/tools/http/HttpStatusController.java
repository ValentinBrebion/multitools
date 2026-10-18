package com.example.multitools.tools.http;

import com.example.multitools.core.NavigationManager;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class HttpStatusController {

    @FXML
    private TextField searchField;

    @FXML
    private ListView<HttpStatusInfo> statusList;

    @FXML
    private Label resultCountLabel;

    @FXML
    private Label statusCodeLabel;

    @FXML
    private Label statusPhraseLabel;

    @FXML
    private Label statusClassLabel;

    @FXML
    private Label meaningLabel;

    @FXML
    private VBox causesBox;

    @FXML
    private VBox troubleshootingBox;

    private FilteredList<HttpStatusInfo> filteredStatuses;

    @FXML
    public void initialize() {
        filteredStatuses = new FilteredList<>(
            FXCollections.observableArrayList(HttpStatusCatalog.all()),
            status -> true
        );
        statusList.setItems(filteredStatuses);
        statusList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(HttpStatusInfo status, boolean empty) {
                super.updateItem(status, empty);
                setText(empty || status == null ? null : status.toString());
                setStyle(empty ? "" : "-fx-padding: 10 12; -fx-font-size: 14px;");
            }
        });
        statusList.getSelectionModel().selectedItemProperty().addListener(
            (observable, previous, selected) -> showStatus(selected)
        );
        searchField.textProperty().addListener(
            (observable, previous, query) -> filterStatuses(query)
        );

        statusList.getSelectionModel().select(HttpStatusCatalog.findByCode(401).orElseThrow());
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/security/security.fxml"));
            loader.setControllerFactory(type -> new com.example.multitools.tools.security.SecurityController());
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (IOException e) {
            showNavigationError(e);
        }
    }

    private void filterStatuses(String query) {
        String normalized = query == null ? "" : query.strip().toLowerCase(java.util.Locale.ROOT);
        filteredStatuses.setPredicate(status -> normalized.isEmpty()
            || status.searchText().contains(normalized));
        resultCountLabel.setText(filteredStatuses.size() + " code(s)");

        if (!filteredStatuses.isEmpty()) {
            HttpStatusInfo exactCodeMatch = filteredStatuses.stream()
                .filter(status -> Integer.toString(status.code()).equals(normalized))
                .findFirst()
                .orElse(filteredStatuses.get(0));
            statusList.getSelectionModel().select(exactCodeMatch);
            statusList.scrollTo(exactCodeMatch);
        } else {
            statusList.getSelectionModel().clearSelection();
            showStatus(null);
        }
    }

    private void showStatus(HttpStatusInfo status) {
        causesBox.getChildren().clear();
        troubleshootingBox.getChildren().clear();
        if (status == null) {
            statusCodeLabel.setText("—");
            statusPhraseLabel.setText("Aucun code trouvé");
            statusClassLabel.setText("");
            meaningLabel.setText("Essayez un numéro comme 401, 403, 404 ou 502, ou recherchez par mot-clé.");
            return;
        }

        statusCodeLabel.setText(Integer.toString(status.code()));
        statusPhraseLabel.setText(status.phrase());
        statusClassLabel.setText(classDescription(status.code()));
        meaningLabel.setText(status.meaning());
        addBulletPoints(causesBox, status.commonCauses());
        addBulletPoints(troubleshootingBox, status.troubleshooting());
    }

    private void addBulletPoints(VBox container, java.util.List<String> points) {
        for (String point : points) {
            Label label = new Label("•  " + point);
            label.setWrapText(true);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-padding: 2 0;");
            container.getChildren().add(label);
        }
    }

    private String classDescription(int code) {
        int statusClass = code / 100;
        String description = switch (statusClass) {
            case 1 -> "1xx · Information";
            case 2 -> "2xx · Succès";
            case 3 -> "3xx · Redirection";
            case 4 -> "4xx · Erreur côté client";
            case 5 -> "5xx · Erreur côté serveur";
            default -> "Code HTTP";
        };
        return description;
    }

    private void showNavigationError(IOException exception) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Erreur");
        alert.setHeaderText("Impossible de revenir au menu principal");
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
