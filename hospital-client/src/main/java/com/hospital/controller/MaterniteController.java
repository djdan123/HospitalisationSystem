package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.maternite.Accouchement;
import com.hospital.grpc.maternite.DossierMaternite;
import com.hospital.grpc.maternite.SuiviGrossesse;
import com.hospital.service.MaterniteService;
import com.hospital.session.UserSession;
import com.hospital.util.FxUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.List;

/** Écran de maternité connecté au microservice gRPC Maternité. */
public class MaterniteController {

    @FXML private TextField patientIdField, dossierIdField;
    @FXML private TableView<DossierMaternite> dossiersTable;
    @FXML private TableColumn<DossierMaternite, String> colDossierId, colPatient, colOuverture, colDpa, colGrossesses, colStatut;
    @FXML private TableView<SuiviGrossesse> suivisTable;
    @FXML private TableColumn<SuiviGrossesse, String> colSuiviDate, colSuiviAge, colSuiviPoids, colSuiviTension, colSuiviMedecin, colSuiviObs;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;

    private final MaterniteService service = new MaterniteService();
    private final ObservableList<DossierMaternite> dossiers = FXCollections.observableArrayList();
    private final ObservableList<SuiviGrossesse> suivis = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colDossierId.setCellValueFactory(c -> text(c.getValue().getId()));
        colPatient.setCellValueFactory(c -> text(c.getValue().getPatientId()));
        colOuverture.setCellValueFactory(c -> text(c.getValue().getDateOuverture()));
        colDpa.setCellValueFactory(c -> text(c.getValue().getDatePrevueAccouchement()));
        colGrossesses.setCellValueFactory(c -> text(c.getValue().getNombreGrossesses()));
        colStatut.setCellValueFactory(c -> text(c.getValue().getStatut()));
        colSuiviDate.setCellValueFactory(c -> text(c.getValue().getDateSuivi()));
        colSuiviAge.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAgeGestationnelSemaines() + " semaines"));
        colSuiviPoids.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.1f kg", c.getValue().getPoidsKg())));
        colSuiviTension.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.0f / %.0f", c.getValue().getTensionSystolique(), c.getValue().getTensionDiastolique())));
        colSuiviMedecin.setCellValueFactory(c -> text(c.getValue().getMedecin()));
        colSuiviObs.setCellValueFactory(c -> text(c.getValue().getObservations()));
        dossiersTable.setItems(dossiers);
        suivisTable.setItems(suivis);
        dossiersTable.getSelectionModel().selectedItemProperty().addListener((o, old, selected) -> {
            if (selected != null) dossierIdField.setText(String.valueOf(selected.getId()));
        });
    }

    @FXML
    private void onSearchPatient() {
        Long patientId = parseId(patientIdField, "ID patiente");
        if (patientId == null) return;
        setLoading(true);
        Task<List<DossierMaternite>> task = new Task<>() {
            @Override protected List<DossierMaternite> call() { return service.getHistorique(patientId); }
        };
        task.setOnSucceeded(e -> {
            dossiers.setAll(task.getValue());
            suivis.clear();
            statusLabel.setText(dossiers.size() + " dossier(s) de maternité.");
            setLoading(false);
        });
        task.setOnFailed(e -> failure("Recherche impossible.", task.getException()));
        new Thread(task, "hms-maternite-history").start();
    }

    @FXML
    private void onOpenDossier() {
        Long dossierId = parseId(dossierIdField, "ID dossier");
        if (dossierId == null) return;
        setLoading(true);
        Task<List<SuiviGrossesse>> task = new Task<>() {
            @Override protected List<SuiviGrossesse> call() { return service.getSuiviGrossesse(dossierId); }
        };
        task.setOnSucceeded(e -> {
            suivis.setAll(task.getValue());
            statusLabel.setText(suivis.size() + " suivi(s) prénatal(aux).");
            setLoading(false);
        });
        task.setOnFailed(e -> failure("Chargement du suivi impossible.", task.getException()));
        new Thread(task, "hms-maternite-followup").start();
    }

    @FXML
    private void onNewDossier() {
        Dialog<DossierMaternite> dialog = new Dialog<>();
        dialog.setTitle("Nouveau dossier de maternité");
        dialog.setHeaderText("Ouvrir le suivi d'une grossesse");
        ButtonType save = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        GridPane grid = grid();
        TextField patient = new TextField(patientIdField.getText());
        TextField ddr = new TextField(LocalDate.now().minusMonths(1).toString());
        TextField grossesses = new TextField("1");
        ComboBox<String> groupe = new ComboBox<>(FXCollections.observableArrayList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Inconnu"));
        groupe.setValue("Inconnu");
        TextArea observations = new TextArea(); observations.setPrefRowCount(3);
        add(grid, "Patiente ID *", patient, 0); add(grid, "DDR (yyyy-MM-dd)", ddr, 1);
        add(grid, "Nombre de grossesses", grossesses, 2); add(grid, "Groupe sanguin", groupe, 3);
        add(grid, "Observations", observations, 4);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button != save) return null;
            try { return service.createDossier(Long.parseLong(patient.getText().trim()), ddr.getText().trim(), Integer.parseInt(grossesses.getText().trim()), groupe.getValue(), observations.getText().trim()); }
            catch (Exception ex) { FxUtils.showError("Maternité", ex.getMessage()); return null; }
        });
        dialog.showAndWait().ifPresent(dossier -> {
            patientIdField.setText(String.valueOf(dossier.getPatientId()));
            dossierIdField.setText(String.valueOf(dossier.getId()));
            FxUtils.showSuccess("Dossier de maternité créé.");
            onSearchPatient();
        });
    }

    @FXML
    private void onAddSuivi() {
        Long dossierId = parseId(dossierIdField, "ID dossier");
        if (dossierId == null) return;
        Dialog<SuiviGrossesse> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un suivi prénatal");
        ButtonType save = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        GridPane grid = grid();
        TextField date = new TextField(LocalDate.now().toString()); TextField age = new TextField(); TextField poids = new TextField();
        TextField systolique = new TextField(); TextField diastolique = new TextField(); TextArea observations = new TextArea(); observations.setPrefRowCount(2);
        add(grid, "Date", date, 0); add(grid, "Âge gestationnel (semaines)", age, 1); add(grid, "Poids (kg)", poids, 2);
        add(grid, "Tension systolique", systolique, 3); add(grid, "Tension diastolique", diastolique, 4); add(grid, "Observations", observations, 5);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button != save) return null;
            try { return service.addSuiviGrossesse(dossierId, date.getText().trim(), Integer.parseInt(age.getText().trim()), Double.parseDouble(poids.getText().trim().replace(',', '.')), Double.parseDouble(systolique.getText().trim().replace(',', '.')), Double.parseDouble(diastolique.getText().trim().replace(',', '.')), observations.getText().trim(), UserSession.getInstance().getFullName()); }
            catch (Exception ex) { FxUtils.showError("Maternité", ex.getMessage()); return null; }
        });
        dialog.showAndWait().ifPresent(suivi -> { FxUtils.showSuccess("Suivi prénatal enregistré."); onOpenDossier(); });
    }

    @FXML
    private void onAccouchement() {
        Long dossierId = parseId(dossierIdField, "ID dossier");
        if (dossierId == null) return;
        Dialog<Accouchement> dialog = new Dialog<>();
        dialog.setTitle("Enregistrer un accouchement");
        ButtonType save = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        GridPane grid = grid();
        TextField date = new TextField(LocalDate.now().toString()); ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("VOIE_BASSE", "CESARIENNE")); type.setValue("VOIE_BASSE");
        TextField nombre = new TextField("1"); TextArea observations = new TextArea(); observations.setPrefRowCount(2);
        add(grid, "Date", date, 0); add(grid, "Type", type, 1); add(grid, "Nombre d'enfants", nombre, 2); add(grid, "Observations", observations, 3);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button != save) return null;
            try { return service.registerAccouchement(dossierId, date.getText().trim(), type.getValue(), Integer.parseInt(nombre.getText().trim()), observations.getText().trim(), List.of()); }
            catch (Exception ex) { FxUtils.showError("Maternité", ex.getMessage()); return null; }
        });
        dialog.showAndWait().ifPresent(a -> { FxUtils.showSuccess("Accouchement enregistré."); onOpenDossier(); });
    }

    private GridPane grid() { GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20)); return grid; }
    private void add(GridPane grid, String label, javafx.scene.Node control, int row) { grid.add(new Label(label), 0, row); grid.add(control, 1, row); }
    private Long parseId(TextField field, String label) { try { String value = field.getText() == null ? "" : field.getText().trim(); if (value.isBlank()) throw new NumberFormatException(); return Long.parseLong(value); } catch (NumberFormatException e) { FxUtils.showWarning("Maternité", label + " invalide."); return null; } }
    private SimpleStringProperty text(Object value) { return new SimpleStringProperty(String.valueOf(value == null ? "—" : value)); }
    private void setLoading(boolean loading) { progress.setVisible(loading); if (loading) statusLabel.setText("Chargement..."); }
    private void failure(String fallback, Throwable error) { setLoading(false); statusLabel.setText("Erreur"); FxUtils.showError("Maternité", error instanceof GrpcClientException ? error.getMessage() : fallback); }
}
