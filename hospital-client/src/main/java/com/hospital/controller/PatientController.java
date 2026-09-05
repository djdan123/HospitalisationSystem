package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.AccueilClient;
import com.hospital.grpc.accueil.Patient;
import com.hospital.util.FxUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class PatientController {

    private static final Logger log = LoggerFactory.getLogger(PatientController.class);

    @FXML private TextField searchField;
    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, String> colDossier;
    @FXML private TableColumn<Patient, String> colNom;
    @FXML private TableColumn<Patient, String> colPrenom;
    @FXML private TableColumn<Patient, String> colSexe;
    @FXML private TableColumn<Patient, String> colNaissance;
    @FXML private TableColumn<Patient, String> colTelephone;
    @FXML private TableColumn<Patient, String> colStatut;
    @FXML private TableColumn<Patient, Void> colActions;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;

    private final AccueilClient accueilClient = new AccueilClient();
    private final ObservableList<Patient> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colDossier.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroDossier()));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colPrenom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrenom()));
        colSexe.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSexe()));
        colNaissance.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateNaissance()));
        colTelephone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelephone()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Désactiver");
            private final HBox box = new HBox(6, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-outline");
                btnDelete.getStyleClass().add("btn-danger");
                btnEdit.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                btnDelete.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                btnEdit.setOnAction(e -> {
                    Patient p = getTableView().getItems().get(getIndex());
                    openEditDialog(p);
                });
                btnDelete.setOnAction(e -> {
                    Patient p = getTableView().getItems().get(getIndex());
                    onDelete(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        patientTable.setItems(data);
        patientTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        onRefresh();
    }

    @FXML
    private void onRefresh() {
        setLoading(true);
        Task<List<Patient>> task = new Task<>() {
            @Override
            protected List<Patient> call() {
                return accueilClient.getPatients(false);
            }
        };
        task.setOnSucceeded(e -> {
            data.setAll(task.getValue());
            statusLabel.setText(data.size() + " patient(s)");
            setLoading(false);
        });
        task.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = task.getException();
            String msg = ex instanceof GrpcClientException ? ex.getMessage() : "Erreur de chargement des patients.";
            statusLabel.setText("Erreur");
            FxUtils.showError("Erreur", msg);
        });
        new Thread(task).start();
    }

    @FXML
    private void onSearch() {
        String nom = searchField.getText() != null ? searchField.getText().trim() : "";
        if (nom.isBlank()) {
            onRefresh();
            return;
        }
        setLoading(true);
        Task<List<Patient>> task = new Task<>() {
            @Override
            protected List<Patient> call() {
                return accueilClient.searchPatients(nom, null);
            }
        };
        task.setOnSucceeded(e -> {
            data.setAll(task.getValue());
            statusLabel.setText(data.size() + " résultat(s)");
            setLoading(false);
        });
        task.setOnFailed(e -> {
            setLoading(false);
            FxUtils.showError("Erreur", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    @FXML
    private void onNewPatient() {
        openCreateDialog();
    }

    private void openCreateDialog() {
        Dialog<Patient> dialog = new Dialog<>();
        dialog.setTitle("Nouveau patient");
        dialog.setHeaderText("Créer un nouveau dossier patient");

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField dossier = new TextField();
        dossier.setPromptText("DOS-2026-XXX");
        TextField nom = new TextField();
        TextField prenom = new TextField();
        DatePicker naissance = new DatePicker();
        ComboBox<String> sexe = new ComboBox<>(FXCollections.observableArrayList("M", "F"));
        sexe.setValue("M");
        TextField tel = new TextField();
        TextField email = new TextField();
        TextField adresse = new TextField();

        grid.add(new Label("N° Dossier *"), 0, 0); grid.add(dossier, 1, 0);
        grid.add(new Label("Nom *"), 0, 1); grid.add(nom, 1, 1);
        grid.add(new Label("Prénom *"), 0, 2); grid.add(prenom, 1, 2);
        grid.add(new Label("Date naissance *"), 0, 3); grid.add(naissance, 1, 3);
        grid.add(new Label("Sexe *"), 0, 4); grid.add(sexe, 1, 4);
        grid.add(new Label("Téléphone"), 0, 5); grid.add(tel, 1, 5);
        grid.add(new Label("Email"), 0, 6); grid.add(email, 1, 6);
        grid.add(new Label("Adresse"), 0, 7); grid.add(adresse, 1, 7);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                if (dossier.getText().isBlank() || nom.getText().isBlank() || prenom.getText().isBlank()
                        || naissance.getValue() == null) {
                    FxUtils.showWarning("Validation", "Les champs marqués * sont obligatoires.");
                    return null;
                }
                try {
                    return accueilClient.createPatient(
                            dossier.getText().trim(),
                            nom.getText().trim(),
                            prenom.getText().trim(),
                            naissance.getValue().toString(),
                            sexe.getValue(),
                            tel.getText(),
                            email.getText(),
                            adresse.getText()
                    );
                } catch (GrpcClientException ex) {
                    FxUtils.showError("Erreur", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<Patient> result = dialog.showAndWait();
        result.ifPresent(p -> {
            FxUtils.showSuccess("Patient créé : " + p.getNumeroDossier());
            onRefresh();
        });
    }

    private void openEditDialog(Patient patient) {
        Dialog<Patient> dialog = new Dialog<>();
        dialog.setTitle("Modifier patient");
        dialog.setHeaderText("Dossier : " + patient.getNumeroDossier());

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nom = new TextField(patient.getNom());
        TextField prenom = new TextField(patient.getPrenom());
        TextField tel = new TextField(patient.getTelephone());
        TextField email = new TextField(patient.getEmail());
        TextField adresse = new TextField(patient.getAdresse());
        ComboBox<String> statut = new ComboBox<>(FXCollections.observableArrayList("ACTIF", "INACTIF"));
        statut.setValue(patient.getStatut());

        grid.add(new Label("Nom"), 0, 0); grid.add(nom, 1, 0);
        grid.add(new Label("Prénom"), 0, 1); grid.add(prenom, 1, 1);
        grid.add(new Label("Téléphone"), 0, 2); grid.add(tel, 1, 2);
        grid.add(new Label("Email"), 0, 3); grid.add(email, 1, 3);
        grid.add(new Label("Adresse"), 0, 4); grid.add(adresse, 1, 4);
        grid.add(new Label("Statut"), 0, 5); grid.add(statut, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    return accueilClient.updatePatient(
                            patient.getId(), nom.getText(), prenom.getText(),
                            null, null, tel.getText(), email.getText(), adresse.getText(), statut.getValue()
                    );
                } catch (GrpcClientException ex) {
                    FxUtils.showError("Erreur", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            FxUtils.showSuccess("Patient mis à jour.");
            onRefresh();
        });
    }

    private void onDelete(Patient patient) {
        if (!FxUtils.confirm("Désactivation", "Désactiver le patient " + patient.getNom() + " " + patient.getPrenom() + " ?")) {
            return;
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                accueilClient.deletePatient(patient.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            FxUtils.showSuccess("Patient désactivé.");
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
