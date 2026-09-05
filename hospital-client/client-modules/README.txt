MODULES CLIENT - Données dynamiques gRPC
========================================

1) Contrôleurs → src/main/java/com/hospital/controller/
   - ConsultationController.java
   - PharmacieController.java
   - HospitalisationController.java
   - PaiementController.java
   - LaboratoireController.java
   - DashboardController.java  (navigation mise à jour)

2) FXML → src/main/resources/fxml/
   - consultations.fxml
   - pharmacie.fxml
   - hospitalisation.fxml
   - paiements.fxml
   - laboratoire.fxml
   - dashboard.fxml  (si fourni)

3) Config client → src/main/resources/application.properties
   grpc.server.host=127.0.0.1   (même machine)

4) Services serveur à démarrer selon les modules utilisés :
   - Patients / Accueil     → accueil-service      :50051
   - Consultations          → consultation-service :50054
   - Pharmacie / Stocks     → pharmacie-service    :50056
   - Hospitalisation        → hospitalisation-service :50052
   - Paiements / Factures   → paiement-service     :50053
   - Laboratoire / Analyses → laboratoire-service  :50055

5) Sur la même machine :
   - MySQL + schema.sql + data.sql
   - mvn spring-boot:run dans chaque service nécessaire
   - Client avec grpc.server.host=127.0.0.1

Pages encore en placeholder (pas de service dédié côté client pour l'instant) :
   - Rendez-vous, File d'attente, Historique, Dossiers médicaux, Reçus, Rapports, Profil, Maternité
