package com.hospital.controller;

import com.hospital.config.AppConfig;
import com.hospital.session.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Vue locale de la session active : aucune donnée sensible n'est stockée dans le FXML. */
public class ProfilController {

    @FXML private Label fullNameLabel, usernameLabel, roleLabel, sessionLabel;
    @FXML private Label serverLabel, deadlineLabel, themeLabel;

    @FXML
    public void initialize() { refresh(); }

    @FXML
    private void onRefresh() { refresh(); }

    private void refresh() {
        UserSession session = UserSession.getInstance();
        fullNameLabel.setText(value(session.getFullName()));
        usernameLabel.setText(value(session.getUsername()));
        roleLabel.setText(session.getRole() == null ? "—" : session.getRole().name());
        sessionLabel.setText(session.isAuthenticated() ? "Session active" : "Non connecté");
        serverLabel.setText(AppConfig.getServerHost() + ":" + AppConfig.getPort("accueil"));
        deadlineLabel.setText(AppConfig.getDeadlineSeconds() + " secondes");
        themeLabel.setText(AppConfig.getTheme());
    }

    private String value(String value) { return value == null || value.isBlank() ? "—" : value; }
}
