package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.AccueilClient;
import com.hospital.grpc.ConsultationClient;
import com.hospital.grpc.PharmacieClient;
import com.hospital.grpc.accueil.Patient;
import com.hospital.grpc.consultation.Consultation;
import com.hospital.grpc.pharmacie.Ordonnance;
import com.hospital.grpc.pharmacie.OrdonnanceLigne;
import com.hospital.session.UserSession;
import com.hospital.util.FxUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DossiersController {

    @FXML private TextField patientIdField;
    @FXML private Label patientInfo, nomLabel, prenomLabel, naissanceLabel, ippLabel;
    @FXML private VBox alertesContainer;
    @FXML private Label maladiesLabel, operationsLabel, familiauxLabel;
    @FXML private TableView<TraitementRow> traitementTable;
    @FXML private Label tensionLabel, poulsLabel, poidsLabel, temperatureLabel, constanteDateLabel;
    @FXML private ListView<String> documentsList;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;

    private final AccueilClient accueilClient = new AccueilClient();
    private final ConsultationClient consultationClient = new ConsultationClient();
    private final PharmacieClient pharmacieClient = new PharmacieClient();

    private final ObservableList<TraitementRow> traitementData = FXCollections.observableArrayList();
    private final ObservableList<String> documentsData = FXCollections.observableArrayList();

    private Patient currentPatient = null;

    // Classe interne pour les lignes de traitement
    public static class TraitementRow {
        private final SimpleStringProperty medicament;
        private final SimpleStringProperty posologie;
        private final SimpleStringProperty debut;
        private final SimpleStringProperty fin;

        public TraitementRow(String medicament, String posologie, String debut, String fin) {
            this.medicament = new SimpleStringProperty(medicament);
            this.posologie = new SimpleStringProperty(posologie);
            this.debut = new SimpleStringProperty(debut);
            this.fin = new SimpleStringProperty(fin);
        }
        public SimpleStringProperty medicamentProperty() { return medicament; }
        public SimpleStringProperty posologieProperty() { return posologie; }
        public SimpleStringProperty debutProperty() { return debut; }
        public SimpleStringProperty finProperty() { return fin; }
    }

    @FXML
    public void initialize() {
        traitementTable.setItems(traitementData);
        // Configurer les colonnes (déjà fait dans FXML avec PropertyValueFactory)
        traitementTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        documentsList.setItems(documentsData);

        // Exemple d'alertes (seront remplacées par les données réelles)
        afficherAlertes(null);
        afficherConstantes(null);
    }

    @FXML
    private void onRefresh() {
        if (patientIdField.getText() != null && !patientIdField.getText().isBlank()) {
            onLoad();
        }
    }

    @FXML
    private void onLoad() {
        String idStr = patientIdField.getText() != null ? patientIdField.getText().trim() : "";
        if (idStr.isBlank()) {
            FxUtils.showWarning("Dossier", "Saisissez l'ID du patient.");
            return;
        }
        long patientId;
        try {
            patientId = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            FxUtils.showWarning("Dossier", "ID patient invalide.");
            return;
        }

        setLoading(true);
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                try {
                    // 1. Récupérer les infos du patient
                    Patient p = accueilClient.getPatient(patientId);
                    currentPatient = p;

                    // 2. Mettre à jour l'identité
                    Platform.runLater(() -> updateIdentity(p));

                    // 3. Alertes (simulées pour l'instant, à améliorer avec un service dédié)
                    Platform.runLater(() -> afficherAlertes(p));

                    // 4. Antécédents (simulés)
                    Platform.runLater(() -> afficherAntecedents(p));

                    // 5. Traitement en cours (via ordonnances récentes)
                    List<Ordonnance> ordonnances = pharmacieClient.getOrdonnancesByPatient(patientId);
                    Platform.runLater(() -> updateTraitement(ordonnances));

                    // 6. Constantes vitales (simulées)
                    Platform.runLater(() -> afficherConstantes(p));

                    // 7. Documents récents (simulés)
                    Platform.runLater(() -> afficherDocuments(p));

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
            statusLabel.setText("Dossier chargé");
        });
        task.setOnFailed(e -> {
            setLoading(false);
            statusLabel.setText("Erreur");
            Throwable ex = task.getException();
            FxUtils.showError("Erreur", ex instanceof GrpcClientException ? ex.getMessage() : "Chargement impossible.");
        });
        new Thread(task).start();
    }

    // ===================== MISE À JOUR DE L'IDENTITÉ =====================
    private void updateIdentity(Patient p) {
        if (p == null) {
            patientInfo.setText("Patient non trouvé");
            return;
        }
        patientInfo.setText("Patient : " + p.getNom() + " " + p.getPrenom());
        nomLabel.setText(p.getNom() != null ? p.getNom() : "—");
        prenomLabel.setText(p.getPrenom() != null ? p.getPrenom() : "—");
        naissanceLabel.setText(p.getDateNaissance() != null ? p.getDateNaissance() : "—");
        ippLabel.setText(p.getNumeroDossier() != null ? p.getNumeroDossier() : "—");
    }

    // ===================== ALERTES CRITIQUES =====================
    private void afficherAlertes(Patient p) {
        alertesContainer.getChildren().clear();
        // Pour l'exemple, on simule des alertes
        // Dans la réalité, ces données viendraient d'un service dédié
        String[] alertes = {
                "⚠️ Allergie : Pénicilline (grave)",
                "⚠️ Insuffisance rénale chronique",
                "⚠️ Groupe sanguin : A+"
        };
        for (String alerte : alertes) {
            Label label = new Label(alerte);
            label.setStyle("-fx-background-color: #fee2e2; -fx-padding: 4 8; -fx-background-radius: 6; -fx-text-fill: #991b1b;");
            alertesContainer.getChildren().add(label);
        }
        if (alertesContainer.getChildren().isEmpty()) {
            Label none = new Label("Aucune alerte critique");
            none.setStyle("-fx-text-fill: #64748b;");
            alertesContainer.getChildren().add(none);
        }
    }

    // ===================== ANTÉCÉDENTS =====================
    private void afficherAntecedents(Patient p) {
        // Simulé
        maladiesLabel.setText("Hypertension, Diabète type 2");
        operationsLabel.setText("Appendicectomie (2018), Hernie inguinale (2020)");
        familiauxLabel.setText("Père : diabète, Mère : hypertension");
    }

    // ===================== TRAITEMENT EN COURS =====================
    private void updateTraitement(List<Ordonnance> ordonnances) {
        traitementData.clear();
        if (ordonnances == null || ordonnances.isEmpty()) {
            traitementData.add(new TraitementRow("Aucun traitement prescrit", "", "", ""));
            return;
        }
        // Prendre les ordonnances les plus récentes (on suppose que les lignes contiennent les médicaments)
        Ordonnance recente = ordonnances.get(0);
        for (OrdonnanceLigne ligne : recente.getLignesList()) {
            traitementData.add(new TraitementRow(
                    ligne.getNomMedicament(),
                    ligne.getPosologie() != null ? ligne.getPosologie() : "—",
                    recente.getDateOrdonnance().substring(0, 10),
                    "En cours"
            ));
        }
    }

    // ===================== CONSTANTES VITALES =====================
    private void afficherConstantes(Patient p) {
        // Simulé
        tensionLabel.setText("120/80 mmHg");
        poulsLabel.setText("72 bpm");
        poidsLabel.setText("78 kg");
        temperatureLabel.setText("36.7 °C");
        constanteDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    // ===================== DOCUMENTS RÉCENTS =====================
    private void afficherDocuments(Patient p) {
        documentsData.clear();
        // Simulé
        documentsData.addAll(
                "📄 Biologie – NFS (15/08/2026)",
                "📄 Radiographie thoracique (10/08/2026)",
                "📄 Échographie abdominale (02/08/2026)"
        );
    }

    // ===================== UTILITAIRES =====================
    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        statusLabel.setText(loading ? "Chargement..." : statusLabel.getText());
    }
}