package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.ConsultationClient;
import com.hospital.grpc.consultation.Consultation;
import com.hospital.util.FxUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Optional;

public class ConsultationController {

    @FXML private TextField searchField;
    @FXML private TableView<Consultation> table;
    @FXML private TableColumn<Consultation, String> colId, colPatient, colDate, colMotif, colStatut;
    @FXML private TableColumn<Consultation, Void> colActions;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;

    private final ConsultationClient client = new ConsultationClient();
    private final ObservableList<Consultation> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colPatient.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getPatientId())));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateConsultation()));
        colMotif.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMotif()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnCancel = new Button("Annuler");
            {
                btnCancel.getStyleClass().add("btn-danger");
                btnCancel.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                btnCancel.setOnAction(e -> {
                    Consultation c = getTableView().getItems().get(getIndex());
                    onCancel(c);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnCancel);
            }
        });

        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        statusLabel.setText("Entrez un ID patient puis Rechercher, ou créez une consultation");
    }

    @FXML
    private void onRefresh() {
        String id = searchField.getText() != null ? searchField.getText().trim() : "";
        if (id.isBlank()) {
            statusLabel.setText("Indiquez un ID patient pour charger ses consultations");
            return;
        }
        onSearch();
    }

    @FXML
    private void onSearch() {
        String idStr = searchField.getText() != null ? searchField.getText().trim() : "";
        if (idStr.isBlank()) {
            FxUtils.showWarning("Recherche", "Saisissez l'ID du patient.");
            return;
        }
        long patientId;
        try { patientId = Long.parseLong(idStr); }
        catch (NumberFormatException e) {
            FxUtils.showWarning("Recherche", "ID patient invalide.");
            return;
        }
        setLoading(true);
        Task<List<Consultation>> task = new Task<>() {
            @Override protected List<Consultation> call() {
                return client.getByPatient(patientId);
            }
        };
        task.setOnSucceeded(e -> {
            data.setAll(task.getValue());
            statusLabel.setText(data.size() + " consultation(s)");
            setLoading(false);
        });
        task.setOnFailed(e -> {
            setLoading(false);
            statusLabel.setText("Erreur");
            Throwable ex = task.getException();
            FxUtils.showError("Erreur", ex instanceof GrpcClientException ? ex.getMessage() : "Impossible de charger les consultations.");
        });
        new Thread(task).start();
    }

    @FXML
    private void onNew() {
        Dialog<Consultation> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle consultation");
        dialog.setHeaderText("Créer une consultation");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField patientId = new TextField();
        TextField medecinId = new TextField("1");
        TextField motif = new TextField();
        TextField obs = new TextField();
        grid.add(new Label("Patient ID *"), 0, 0); grid.add(patientId, 1, 0);
        grid.add(new Label("Médecin ID"), 0, 1); grid.add(medecinId, 1, 1);
        grid.add(new Label("Motif *"), 0, 2); grid.add(motif, 1, 2);
        grid.add(new Label("Observations"), 0, 3); grid.add(obs, 1, 3);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            if (patientId.getText().isBlank() || motif.getText().isBlank()) {
                FxUtils.showWarning("Validation", "Patient ID et Motif obligatoires.");
                return null;
            }
            try {
                long pid = Long.parseLong(patientId.getText().trim());
                long mid = medecinId.getText().isBlank() ? 0 : Long.parseLong(medecinId.getText().trim());
                return client.createConsultation(pid, mid, null, motif.getText().trim(), obs.getText());
            } catch (Exception ex) {
                FxUtils.showError("Erreur", ex.getMessage());
                return null;
            }
        });

        Optional<Consultation> result = dialog.showAndWait();
        result.ifPresent(c -> {
            FxUtils.showSuccess("Consultation créée (id=" + c.getId() + ")");
            searchField.setText(String.valueOf(c.getPatientId()));
            onSearch();
        });
    }

    private void onCancel(Consultation c) {
        if (!FxUtils.confirm("Annulation", "Annuler la consultation #" + c.getId() + " ?")) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                client.cancelConsultation(c.getId(), "Annulée depuis le client");
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            FxUtils.showSuccess("Consultation annulée.");
            onSearch();
        });
        task.setOnFailed(e -> FxUtils.showError("Erreur", task.getException().getMessage()));
        new Thread(task).start();
    }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        if (loading) statusLabel.setText("Chargement...");
    }
}
