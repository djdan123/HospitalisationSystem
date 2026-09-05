package com.hospital.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Utilitaires JavaFX (notifications, dialogs, thread UI).
 */
public final class FxUtils {

    private FxUtils() {}

    public static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    public static void showInfo(String title, String message) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void showSuccess(String message) {
        showInfo("Succès", message);
    }

    public static void showError(String title, String message) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void showWarning(String title, String message) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Affiche un overlay de chargement sur un StackPane parent.
     */
    public static Region createLoadingOverlay(String text) {
        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(50, 50);
        Label label = new Label(text != null ? text : "Chargement...");
        label.setStyle("-fx-text-fill: #1e3a5f; -fx-font-size: 13px;");
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(12, pi, label);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.85);");
        box.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return box;
    }
}
