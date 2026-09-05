package com.hospital.controller;

import com.hospital.config.AppConfig;
import com.hospital.config.GrpcConfig;
import com.hospital.grpc.*;
import com.hospital.session.UserSession;
import com.hospital.util.FxUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Dashboard principal – interface adaptée au rôle (Médecin, Pharmacien, Réceptionniste, Caissier, Admin).
 * Design inspiré des dashboards modernes (cartes stats, listes, alertes, actions rapides).
 */
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @FXML private Label pageTitleLabel;
    @FXML private Label serverStatusBadge;
    @FXML private Label userLabel;
    @FXML private VBox sidebar;
    @FXML private Label sidebarSubtitle;
    @FXML private Label menuSectionLabel;

    @FXML private Button btnDashboard, btnPatients, btnConsultations, btnDossiers, btnOrdonnances;
    @FXML private Button btnExamens, btnResultats, btnRendezVous, btnHistorique;
    @FXML private Button btnHospitalisation, btnFileAttente, btnAdmissions, btnSorties;
    @FXML private Button btnLaboratoire, btnPharmacie, btnStocks;
    @FXML private Button btnPaiements, btnFacturation, btnFactures, btnImpayes, btnRecus, btnRapports;
    @FXML private Button btnMaternite, btnSettings, btnProfil, btnLogout;

    @FXML private StackPane contentPane;
    @FXML private ScrollPane dashboardScroll;
    @FXML private VBox dashboardContent;
    @FXML private HBox statsBox;
    @FXML private Label welcomeTitle, welcomeLabel, welcomeHint;
    @FXML private HBox quickActionsBox;
    @FXML private VBox roleMainContent;

    @FXML private Label statIcon1, statValue1, statLabel1, statSub1;
    @FXML private Label statIcon2, statValue2, statLabel2, statSub2;
    @FXML private Label statIcon3, statValue3, statLabel3, statSub3;
    @FXML private Label statIcon4, statValue4, statLabel4, statSub4;
    @FXML private Label statIcon5, statValue5, statLabel5, statSub5;
    @FXML private Label statIcon6, statValue6, statLabel6, statSub6;
    @FXML private VBox statCard1, statCard2, statCard3, statCard4, statCard5, statCard6;

    private Button currentActiveBtn;
    private UserSession.Role currentRole;

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        currentRole = session.getRole() != null ? session.getRole() : UserSession.Role.ADMIN;
        userLabel.setText(session.getFullName() + " (" + currentRole + ")");
        currentActiveBtn = btnDashboard;
        setupRoleBasedUI();
        checkServerStatus();
        refreshStats();
    }

    // ===================== RÔLES =====================

    private void setupRoleBasedUI() {
        hideAllMenuButtons();
        showBtn(btnDashboard);
        showBtn(btnSettings);
        showBtn(btnProfil);
        showBtn(btnLogout);

        switch (currentRole) {
            case MEDECIN -> setupMedecinUI();
            case PHARMACIEN -> setupPharmacienUI();
            case RECEPTIONNISTE -> setupReceptionnisteUI();
            case CAISSIER -> setupCaissierUI();
            default -> setupAdminUI();
        }
    }

    private void hideAllMenuButtons() {
        Button[] all = {
            btnPatients, btnConsultations, btnDossiers, btnOrdonnances, btnExamens,
            btnResultats, btnRendezVous, btnHistorique, btnHospitalisation,
            btnFileAttente, btnAdmissions, btnSorties, btnLaboratoire,
            btnPharmacie, btnStocks, btnPaiements, btnFacturation, btnFactures,
            btnImpayes, btnRecus, btnRapports, btnMaternite
        };
        for (Button b : all) if (b != null) { b.setVisible(false); b.setManaged(false); }
    }

    private void showBtn(Button b) {
        if (b != null) { b.setVisible(true); b.setManaged(true); }
    }

    // ---------- MÉDECIN ----------
    private void setupMedecinUI() {
        sidebarSubtitle.setText("Espace Médecin");
        menuSectionLabel.setText("ESPACE MÉDECIN");
        showBtn(btnPatients); showBtn(btnConsultations); showBtn(btnDossiers);
        showBtn(btnOrdonnances); showBtn(btnExamens); showBtn(btnResultats);
        showBtn(btnRendezVous); showBtn(btnHistorique);

        setStat(1, "👥", "—", "Patients", "Total actifs");
        setStat(2, "📅", "—", "Consultations", "Aujourd'hui");
        setStat(3, "🩺", "—", "En attente", "PLANIFIEE / EN_COURS");
        setStat(4, "🔬", "—", "Analyses", "Patients suivis");
        setStat(5, "💊", "—", "Ordonnances", "En attente");
        setStat(6, "📋", "—", "Terminées", "Consultations");
        hideStatCards(false);

        welcomeTitle.setText("Bienvenue, Docteur");
        welcomeLabel.setText("Connecté en tant que " + UserSession.getInstance().getFullName() + " — Rôle : MÉDECIN");
        welcomeHint.setText("Données en direct depuis les services gRPC (patients, consultations, labo, pharmacie).");

        fillQuickActions(
            qa("➕ Nouvelle consultation", "btn-primary", e -> showConsultations()),
            qa("👤 Rechercher patient", "btn-outline", e -> showPatients()),
            qa("📋 Voir dossier", "btn-outline", e -> showDossiers()),
            qa("💊 Créer ordonnance", "btn-outline", e -> showOrdonnances()),
            qa("🔬 Demander analyse", "btn-outline", e -> showLaboratoire()),
            qa("📄 Voir résultats", "btn-secondary", e -> showResultats())
        );
        buildMedecinMain();
    }

    /**
     * Contenu principal médecin — alimenté par ConsultationClient + AccueilClient.
     * Médecin ID : 1 par défaut (compte demo medecin). À brancher sur UserSession plus tard.
     */
    private void buildMedecinMain() {
        roleMainContent.getChildren().clear();
        HBox row = new HBox(16);
        row.setFillHeight(true);

        VBox left = createCard("Mes consultations");
        left.setPrefWidth(520);
        Label loadingLeft = new Label("Chargement des consultations...");
        loadingLeft.setStyle("-fx-text-fill: #64748b;");
        left.getChildren().add(loadingLeft);

        VBox right = createCard("Patients récents");
        right.setPrefWidth(420);
        Label loadingRight = new Label("Chargement des patients...");
        loadingRight.setStyle("-fx-text-fill: #64748b;");
        right.getChildren().add(loadingRight);

        row.getChildren().addAll(left, right);
        roleMainContent.getChildren().add(row);

        final long medecinId = resolveMedecinId();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                // Consultations du médecin
                java.util.List<com.hospital.grpc.consultation.Consultation> consultations = java.util.List.of();
                try {
                    consultations = new ConsultationClient().getByDoctor(medecinId);
                } catch (Exception ex) {
                    log.warn("Consultations médecin: {}", ex.getMessage());
                }

                // Patients (derniers 8)
                java.util.List<com.hospital.grpc.accueil.Patient> patients = java.util.List.of();
                try {
                    patients = new AccueilClient().getPatients(false);
                    if (patients.size() > 8) {
                        patients = patients.subList(Math.max(0, patients.size() - 8), patients.size());
                    }
                } catch (Exception ex) {
                    log.warn("Patients récents: {}", ex.getMessage());
                }

                final java.util.List<com.hospital.grpc.consultation.Consultation> fCons = consultations;
                final java.util.List<com.hospital.grpc.accueil.Patient> fPat = patients;

                Platform.runLater(() -> {
                    // --- Consultations ---
                    left.getChildren().remove(loadingLeft);
                    if (fCons.isEmpty()) {
                        Label empty = new Label("Aucune consultation pour ce médecin (id=" + medecinId + ").\nCréez-en depuis le module Consultations.");
                        empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
                        empty.setWrapText(true);
                        left.getChildren().add(empty);
                    } else {
                        // Trier : en attente d'abord, puis par date desc
                        java.util.List<com.hospital.grpc.consultation.Consultation> sorted = new java.util.ArrayList<>(fCons);
                        sorted.sort((a, b) -> {
                            int pa = priorityStatut(a.getStatut());
                            int pb = priorityStatut(b.getStatut());
                            if (pa != pb) return Integer.compare(pa, pb);
                            return b.getDateConsultation().compareTo(a.getDateConsultation());
                        });
                        int limit = Math.min(8, sorted.size());
                        String[][] rows = new String[limit][5];
                        for (int i = 0; i < limit; i++) {
                            var c = sorted.get(i);
                            String heure = c.getDateConsultation();
                            if (heure != null && heure.length() >= 16) {
                                heure = heure.substring(11, 16); // HH:mm
                            } else if (heure == null || heure.isBlank()) {
                                heure = "—";
                            }
                            String badge = switch (c.getStatut() != null ? c.getStatut() : "") {
                                case "TERMINEE" -> "success";
                                case "EN_COURS" -> "info";
                                case "ANNULEE" -> "danger";
                                default -> "warning"; // PLANIFIEE
                            };
                            String statutLabel = switch (c.getStatut() != null ? c.getStatut() : "") {
                                case "PLANIFIEE" -> "En attente";
                                case "EN_COURS" -> "En consultation";
                                case "TERMINEE" -> "Terminé";
                                case "ANNULEE" -> "Annulé";
                                default -> c.getStatut();
                            };
                            String motif = c.getMotif() != null && !c.getMotif().isBlank() ? c.getMotif() : "—";
                            if (motif.length() > 28) motif = motif.substring(0, 28) + "…";
                            rows[i] = new String[]{
                                    "Patient #" + c.getPatientId(),
                                    heure,
                                    motif,
                                    statutLabel,
                                    badge
                            };
                        }
                        left.getChildren().add(buildStatusList(rows, new String[]{"Patient", "Heure", "Motif", "Statut"}));
                    }

                    // --- Patients récents ---
                    right.getChildren().remove(loadingRight);
                    if (fPat.isEmpty()) {
                        Label empty = new Label("Aucun patient en base.");
                        empty.setStyle("-fx-text-fill: #64748b;");
                        right.getChildren().add(empty);
                    } else {
                        String[][] prows = new String[fPat.size()][1];
                        for (int i = 0; i < fPat.size(); i++) {
                            var p = fPat.get(i);
                            String line = p.getNom() + " " + p.getPrenom()
                                    + " · " + (p.getSexe() != null ? p.getSexe() : "?")
                                    + " · " + (p.getDateNaissance() != null && !p.getDateNaissance().isBlank()
                                    ? p.getDateNaissance() : "—")
                                    + " · " + p.getNumeroDossier();
                            prows[i] = new String[]{line};
                        }
                        right.getChildren().add(buildSimpleList(prows));
                    }
                });
                return null;
            }
        };
        task.setOnFailed(e -> log.error("buildMedecinMain", task.getException()));
        new Thread(task).start();
    }

    private static int priorityStatut(String s) {
        if (s == null) return 9;
        return switch (s) {
            case "EN_COURS" -> 0;
            case "PLANIFIEE" -> 1;
            case "TERMINEE" -> 2;
            case "ANNULEE" -> 3;
            default -> 9;
        };
    }

    /** ID médecin pour le compte demo (medecin). Extensible via UserSession. */
    private long resolveMedecinId() {
        try {
            String u = UserSession.getInstance().getUsername();
            if (u != null && u.equalsIgnoreCase("medecin")) return 1L;
        } catch (Exception ignored) {}
        return 1L;
    }

    // ---------- PHARMACIEN (design type Pharma) ----------
    private void setupPharmacienUI() {
        sidebarSubtitle.setText("Espace Pharmacie");
        menuSectionLabel.setText("ESPACE PHARMACIE");
        showBtn(btnPharmacie); showBtn(btnOrdonnances); showBtn(btnStocks); showBtn(btnRapports);

        setStat(1, "💊", "156", "Médicaments dispo.", "Catalogue");
        setStat(2, "⚠️", "23", "Stocks faibles", "Attention requise");
        setStat(3, "❌", "8", "Épuisés", "Rupture");
        setStat(4, "⏰", "8", "Proches expiration", "Sous 30 jours");
        setStat(5, "📦", "12", "Délivrances du jour", "Aujourd'hui");
        setStat(6, "📋", "4", "Ordonnances en attente", "À traiter");
        hideStatCards(false);

        welcomeTitle.setText("Pharmacy Dashboard");
        welcomeLabel.setText("Bienvenue, " + UserSession.getInstance().getFullName() + " — Gérez vos opérations pharmacie");
        welcomeHint.setText("Stocks, ordonnances, alertes et délivrances en un coup d'œil.");

        fillQuickActions(
            qa("💊 Délivrer ordonnance", "btn-primary", e -> showOrdonnances()),
            qa("➕ Ajouter médicament", "btn-success", e -> showPharmacie()),
            qa("📦 Entrée de stock", "btn-outline", e -> showStocks()),
            qa("📤 Sortie de stock", "btn-outline", e -> showStocks()),
            qa("🔍 Rechercher", "btn-secondary", e -> showPharmacie()),
            qa("📊 Inventaire", "btn-secondary", e -> showStocks())
        );
        buildPharmacienMain();
    }

    private void buildPharmacienMain() {
        roleMainContent.getChildren().clear();

        // Ligne 1 : Ordonnances récentes + Actions
        HBox row1 = new HBox(16);
        VBox prescriptions = createCard("Ordonnances récentes");
        prescriptions.setPrefWidth(560);
        prescriptions.getChildren().add(buildPrescriptionRows(new Object[][]{
            {"PRX-2026-001", "Jean Mukendi", "2 items", "Completed", "success"},
            {"PRX-2026-002", "Marie Kabeya", "3 items", "Processing", "warning"},
            {"PRX-2026-003", "David Johnson", "5 items", "Pending", "info"}
        }));
        VBox actions = createCard("Actions rapides");
        actions.setPrefWidth(280);
        VBox actionBtns = new VBox(10);
        actionBtns.getChildren().addAll(
            fullBtn("+ Nouvelle vente", "btn-primary", e -> showPharmacie()),
            fullBtn("↑ Upload ordonnance", "btn-success", e -> showOrdonnances()),
            fullBtn("+ Ajouter stock", "btn-outline", e -> showStocks()),
            fullBtn("📄 Générer rapport", "btn-secondary", e -> showRapports())
        );
        actions.getChildren().add(actionBtns);
        row1.getChildren().addAll(prescriptions, actions);

        // Ligne 2 : Alertes stock + Expiration
        HBox row2 = new HBox(16);
        VBox lowStock = createCard("Alertes de stock faible");
        lowStock.setPrefWidth(420);
        lowStock.getChildren().add(buildAlertRows(new String[][]{
            {"Paracétamol 500mg", "Only 5 left", "Reorder", "danger"},
            {"Amoxicilline 250mg", "12 left", "Reorder", "warning"},
            {"Ibuprofène 400mg", "18 left", "Reorder", "warning"}
        }));
        VBox expiry = createCard("Suivi d'expiration");
        expiry.setPrefWidth(420);
        expiry.getChildren().add(buildAlertRows(new String[][]{
            {"Cough Syrup", "Expires in 5 days", "Mark Return", "danger"},
            {"Vitamin D3", "Expires in 15 days", "Discount Sale", "warning"},
            {"Multivitamin", "Expires in 28 days", "Monitor", "info"}
        }));
        row2.getChildren().addAll(lowStock, expiry);

        roleMainContent.getChildren().addAll(row1, row2);
    }

    // ---------- RÉCEPTIONNISTE ----------
    private void setupReceptionnisteUI() {
        sidebarSubtitle.setText("Espace Accueil");
        menuSectionLabel.setText("ESPACE RÉCEPTION");
        showBtn(btnPatients); showBtn(btnRendezVous); showBtn(btnFileAttente);
        showBtn(btnAdmissions); showBtn(btnHospitalisation); showBtn(btnSorties);

        setStat(1, "👥", "100", "Patients enregistrés", "Aujourd'hui");
        setStat(2, "📅", "70", "Rendez-vous", "Aujourd'hui");
        setStat(3, "⏳", "4", "En attente", "File d'attente");
        setStat(4, "🏥", "15", "Hospitalisés", "Actuellement");
        setStat(5, "🆕", "8", "Nouveaux patients", "Aujourd'hui");
        setStat(6, "🛏️", "15", "Lits disponibles", "Disponibles");
        hideStatCards(false);

        welcomeTitle.setText("Bonjour, " + UserSession.getInstance().getFullName());
        welcomeLabel.setText("Bonne journée de travail — Espace Réception");
        welcomeHint.setText("Enregistrez les patients, gérez les rendez-vous et la file d'attente.");

        fillQuickActions(
            qa("➕ Enregistrer patient", "btn-primary", e -> showPatients()),
            qa("🔍 Rechercher patient", "btn-outline", e -> showPatients()),
            qa("📅 Nouveau RDV", "btn-outline", e -> showRendezVous()),
            qa("🏥 Admission", "btn-outline", e -> showHospitalisation()),
            qa("🚪 Sortie patient", "btn-outline", e -> showSorties()),
            qa("🪪 Imprimer fiche", "btn-secondary", e -> showPatients())
        );
        buildReceptionnisteMain();
    }

    private void buildReceptionnisteMain() {
        roleMainContent.getChildren().clear();

        // Bannière welcome style
        HBox banner = new HBox(20);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setStyle("-fx-background-color: #0d9488; -fx-background-radius: 12; -fx-padding: 20 24;");
        VBox bannerText = new VBox(4);
        Label g = new Label("Bonne journée à l'hôpital");
        g.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label s = new Label("Gérez les patients, rendez-vous et admissions");
        s.setStyle("-fx-text-fill: #ccfbf1; -fx-font-size: 13px;");
        bannerText.getChildren().addAll(g, s);
        banner.getChildren().add(bannerText);

        HBox row = new HBox(16);
        VBox queue = createCard("File d'attente");
        queue.setPrefWidth(520);
        queue.getChildren().add(buildStatusList(new String[][]{
            {"#01", "Jean Mukendi", "Consultation générale", "En attente", "warning"},
            {"#02", "Marie Kabeya", "Cardiologie", "En consultation", "info"},
            {"#03", "Pierre Ilunga", "Pédiatrie", "En attente", "warning"}
        }, new String[]{"#", "Patient", "Service", "Statut"}));

        VBox appts = createCard("Prochains rendez-vous");
        appts.setPrefWidth(380);
        appts.getChildren().add(buildSimpleList(new String[][]{
            {"Steve Harrington · 08:00 · Nouveau patient"},
            {"Dakota Johnson · 10:00 · Nouveau patient"},
            {"Peter Quill · 10:30 · Ancien patient"}
        }));

        row.getChildren().addAll(queue, appts);
        roleMainContent.getChildren().addAll(banner, row);
    }

    // ---------- CAISSIER ----------
    private void setupCaissierUI() {
        sidebarSubtitle.setText("Espace Caisse");
        menuSectionLabel.setText("ESPACE CAISSE");
        showBtn(btnFacturation); showBtn(btnPaiements); showBtn(btnFactures);
        showBtn(btnImpayes); showBtn(btnRecus); showBtn(btnRapports);

        setStat(1, "💰", "185 000", "Recettes du jour", "BIF");
        setStat(2, "🧾", "7", "Factures payées", "Aujourd'hui");
        setStat(3, "⏳", "3", "Factures impayées", "En attente");
        setStat(4, "💳", "9", "Paiements", "Aujourd'hui");
        setStat(5, "📊", "185 000", "Total encaissé", "BIF");
        setStat(6, "📋", "12", "Factures créées", "Aujourd'hui");
        hideStatCards(false);

        welcomeTitle.setText("Patient Payments & Caisse");
        welcomeLabel.setText("Bienvenue, " + UserSession.getInstance().getFullName() + " — Espace Caissier");
        welcomeHint.setText("Facturation, encaissements, reçus et rapports journaliers.");

        fillQuickActions(
            qa("💰 Nouveau paiement", "btn-primary", e -> showPaiements()),
            qa("🧾 Créer facture", "btn-outline", e -> showFacturation()),
            qa("🔍 Rechercher facture", "btn-outline", e -> showFactures()),
            qa("🖨️ Imprimer reçu", "btn-outline", e -> showRecus()),
            qa("📋 Impayés", "btn-secondary", e -> showImpayes()),
            qa("📊 Rapport journalier", "btn-secondary", e -> showRapports())
        );
        buildCaissierMain();
    }

    private void buildCaissierMain() {
        roleMainContent.getChildren().clear();

        HBox row1 = new HBox(16);
        VBox payments = createCard("Paiements patients récents");
        payments.setPrefWidth(560);
        payments.getChildren().add(buildStatusList(new String[][]{
            {"Patient 001", "FAC001", "25 000 BIF", "Payé", "success"},
            {"Patient 002", "FAC002", "40 000 BIF", "Payé", "success"},
            {"Patient 003", "FAC003", "15 000 BIF", "En attente", "warning"}
        }, new String[]{"Patient", "Facture", "Montant", "Statut"}));

        VBox overview = createCard("Aperçu financier");
        overview.setPrefWidth(320);
        VBox ov = new VBox(12);
        ov.getChildren().addAll(
            finRow("Recettes", "185 000 BIF", "#166534"),
            finRow("Dépenses", "42 000 BIF", "#991b1b"),
            finRow("Solde", "143 000 BIF", "#1e40af")
        );
        overview.getChildren().add(ov);
        row1.getChildren().addAll(payments, overview);

        HBox row2 = new HBox(16);
        VBox unpaid = createCard("Factures impayées");
        unpaid.setPrefWidth(420);
        unpaid.getChildren().add(buildAlertRows(new String[][]{
            {"FAC010 · Patient 010", "18 000 BIF", "Relancer", "warning"},
            {"FAC011 · Patient 011", "9 500 BIF", "Relancer", "warning"},
            {"FAC012 · Patient 012", "22 000 BIF", "Relancer", "danger"}
        }));
        VBox modes = createCard("Modes de paiement du jour");
        modes.setPrefWidth(420);
        modes.getChildren().add(buildSimpleList(new String[][]{
            {"Cash — 5 paiements — 95 000 BIF"},
            {"Mobile Money — 3 paiements — 72 000 BIF"},
            {"Carte — 1 paiement — 18 000 BIF"}
        }));
        row2.getChildren().addAll(unpaid, modes);

        roleMainContent.getChildren().addAll(row1, row2);
    }

    // ---------- ADMIN ----------
    private void setupAdminUI() {
        sidebarSubtitle.setText("Administration");
        menuSectionLabel.setText("MENU PRINCIPAL");
        showBtn(btnPatients); showBtn(btnConsultations); showBtn(btnHospitalisation);
        showBtn(btnLaboratoire); showBtn(btnPharmacie); showBtn(btnPaiements);
        showBtn(btnMaternite); showBtn(btnRendezVous); showBtn(btnOrdonnances); showBtn(btnRapports);

        setStat(1, "👥", "—", "Patients", "");
        setStat(2, "🏥", "—", "Hospitalisés", "");
        setStat(3, "🩺", "—", "Consultations", "");
        setStat(4, "🧪", "—", "Analyses", "");
        setStat(5, "💊", "—", "Médicaments", "");
        setStat(6, "💰", "—", "Factures", "");
        hideStatCards(false);

        welcomeTitle.setText("Hospital Management System");
        welcomeLabel.setText("Connecté en tant que " + UserSession.getInstance().getFullName() + " — ADMIN");
        welcomeHint.setText("Accès à tous les modules.");

        fillQuickActions(
            qa("Nouveau patient", "btn-primary", e -> showPatients()),
            qa("Nouvelle consultation", "btn-outline", e -> showConsultations()),
            qa("Admission", "btn-outline", e -> showHospitalisation()),
            qa("Actualiser", "btn-secondary", e -> refreshStats())
        );
        roleMainContent.getChildren().clear();
    }

    // ===================== UI HELPERS =====================

    private void setStat(int i, String icon, String value, String label, String sub) {
        switch (i) {
            case 1 -> { statIcon1.setText(icon); statValue1.setText(value); statLabel1.setText(label); if (statSub1 != null) statSub1.setText(sub); }
            case 2 -> { statIcon2.setText(icon); statValue2.setText(value); statLabel2.setText(label); if (statSub2 != null) statSub2.setText(sub); }
            case 3 -> { statIcon3.setText(icon); statValue3.setText(value); statLabel3.setText(label); if (statSub3 != null) statSub3.setText(sub); }
            case 4 -> { statIcon4.setText(icon); statValue4.setText(value); statLabel4.setText(label); if (statSub4 != null) statSub4.setText(sub); }
            case 5 -> { statIcon5.setText(icon); statValue5.setText(value); statLabel5.setText(label); if (statSub5 != null) statSub5.setText(sub); }
            case 6 -> { statIcon6.setText(icon); statValue6.setText(value); statLabel6.setText(label); if (statSub6 != null) statSub6.setText(sub); }
        }
    }

    private void hideStatCards(boolean hide) {
        for (VBox c : new VBox[]{statCard1, statCard2, statCard3, statCard4, statCard5, statCard6}) {
            if (c != null) { c.setVisible(!hide); c.setManaged(!hide); }
        }
    }

    private void fillQuickActions(Button... buttons) {
        quickActionsBox.getChildren().clear();
        quickActionsBox.getChildren().addAll(buttons);
    }

    private Button qa(String text, String style, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        Button b = new Button(text);
        b.getStyleClass().add(style);
        b.setOnAction(h);
        return b;
    }

    private Button fullBtn(String text, String style, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        Button b = qa(text, style, h);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private VBox createCard(String title) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        card.getChildren().add(lbl);
        return card;
    }

    private VBox buildStatusList(String[][] rows, String[] headers) {
        VBox box = new VBox(8);
        for (String[] r : rows) {
            HBox line = new HBox(12);
            line.setAlignment(Pos.CENTER_LEFT);
            line.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-padding: 10 12;");
            for (int i = 0; i < r.length - 1; i++) {
                Label l = new Label(r[i]);
                l.setStyle("-fx-text-fill: #334155; -fx-font-size: 13px;");
                if (i == 0) l.setStyle("-fx-text-fill: #1e293b; -fx-font-weight: bold; -fx-font-size: 13px;");
                HBox.setHgrow(l, Priority.ALWAYS);
                line.getChildren().add(l);
            }
            Label badge = statusBadge(r[r.length - 2], r[r.length - 1]);
            line.getChildren().add(badge);
            box.getChildren().add(line);
        }
        return box;
    }

    private VBox buildSimpleList(String[][] rows) {
        VBox box = new VBox(8);
        for (String[] r : rows) {
            Label l = new Label(r[0]);
            l.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-padding: 10 12; -fx-text-fill: #334155;");
            l.setMaxWidth(Double.MAX_VALUE);
            box.getChildren().add(l);
        }
        return box;
    }

    private VBox buildPrescriptionRows(Object[][] data) {
        VBox box = new VBox(10);
        for (Object[] d : data) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-padding: 12 14;");
            VBox left = new VBox(2);
            Label code = new Label((String) d[0]);
            code.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
            Label patient = new Label("Patient: " + d[1]);
            patient.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            left.getChildren().addAll(code, patient);
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label items = new Label((String) d[2]);
            items.setStyle("-fx-text-fill: #64748b;");
            Label badge = statusBadge((String) d[3], (String) d[4]);
            row.getChildren().addAll(left, sp, items, badge);
            box.getChildren().add(row);
        }
        return box;
    }

    private VBox buildAlertRows(String[][] data) {
        VBox box = new VBox(8);
        for (String[] d : data) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            String bg = "danger".equals(d[3]) ? "#fef2f2" : "warning".equals(d[3]) ? "#fff7ed" : "#f0f9ff";
            row.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8; -fx-padding: 10 12;");
            VBox left = new VBox(2);
            Label name = new Label(d[0]);
            name.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
            Label detail = new Label(d[1]);
            detail.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            left.getChildren().addAll(name, detail);
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Button action = new Button(d[2]);
            action.getStyleClass().add("btn-danger");
            action.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
            row.getChildren().addAll(left, sp, action);
            box.getChildren().add(row);
        }
        return box;
    }

    private Label statusBadge(String text, String type) {
        Label b = new Label(text);
        b.getStyleClass().add("badge");
        switch (type) {
            case "success" -> b.getStyleClass().add("badge-success");
            case "warning" -> b.getStyleClass().add("badge-warning");
            case "danger" -> b.getStyleClass().add("badge-danger");
            default -> b.getStyleClass().add("badge-info");
        }
        return b;
    }

    private HBox finRow(String label, String value, String color) {
        HBox h = new HBox(12);
        h.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #64748b;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value);
        v.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-font-size: 15px;");
        h.getChildren().addAll(l, sp, v);
        return h;
    }

    // ===================== STATS / SERVEUR =====================

    private void checkServerStatus() {
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() {
                return GrpcConfig.isServerReachable("accueil", AppConfig.getPort("accueil"));
            }
        };
        task.setOnSucceeded(e -> updateServerBadge(task.getValue()));
        task.setOnFailed(e -> updateServerBadge(false));
        new Thread(task).start();
    }

    private void updateServerBadge(boolean ok) {
        Platform.runLater(() -> {
            if (ok) {
                serverStatusBadge.setText("● Serveur connecté");
                serverStatusBadge.getStyleClass().setAll("server-status", "connected");
            } else {
                serverStatusBadge.setText("● Serveur déconnecté");
                serverStatusBadge.getStyleClass().setAll("server-status", "disconnected");
            }
        });
    }

    @FXML
    public void refreshStats() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                // ---- Patients ----
                int patientsCount = 0;
                try {
                    patientsCount = new AccueilClient().getPatients(false).size();
                } catch (Exception ignored) {}
                final int fPatients = patientsCount;

                // ---- Hospitalisations ----
                int hospCount = 0;
                try {
                    hospCount = new HospitalisationClient().getHospitalisations(null, "EN_COURS").size();
                } catch (Exception ignored) {}
                final int fHosp = hospCount;

                // ---- Pharmacie ----
                int medsCount = 0;
                int ordAttente = 0;
                try {
                    medsCount = new PharmacieClient().getMedicaments(false).size();
                } catch (Exception ignored) {}
                try {
                    ordAttente = new PharmacieClient().getOrdonnancesEnAttente().size();
                } catch (Exception ignored) {}
                final int fMeds = medsCount;
                final int fOrd = ordAttente;

                // ---- Consultations médecin ----
                int totalCons = 0, attente = 0, terminees = 0, today = 0;
                if (currentRole == UserSession.Role.MEDECIN || currentRole == UserSession.Role.ADMIN) {
                    try {
                        long mid = resolveMedecinId();
                        var list = new ConsultationClient().getByDoctor(mid);
                        totalCons = list.size();
                        String todayStr = java.time.LocalDate.now().toString();
                        for (var c : list) {
                            String st = c.getStatut() != null ? c.getStatut() : "";
                            if ("PLANIFIEE".equals(st) || "EN_COURS".equals(st)) attente++;
                            if ("TERMINEE".equals(st)) terminees++;
                            String d = c.getDateConsultation();
                            if (d != null && d.startsWith(todayStr)) today++;
                        }
                    } catch (Exception ignored) {}
                }
                final int fTotalCons = totalCons, fAttente = attente, fTerminees = terminees, fToday = today;

                // ---- Analyses (échantillon via patients récents) ----
                int analysesCount = 0;
                if (currentRole == UserSession.Role.MEDECIN) {
                    try {
                        var pats = new AccueilClient().getPatients(false);
                        int checked = 0;
                        LaboratoireClient lab = new LaboratoireClient();
                        for (var p : pats) {
                            if (checked >= 5) break;
                            try {
                                analysesCount += lab.getByPatient(p.getId()).size();
                            } catch (Exception ignored) {}
                            checked++;
                        }
                    } catch (Exception ignored) {}
                }
                final int fAnalyses = analysesCount;

                Platform.runLater(() -> {
                    if (currentRole == UserSession.Role.MEDECIN) {
                        statValue1.setText(String.valueOf(fPatients));
                        statValue2.setText(String.valueOf(fToday > 0 ? fToday : fTotalCons));
                        statValue3.setText(String.valueOf(fAttente));
                        statValue4.setText(String.valueOf(fAnalyses));
                        statValue5.setText(String.valueOf(fOrd));
                        statValue6.setText(String.valueOf(fTerminees));
                    } else if (currentRole == UserSession.Role.ADMIN) {
                        statValue1.setText(String.valueOf(fPatients));
                        statValue2.setText(String.valueOf(fHosp));
                        statValue5.setText(String.valueOf(fMeds));
                    } else if (currentRole == UserSession.Role.RECEPTIONNISTE) {
                        statValue1.setText(String.valueOf(fPatients));
                        statValue4.setText(String.valueOf(fHosp));
                    } else if (currentRole == UserSession.Role.PHARMACIEN) {
                        statValue1.setText(String.valueOf(fMeds));
                        statValue6.setText(String.valueOf(fOrd));
                    }
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    // ===================== NAVIGATION =====================

    private void setActive(Button btn, String title) {
        if (currentActiveBtn != null) currentActiveBtn.getStyleClass().remove("active");
        if (btn != null) btn.getStyleClass().add("active");
        currentActiveBtn = btn;
        pageTitleLabel.setText(title);
    }

    private void loadView(String path) {
        try {
            Parent view = new FXMLLoader(getClass().getResource(path)).load();
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Load {}", path, e);
            FxUtils.showError("Erreur", "Impossible de charger : " + path);
        }
    }

    @FXML private void showDashboard() {
        setActive(btnDashboard, "Dashboard");
        contentPane.getChildren().setAll(dashboardScroll);
        refreshStats();
    }
    @FXML private void showPatients() { setActive(btnPatients, "Patients"); loadView("/fxml/patients.fxml"); }
    @FXML private void showConsultations() { setActive(btnConsultations, "Consultations"); loadView("/fxml/consultations.fxml"); }
    @FXML private void showDossiers() {setActive(btnDossiers, "Dossiers médicaux");loadView("/fxml/dossiers.fxml");}
    @FXML private void showOrdonnances() { setActive(btnOrdonnances, "Ordonnances"); loadView("/fxml/ordonnances.fxml"); }
    @FXML private void showResultats() { setActive(btnResultats, "Résultats"); loadView("/fxml/resultats.fxml"); }
    @FXML private void showRendezVous() {setActive(btnRendezVous, "Rendez-vous");loadView("/fxml/rendezvous.fxml");}
    @FXML private void showHistorique() {setActive(btnHistorique, "Historique"); loadView("/fxml/historique.fxml");}
    @FXML private void showHospitalisation() {
        Button a = (btnHospitalisation != null && btnHospitalisation.isVisible()) ? btnHospitalisation : btnAdmissions;
        setActive(a, "Hospitalisation"); loadView("/fxml/hospitalisation.fxml");
    }
    @FXML private void showFileAttente() {setActive(btnFileAttente, "File d'attente");loadView("/fxml/file_attente.fxml");}
    @FXML private void showSorties() { setActive(btnSorties, "Sorties"); loadView("/fxml/hospitalisation.fxml"); }
    @FXML private void showLaboratoire() {
        Button a = (btnExamens != null && btnExamens.isVisible()) ? btnExamens : btnLaboratoire;
        setActive(a, "Laboratoire"); loadView("/fxml/laboratoire.fxml");
    }
    @FXML private void showPharmacie() { setActive(btnPharmacie, "Pharmacie"); loadView("/fxml/pharmacie.fxml"); }

    // === MODIFICATION : showStocks() charge désormais stocks.fxml ===
    @FXML private void showStocks() { setActive(btnStocks, "Stocks"); loadView("/fxml/stocks.fxml"); }

    // === MODIFICATION : showRapports() charge désormais rapports.fxml ===
    @FXML private void showRapports() { setActive(btnRapports, "Rapports"); loadView("/fxml/rapports.fxml"); }

    @FXML private void showPaiements() {
        Button a = (btnPaiements != null && btnPaiements.isVisible()) ? btnPaiements : btnFacturation;
        setActive(a, "Paiements"); loadView("/fxml/paiements.fxml");
    }
    @FXML private void showFacturation() { setActive(btnFacturation, "Facturation"); loadView("/fxml/paiements.fxml"); }
    @FXML private void showFactures() { setActive(btnFactures, "Factures"); loadView("/fxml/paiements.fxml"); }
    @FXML private void showImpayes() { setActive(btnImpayes, "Impayés"); loadView("/fxml/paiements.fxml"); }
    @FXML private void showRecus() { setActive(btnRecus, "Reçus"); loadView("/fxml/recus.fxml"); }
    @FXML private void showMaternite() { setActive(btnMaternite, "Maternité"); loadView("/fxml/maternite.fxml"); }
    @FXML private void showSettings() { setActive(btnSettings, "Paramètres"); loadView("/fxml/settings.fxml"); }
    @FXML private void showProfil() { setActive(btnProfil, "Profil"); loadView("/fxml/profil.fxml"); }

    @FXML
    private void onLogout() {
        if (!FxUtils.confirm("Déconnexion", "Voulez-vous vraiment vous déconnecter ?")) return;
        UserSession.getInstance().logout();
        GrpcConfig.shutdownAll();
        try {
            Parent root = new FXMLLoader(getClass().getResource("/fxml/login.fxml")).load();
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            Scene scene = new Scene(root, 1100, 700);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(AppConfig.getAppTitle());
            stage.setMaximized(false);
            stage.centerOnScreen();
        } catch (IOException e) {
            log.error("Erreur déconnexion", e);
        }
    }
}
