package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.ConsultationClient;
import com.hospital.grpc.HospitalisationClient;
import com.hospital.grpc.PharmacieClient;
import com.hospital.grpc.consultation.Consultation;
import com.hospital.grpc.hospitalisation.Hospitalisation;
import com.hospital.grpc.pharmacie.Ordonnance;
import com.hospital.util.FxUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class HistoriqueController {

    @FXML private TextField patientIdField;
    @FXML private TableView<Consultation> tableConsultations;
    @FXML private TableView<Hospitalisation> tableHospitalisations;
    @FXML private TableView<Ordonnance> tableOrdonnances;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;

    private final ConsultationClient consultationClient = new ConsultationClient();
    private final HospitalisationClient hospitalisationClient = new HospitalisationClient();
    private final PharmacieClient pharmacieClient = new PharmacieClient();

    private final ObservableList<Consultation> consultations = FXCollections.observableArrayList();
    private final ObservableList<Hospitalisation> hospitalisations = FXCollections.observableArrayList();
    private final ObservableList<Ordonnance> ordonnances = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // ---------- Table des consultations ----------
        tableConsultations.setItems(consultations);
        // Colonne ID
        TableColumn<Consultation, String> colIdCons = (TableColumn<Consultation, String>) tableConsultations.getColumns().get(0);
        colIdCons.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getId())));

        TableColumn<Consultation, String> colDateCons = (TableColumn<Consultation, String>) tableConsultations.getColumns().get(1);
        colDateCons.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateConsultation()));

        TableColumn<Consultation, String> colMotifCons = (TableColumn<Consultation, String>) tableConsultations.getColumns().get(2);
        colMotifCons.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMotif()));

        TableColumn<Consultation, String> colMedecinCons = (TableColumn<Consultation, String>) tableConsultations.getColumns().get(3);
        colMedecinCons.setCellValueFactory(cellData -> new SimpleStringProperty("Dr. " + cellData.getValue().getMedecinId())); // À améliorer avec nom réel

        TableColumn<Consultation, String> colDiagCons = (TableColumn<Consultation, String>) tableConsultations.getColumns().get(4);
        colDiagCons.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDiagnostic()));

        TableColumn<Consultation, String> colStatutCons = (TableColumn<Consultation, String>) tableConsultations.getColumns().get(5);
        colStatutCons.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatut()));

        // ---------- Table des hospitalisations ----------
        tableHospitalisations.setItems(hospitalisations);
        TableColumn<Hospitalisation, String> colIdHosp = (TableColumn<Hospitalisation, String>) tableHospitalisations.getColumns().get(0);
        colIdHosp.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getId())));

        TableColumn<Hospitalisation, String> colAdmHosp = (TableColumn<Hospitalisation, String>) tableHospitalisations.getColumns().get(1);
        colAdmHosp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateAdmission()));

        TableColumn<Hospitalisation, String> colSortieHosp = (TableColumn<Hospitalisation, String>) tableHospitalisations.getColumns().get(2);
        colSortieHosp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateSortie()));

        TableColumn<Hospitalisation, String> colMotifHosp = (TableColumn<Hospitalisation, String>) tableHospitalisations.getColumns().get(3);
        colMotifHosp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMotif()));

        TableColumn<Hospitalisation, String> colStatutHosp = (TableColumn<Hospitalisation, String>) tableHospitalisations.getColumns().get(4);
        colStatutHosp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatut()));

        // ---------- Table des ordonnances ----------
        tableOrdonnances.setItems(ordonnances);
        TableColumn<Ordonnance, String> colIdOrd = (TableColumn<Ordonnance, String>) tableOrdonnances.getColumns().get(0);
        colIdOrd.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getId())));

        TableColumn<Ordonnance, String> colDateOrd = (TableColumn<Ordonnance, String>) tableOrdonnances.getColumns().get(1);
        colDateOrd.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateOrdonnance()));

        TableColumn<Ordonnance, String> colStatutOrd = (TableColumn<Ordonnance, String>) tableOrdonnances.getColumns().get(2);
        colStatutOrd.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatut()));
    }

    @FXML
    private void onLoad() {
        String idStr = patientIdField.getText() != null ? patientIdField.getText().trim() : "";
        if (idStr.isBlank()) {
            FxUtils.showWarning("Historique", "Saisissez l'ID du patient.");
            return;
        }
        long patientId;
        try {
            patientId = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            FxUtils.showWarning("Historique", "ID patient invalide.");
            return;
        }

        setLoading(true);
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                try {
                    List<Consultation> cons = consultationClient.getByPatient(patientId);
                    Platform.runLater(() -> consultations.setAll(cons));

                    List<Hospitalisation> hosp = hospitalisationClient.getHospitalisations(patientId, null);
                    Platform.runLater(() -> hospitalisations.setAll(hosp));

                    List<Ordonnance> ords = pharmacieClient.getOrdonnancesByPatient(patientId);
                    Platform.runLater(() -> ordonnances.setAll(ords));

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        FxUtils.showError("Erreur", e.getMessage());
                        statusLabel.setText("Erreur");
                    });
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            setLoading(false);
            statusLabel.setText("Historique chargé");
        });
        task.setOnFailed(e -> {
            setLoading(false);
            statusLabel.setText("Erreur");
            FxUtils.showError("Erreur", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        statusLabel.setText(loading ? "Chargement..." : statusLabel.getText());
    }
}