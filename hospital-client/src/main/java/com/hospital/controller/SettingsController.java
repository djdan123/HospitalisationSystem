package com.hospital.controller;

import com.hospital.config.AppConfig;
import com.hospital.config.GrpcConfig;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class SettingsController {

    @FXML private TextField hostField;
    @FXML private TextField portAccueil;
    @FXML private Label testResult;

    @FXML
    public void initialize() {
        hostField.setText(AppConfig.getServerHost());
        portAccueil.setText(String.valueOf(AppConfig.getPort("accueil")));
        hostField.setEditable(false);
        portAccueil.setEditable(false);
    }

    @FXML
    private void testConnection() {
        testResult.setText("Test en cours...");
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return GrpcConfig.isServerReachable("accueil", AppConfig.getPort("accueil"));
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                testResult.setText("● Serveur joignable");
                testResult.setStyle("-fx-text-fill: #166534;");
            } else {
                testResult.setText("● Serveur inaccessible");
                testResult.setStyle("-fx-text-fill: #991b1b;");
            }
        });
        task.setOnFailed(e -> {
            testResult.setText("● Erreur de test");
            testResult.setStyle("-fx-text-fill: #991b1b;");
        });
        new Thread(task).start();
    }
}
