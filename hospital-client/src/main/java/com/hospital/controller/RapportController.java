package com.hospital.controller;

import com.hospital.grpc.PharmacieClient;
import com.hospital.grpc.pharmacie.*;
import com.hospital.util.FxUtils;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Rapports pharmacie + export PDF simple (texte structuré PDF 1.4 minimal).
 */
public class RapportController {

    @FXML private ComboBox<String> comboRapport;
    @FXML private TableView<List<String>> table;
    @FXML private BarChart<String, Number> chart;
    @FXML private Label statusLabel;
    @FXML private Label kpiValorisation, kpiOrdonnances, kpiRuptures, kpiPeremp;

    private final PharmacieClient client = new PharmacieClient();
    private String lastReportTitle = "";
    private final List<List<String>> lastRows = new ArrayList<>();
    private final List<String> lastHeaders = new ArrayList<>();

    private static final String[] REPORTS = {
            "Valorisation du stock",
            "Top consommations",
            "Dépenses par service",
            "Péremption 30 jours",
            "Péremption 60 jours",
            "Péremption 90 jours",
            "Ruptures de stock",
            "Rappel de lot",
            "Registre stupéfiants",
            "Stats ordonnances",
            "Audit utilisateurs",
            "KPI rotation stock"
    };

    @FXML
    public void initialize() {
        comboRapport.setItems(FXCollections.observableArrayList(REPORTS));
        comboRapport.setValue(REPORTS[0]);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        onRefreshKpi();
    }

    @FXML
    private void onRefreshKpi() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                String val = "—", ord = "—", rup = "—", per = "—";
                try {
                    RapportValorisationResponse v = client.getRapportValorisation(null);
                    val = String.format("%.0f", v.getTotal());
                } catch (Exception ignored) {}
                try {
                    RapportOrdonnancesResponse o = client.getRapportOrdonnances();
                    ord = String.valueOf(o.getTotal());
                } catch (Exception ignored) {}
                try {
                    List<Medicament> r = client.getRapportRuptures();
                    rup = String.valueOf(r.size());
                } catch (Exception ignored) {}
                try {
                    List<Medicament> p = client.getRapportPeremption(30);
                    per = String.valueOf(p.size());
                } catch (Exception ignored) {}
                final String fVal = val, fOrd = ord, fRup = rup, fPer = per;
                javafx.application.Platform.runLater(() -> {
                    kpiValorisation.setText(fVal);
                    kpiOrdonnances.setText(fOrd);
                    kpiRuptures.setText(fRup);
                    kpiPeremp.setText(fPer);
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void onCharger() {
        String type = comboRapport.getValue();
        if (type == null) return;
        statusLabel.setText("Chargement: " + type + "...");
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                lastReportTitle = type;
                lastHeaders.clear();
                lastRows.clear();
                List<String> headers = new ArrayList<>();
                List<List<String>> rows = new ArrayList<>();
                List<double[]> chartData = new ArrayList<>(); // [labelIndex unused]
                List<String> chartLabels = new ArrayList<>();
                List<Number> chartValues = new ArrayList<>();

                switch (type) {
                    case "Valorisation du stock" -> {
                        RapportValorisationResponse r = client.getRapportValorisation(null);
                        headers.addAll(List.of("Code", "Nom", "Stock", "Prix", "Valeur", "Unité"));
                        for (LigneValorisationMsg l : r.getLignesList()) {
                            rows.add(List.of(l.getCode(), l.getNom(), String.valueOf(l.getStock()),
                                    fmt(l.getPrix()), fmt(l.getValeur()), l.getUnite()));
                            if (chartLabels.size() < 8) {
                                chartLabels.add(shorten(l.getNom()));
                                chartValues.add(l.getValeur());
                            }
                        }
                        javafx.application.Platform.runLater(() ->
                                kpiValorisation.setText(fmt(r.getTotal())));
                    }
                    case "Top consommations" -> {
                        RapportTopConsoResponse r = client.getRapportTopConso(15, null, null);
                        headers.addAll(List.of("Code", "Nom", "Quantité", "Service"));
                        for (TopConsoMsg t : r.getLignesList()) {
                            rows.add(List.of(t.getCode(), t.getNom(), String.valueOf(t.getQuantite()), t.getService()));
                            if (chartLabels.size() < 10) {
                                chartLabels.add(shorten(t.getNom()));
                                chartValues.add(t.getQuantite());
                            }
                        }
                    }
                    case "Dépenses par service" -> {
                        RapportDepensesResponse r = client.getRapportDepensesService(null);
                        headers.addAll(List.of("Service", "Quantité", "Valeur"));
                        for (DepenseServiceMsg d : r.getLignesList()) {
                            rows.add(List.of(d.getService(), String.valueOf(d.getQuantite()), fmt(d.getValeur())));
                            chartLabels.add(shorten(d.getService()));
                            chartValues.add(d.getValeur());
                        }
                    }
                    case "Péremption 30 jours", "Péremption 60 jours", "Péremption 90 jours" -> {
                        int j = type.contains("90") ? 90 : type.contains("60") ? 60 : 30;
                        List<Medicament> list = client.getRapportPeremption(j);
                        headers.addAll(List.of("Code", "Nom", "Stock", "Lot", "Péremption", "Statut"));
                        for (Medicament m : list) {
                            rows.add(List.of(m.getCode(), m.getNom(), String.valueOf(m.getStock()),
                                    m.getNumeroLot(), m.getDatePeremption(), m.getStatut()));
                        }
                    }
                    case "Ruptures de stock" -> {
                        List<Medicament> list = client.getRapportRuptures();
                        headers.addAll(List.of("Code", "Nom", "Stock", "Seuil min", "Statut"));
                        for (Medicament m : list) {
                            rows.add(List.of(m.getCode(), m.getNom(), String.valueOf(m.getStock()),
                                    String.valueOf(m.getSeuilMin()), m.getStatut()));
                        }
                    }
                    case "Rappel de lot" -> {
                        String lot = ask("N° de lot à tracer");
                        if (lot == null || lot.isBlank()) return null;
                        List<StockMouvementMsg> list = client.getRapportLot(lot.trim());
                        headers.addAll(List.of("Date", "Type", "Méd.ID", "Qté", "Patient", "Service", "User"));
                        for (StockMouvementMsg mv : list) {
                            rows.add(List.of(mv.getDateMouvement(), mv.getTypeMouvement(),
                                    String.valueOf(mv.getMedicamentId()), String.valueOf(mv.getQuantite()),
                                    String.valueOf(mv.getPatientId()), mv.getServiceSoins(), mv.getUserLogin()));
                        }
                    }
                    case "Registre stupéfiants" -> {
                        List<StockMouvementMsg> list = client.getRapportStupefiants(null);
                        headers.addAll(List.of("Date", "Type", "Méd.ID", "Qté", "Lot", "User", "Contrôleur", "Patient"));
                        for (StockMouvementMsg mv : list) {
                            rows.add(List.of(mv.getDateMouvement(), mv.getTypeMouvement(),
                                    String.valueOf(mv.getMedicamentId()), String.valueOf(mv.getQuantite()),
                                    mv.getNumeroLot(), mv.getUserLogin(), mv.getControleur(),
                                    String.valueOf(mv.getPatientId())));
                        }
                    }
                    case "Stats ordonnances" -> {
                        RapportOrdonnancesResponse r = client.getRapportOrdonnances();
                        headers.addAll(List.of("Indicateur", "Valeur"));
                        rows.add(List.of("Total", String.valueOf(r.getTotal())));
                        rows.add(List.of("Délivrées", String.valueOf(r.getDelivrees())));
                        rows.add(List.of("Partielles", String.valueOf(r.getPartielles())));
                        rows.add(List.of("Créées", String.valueOf(r.getCreees())));
                        chartLabels.addAll(List.of("Délivrées", "Partielles", "Créées"));
                        chartValues.addAll(List.of(r.getDelivrees(), r.getPartielles(), r.getCreees()));
                    }
                    case "Audit utilisateurs" -> {
                        List<StockMouvementMsg> list = client.getRapportAudit(null);
                        headers.addAll(List.of("Date", "User", "Type", "Méd.ID", "Qté", "Motif"));
                        for (StockMouvementMsg mv : list) {
                            rows.add(List.of(mv.getDateMouvement(), mv.getUserLogin(), mv.getTypeMouvement(),
                                    String.valueOf(mv.getMedicamentId()), String.valueOf(mv.getQuantite()),
                                    mv.getMotif()));
                        }
                    }
                    case "KPI rotation stock" -> {
                        RapportKpiRotationResponse r = client.getRapportKpiRotation(30);
                        headers.addAll(List.of("Code", "Nom", "Sorties 30j", "Stock", "Rotation"));
                        for (KpiRotationMsg k : r.getLignesList()) {
                            rows.add(List.of(k.getCode(), k.getNom(), String.valueOf(k.getSorties()),
                                    String.valueOf(k.getStockActuel()), String.format("%.2f", k.getRotation())));
                            if (chartLabels.size() < 10) {
                                chartLabels.add(shorten(k.getNom()));
                                chartValues.add(k.getRotation());
                            }
                        }
                    }
                    default -> {}
                }

                final List<String> fHeaders = new ArrayList<>(headers);
                final List<List<String>> fRows = new ArrayList<>(rows);
                final List<String> fCL = new ArrayList<>(chartLabels);
                final List<Number> fCV = new ArrayList<>(chartValues);

                javafx.application.Platform.runLater(() -> {
                    lastHeaders.clear();
                    lastHeaders.addAll(fHeaders);
                    lastRows.clear();
                    lastRows.addAll(fRows);
                    buildTable(fHeaders, fRows);
                    buildChart(type, fCL, fCV);
                    statusLabel.setText(type + " — " + fRows.size() + " ligne(s)");
                });
                return null;
            }
        };
        task.setOnFailed(e -> {
            statusLabel.setText("Erreur: " + task.getException().getMessage());
            FxUtils.showError("Rapport", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    private void buildTable(List<String> headers, List<List<String>> rows) {
        table.getColumns().clear();
        table.getItems().clear();
        for (int i = 0; i < headers.size(); i++) {
            final int col = i;
            TableColumn<List<String>, String> tc = new TableColumn<>(headers.get(i));
            tc.setCellValueFactory(c -> {
                List<String> row = c.getValue();
                String v = col < row.size() ? row.get(col) : "";
                return new javafx.beans.property.SimpleStringProperty(v);
            });
            table.getColumns().add(tc);
        }
        table.setItems(FXCollections.observableArrayList(rows));
    }

    private void buildChart(String title, List<String> labels, List<Number> values) {
        chart.setTitle(title);
        chart.getData().clear();
        if (labels.isEmpty()) return;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(title);
        for (int i = 0; i < labels.size() && i < values.size(); i++) {
            series.getData().add(new XYChart.Data<>(labels.get(i), values.get(i)));
        }
        chart.getData().add(series);
    }

    @FXML
    private void onExportPdf() {
        if (lastRows.isEmpty()) {
            FxUtils.showWarning("PDF", "Chargez d'abord un rapport.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter le rapport PDF");
        fc.setInitialFileName("rapport-pharmacie-" + LocalDate.now() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;
        try {
            writeSimplePdf(file, lastReportTitle, lastHeaders, lastRows);
            FxUtils.showSuccess("PDF exporté :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            FxUtils.showError("PDF", e.getMessage());
        }
    }

    /** PDF minimal sans bibliothèque externe (texte). */
    private void writeSimplePdf(File file, String title, List<String> headers, List<List<String>> rows) throws Exception {
        StringBuilder content = new StringBuilder();
        content.append("BT /F1 14 Tf 50 800 Td (").append(escapePdf("HMS — " + title)).append(") Tj ET\n");
        content.append("BT /F1 10 Tf 50 780 Td (").append(escapePdf("Généré le " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))).append(") Tj ET\n");

        int y = 750;
        String headerLine = String.join(" | ", headers);
        content.append("BT /F1 9 Tf 50 ").append(y).append(" Td (").append(escapePdf(headerLine)).append(") Tj ET\n");
        y -= 16;
        for (List<String> row : rows) {
            if (y < 50) break;
            String line = String.join(" | ", row);
            if (line.length() > 100) line = line.substring(0, 100) + "...";
            content.append("BT /F1 8 Tf 50 ").append(y).append(" Td (").append(escapePdf(line)).append(") Tj ET\n");
            y -= 12;
        }

        String stream = content.toString();
        byte[] streamBytes = stream.getBytes(StandardCharsets.ISO_8859_1);

        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        int[] offsets = new int[6];
        offsets[1] = pdf.length();
        pdf.append("1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n");
        offsets[2] = pdf.length();
        pdf.append("2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n");
        offsets[3] = pdf.length();
        pdf.append("3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>endobj\n");
        offsets[4] = pdf.length();
        pdf.append("4 0 obj<< /Length ").append(streamBytes.length).append(" >>stream\n");
        pdf.append(stream);
        pdf.append("endstream\nendobj\n");
        offsets[5] = pdf.length();
        pdf.append("5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n");
        int xref = pdf.length();
        pdf.append("xref\n0 6\n");
        pdf.append("0000000000 65535 f \n");
        for (int i = 1; i <= 5; i++) {
            pdf.append(String.format("%010d 00000 n \n", offsets[i]));
        }
        pdf.append("trailer<< /Size 6 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");

        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(pdf.toString().getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    private static String escapePdf(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static String fmt(double v) {
        return String.format("%.0f", v);
    }

    private static String shorten(String s) {
        if (s == null) return "";
        return s.length() > 12 ? s.substring(0, 12) + "…" : s;
    }

    private String ask(String prompt) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Rapport");
        d.setHeaderText(prompt);
        d.setContentText(prompt);
        return d.showAndWait().orElse(null);
    }
}
