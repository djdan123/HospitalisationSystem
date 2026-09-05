package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.PaiementClient;
import com.hospital.grpc.paiement.Facture;
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

public class PaiementController {

    @FXML private TextField patientIdField;
    @FXML private TableView<Facture> table;
    @FXML private TableColumn<Facture, String> colNumero, colPatient, colTotal, colPaye, colRestant, colStatut, colDate;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;

    private final PaiementClient client = new PaiementClient();
    private final ObservableList<Facture> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroFacture()));
        colPatient.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getPatientId())));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.0f", c.getValue().getMontantTotal())));
        colPaye.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.0f", c.getValue().getMontantPaye())));
        colRestant.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.0f", c.getValue().getMontantRestant())));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateCreation()));

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
            FxUtils.showWarning("Factures", "Saisissez l'ID du patient.");
            return;
        }
        long patientId;
        try { patientId = Long.parseLong(idStr); }
        catch (NumberFormatException e) {
            FxUtils.showWarning("Factures", "ID patient invalide.");
            return;
        }
        setLoading(true);
        Task<List<Facture>> task = new Task<>() {
            @Override protected List<Facture> call() {
                return client.getFacturesByPatient(patientId);
            }
        };
        task.setOnSucceeded(e -> {
            data.setAll(task.getValue());
            statusLabel.setText(data.size() + " facture(s)");
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
    private void onNewFacture() {
        Dialog<Facture> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle facture");
        dialog.setHeaderText("Créer une facture");
        ButtonType saveBtn = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField patientId = new TextField(patientIdField.getText());
        TextField montant = new TextField();
        TextField desc = new TextField();
        grid.add(new Label("Patient ID *"), 0, 0); grid.add(patientId, 1, 0);
        grid.add(new Label("Montant *"), 0, 1); grid.add(montant, 1, 1);
        grid.add(new Label("Description"), 0, 2); grid.add(desc, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            try {
                long pid = Long.parseLong(patientId.getText().trim());
                double m = Double.parseDouble(montant.getText().trim().replace(",", "."));
                return client.createFacture(pid, m, desc.getText());
            } catch (Exception ex) {
                FxUtils.showError("Erreur", ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(f -> {
            FxUtils.showSuccess("Facture créée : " + f.getNumeroFacture());
            patientIdField.setText(String.valueOf(f.getPatientId()));
            onLoadByPatient();
        });
    }

    @FXML
    private void onPay() {
        Facture selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxUtils.showWarning("Paiement", "Sélectionnez une facture.");
            return;
        }
        if ("PAYEE".equals(selected.getStatut())) {
            FxUtils.showInfo("Paiement", "Cette facture est déjà payée.");
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Enregistrer un paiement");
        dialog.setHeaderText("Facture " + selected.getNumeroFacture() + " — Restant : " + selected.getMontantRestant());
        ButtonType saveBtn = new ButtonType("Payer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField montant = new TextField(String.valueOf(selected.getMontantRestant()));
        ComboBox<String> mode = new ComboBox<>(FXCollections.observableArrayList("CASH", "MOBILE_MONEY", "CARTE", "VIREMENT"));
        mode.setValue("CASH");
        TextField ref = new TextField();
        grid.add(new Label("Montant *"), 0, 0); grid.add(montant, 1, 0);
        grid.add(new Label("Mode *"), 0, 1); grid.add(mode, 1, 1);
        grid.add(new Label("Référence"), 0, 2); grid.add(ref, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return false;
            try {
                double m = Double.parseDouble(montant.getText().trim().replace(",", "."));
                client.makePayment(selected.getId(), m, mode.getValue(), ref.getText());
                return true;
            } catch (Exception ex) {
                FxUtils.showError("Erreur", ex.getMessage());
                return false;
            }
        });

        dialog.showAndWait().ifPresent(ok -> {
            if (Boolean.TRUE.equals(ok)) {
                FxUtils.showSuccess("Paiement enregistré.");
                onLoadByPatient();
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        if (loading) statusLabel.setText("Chargement...");
    }
}
