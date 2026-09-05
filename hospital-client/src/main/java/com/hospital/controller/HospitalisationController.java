package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.AccueilClient;
import com.hospital.grpc.HospitalisationClient;
import com.hospital.grpc.accueil.Patient;
import com.hospital.grpc.hospitalisation.Hospitalisation;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HospitalisationController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private TableView<HospitalisationRow> table;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;

    private final HospitalisationClient hospClient = new HospitalisationClient();
    private final AccueilClient accueilClient = new AccueilClient();
    private final ObservableList<HospitalisationRow> data = FXCollections.observableArrayList();
    private final Map<Long, Patient> patientCache = new ConcurrentHashMap<>();

    // ---------- Classe interne (non statique) ----------
    public class HospitalisationRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty patientNom;
        private final SimpleStringProperty dateAdmission;
        private final SimpleStringProperty motif;
        private final SimpleStringProperty chambre;
        private final SimpleStringProperty typeChambre;
        private final SimpleStringProperty statut;
        private final Node actions;

        public HospitalisationRow(Hospitalisation h, Patient p) {
            this.id = new SimpleStringProperty(String.valueOf(h.getId()));
            this.dateAdmission = new SimpleStringProperty(h.getDateAdmission());
            this.motif = new SimpleStringProperty(h.getMotif());
            this.statut = new SimpleStringProperty(h.getStatut());
            this.chambre = new SimpleStringProperty(h.getNumeroChambre() != null && !h.getNumeroChambre().isBlank()
                    ? h.getNumeroChambre() : "—");
            this.typeChambre = new SimpleStringProperty(detectTypeChambre(h));
            this.patientNom = new SimpleStringProperty(p != null ? p.getNom() + " " + p.getPrenom() : "Patient #" + h.getPatientId());

            HBox box = new HBox(8);
            String stat = h.getStatut();
            if ("EN_ATTENTE".equals(stat)) {
                Button assignBtn = new Button("Attribuer chambre");
                assignBtn.getStyleClass().add("btn-primary");
                assignBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                assignBtn.setOnAction(e -> assignRoom(h));          // appel direct, sans "this"
                box.getChildren().add(assignBtn);
            } else if ("EN_COURS".equals(stat)) {
                Button sortieBtn = new Button("Sortie");
                sortieBtn.getStyleClass().add("btn-danger");
                sortieBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                sortieBtn.setOnAction(e -> discharge(h));           // appel direct
                box.getChildren().add(sortieBtn);
            } else {
                Label done = new Label("Terminé");
                done.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                box.getChildren().add(done);
            }
            this.actions = box;
        }

        private String detectTypeChambre(Hospitalisation h) {
            String num = h.getNumeroChambre();
            if (num == null || num.isBlank()) return "—";
            if (num.startsWith("V")) return "VIP";
            if (num.startsWith("P")) return "Privée";
            return "Commune";
        }

        // Getters pour les propriétés
        public SimpleStringProperty idProperty() { return id; }
        public SimpleStringProperty patientNomProperty() { return patientNom; }
        public SimpleStringProperty dateAdmissionProperty() { return dateAdmission; }
        public SimpleStringProperty motifProperty() { return motif; }
        public SimpleStringProperty chambreProperty() { return chambre; }
        public SimpleStringProperty typeChambreProperty() { return typeChambre; }
        public SimpleStringProperty statutProperty() { return statut; }
        public Node getActions() { return actions; }
    }

    // ---------- Initialisation ----------
    @FXML
    public void initialize() {
        filtreStatut.setItems(FXCollections.observableArrayList("TOUS", "EN_ATTENTE", "EN_COURS", "SORTI"));
        filtreStatut.setValue("EN_COURS");
        filtreStatut.setOnAction(e -> onRefresh());

        // Configuration des colonnes du tableau
        TableColumn<HospitalisationRow, String> colId = (TableColumn<HospitalisationRow, String>) table.getColumns().get(0);
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());

        TableColumn<HospitalisationRow, String> colPatient = (TableColumn<HospitalisationRow, String>) table.getColumns().get(1);
        colPatient.setCellValueFactory(cellData -> cellData.getValue().patientNomProperty());

        TableColumn<HospitalisationRow, String> colDate = (TableColumn<HospitalisationRow, String>) table.getColumns().get(2);
        colDate.setCellValueFactory(cellData -> cellData.getValue().dateAdmissionProperty());

        TableColumn<HospitalisationRow, String> colMotif = (TableColumn<HospitalisationRow, String>) table.getColumns().get(3);
        colMotif.setCellValueFactory(cellData -> cellData.getValue().motifProperty());

        TableColumn<HospitalisationRow, String> colChambre = (TableColumn<HospitalisationRow, String>) table.getColumns().get(4);
        colChambre.setCellValueFactory(cellData -> cellData.getValue().chambreProperty());

        TableColumn<HospitalisationRow, String> colType = (TableColumn<HospitalisationRow, String>) table.getColumns().get(5);
        colType.setCellValueFactory(cellData -> cellData.getValue().typeChambreProperty());

        TableColumn<HospitalisationRow, String> colStatut = (TableColumn<HospitalisationRow, String>) table.getColumns().get(6);
        colStatut.setCellValueFactory(cellData -> cellData.getValue().statutProperty());

        TableColumn<HospitalisationRow, Void> colActions = (TableColumn<HospitalisationRow, Void>) table.getColumns().get(7);
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

    // ---------- Chargement des données ----------
    @FXML
    private void onRefresh() {
        String statut = filtreStatut.getValue();
        if ("TOUS".equals(statut)) statut = null;
        final String statutFiltre = statut;
        setLoading(true);

        Task<List<Hospitalisation>> task = new Task<>() {
            @Override protected List<Hospitalisation> call() {
                return hospClient.getHospitalisations(null, statutFiltre);
            }
        };
        task.setOnSucceeded(e -> {
            List<Hospitalisation> hospList = task.getValue();
            loadPatientsAndRefresh(hospList);
        });
        task.setOnFailed(e -> {
            setLoading(false);
            statusLabel.setText("Erreur");
            FxUtils.showError("Erreur", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    private void loadPatientsAndRefresh(List<Hospitalisation> hospList) {
        Task<Void> patientTask = new Task<>() {
            @Override protected Void call() {
                for (Hospitalisation h : hospList) {
                    long pid = h.getPatientId();
                    if (!patientCache.containsKey(pid)) {
                        try {
                            Patient p = accueilClient.getPatient(pid);
                            patientCache.put(pid, p);
                        } catch (Exception ignored) {}
                    }
                }
                return null;
            }
        };
        patientTask.setOnSucceeded(e -> {
            ObservableList<HospitalisationRow> rows = FXCollections.observableArrayList();
            for (Hospitalisation h : hospList) {
                Patient p = patientCache.get(h.getPatientId());
                rows.add(new HospitalisationRow(h, p));
            }
            data.setAll(rows);
            statusLabel.setText(data.size() + " hospitalisation(s)");
            setLoading(false);
        });
        patientTask.setOnFailed(e -> {
            setLoading(false);
            statusLabel.setText("Erreur chargement patients");
        });
        new Thread(patientTask).start();
    }

    @FXML
    private void onSearch() {
        String term = searchField.getText() != null ? searchField.getText().trim() : "";
        if (term.isBlank()) {
            onRefresh();
            return;
        }
        try {
            long patientId = Long.parseLong(term);
            List<Hospitalisation> list = hospClient.getHospitalisations(patientId, null);
            loadPatientsAndRefresh(list);
        } catch (NumberFormatException e) {
            FxUtils.showWarning("Recherche", "Utilisez l'ID patient pour rechercher.");
        }
    }

    // ---------- Admission ----------
    @FXML
    private void onAdmit() {
        Dialog<Hospitalisation> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle admission");
        dialog.setHeaderText("Enregistrer l'admission d'un patient");
        ButtonType saveBtn = new ButtonType("Admettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField patientId = new TextField();
        patientId.setPromptText("ID patient");
        TextField motif = new TextField();
        motif.setPromptText("Motif de l'hospitalisation");
        TextField obs = new TextField();
        obs.setPromptText("Observations");

        grid.add(new Label("Patient *"), 0, 0); grid.add(patientId, 1, 0);
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
                Hospitalisation h = hospClient.admitPatient(pid, motif.getText().trim(), obs.getText(), null, null);
                return h;
            } catch (Exception ex) {
                FxUtils.showError("Erreur", ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(h -> {
            FxUtils.showSuccess("Admission enregistrée (id=" + h.getId() + ")");
            onRefresh();
        });
    }

    // ---------- Attribuer une chambre ----------
    private void assignRoom(Hospitalisation h) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Attribuer une chambre");
        dialog.setHeaderText("Choisissez une chambre pour le patient " + h.getPatientId());

        ButtonType okBtn = new ButtonType("Attribuer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        ComboBox<String> chambres = new ComboBox<>(FXCollections.observableArrayList(
                "A101 (Commune)", "A102 (Commune)", "B201 (Privée)", "B202 (Privée)", "C301 (VIP)"
        ));
        chambres.setValue("A101 (Commune)");
        grid.add(new Label("Chambre"), 0, 0); grid.add(chambres, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> btn == okBtn ? chambres.getValue() : null);
        dialog.showAndWait().ifPresent(selected -> {
            if (selected != null) {
                // Simuler l'attribution (à adapter avec le vrai service)
                long chambreId = 101;
                long litId = 1;
                try {
                    Hospitalisation updated = hospClient.assignRoom(h.getId(), chambreId, litId);
                    FxUtils.showSuccess("Chambre attribuée : " + selected);
                    onRefresh();
                } catch (Exception ex) {
                    FxUtils.showError("Erreur", ex.getMessage());
                }
            }
        });
    }

    // ---------- Sortie ----------
    private void discharge(Hospitalisation h) {
        if (!FxUtils.confirm("Sortie", "Enregistrer la sortie du patient ?")) return;
        Task<Hospitalisation> task = new Task<>() {
            @Override protected Hospitalisation call() {
                return hospClient.dischargePatient(h.getId(), "Sortie du patient");
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
        statusLabel.setText(loading ? "Chargement..." : statusLabel.getText());
    }
}