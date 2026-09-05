package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.HospitalisationClient;
import com.hospital.grpc.hospitalisation.Hospitalisation;
import com.hospital.util.FxUtils;
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

public class HospitalisationController {

    @FXML private ComboBox<String> filtreStatut;
    @FXML private TableView<Hospitalisation> table;
    @FXML private TableColumn<Hospitalisation, String> colId, colPatient, colAdmission, colMotif, colChambre, colStatut;
    @FXML private TableColumn<Hospitalisation, Void> colActions;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;

    private final HospitalisationClient client = new HospitalisationClient();
    private final ObservableList<Hospitalisation> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        filtreStatut.setItems(FXCollections.observableArrayList("TOUS", "EN_COURS", "SORTI", "TRANSFERE"));
        filtreStatut.setValue("EN_COURS");

        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colPatient.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getPatientId())));
        colAdmission.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateAdmission()));
        colMotif.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMotif()));
        colChambre.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNumeroChambre().isBlank() ? "—" : c.getValue().getNumeroChambre()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnSortie = new Button("Sortie");
            {
                btnSortie.getStyleClass().add("btn-outline");
                btnSortie.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                btnSortie.setOnAction(e -> {
                    Hospitalisation h = getTableView().getItems().get(getIndex());
                    onDischarge(h);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Hospitalisation h = getTableView().getItems().get(getIndex());
                setGraphic("EN_COURS".equals(h.getStatut()) ? btnSortie : null);
            }
        });

        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        onRefresh();
    }

    @FXML
    private void onRefresh() {
        String statut = filtreStatut.getValue();
        if ("TOUS".equals(statut)) statut = null;
        final String s = statut;
        setLoading(true);
        Task<List<Hospitalisation>> task = new Task<>() {
            @Override protected List<Hospitalisation> call() {
                return client.getHospitalisations(null, s);
            }
        };
        task.setOnSucceeded(e -> {
            data.setAll(task.getValue());
            statusLabel.setText(data.size() + " hospitalisation(s)");
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
    private void onAdmit() {
        Dialog<Hospitalisation> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle admission");
        dialog.setHeaderText("Admettre un patient");
        ButtonType saveBtn = new ButtonType("Admettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField patientId = new TextField();
        TextField motif = new TextField();
        TextField obs = new TextField();
        grid.add(new Label("Patient ID *"), 0, 0); grid.add(patientId, 1, 0);
        grid.add(new Label("Motif *"), 0, 1); grid.add(motif, 1, 1);
        grid.add(new Label("Observations"), 0, 2); grid.add(obs, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            if (patientId.getText().isBlank() || motif.getText().isBlank()) {
                FxUtils.showWarning("Validation", "Patient ID et Motif obligatoires.");
                return null;
            }
            try {
                long pid = Long.parseLong(patientId.getText().trim());
                return client.admitPatient(pid, motif.getText().trim(), obs.getText(), null, null);
            } catch (Exception ex) {
                FxUtils.showError("Erreur", ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(h -> {
            FxUtils.showSuccess("Admission créée (id=" + h.getId() + ")");
            filtreStatut.setValue("EN_COURS");
            onRefresh();
        });
    }

    private void onDischarge(Hospitalisation h) {
        if (!FxUtils.confirm("Sortie", "Enregistrer la sortie de l'hospitalisation #" + h.getId() + " ?")) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                client.dischargePatient(h.getId(), "Sortie depuis le client");
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            FxUtils.showSuccess("Patient sorti.");
            onRefresh();
        });
        task.setOnFailed(e -> FxUtils.showError("Erreur", task.getException().getMessage()));
        new Thread(task).start();
    }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        if (loading) statusLabel.setText("Chargement...");
    }
}
