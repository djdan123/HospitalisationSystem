package com.hospital.controller;

import com.hospital.grpc.PharmacieClient;
import com.hospital.grpc.pharmacie.*;
import com.hospital.session.UserSession;
import com.hospital.util.FxUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.List;

public class StockController {

    @FXML private ListView<String> listFaible, listPeremp, listSurstock;
    @FXML private TableView<Medicament> table;
    @FXML private TableColumn<Medicament, String> colCode, colNom, colStock, colSeuil, colLot, colPeremp, colStatut, colStup;
    @FXML private TableView<StockMouvementMsg> tableMvt;
    @FXML private TableColumn<StockMouvementMsg, String> colMvtDate, colMvtType, colMvtMed, colMvtQte, colMvtStock, colMvtLot, colMvtUser, colMvtMotif;
    @FXML private Label statusLabel;

    private final PharmacieClient client = new PharmacieClient();
    private final ObservableList<Medicament> data = FXCollections.observableArrayList();
    private final ObservableList<StockMouvementMsg> mvts = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colStock.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getStock())));
        colSeuil.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getSeuilMin())));
        colLot.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroLot()));
        colPeremp.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDatePeremption()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
        colStup.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIsStupefiant() ? "Oui" : ""));

        colMvtDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateMouvement()));
        colMvtType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeMouvement()));
        colMvtMed.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getMedicamentId())));
        colMvtQte.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantite())));
        colMvtStock.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStockAvant() + " → " + c.getValue().getStockApres()));
        colMvtLot.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroLot()));
        colMvtUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUserLogin()));
        colMvtMotif.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMotif()));

        table.setItems(data);
        tableMvt.setItems(mvts);
        onRefresh();
    }

    @FXML
    private void onRefresh() {
        statusLabel.setText("Chargement...");
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                List<Medicament> meds = client.getMedicaments(false);
                AlertesStockResponse alertes = client.getAlertesStock(30);
                List<StockMouvementMsg> hist = client.getHistoriqueStock(0);
                javafx.application.Platform.runLater(() -> {
                    data.setAll(meds);
                    listFaible.getItems().setAll(alertes.getStockFaibleList().stream()
                            .map(m -> m.getNom() + " (" + m.getStock() + ")")
                            .toList());
                    listPeremp.getItems().setAll(alertes.getPeremptionList().stream()
                            .map(m -> m.getNom() + " · " + m.getDatePeremption() + " · lot " + m.getNumeroLot())
                            .toList());
                    listSurstock.getItems().setAll(alertes.getSurstockList().stream()
                            .map(m -> m.getNom() + " (" + m.getStock() + ")")
                            .toList());
                    mvts.setAll(hist);
                    statusLabel.setText(meds.size() + " médicaments · "
                            + alertes.getStockFaibleCount() + " faibles · "
                            + alertes.getPeremptionCount() + " péremption");
                });
                return null;
            }
        };
        task.setOnFailed(e -> {
            statusLabel.setText("Erreur: " + task.getException().getMessage());
            FxUtils.showError("Stock", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    private String user() {
        try { return UserSession.getInstance().getUsername(); } catch (Exception e) { return "pharmacien"; }
    }

    private Medicament selected() {
        return table.getSelectionModel().getSelectedItem();
    }

    @FXML
    private void onEntree() {
        Medicament m = selected();
        if (m == null) { FxUtils.showWarning("Entrée", "Sélectionnez un médicament."); return; }
        Dialog<Boolean> d = new Dialog<>();
        d.setTitle("Entrée de stock");
        d.setHeaderText(m.getNom() + " — livraison labo / grossiste");
        ButtonType ok = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(16));
        TextField qte = new TextField("10");
        TextField lot = new TextField(m.getNumeroLot());
        TextField peremp = new TextField();
        peremp.setPromptText("yyyy-MM-dd");
        TextField source = new TextField("Grossiste");
        TextField motif = new TextField("Livraison");
        g.addRow(0, new Label("Quantité *"), qte);
        g.addRow(1, new Label("N° lot"), lot);
        g.addRow(2, new Label("Péremption"), peremp);
        g.addRow(3, new Label("Source"), source);
        g.addRow(4, new Label("Motif"), motif);
        d.getDialogPane().setContent(g);
        d.setResultConverter(b -> b == ok);
        if (!d.showAndWait().orElse(false)) return;
        try {
            int q = Integer.parseInt(qte.getText().trim());
            client.stockEntree(m.getId(), q, lot.getText(), peremp.getText(), source.getText(), user(), motif.getText());
            FxUtils.showSuccess("Entrée enregistrée.");
            onRefresh();
        } catch (Exception ex) {
            FxUtils.showError("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void onSortie() {
        Medicament m = selected();
        if (m == null) { FxUtils.showWarning("Sortie", "Sélectionnez un médicament."); return; }
        Dialog<Boolean> d = new Dialog<>();
        d.setTitle("Sortie de stock");
        d.setHeaderText(m.getNom() + (m.getIsStupefiant() ? " ⚠ STUPÉFIANT" : ""));
        ButtonType ok = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(16));
        TextField qte = new TextField("1");
        TextField patientId = new TextField();
        TextField service = new TextField();
        TextField motif = new TextField("Délivrance");
        TextField controleur = new TextField();
        CheckBox doubleCtrl = new CheckBox("Double contrôle stupéfiant");
        doubleCtrl.setSelected(m.getIsStupefiant());
        controleur.disableProperty().bind(doubleCtrl.selectedProperty().not());
        g.addRow(0, new Label("Quantité *"), qte);
        g.addRow(1, new Label("Patient ID"), patientId);
        g.addRow(2, new Label("Service"), service);
        g.addRow(3, new Label("Motif"), motif);
        g.addRow(4, doubleCtrl, controleur);
        d.getDialogPane().setContent(g);
        d.setResultConverter(b -> b == ok);
        if (!d.showAndWait().orElse(false)) return;
        try {
            int q = Integer.parseInt(qte.getText().trim());
            long pid = patientId.getText().isBlank() ? 0 : Long.parseLong(patientId.getText().trim());
            client.stockSortie(m.getId(), q, motif.getText(), pid, service.getText(), user(),
                    doubleCtrl.isSelected(), controleur.getText());
            FxUtils.showSuccess("Sortie enregistrée — stock mis à jour.");
            onRefresh();
        } catch (Exception ex) {
            FxUtils.showError("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void onRetour() {
        Medicament m = selected();
        if (m == null) { FxUtils.showWarning("Retour", "Sélectionnez un médicament."); return; }
        Dialog<Boolean> d = new Dialog<>();
        d.setTitle("Retour de stock");
        d.setHeaderText("Réintégration depuis un service");
        ButtonType ok = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(16));
        TextField qte = new TextField("1");
        TextField service = new TextField("Urgences");
        TextField motif = new TextField("Non utilisé");
        g.addRow(0, new Label("Quantité *"), qte);
        g.addRow(1, new Label("Service *"), service);
        g.addRow(2, new Label("Motif"), motif);
        d.getDialogPane().setContent(g);
        d.setResultConverter(b -> b == ok);
        if (!d.showAndWait().orElse(false)) return;
        try {
            int q = Integer.parseInt(qte.getText().trim());
            client.stockRetour(m.getId(), q, service.getText(), user(), motif.getText());
            FxUtils.showSuccess("Retour enregistré.");
            onRefresh();
        } catch (Exception ex) {
            FxUtils.showError("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void onInventaire() {
        Medicament m = selected();
        if (m == null) { FxUtils.showWarning("Inventaire", "Sélectionnez un médicament."); return; }
        Dialog<Boolean> d = new Dialog<>();
        d.setTitle("Inventaire physique");
        d.setHeaderText(m.getNom() + " — stock théorique: " + m.getStock());
        ButtonType ok = new ButtonType("Ajuster", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(16));
        TextField qte = new TextField(String.valueOf(m.getStock()));
        TextField motif = new TextField("Comptage physique");
        g.addRow(0, new Label("Quantité comptée *"), qte);
        g.addRow(1, new Label("Motif"), motif);
        d.getDialogPane().setContent(g);
        d.setResultConverter(b -> b == ok);
        if (!d.showAndWait().orElse(false)) return;
        try {
            int q = Integer.parseInt(qte.getText().trim());
            client.stockInventaire(m.getId(), q, user(), motif.getText());
            FxUtils.showSuccess("Inventaire enregistré.");
            onRefresh();
        } catch (Exception ex) {
            FxUtils.showError("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void onBloquerLot() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Blocage lot (rappel sanitaire)");
        dlg.setHeaderText("Bloquer tous les médicaments de ce lot");
        dlg.setContentText("N° de lot :");
        dlg.showAndWait().ifPresent(lot -> {
            if (lot.isBlank()) return;
            try {
                client.bloquerLot(lot.trim(), "Rappel sanitaire");
                FxUtils.showSuccess("Lot " + lot + " bloqué.");
                onRefresh();
            } catch (Exception ex) {
                FxUtils.showError("Erreur", ex.getMessage());
            }
        });
    }
}
