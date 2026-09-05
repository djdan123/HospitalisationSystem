package com.hospital.controller;

import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.PharmacieClient;
import com.hospital.grpc.pharmacie.Medicament;
import com.hospital.util.FxUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Optional;

public class PharmacieController {

    @FXML private CheckBox onlyAvailable;
    @FXML private TableView<Medicament> table;
    @FXML private TableColumn<Medicament, String> colCode, colNom, colStock, colPrix, colUnite, colStatut;
    @FXML private TableColumn<Medicament, Void> colActions;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progress;

    private final PharmacieClient client = new PharmacieClient();
    private final ObservableList<Medicament> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colStock.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getStock())));
        colPrix.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getPrixUnitaire())));
        colUnite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnite()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnStock = new Button("Stock");
            {
                btnStock.getStyleClass().add("btn-outline");
                btnStock.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                btnStock.setOnAction(e -> {
                    Medicament m = getTableView().getItems().get(getIndex());
                    openStockDialog(m);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnStock);
            }
        });

        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        onRefresh();
    }

    @FXML
    private void onRefresh() {
        System.out.println("=== onRefresh : " + data.size() + " médicaments chargés");
        boolean only = onlyAvailable != null && onlyAvailable.isSelected();
        setLoading(true);
        Task<List<Medicament>> task = new Task<>() {
            @Override protected List<Medicament> call() {
                return client.getMedicaments(only);
            }
        };
        task.setOnSucceeded(e -> {
            data.setAll(task.getValue());
            statusLabel.setText(data.size() + " médicament(s)");
            setLoading(false);
        });
        task.setOnFailed(e -> {
            setLoading(false);
            statusLabel.setText("Erreur");
            Throwable ex = task.getException();
            FxUtils.showError("Erreur", ex instanceof GrpcClientException ? ex.getMessage() : "Impossible de charger les médicaments.");
        });
        new Thread(task).start();

    }

    @FXML
    private void onNew() {
        Dialog<Medicament> dialog = new Dialog<>();
        dialog.setTitle("Nouveau médicament");
        dialog.setHeaderText("Ajouter un médicament");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField code = new TextField();
        TextField nom = new TextField();
        TextField desc = new TextField();
        TextField stock = new TextField("0");
        TextField prix = new TextField("0");
        TextField unite = new TextField("comprimé");
        grid.add(new Label("Code *"), 0, 0); grid.add(code, 1, 0);
        grid.add(new Label("Nom *"), 0, 1); grid.add(nom, 1, 1);
        grid.add(new Label("Description"), 0, 2); grid.add(desc, 1, 2);
        grid.add(new Label("Stock initial"), 0, 3); grid.add(stock, 1, 3);
        grid.add(new Label("Prix unitaire"), 0, 4); grid.add(prix, 1, 4);
        grid.add(new Label("Unité"), 0, 5); grid.add(unite, 1, 5);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            if (code.getText().isBlank() || nom.getText().isBlank()) {
                FxUtils.showWarning("Validation", "Code et Nom obligatoires.");
                return null;
            }
            try {
                int s = Integer.parseInt(stock.getText().trim());
                double p = Double.parseDouble(prix.getText().trim().replace(",", "."));
                return client.createMedicament(code.getText().trim(), nom.getText().trim(),
                        desc.getText(), s, p, unite.getText());
            } catch (Exception ex) {
                FxUtils.showError("Erreur", ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(m -> {
            FxUtils.showSuccess("Médicament créé : " + m.getCode());
            onRefresh();
        });
    }

    @FXML
    private void onUpdateStock() {
        Medicament selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxUtils.showWarning("Stock", "Sélectionnez un médicament dans le tableau.");
            return;
        }
        openStockDialog(selected);
    }

    private void openStockDialog(Medicament m) {
        Dialog<Medicament> dialog = new Dialog<>();
        dialog.setTitle("Mise à jour stock");
        dialog.setHeaderText(m.getNom() + " — stock actuel : " + m.getStock());
        ButtonType saveBtn = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField quantite = new TextField();
        quantite.setPromptText("+10 entrée / -5 sortie");
        TextField motif = new TextField("Ajustement");
        grid.add(new Label("Quantité (+/-)"), 0, 0); grid.add(quantite, 1, 0);
        grid.add(new Label("Motif"), 0, 1); grid.add(motif, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            try {
                int q = Integer.parseInt(quantite.getText().trim());
                return client.updateStock(m.getId(), q, motif.getText());
            } catch (Exception ex) {
                FxUtils.showError("Erreur", ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(updated -> {
            FxUtils.showSuccess("Stock mis à jour : " + updated.getStock());
            onRefresh();
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        if (loading) statusLabel.setText("Chargement...");
    }
}
