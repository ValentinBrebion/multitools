package com.example.multitools.tools.security;

import com.example.multitools.core.NavigationManager;
import org.pcap4j.core.NotOpenException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class NetworkAnalyzerController {

    private static final int MAX_DISPLAYED_PACKETS = 1_000;
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    @FXML
    private ComboBox<NetworkPacketCaptureService.CaptureInterface> interfaceSelector;

    @FXML
    private Button refreshButton;

    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;

    @FXML
    private Button clearButton;

    @FXML
    private Label summaryLabel;

    @FXML
    private TableView<NetworkPacketCaptureService.PacketInfo> packetTable;

    @FXML
    private TableColumn<NetworkPacketCaptureService.PacketInfo, String> timeColumn;

    @FXML
    private TableColumn<NetworkPacketCaptureService.PacketInfo, String> protocolColumn;

    @FXML
    private TableColumn<NetworkPacketCaptureService.PacketInfo, String> sourceColumn;

    @FXML
    private TableColumn<NetworkPacketCaptureService.PacketInfo, String> destinationColumn;

    @FXML
    private TableColumn<NetworkPacketCaptureService.PacketInfo, String> lengthColumn;

    private final NetworkPacketCaptureService captureService = new NetworkPacketCaptureService();
    private final BlockingQueue<NetworkPacketCaptureService.PacketInfo> pendingPackets =
        new ArrayBlockingQueue<>(MAX_DISPLAYED_PACKETS);
    private final Timeline tableRefresh = new Timeline(
        new KeyFrame(Duration.millis(120), event -> appendPendingPackets())
    );
    private Task<Void> captureTask;

    @FXML
    public void initialize() {
        timeColumn.setCellValueFactory(cell ->
            new ReadOnlyStringWrapper(TIME_FORMAT.format(cell.getValue().capturedAt())));
        protocolColumn.setCellValueFactory(cell ->
            new ReadOnlyStringWrapper(cell.getValue().protocol()));
        sourceColumn.setCellValueFactory(cell ->
            new ReadOnlyStringWrapper(cell.getValue().source()));
        destinationColumn.setCellValueFactory(cell ->
            new ReadOnlyStringWrapper(cell.getValue().destination()));
        lengthColumn.setCellValueFactory(cell ->
            new ReadOnlyStringWrapper(cell.getValue().length() + " octets"));
        packetTable.setItems(FXCollections.observableArrayList());
        packetTable.setPlaceholder(new Label("Démarrez la capture pour voir les paquets."));
        tableRefresh.setCycleCount(Timeline.INDEFINITE);
        stopButton.setDisable(true);
        loadInterfaces();
    }

    @FXML
    private void handleRefreshInterfaces() {
        if (captureTask == null || !captureTask.isRunning()) {
            loadInterfaces();
        }
    }

    @FXML
    private void handleStartCapture() {
        NetworkPacketCaptureService.CaptureInterface selected = interfaceSelector.getValue();
        if (selected == null) {
            summaryLabel.setText("Sélectionnez d'abord une interface réseau.");
            summaryLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 14px;");
            return;
        }

        setCaptureRunning(true);
        summaryLabel.setText("Capture en cours sur « " + selected.description() + " »…");
        summaryLabel.setStyle("-fx-text-fill: #166534; -fx-font-size: 14px; -fx-font-weight: bold;");
        tableRefresh.play();

        captureTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                captureService.capture(selected.name(), packet -> {
                    if (!pendingPackets.offer(packet)) {
                        pendingPackets.poll();
                        pendingPackets.offer(packet);
                    }
                });
                return null;
            }
        };
        captureTask.setOnSucceeded(event -> finishCapture());
        captureTask.setOnCancelled(event -> finishCapture());
        captureTask.setOnFailed(event -> {
            finishCapture();
            summaryLabel.setText(captureError(captureTask.getException()));
            summaryLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 14px;");
        });

        Thread captureThread = new Thread(captureTask, "network-packet-capture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    @FXML
    private void handleStopCapture() {
        stopButton.setDisable(true);
        summaryLabel.setText("Arrêt de la capture…");
        try {
            captureService.stopCapture();
        } catch (NotOpenException e) {
            summaryLabel.setText("La capture est déjà en cours de fermeture.");
            summaryLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");
        }
    }

    @FXML
    private void handleClearPackets() {
        packetTable.getItems().clear();
        pendingPackets.clear();
    }

    @FXML
    private void handleBack() {
        try {
            captureService.stopCapture();
        } catch (NotOpenException e) {
            summaryLabel.setText("La capture est déjà en cours de fermeture.");
            summaryLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");
        }
        tableRefresh.stop();
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/security/security.fxml")
            );
            loader.setControllerFactory(type -> new SecurityController());
            Parent root = loader.load();
            NavigationManager.getInstance().navigateTo(root);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de revenir au menu Sécurité");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void loadInterfaces() {
        refreshButton.setDisable(true);
        interfaceSelector.setDisable(true);
        startButton.setDisable(true);
        summaryLabel.setText("Recherche des interfaces réseau…");
        summaryLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");

        Task<java.util.List<NetworkPacketCaptureService.CaptureInterface>> task = new Task<>() {
            @Override
            protected java.util.List<NetworkPacketCaptureService.CaptureInterface> call() throws Exception {
                return captureService.findInterfaces();
            }
        };
        task.setOnSucceeded(event -> {
            interfaceSelector.getItems().setAll(task.getValue());
            if (!task.getValue().isEmpty()) {
                interfaceSelector.getSelectionModel().selectFirst();
                summaryLabel.setText("Interface(s) Wi-Fi uniquement : choisissez-en une puis démarrez la capture.");
            } else {
                summaryLabel.setText("Aucune interface Wi-Fi active détectée. Vérifiez que le Wi-Fi est activé et que son pilote de capture fonctionne.");
            }
            summaryLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");
            refreshButton.setDisable(false);
            interfaceSelector.setDisable(task.getValue().isEmpty());
            startButton.setDisable(task.getValue().isEmpty());
        });
        task.setOnFailed(event -> {
            refreshButton.setDisable(false);
            interfaceSelector.setDisable(true);
            startButton.setDisable(true);
            summaryLabel.setText(captureError(task.getException()));
            summaryLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 14px;");
        });
        Thread discoveryThread = new Thread(task, "network-interface-discovery");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    private void appendPendingPackets() {
        NetworkPacketCaptureService.PacketInfo packet;
        int appended = 0;
        while (appended < 100 && (packet = pendingPackets.poll()) != null) {
            packetTable.getItems().add(packet);
            appended++;
        }
        trimPacketTable();
    }

    private void flushPendingPackets() {
        NetworkPacketCaptureService.PacketInfo packet;
        while ((packet = pendingPackets.poll()) != null) {
            packetTable.getItems().add(packet);
        }
        trimPacketTable();
    }

    private void trimPacketTable() {
        int excess = packetTable.getItems().size() - MAX_DISPLAYED_PACKETS;
        if (excess > 0) {
            packetTable.getItems().remove(0, excess);
        }
    }

    private void finishCapture() {
        flushPendingPackets();
        tableRefresh.stop();
        setCaptureRunning(false);
        summaryLabel.setText("Capture arrêtée. " + packetTable.getItems().size()
            + " paquet(s) conservé(s) dans la liste temporaire.");
        summaryLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");
    }

    private void setCaptureRunning(boolean running) {
        startButton.setDisable(running || interfaceSelector.getValue() == null);
        stopButton.setDisable(!running);
        refreshButton.setDisable(running);
        interfaceSelector.setDisable(running);
    }

    private String captureError(Throwable error) {
        Throwable cause = error;
        while (cause != null && !(cause instanceof UnsatisfiedLinkError)) {
            cause = cause.getCause();
        }
        if (cause instanceof UnsatisfiedLinkError) {
            return "La capture nécessite Npcap sur Windows (ou libpcap sur Linux/macOS). Installez le pilote puis relancez l'application.";
        }
        if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
            return "La capture n'a pas pu démarrer. Vérifiez l'interface Wi-Fi choisie et les autorisations de capture.";
        }
        return "La capture a échoué : " + error.getMessage()
            + ". Npcap est chargé ; vérifiez l'interface Wi-Fi choisie et les autorisations de capture.";
    }
}
