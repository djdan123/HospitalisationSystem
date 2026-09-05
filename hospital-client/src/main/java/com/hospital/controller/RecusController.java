package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.PaiementClient;
import com.hospital.grpc.paiement.Payment;
import com.hospital.grpc.paiement.ReceiptResponse;
import com.hospital.util.FxUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;

/** Affiche les paiements d'un patient et le reçu émis par le service Paiement. */
public class RecusController {

    @FXML private TextField patientIdField;
    @FXML private TableView<Payment> paymentsTable;
    @FXML private TableColumn<Payment, String> colPaymentId, colFacture, colMontant, colMode, colDate, colReference;
    @FXML private Label receiptNumberLabel, statusLabel;
    @FXML private TextArea receiptContentArea;
    @FXML private ProgressIndicator progress;

    private final PaiementClient client = new PaiementClient();
    private final ObservableList<Payment> payments = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colPaymentId.setCellValueFactory(c -> text(c.getValue().getId()));
        colFacture.setCellValueFactory(c -> text(c.getValue().getFactureId()));
        colMontant.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.0f", c.getValue().getMontant())));
        colMode.setCellValueFactory(c -> text(c.getValue().getModePaiement()));
        colDate.setCellValueFactory(c -> text(c.getValue().getDatePaiement()));
        colReference.setCellValueFactory(c -> text(c.getValue().getReference()));
        paymentsTable.setItems(payments);
        paymentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @FXML
    private void onLoadPayments() {
        Long patientId = patientId();
        if (patientId == null) return;
        setLoading(true);
        Task<List<Payment>> task = new Task<>() {
            @Override protected List<Payment> call() { return client.getPaymentsByPatient(patientId); }
        };
        task.setOnSucceeded(e -> {
            payments.setAll(task.getValue());
            receiptContentArea.clear();
            receiptNumberLabel.setText("Sélectionnez un paiement puis générez son reçu.");
            statusLabel.setText(payments.size() + " paiement(s) trouvé(s).");
            setLoading(false);
        });
        task.setOnFailed(e -> showFailure("Chargement des paiements impossible.", task.getException()));
        new Thread(task, "hms-recus-load").start();
    }

    @FXML
    private void onGenerateReceipt() {
        Payment selected = paymentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxUtils.showWarning("Reçu", "Sélectionnez un paiement dans la liste.");
            return;
        }
        setLoading(true);
        Task<ReceiptResponse> task = new Task<>() {
            @Override protected ReceiptResponse call() { return client.generateReceipt(selected.getId()); }
        };
        task.setOnSucceeded(e -> {
            ReceiptResponse receipt = task.getValue();
            receiptNumberLabel.setText("Reçu " + receipt.getNumeroRecu() + " · émis le " + receipt.getDateEmission());
            receiptContentArea.setText(receipt.getContenu());
            statusLabel.setText("Reçu généré.");
            setLoading(false);
        });
        task.setOnFailed(e -> showFailure("Génération du reçu impossible.", task.getException()));
        new Thread(task, "hms-recus-generate").start();
    }

    private Long patientId() {
        try {
            String value = patientIdField.getText() == null ? "" : patientIdField.getText().trim();
            if (value.isBlank()) throw new NumberFormatException();
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            FxUtils.showWarning("Reçus", "Saisissez un ID patient valide.");
            return null;
        }
    }

    private SimpleStringProperty text(Object value) { return new SimpleStringProperty(String.valueOf(value == null ? "—" : value)); }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        if (loading) statusLabel.setText("Chargement...");
    }

    private void showFailure(String fallback, Throwable error) {
        setLoading(false);
        statusLabel.setText("Erreur");
        FxUtils.showError("Reçus", error instanceof GrpcClientException ? error.getMessage() : fallback);
    }
}
