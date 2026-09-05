package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.LaboratoireClient;
import com.hospital.grpc.laboratoire.Analyse;
import com.hospital.grpc.laboratoire.Resultat;
import com.hospital.util.FxUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ResultatsController {

    @FXML private TextField patientIdField;
    @FXML private TableView<Analyse> table;
    @FXML private TableColumn<Analyse, String> colId, colPatient, colType, colDate, colStatut, colResultat;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;

    private final LaboratoireClient client = new LaboratoireClient();
    private final ObservableList<Analyse> data = FXCollections.observableArrayList();
    private final Map<Long, Resultat> resultats = new ConcurrentHashMap<>();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPatient.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getPatientId())));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeAnalyse"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateDemande"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colResultat.setCellValueFactory(c -> {
            Resultat resultat = resultats.get(c.getValue().getId());
            if (resultat == null) return new SimpleStringProperty("—");
            String valeur = resultat.getValeur();
            String unite = resultat.getUnite();
            String interpretation = resultat.getInterpretation();
            String resume = valeur + (unite.isBlank() ? "" : " " + unite);
            return new SimpleStringProperty(interpretation.isBlank() ? resume : resume + " · " + interpretation);
        });

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
            FxUtils.showWarning("Résultats", "Saisissez l'ID du patient.");
            return;
        }
        long patientId;
        try {
            patientId = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            FxUtils.showWarning("Résultats", "ID patient invalide.");
            return;
        }
        setLoading(true);
        Task<List<Analyse>> task = new Task<>() {
            @Override protected List<Analyse> call() {
                List<Analyse> all = client.getByPatient(patientId);
                List<Analyse> terminees = all.stream()
                        .filter(a -> "TERMINEE".equals(a.getStatut()))
                        .collect(Collectors.toList());
                resultats.clear();
                for (Analyse analyse : terminees) {
                    try {
                        resultats.put(analyse.getId(), client.getResultat(analyse.getId()));
                    } catch (Exception ignored) {
                        // Une analyse terminée peut ne pas encore avoir de résultat détaillé.
                    }
                }
                return terminees;
            }
        };
        task.setOnSucceeded(e -> {
            data.setAll(task.getValue());
            table.refresh();
            statusLabel.setText(data.size() + " analyse(s) avec résultats");
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

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        if (loading) statusLabel.setText("Chargement...");
    }
}