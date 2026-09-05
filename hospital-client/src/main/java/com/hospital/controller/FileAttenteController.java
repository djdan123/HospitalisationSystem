package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.AccueilClient;
import com.hospital.grpc.ConsultationClient;
import com.hospital.grpc.accueil.Patient;
import com.hospital.grpc.consultation.Consultation;
import com.hospital.util.FxUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FileAttenteController {

    @FXML private Label dateLabel;
    @FXML private TableView<FileAttenteRow> table;
    @FXML private ComboBox<String> filtreMedecin;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;

    private final ConsultationClient consultationClient = new ConsultationClient();
    private final AccueilClient accueilClient = new AccueilClient();
    private final ObservableList<FileAttenteRow> data = FXCollections.observableArrayList();
    private final Map<Long, Patient> patientCache = new ConcurrentHashMap<>();
    private final Map<Long, String> medecinNomCache = new ConcurrentHashMap<>();

    private LocalDate currentDate = LocalDate.now();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Modèle de ligne
    public class FileAttenteRow {
        private final Consultation consultation;
        private final SimpleStringProperty heure;
        private final SimpleStringProperty patientNom;
        private final SimpleStringProperty medecinNom;
        private final SimpleStringProperty motif;
        private final SimpleStringProperty statut;
        private final Node actions;

        public FileAttenteRow(Consultation c, Patient p, String medecinName) {
            this.consultation = c;
            String date = c.getDateConsultation();
            this.heure = new SimpleStringProperty(date != null && date.length() >= 16 ? date.substring(11, 16) : "--:--");
            this.patientNom = new SimpleStringProperty(p != null ? p.getNom() + " " + p.getPrenom() : "Patient #" + c.getPatientId());
            this.medecinNom = new SimpleStringProperty(medecinName != null ? medecinName : "Dr. " + c.getMedecinId());
            this.motif = new SimpleStringProperty(c.getMotif() != null ? c.getMotif() : "—");
            this.statut = new SimpleStringProperty(c.getStatut() != null ? c.getStatut() : "—");

            // Actions selon statut
            HBox box = new HBox(8);
            if ("PLANIFIEE".equals(c.getStatut())) {
                Button checkInBtn = new Button("⬇️ Arrivée");
                checkInBtn.getStyleClass().add("btn-primary");
                checkInBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                checkInBtn.setOnAction(e -> checkIn(c));
                box.getChildren().add(checkInBtn);
            } else if ("EN_COURS".equals(c.getStatut())) {
                Label lbl = new Label("🔄 En consultation");
                lbl.setStyle("-fx-text-fill: #92400e;");
                box.getChildren().add(lbl);
            } else if ("TERMINEE".equals(c.getStatut())) {
                Label lbl = new Label("✅ Terminée");
                lbl.setStyle("-fx-text-fill: #166534;");
                box.getChildren().add(lbl);
            } else {
                Label lbl = new Label(c.getStatut());
                lbl.setStyle("-fx-text-fill: #64748b;");
                box.getChildren().add(lbl);
            }
            this.actions = box;
        }

        // Getters pour les colonnes
        public SimpleStringProperty heureProperty() { return heure; }
        public SimpleStringProperty patientNomProperty() { return patientNom; }
        public SimpleStringProperty medecinNomProperty() { return medecinNom; }
        public SimpleStringProperty motifProperty() { return motif; }
        public SimpleStringProperty statutProperty() { return statut; }
        public Node getActions() { return actions; }
    }

    @FXML
    public void initialize() {
        dateLabel.setText("📆 " + currentDate.format(DATE_FMT));

        // Filtres
        filtreStatut.setItems(FXCollections.observableArrayList("TOUS", "PLANIFIEE", "EN_COURS", "TERMINEE"));
        filtreStatut.setValue("TOUS");
        filtreStatut.setOnAction(e -> onRefresh());

        filtreMedecin.setItems(FXCollections.observableArrayList("Tous"));
        filtreMedecin.setValue("Tous");
        filtreMedecin.setOnAction(e -> onRefresh());

        // Configuration des colonnes
        TableColumn<FileAttenteRow, String> colHeure = (TableColumn<FileAttenteRow, String>) table.getColumns().get(0);
        colHeure.setCellValueFactory(cell -> cell.getValue().heureProperty());

        TableColumn<FileAttenteRow, String> colPatient = (TableColumn<FileAttenteRow, String>) table.getColumns().get(1);
        colPatient.setCellValueFactory(cell -> cell.getValue().patientNomProperty());

        TableColumn<FileAttenteRow, String> colMedecin = (TableColumn<FileAttenteRow, String>) table.getColumns().get(2);
        colMedecin.setCellValueFactory(cell -> cell.getValue().medecinNomProperty());

        TableColumn<FileAttenteRow, String> colMotif = (TableColumn<FileAttenteRow, String>) table.getColumns().get(3);
        colMotif.setCellValueFactory(cell -> cell.getValue().motifProperty());

        TableColumn<FileAttenteRow, String> colStatut = (TableColumn<FileAttenteRow, String>) table.getColumns().get(4);
        colStatut.setCellValueFactory(cell -> cell.getValue().statutProperty());

        TableColumn<FileAttenteRow, Void> colActions = (TableColumn<FileAttenteRow, Void>) table.getColumns().get(5);
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(getTableView().getItems().get(getIndex()).getActions());
            }
        });

        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        onRefresh();
    }

    @FXML
    private void onRefresh() {
        setLoading(true);
        // Récupérer toutes les consultations pour aujourd'hui (ou selon date)
        Task<List<Consultation>> task = new Task<>() {
            @Override protected List<Consultation> call() {
                return consultationClient.getAll();
            }
        };
        task.setOnSucceeded(e -> {
            List<Consultation> consultations = task.getValue();
            loadPatientsAndRefresh(consultations);
        });
        task.setOnFailed(e -> {
            setLoading(false);
            statusLabel.setText("Erreur");
            FxUtils.showError("Erreur", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    private void loadPatientsAndRefresh(List<Consultation> consultations) {
        // Filtrer sur la date d'aujourd'hui (si la date est présente)
        String todayStr = currentDate.toString();
        List<Consultation> todayCons = consultations.stream()
                .filter(c -> c.getDateConsultation() != null && c.getDateConsultation().startsWith(todayStr))
                .collect(Collectors.toList());

        // Charger les patients et noms des médecins
        Task<Void> patientTask = new Task<>() {
            @Override protected Void call() {
                for (Consultation c : todayCons) {
                    long pid = c.getPatientId();
                    if (!patientCache.containsKey(pid)) {
                        try {
                            Patient p = accueilClient.getPatient(pid);
                            patientCache.put(pid, p);
                        } catch (Exception ignored) {}
                    }
                    medecinNomCache.put(c.getMedecinId(), "Dr. " + c.getMedecinId());
                }
                return null;
            }
        };
        patientTask.setOnSucceeded(e -> {
            // Appliquer les filtres
            String statutFiltre = filtreStatut.getValue();
            String medecinFiltre = filtreMedecin.getValue();

            String selectedDoctor = medecinFiltre;
            filtreMedecin.setItems(FXCollections.observableArrayList("Tous"));
            medecinNomCache.values().stream().distinct().sorted().forEach(filtreMedecin.getItems()::add);
            filtreMedecin.setValue(selectedDoctor != null && filtreMedecin.getItems().contains(selectedDoctor) ? selectedDoctor : "Tous");

            List<Consultation> filtered = todayCons;
            if (statutFiltre != null && !"TOUS".equals(statutFiltre)) {
                filtered = filtered.stream().filter(c -> statutFiltre.equals(c.getStatut())).collect(Collectors.toList());
            }
            if (medecinFiltre != null && !"Tous".equals(medecinFiltre) && !medecinFiltre.isBlank()) {
                String doctor = medecinFiltre;
                filtered = filtered.stream()
                        .filter(c -> doctor.equals(medecinNomCache.get(c.getMedecinId())))
                        .collect(Collectors.toList());
            }

            ObservableList<FileAttenteRow> rows = FXCollections.observableArrayList();
            for (Consultation c : filtered) {
                Patient p = patientCache.get(c.getPatientId());
                String medName = medecinNomCache.getOrDefault(c.getMedecinId(), "Dr. " + c.getMedecinId());
                rows.add(new FileAttenteRow(c, p, medName));
            }
            data.setAll(rows);
            statusLabel.setText(data.size() + " patient(s) en file d'attente");
            setLoading(false);
        });
        patientTask.setOnFailed(e -> {
            setLoading(false);
            statusLabel.setText("Erreur chargement patients");
        });
        new Thread(patientTask).start();
    }

    // Action : Check-in (marquer arrivé)
    private void checkIn(Consultation c) {
        // Passer le statut à EN_COURS (le médecin pourra le terminer)
        // Ou on peut créer un statut ARRIVEE si disponible
        if (!FxUtils.confirm("Check-in", "Marquer le patient comme arrivé ?")) return;
        Task<Consultation> task = new Task<>() {
            @Override protected Consultation call() {
                return consultationClient.updateConsultation(c.getId(), null, null, null, "EN_COURS");
            }
        };
        task.setOnSucceeded(e -> {
            FxUtils.showSuccess("Patient marqué comme en consultation.");
            onRefresh();
        });
        task.setOnFailed(e -> FxUtils.showError("Erreur", task.getException().getMessage()));
        new Thread(task).start();
    }

    // Action : Ajouter un patient à la file (check-in sans rendez-vous ?)
    @FXML
    private void onCheckIn() {
        // Ouvrir une boîte de dialogue pour rechercher un patient et créer une consultation rapide
        Dialog<Consultation> dialog = new Dialog<>();
        dialog.setTitle("Check-in patient");
        dialog.setHeaderText("Ajouter un patient à la file d'attente");
        ButtonType okBtn = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField patientIdField = new TextField();
        patientIdField.setPromptText("ID patient");
        ComboBox<String> medecinBox = new ComboBox<>(FXCollections.observableArrayList(filtreMedecin.getItems()));
        medecinBox.setValue(medecinBox.getItems().size() > 1 ? medecinBox.getItems().get(1) : "Dr. 1");
        TextField motifField = new TextField("Consultation générale");
        grid.add(new Label("Patient ID *"), 0, 0); grid.add(patientIdField, 1, 0);
        grid.add(new Label("Médecin"), 0, 1); grid.add(medecinBox, 1, 1);
        grid.add(new Label("Motif"), 0, 2); grid.add(motifField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != okBtn) return null;
            try {
                long pid = Long.parseLong(patientIdField.getText().trim());
                // Créer une consultation pour aujourd'hui avec statut PLANIFIEE (ou EN_COURS pour l'ajouter directement)
                long medecinId = Long.parseLong(medecinBox.getValue().replace("Dr. ", "").trim());
                String motif = motifField.getText();
                String date = LocalDate.now().toString() + " 10:00"; // heure par défaut
                return consultationClient.createConsultation(pid, medecinId, date, motif, "");
            } catch (Exception ex) {
                FxUtils.showError("Erreur", ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(c -> {
            FxUtils.showSuccess("Patient ajouté à la file.");
            onRefresh();
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        statusLabel.setText(loading ? "Chargement..." : statusLabel.getText());
    }
}
