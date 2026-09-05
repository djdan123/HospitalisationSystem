package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.AccueilClient;
import com.hospital.grpc.ConsultationClient;
import com.hospital.grpc.accueil.Patient;
import com.hospital.grpc.consultation.Consultation;
import com.hospital.session.UserSession;
import com.hospital.util.FxUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RendezVousController {

    @FXML private Label dateLabel;
    @FXML private TableView<RendezVousRow> table;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;
    @FXML private ComboBox<String> filtreMedecin;
    @FXML private ComboBox<String> filtreStatut;

    private final ConsultationClient consultationClient = new ConsultationClient();
    private final AccueilClient accueilClient = new AccueilClient();

    private final ObservableList<RendezVousRow> data = FXCollections.observableArrayList();
    private LocalDate currentDate = LocalDate.now();

    private final Map<Long, Patient> patientCache = new ConcurrentHashMap<>();
    private final Map<Long, String> medecinCache = new ConcurrentHashMap<>(); // id -> nom

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ===================== MODÈLE DE LIGNE =====================
    public class RendezVousRow {
        private final Consultation consultation;
        private final SimpleStringProperty heure;
        private final SimpleStringProperty patientNom;
        private final SimpleStringProperty medecinNom;
        private final SimpleStringProperty motif;
        private final SimpleObjectProperty<Node> statutLabel;
        private final SimpleObjectProperty<Node> actions;

        public RendezVousRow(Consultation c, Patient p, String medecinNom) {
            this.consultation = c;
            // Heure
            String date = c.getDateConsultation();
            if (date != null && date.length() >= 16) {
                this.heure = new SimpleStringProperty(date.substring(11, 16));
            } else {
                this.heure = new SimpleStringProperty("--:--");
            }
            // Patient
            if (p != null) {
                this.patientNom = new SimpleStringProperty(p.getNom() + " " + p.getPrenom());
            } else {
                this.patientNom = new SimpleStringProperty("Patient #" + c.getPatientId());
            }
            // Médecin
            this.medecinNom = new SimpleStringProperty(medecinNom != null ? medecinNom : "Dr. #" + c.getMedecinId());
            // Motif
            this.motif = new SimpleStringProperty(c.getMotif() != null ? c.getMotif() : "—");
            // Statut
            this.statutLabel = new SimpleObjectProperty<>(createStatusBadge(c.getStatut()));
            // Actions
            this.actions = new SimpleObjectProperty<>(createActionButtons(c));
        }

        private Label createStatusBadge(String statut) {
            Label badge = new Label(statut != null ? statut : "INCONNU");
            badge.getStyleClass().add("badge");
            if (statut == null) {
                badge.getStyleClass().add("badge-info");
            } else {
                switch (statut.toUpperCase()) {
                    case "PLANIFIEE":
                        badge.setText("📅 Planifiée");
                        badge.getStyleClass().add("badge-info");
                        break;
                    case "ARRIVEE":
                        badge.setText("🏥 Arrivé");
                        badge.getStyleClass().add("badge-warning");
                        break;
                    case "EN_COURS":
                        badge.setText("🔄 En consultation");
                        badge.getStyleClass().add("badge-warning");
                        break;
                    case "TERMINEE":
                        badge.setText("✅ Terminée");
                        badge.getStyleClass().add("badge-success");
                        break;
                    case "ANNULEE":
                        badge.setText("❌ Annulée");
                        badge.getStyleClass().add("badge-danger");
                        break;
                    default:
                        badge.getStyleClass().add("badge-info");
                        break;
                }
            }
            return badge;
        }

        private HBox createActionButtons(Consultation c) {
            HBox box = new HBox(8);
            String statut = c.getStatut();
            if ("PLANIFIEE".equals(statut) || "ARRIVEE".equals(statut)) {
                // Pour réceptionniste : Arrivé / En consultation / Annuler
                if ("PLANIFIEE".equals(statut)) {
                    Button arriveBtn = new Button("Arrivé");
                    arriveBtn.getStyleClass().add("btn-primary");
                    arriveBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                    arriveBtn.setOnAction(e -> updateStatus(c, "ARRIVEE"));
                    box.getChildren().add(arriveBtn);
                }
                if ("ARRIVEE".equals(statut)) {
                    Button startBtn = new Button("Démarrer");
                    startBtn.getStyleClass().add("btn-warning");
                    startBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                    startBtn.setOnAction(e -> updateStatus(c, "EN_COURS"));
                    box.getChildren().add(startBtn);
                }
                Button cancelBtn = new Button("Annuler");
                cancelBtn.getStyleClass().add("btn-danger");
                cancelBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                cancelBtn.setOnAction(e -> cancelConsultation(c));
                box.getChildren().add(cancelBtn);
            } else if ("EN_COURS".equals(statut)) {
                Button endBtn = new Button("Terminer");
                endBtn.getStyleClass().add("btn-success");
                endBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                endBtn.setOnAction(e -> endConsultation(c));
                box.getChildren().add(endBtn);
            }
            return box;
        }

        // Getters
        public SimpleStringProperty heureProperty() { return heure; }
        public SimpleStringProperty patientNomProperty() { return patientNom; }
        public SimpleStringProperty medecinNomProperty() { return medecinNom; }
        public SimpleStringProperty motifProperty() { return motif; }
        public SimpleObjectProperty<Node> statutLabelProperty() { return statutLabel; }
        public SimpleObjectProperty<Node> actionsProperty() { return actions; }
    }

    // ===================== INITIALISATION =====================
    @FXML
    public void initialize() {
        // Filtres
        filtreStatut.setItems(FXCollections.observableArrayList("TOUS", "PLANIFIEE", "ARRIVEE", "EN_COURS", "TERMINEE", "ANNULEE"));
        filtreStatut.setValue("TOUS");
        filtreStatut.setOnAction(e -> onRefresh());

        // Afficher le filtre médecin seulement pour le réceptionniste
        UserSession.Role role = UserSession.getInstance().getRole();
        if (role == UserSession.Role.RECEPTIONNISTE || role == UserSession.Role.ADMIN) {
            filtreMedecin.setVisible(true);
            filtreMedecin.setManaged(true);
            chargerMedecins();
        }

        dateLabel.setText("📆 " + currentDate.format(DATE_FMT));
        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        onRefresh();
    }

    private void chargerMedecins() {
        // Simuler une liste de médecins (à remplacer par un appel gRPC plus tard)
        // Pour l'exemple, on ajoute "Tous", "Dr. Dupont", "Dr. Martin", "Dr. Bernard"
        ObservableList<String> medecins = FXCollections.observableArrayList("Tous");
        // On pourrait charger depuis un service, mais on simule
        medecins.addAll("Dr. Dupont", "Dr. Martin", "Dr. Bernard");
        filtreMedecin.setItems(medecins);
        filtreMedecin.setValue("Tous");
    }

    // ===================== CHARGEMENT DES DONNÉES =====================
    @FXML
    private void onRefresh() {
        setLoading(true);
        UserSession.Role role = UserSession.getInstance().getRole();
        Task<List<Consultation>> task;

        if (role == UserSession.Role.MEDECIN) {
            // Médecin : voir ses propres rendez-vous
            long medecinId = UserSession.getInstance().getMedecinId();
            if (medecinId <= 0) medecinId = 1; // fallback
            final long id = medecinId;
            task = new Task<>() {
                @Override protected List<Consultation> call() {
                    return consultationClient.getByDoctor(id);
                }
            };
        } else {
            // Réceptionniste / Admin : voir tous les rendez-vous (ou filtrés par médecin)
            task = new Task<>() {
                @Override protected List<Consultation> call() {
                    return consultationClient.getAll();
                }
            };
        }

        task.setOnSucceeded(e -> {
            List<Consultation> cons = task.getValue();
            loadPatientAndMedecinInfo(cons);
        });
        task.setOnFailed(e -> {
            setLoading(false);
            statusLabel.setText("Erreur");
            FxUtils.showError("Erreur", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    private void loadPatientAndMedecinInfo(List<Consultation> consultations) {
        Task<Void> infoTask = new Task<>() {
            @Override protected Void call() {
                for (Consultation c : consultations) {
                    // Charger patient
                    long pid = c.getPatientId();
                    if (!patientCache.containsKey(pid)) {
                        try {
                            Patient p = accueilClient.getPatient(pid);
                            patientCache.put(pid, p);
                        } catch (Exception ex) {
                            // Ignorer
                        }
                    }
                    // Charger médecin (nom) - pour l'instant, on simule avec un cache basé sur l'ID
                    long mid = c.getMedecinId();
                    if (!medecinCache.containsKey(mid)) {
                        // Simuler un nom de médecin
                        medecinCache.put(mid, "Dr. " + mid);
                    }
                }
                return null;
            }
        };
        infoTask.setOnSucceeded(e -> {
            applyFiltersAndRefresh(consultations);
        });
        infoTask.setOnFailed(e -> {
            setLoading(false);
            statusLabel.setText("Erreur chargement infos");
        });
        new Thread(infoTask).start();
    }

    private void applyFiltersAndRefresh(List<Consultation> consultations) {
        String statutFiltre = filtreStatut.getValue();
        String medecinFiltre = filtreMedecin.getValue();

        List<Consultation> filtered = consultations;
        if (statutFiltre != null && !statutFiltre.equals("TOUS")) {
            filtered = filtered.stream()
                    .filter(c -> statutFiltre.equals(c.getStatut()))
                    .collect(Collectors.toList());
        }
        if (medecinFiltre != null && !medecinFiltre.equals("Tous") && !medecinFiltre.equals("")) {
            // Filtrer par nom de médecin (cache)
            filtered = filtered.stream()
                    .filter(c -> {
                        String nom = medecinCache.getOrDefault(c.getMedecinId(), "");
                        return nom.equals(medecinFiltre);
                    })
                    .collect(Collectors.toList());
        }

        ObservableList<RendezVousRow> rows = FXCollections.observableArrayList();
        for (Consultation c : filtered) {
            Patient p = patientCache.get(c.getPatientId());
            String medNom = medecinCache.getOrDefault(c.getMedecinId(), "Dr. " + c.getMedecinId());
            rows.add(new RendezVousRow(c, p, medNom));
        }
        data.setAll(rows);
        statusLabel.setText(data.size() + " rendez-vous");
        setLoading(false);
    }

    // ===================== CRÉATION DE RENDEZ-VOUS =====================
    @FXML
    private void onNewRendezVous() {
        Dialog<Consultation> dialog = new Dialog<>();
        dialog.setTitle("Nouveau rendez-vous");
        dialog.setHeaderText("Planifier une consultation");
        ButtonType saveBtn = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField patientId = new TextField();
        TextField date = new TextField(LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        TextField motif = new TextField();
        ComboBox<String> medecinCombo = new ComboBox<>();
        // Remplir la combo avec les médecins disponibles (simulé)
        medecinCombo.setItems(FXCollections.observableArrayList("1 - Dr. Dupont", "2 - Dr. Martin", "3 - Dr. Bernard"));
        medecinCombo.setValue("1 - Dr. Dupont");

        grid.add(new Label("Patient ID *"), 0, 0); grid.add(patientId, 1, 0);
        grid.add(new Label("Médecin *"), 0, 1); grid.add(medecinCombo, 1, 1);
        grid.add(new Label("Date (yyyy-MM-dd HH:mm)"), 0, 2); grid.add(date, 1, 2);
        grid.add(new Label("Motif *"), 0, 3); grid.add(motif, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            try {
                long pid = Long.parseLong(patientId.getText().trim());
                String medStr = medecinCombo.getValue();
                long mid = Long.parseLong(medStr.split(" - ")[0]);
                String motifStr = motif.getText().trim();
                String dateStr = date.getText().trim();
                // Créer la consultation
                return consultationClient.createConsultation(pid, mid, dateStr, motifStr, "");
            } catch (Exception ex) {
                FxUtils.showError("Erreur", ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(c -> {
            FxUtils.showSuccess("Rendez-vous créé (id=" + c.getId() + ")");
            onRefresh();
        });
    }

    // ===================== ACTIONS SUR STATUT =====================
    private void updateStatus(Consultation c, String newStatut) {
        Task<Consultation> task = new Task<>() {
            @Override protected Consultation call() {
                return consultationClient.updateConsultation(c.getId(), null, null, null, newStatut);
            }
        };
        task.setOnSucceeded(e -> {
            FxUtils.showSuccess("Statut mis à jour : " + newStatut);
            onRefresh();
        });
        task.setOnFailed(e -> FxUtils.showError("Erreur", task.getException().getMessage()));
        new Thread(task).start();
    }

    private void endConsultation(Consultation c) {
        // Pour terminer, on peut demander un diagnostic et prescription ?
        // Pour simplifier, on passe directement à TERMINEE
        updateStatus(c, "TERMINEE");
    }

    private void cancelConsultation( Consultation c) {
        if (!FxUtils.confirm("Annuler", "Annuler ce rendez-vous ?")) return;
        updateStatus(c, "ANNULEE");
    }

    // ===================== CHANGER DE DATE =====================
    @FXML
    private void onChangeDay() {
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Changer de jour");
        dialog.setHeaderText("Sélectionnez une date");
        ButtonType okBtn = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        DatePicker datePicker = new DatePicker(currentDate);
        datePicker.setPrefWidth(200);
        GridPane grid = new GridPane();
        grid.add(datePicker, 0, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> btn == okBtn ? datePicker.getValue() : null);
        dialog.showAndWait().ifPresent(date -> {
            if (date != null) {
                currentDate = date;
                dateLabel.setText("📆 " + currentDate.format(DATE_FMT));
                onRefresh();
            }
        });
    }

    // ===================== UTILITAIRES =====================
    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        statusLabel.setText(loading ? "Chargement..." : statusLabel.getText());
    }
}
