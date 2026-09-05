package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.PharmacieClient;
import com.hospital.grpc.pharmacie.*;
import com.hospital.util.FxUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;

public class OrdonnanceController {

    @FXML private TableView<Ordonnance> tableOrdonnances;
    @FXML private TableColumn<Ordonnance, String> colOrdId, colOrdPatient, colOrdType, colOrdDate, colOrdStatut;
    @FXML private TableView<OrdonnanceLigne> tableLignes;
    @FXML private TableColumn<OrdonnanceLigne, String> colLigMed, colLigQte, colLigPoso;
    @FXML private TableColumn<OrdonnanceLigne, Void> colLigAction;
    @FXML private Label statusLabel, detailHeader, workflowHint;
    @FXML private Button btnDispenseAll;

    private final PharmacieClient client = new PharmacieClient();
    private final ObservableList<Ordonnance> ordonnances = FXCollections.observableArrayList();
    private final ObservableList<OrdonnanceLigne> lignesCourantes = FXCollections.observableArrayList();
    private Ordonnance selected;
    private List<Medicament> catalogue = new ArrayList<>();

    @FXML
    public void initialize() {
        colOrdId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colOrdPatient.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getPatientId())));
        colOrdType.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTypePatient().isBlank() ? "AMBULATOIRE" : c.getValue().getTypePatient()));
        colOrdDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateOrdonnance()));
        colOrdStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));

        tableOrdonnances.setItems(ordonnances);
        tableOrdonnances.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selected = n;
            showDetail(n);
        });

        colLigMed.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNomMedicament()));
        colLigQte.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getQuantite() + " (delivre: " + c.getValue().getQuantiteDelivree() + ")"));
        colLigPoso.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPosologie()));
        colLigAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Delivrer");
            {
                btn.getStyleClass().add("btn-outline");
                btn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                btn.setOnAction(e -> {
                    OrdonnanceLigne l = getTableView().getItems().get(getIndex());
                    dispenseLigne(l);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                OrdonnanceLigne l = getTableView().getItems().get(getIndex());
                int reste = l.getQuantite() - l.getQuantiteDelivree();
                btn.setDisable(reste <= 0 || selected == null || "DELIVREE".equals(selected.getStatut()));
                btn.setText(reste <= 0 ? "OK" : "Delivrer");
                setGraphic(btn);
            }
        });

        tableLignes.setItems(lignesCourantes);
        onRefreshMeds();
        loadEnAttente();
    }

    private void loadEnAttente() {
        setStatus("Chargement des ordonnances...");
        Task<List<Ordonnance>> task = new Task<>() {
            @Override protected List<Ordonnance> call() {
                return client.getOrdonnancesEnAttente();
            }
        };
        task.setOnSucceeded(e -> {
            ordonnances.setAll(task.getValue());
            setStatus(ordonnances.size() + " ordonnance(s) en attente / partielles");
        });
        task.setOnFailed(e -> setStatus("Erreur: " + task.getException().getMessage()));
        new Thread(task).start();
    }

    private void showDetail(Ordonnance row) {
        lignesCourantes.clear();
        if (row == null) {
            detailHeader.setText("Selectionnez ou creez une ordonnance");
            workflowHint.setText("Ambulatoire : remettre au patient. Hospitalise : livrer au service.");
            btnDispenseAll.setDisable(true);
            return;
        }
        String type = row.getTypePatient().isBlank() ? "AMBULATOIRE" : row.getTypePatient();
        detailHeader.setText("#" + row.getId() + " · Patient " + row.getPatientId()
                + " · " + type + " · " + row.getStatut()
                + (row.getServiceSoins().isBlank() ? "" : " · Service: " + row.getServiceSoins()));
        if ("HOSPITALISE".equals(type)) {
            workflowHint.setText("Patient hospitalise — livrer au service. L'infirmier administre au lit.");
        } else {
            workflowHint.setText("Patient ambulatoire — preparer et remettre au patient avec les explications.");
        }
        lignesCourantes.addAll(row.getLignesList());
        boolean canDispense = !"DELIVREE".equals(row.getStatut()) && !"ANNULEE".equals(row.getStatut())
                && row.getLignesList().stream().anyMatch(l -> l.getQuantite() > l.getQuantiteDelivree());
        btnDispenseAll.setDisable(!canDispense);
    }

    @FXML
    private void onRefreshMeds() {
        Task<List<Medicament>> task = new Task<>() {
            @Override protected List<Medicament> call() { return client.getMedicaments(false); }
        };
        task.setOnSucceeded(e -> {
            catalogue = task.getValue() != null ? task.getValue() : List.of();
            setStatus(catalogue.size() + " medicament(s) · " + ordonnances.size() + " ord.");
        });
        task.setOnFailed(e -> setStatus("Catalogue indisponible"));
        new Thread(task).start();
        loadEnAttente();
    }

    @FXML
    private void onNewOrdonnance() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle ordonnance");
        dialog.setHeaderText("Enregistrer une ordonnance (persistee serveur)");
        ButtonType ok = new ButtonType("Creer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField patientId = new TextField();
        TextField medecinId = new TextField("1");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("AMBULATOIRE", "HOSPITALISE"));
        type.setValue("AMBULATOIRE");
        TextField service = new TextField();
        service.setPromptText("Ex: Cardiologie...");
        service.disableProperty().bind(type.valueProperty().isNotEqualTo("HOSPITALISE"));
        TextField obs = new TextField();
        grid.add(new Label("Patient ID *"), 0, 0); grid.add(patientId, 1, 0);
        grid.add(new Label("Medecin ID"), 0, 1); grid.add(medecinId, 1, 1);
        grid.add(new Label("Type *"), 0, 2); grid.add(type, 1, 2);
        grid.add(new Label("Service (hosp.)"), 0, 3); grid.add(service, 1, 3);
        grid.add(new Label("Observations"), 0, 4); grid.add(obs, 1, 4);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> btn == ok);

        if (!dialog.showAndWait().orElse(false)) return;
        if (patientId.getText().isBlank()) {
            FxUtils.showWarning("Validation", "Patient ID obligatoire.");
            return;
        }
        long pid, mid;
        try {
            pid = Long.parseLong(patientId.getText().trim());
            mid = medecinId.getText().isBlank() ? 0 : Long.parseLong(medecinId.getText().trim());
        } catch (NumberFormatException e) {
            FxUtils.showWarning("Validation", "IDs numeriques invalides.");
            return;
        }
        final String typeVal = type.getValue();
        final String svc = service.getText();
        final String observations = obs.getText();

        Task<Ordonnance> task = new Task<>() {
            @Override protected Ordonnance call() {
                return client.createOrdonnance(pid, mid, typeVal, svc, observations, List.of());
            }
        };
        task.setOnSucceeded(e -> {
            Ordonnance o = task.getValue();
            ordonnances.add(0, o);
            tableOrdonnances.getSelectionModel().select(o);
            FxUtils.showSuccess("Ordonnance #" + o.getId() + " creee et enregistree en base.");
            setStatus("Ordonnance #" + o.getId() + " — ajoutez les lignes");
        });
        task.setOnFailed(e -> FxUtils.showError("Erreur", task.getException().getMessage()));
        new Thread(task).start();
    }

    @FXML
    private void onAddLigne() {
        if (selected == null) {
            FxUtils.showWarning("Ordonnance", "Selectionnez d'abord une ordonnance.");
            return;
        }
        if ("DELIVREE".equals(selected.getStatut())) {
            FxUtils.showInfo("Ordonnance", "Deja entierement delivree.");
            return;
        }
        if (catalogue.isEmpty()) {
            FxUtils.showWarning("Catalogue", "Actualisez le catalogue medicaments.");
            return;
        }

        Dialog<OrdonnanceLigne> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une ligne");
        ButtonType ok = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        ComboBox<Medicament> medBox = new ComboBox<>(FXCollections.observableArrayList(catalogue));
        medBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Medicament m) {
                return m == null ? "" : m.getCode() + " — " + m.getNom() + " (stock: " + m.getStock() + ")";
            }
            @Override public Medicament fromString(String s) { return null; }
        });
        if (!catalogue.isEmpty()) medBox.setValue(catalogue.get(0));
        TextField qte = new TextField("1");
        TextField poso = new TextField("1 comprime 3x/jour");
        grid.add(new Label("Medicament *"), 0, 0); grid.add(medBox, 1, 0);
        grid.add(new Label("Quantite *"), 0, 1); grid.add(qte, 1, 1);
        grid.add(new Label("Posologie"), 0, 2); grid.add(poso, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != ok) return null;
            Medicament m = medBox.getValue();
            if (m == null) return null;
            try {
                int q = Integer.parseInt(qte.getText().trim());
                if (q <= 0) return null;
                return OrdonnanceLigne.newBuilder()
                        .setMedicamentId(m.getId())
                        .setNomMedicament(m.getNom())
                        .setQuantite(q)
                        .setPosologie(poso.getText())
                        .setQuantiteDelivree(0)
                        .build();
            } catch (NumberFormatException e) {
                return null;
            }
        });

        dialog.showAndWait().ifPresent(ligne -> {
            List<OrdonnanceLigne> all = new ArrayList<>(selected.getLignesList());
            all.add(ligne);
            final long oldId = selected.getId();
            final long pid = selected.getPatientId();
            final long mid = selected.getMedecinId();
            final String tp = selected.getTypePatient();
            final String svc = selected.getServiceSoins();
            final String obs = selected.getObservations();
            final boolean wasEmpty = selected.getLignesCount() == 0;

            Task<Ordonnance> task = new Task<>() {
                @Override protected Ordonnance call() {
                    if (wasEmpty) {
                        // Creer avec lignes (nouvelle) — le serveur cree une nouvelle ord.
                        // On utilise create avec lignes. L'ancienne vide reste en base.
                        return client.createOrdonnance(pid, mid, tp, svc, obs, all);
                    }
                    return client.createOrdonnance(pid, mid, tp, svc, "Complement ord #" + oldId, List.of(ligne));
                }
            };
            task.setOnSucceeded(e -> {
                Ordonnance o = task.getValue();
                if (!ordonnances.stream().anyMatch(x -> x.getId() == o.getId())) {
                    ordonnances.add(0, o);
                }
                tableOrdonnances.getSelectionModel().select(o);
                selected = o;
                showDetail(o);
                FxUtils.showSuccess("Ligne enregistree sur ordonnance #" + o.getId());
            });
            task.setOnFailed(e -> FxUtils.showError("Erreur", task.getException().getMessage()));
            new Thread(task).start();
        });
    }

    @FXML
    private void onDispense() {
        if (selected == null || selected.getLignesCount() == 0) return;
        String type = selected.getTypePatient().isBlank() ? "AMBULATOIRE" : selected.getTypePatient();
        String msg = "AMBULATOIRE".equals(type)
                ? "Delivrer toute l'ordonnance au patient " + selected.getPatientId() + " ?"
                : "Livrer l'ordonnance au service « " + selected.getServiceSoins() + " » ?";
        if (!FxUtils.confirm("Delivrance", msg)) return;

        long ordId = selected.getId();
        Task<String> task = new Task<>() {
            @Override protected String call() {
                int ok = 0, fail = 0;
                Ordonnance current = client.getOrdonnance(ordId);
                for (OrdonnanceLigne l : current.getLignesList()) {
                    int reste = l.getQuantite() - l.getQuantiteDelivree();
                    if (reste <= 0) { ok++; continue; }
                    try {
                        client.dispense(ordId, l.getMedicamentId(), reste);
                        ok++;
                    } catch (Exception ex) {
                        fail++;
                    }
                }
                return ok + " ligne(s) OK" + (fail > 0 ? ", " + fail + " echec(s)" : "");
            }
        };
        task.setOnSucceeded(e -> {
            FxUtils.showSuccess(task.getValue());
            reloadSelected(ordId);
            loadEnAttente();
        });
        task.setOnFailed(e -> FxUtils.showError("Erreur", task.getException().getMessage()));
        new Thread(task).start();
    }

    private void dispenseLigne(OrdonnanceLigne l) {
        if (selected == null) return;
        int reste = l.getQuantite() - l.getQuantiteDelivree();
        if (reste <= 0) return;
        if (!FxUtils.confirm("Delivrance", "Delivrer " + l.getNomMedicament() + " x" + reste + " ?")) return;
        long ordId = selected.getId();
        Task<DispenseResponse> task = new Task<>() {
            @Override protected DispenseResponse call() {
                return client.dispense(ordId, l.getMedicamentId(), reste);
            }
        };
        task.setOnSucceeded(e -> {
            DispenseResponse r = task.getValue();
            FxUtils.showSuccess(r.getMessage() + " — stock restant: " + r.getStockRestant());
            reloadSelected(ordId);
        });
        task.setOnFailed(e -> FxUtils.showError("Erreur", task.getException().getMessage()));
        new Thread(task).start();
    }

    private void reloadSelected(long ordId) {
        Task<Ordonnance> task = new Task<>() {
            @Override protected Ordonnance call() { return client.getOrdonnance(ordId); }
        };
        task.setOnSucceeded(e -> {
            Ordonnance o = task.getValue();
            for (int i = 0; i < ordonnances.size(); i++) {
                if (ordonnances.get(i).getId() == ordId) {
                    ordonnances.set(i, o);
                    break;
                }
            }
            tableOrdonnances.getSelectionModel().select(o);
            selected = o;
            showDetail(o);
        });
        new Thread(task).start();
    }

    private void setStatus(String s) {
        if (statusLabel != null) statusLabel.setText(s);
    }
}
