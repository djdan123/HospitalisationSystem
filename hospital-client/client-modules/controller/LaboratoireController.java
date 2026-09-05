package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.LaboratoireClient;
import com.hospital.grpc.laboratoire.Analyse;
import com.hospital.util.FxUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.List;

public class LaboratoireController {

    @FXML private TextField patientIdField;
    @FXML private TableView<Analyse> table;
    @FXML private TableColumn<Analyse, String> colId, colPatient, colType, colDate, colStatut, colObs;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;

    private final LaboratoireClient client = new LaboratoireClient();
    private final ObservableList<Analyse> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colPatient.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getPatientId())));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeAnalyse()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateDemande()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
        colObs.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getObservations()));

        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @FXML
    private void onRefresh() {
        if (patientIdField.getText() != null && !patientIdField.getText().isBlank()) {
            onLoadByPatient();
        }
    }

    @FXML
    private void onLoadByPatient() {
        String idStr = patientIdField.getText() != null ? patientIdField.getText().trim() : "";
        if (idStr.isBlank()) {
            FxUtils.showWarning("Analyses", "Saisissez l'ID du patient.");
            return;
        }
        long patientId;
        try { patientId = Long.parseLong(idStr); }
        catch (NumberFormatException e) {
            FxUtils.showWarning("Analyses", "ID patient invalide.");
            return;
        }
        setLoading(true);
        Task<List<Analyse>> task = new Task<>() {
            @Override protected List<Analyse> call() {
                return client.getByPatient(patientId);
            }
        };
        task.setOnSucceeded(e -> {
            data.setAll(task.getValue());
            statusLabel.setText(data.size() + " analyse(s)");
            setLoading(false);
        });
        task.setOnFailed(e -> {
            setLoading(false);
            statusLabel.setText("Erreur");
            Throwable ex = task.getException();
            FxUtils.showError("Erreur", ex instanceof GrpcClientException ? ex.getMessage() : "Chargement impossible.");
        });
        new Thread(task).start();
    }

    @FXML
    private void onNew() {
        Dialog<Analyse> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle analyse");
        dialog.setHeaderText("Demander une analyse");
        ButtonType saveBtn = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField patientId = new TextField(patientIdField.getText());
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(
                "NFS", "Glycémie", "Créatinine", "Bilan hépatique", "CRP", "Groupe sanguin", "Autre"));
        type.setValue("NFS");
        TextField obs = new TextField();
        grid.add(new Label("Patient ID *"), 0, 0); grid.add(patientId, 1, 0);
        grid.add(new Label("Type *"), 0, 1); grid.add(type, 1, 1);
        grid.add(new Label("Observations"), 0, 2); grid.add(obs, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            try {
                long pid = Long.parseLong(patientId.getText().trim());
                return client.createAnalyse(pid, type.getValue(), obs.getText());
            } catch (Exception ex) {
                FxUtils.showError("Erreur", ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(a -> {
            FxUtils.showSuccess("Analyse créée (id=" + a.getId() + ")");
            patientIdField.setText(String.valueOf(a.getPatientId()));
            onLoadByPatient();
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        if (loading) statusLabel.setText("Chargement...");
    }
}
