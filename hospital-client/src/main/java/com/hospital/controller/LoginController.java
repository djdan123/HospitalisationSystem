package com.hospital.controller;

import com.hospital.config.AppConfig;
import com.hospital.config.GrpcConfig;
import com.hospital.session.UserSession;
import com.hospital.util.FxUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordBtn;
    @FXML private Button loginBtn;
    @FXML private Label errorLabel;
    @FXML private Label serverStatusLabel;

    private boolean passwordVisible = false;

    @FXML
    public void initialize() {
        checkServerStatus();
        passwordField.setOnAction(e -> onLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());
    }

    private void checkServerStatus() {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return GrpcConfig.isServerReachable("accueil", AppConfig.getPort("accueil"));
            }
        };
        task.setOnSucceeded(e -> {
            boolean ok = task.getValue();
            if (ok) {
                serverStatusLabel.setText("● Serveur connecté");
                serverStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #166534;");
            } else {
                serverStatusLabel.setText("● Serveur déconnecté");
                serverStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #991b1b;");
            }
        });
        task.setOnFailed(e -> {
            serverStatusLabel.setText("● Serveur déconnecté");
            serverStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #991b1b;");
        });
        new Thread(task).start();
    }

    @FXML
    private void togglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordVisibleField.setText(passwordField.getText());
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordBtn.setText("🙈");
        } else {
            passwordField.setText(passwordVisibleField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            togglePasswordBtn.setText("👁");
        }
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String password = passwordVisible
                ? (passwordVisibleField.getText() != null ? passwordVisibleField.getText() : "")
                : (passwordField.getText() != null ? passwordField.getText() : "");

        hideError();

        if (username.isBlank() || password.isBlank()) {
            showError("Veuillez saisir l'identifiant et le mot de passe.");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Connexion...");

        Task<UserSession.Role> task = new Task<>() {
            @Override
            protected UserSession.Role call() throws Exception {
                Thread.sleep(400);
                return authenticate(username, password);
            }
        };

        task.setOnSucceeded(e -> {
            UserSession.Role role = task.getValue();
            if (role == null) {
                showError("Identifiant ou mot de passe incorrect.");
                loginBtn.setDisable(false);
                loginBtn.setText("SE CONNECTER");
                return;
            }
            UserSession session = UserSession.getInstance();
            session.login(username, username.toUpperCase(), role);

            // --- AJOUT : définir medecinId si le rôle est MEDECIN ---
            if (role == UserSession.Role.MEDECIN) {
                // Pour l'instant, on fixe un ID (à adapter pour une vraie authentification)
                session.setMedecinId(1L);
            } else {
                session.setMedecinId(0L);
            }

            log.info("Utilisateur connecté: {} ({})", username, role);
            openDashboard();
        });

        task.setOnFailed(e -> {
            showError("Erreur de connexion. Réessayez.");
            loginBtn.setDisable(false);
            loginBtn.setText("SE CONNECTER");
        });

        new Thread(task).start();
    }

    private UserSession.Role authenticate(String username, String password) {
        // Pour les tests : utilisateurs fictifs
        return switch (username.toLowerCase()) {
            case "admin" -> "admin123".equals(password) ? UserSession.Role.ADMIN : null;
            case "medecin" -> "med123".equals(password) ? UserSession.Role.MEDECIN : null;
            case "recep" -> "recep123".equals(password) ? UserSession.Role.RECEPTIONNISTE : null;
            case "pharma" -> "pharma123".equals(password) ? UserSession.Role.PHARMACIEN : null;
            case "caisse" -> "caisse123".equals(password) ? UserSession.Role.CAISSIER : null;
            default -> null;
        };
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(AppConfig.getAppTitle() + " — Dashboard");
            stage.setMaximized(true);
            stage.centerOnScreen();
        } catch (IOException ex) {
            log.error("Impossible de charger le dashboard", ex);
            showError("Erreur de chargement de l'interface.");
            loginBtn.setDisable(false);
            loginBtn.setText("SE CONNECTER");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}