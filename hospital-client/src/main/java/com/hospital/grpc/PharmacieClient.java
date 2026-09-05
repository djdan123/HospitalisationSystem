package com.hospital.grpc;

import com.hospital.config.AppConfig;
import com.hospital.config.GrpcConfig;
import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.pharmacie.*;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PharmacieClient {

    private final ManagedChannel channel;

    public PharmacieClient() {
        this.channel = GrpcConfig.getPharmacieChannel();
    }

    private PharmacieServiceGrpc.PharmacieServiceBlockingStub stub() {
        return PharmacieServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(AppConfig.getDeadlineSeconds(), TimeUnit.SECONDS);
    }

    // ===================== MÉDICAMENTS =====================

    public Medicament createMedicament(String code, String nom, String description, int stock, double prix, String unite) {
        try {
            return stub().createMedicament(CreateMedicamentRequest.newBuilder()
                    .setCode(code).setNom(nom)
                    .setDescription(description != null ? description : "")
                    .setStockInitial(stock).setPrixUnitaire(prix)
                    .setUnite(unite != null ? unite : "")
                    .build()).getMedicament();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Medicament getMedicament(long id) {
        try {
            return stub().getMedicament(GetMedicamentRequest.newBuilder().setId(id).build()).getMedicament();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Medicament> getMedicaments(boolean onlyAvailable) {
        try {
            return stub().getMedicaments(GetMedicamentsRequest.newBuilder()
                    .setOnlyAvailable(onlyAvailable).build()).getMedicamentsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Medicament updateMedicament(long id, String nom, String description, double prixUnitaire, String statut) {
        try {
            UpdateMedicamentRequest.Builder b = UpdateMedicamentRequest.newBuilder()
                    .setId(id).setPrixUnitaire(prixUnitaire);
            if (nom != null && !nom.isBlank()) b.setNom(nom);
            if (description != null && !description.isBlank()) b.setDescription(description);
            if (statut != null && !statut.isBlank()) b.setStatut(statut);
            return stub().updateMedicament(b.build()).getMedicament();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Medicament updateStock(long medicamentId, int quantite, String motif) {
        try {
            return stub().updateStock(UpdateStockRequest.newBuilder()
                    .setMedicamentId(medicamentId)
                    .setQuantite(quantite)
                    .setMotif(motif != null ? motif : "")
                    .build()).getMedicament();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    // ===================== ORDONNANCES =====================

    public Ordonnance createOrdonnance(long patientId, long medecinId, String typePatient,
                                       String serviceSoins, String observations,
                                       List<OrdonnanceLigne> lignes) {
        try {
            CreateOrdonnanceRequest.Builder b = CreateOrdonnanceRequest.newBuilder()
                    .setPatientId(patientId)
                    .setMedecinId(medecinId)
                    .setTypePatient(typePatient != null ? typePatient : "AMBULATOIRE")
                    .setServiceSoins(serviceSoins != null ? serviceSoins : "")
                    .setObservations(observations != null ? observations : "");
            if (lignes != null) b.addAllLignes(lignes);
            return stub().createOrdonnance(b.build()).getOrdonnance();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Ordonnance createOrdonnance(long patientId, long medecinId, List<OrdonnanceLigne> lignes) {
        return createOrdonnance(patientId, medecinId, "AMBULATOIRE", "", "", lignes);
    }

    public Ordonnance getOrdonnance(long id) {
        try {
            return stub().getOrdonnance(GetOrdonnanceRequest.newBuilder().setId(id).build()).getOrdonnance();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Ordonnance> getOrdonnancesByPatient(long patientId) {
        try {
            return stub().getOrdonnancesByPatient(
                    GetOrdonnancesByPatientRequest.newBuilder().setPatientId(patientId).build()
            ).getOrdonnancesList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Ordonnance> getOrdonnancesEnAttente() {
        try {
            return stub().getOrdonnancesEnAttente(
                    GetOrdonnancesEnAttenteRequest.newBuilder().build()
            ).getOrdonnancesList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public DispenseResponse dispense(long ordonnanceId, long medicamentId, int quantite) {
        try {
            return stub().dispenseMedicament(DispenseMedicamentRequest.newBuilder()
                    .setOrdonnanceId(ordonnanceId)
                    .setMedicamentId(medicamentId)
                    .setQuantite(quantite)
                    .build());
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    // ===================== GESTION DE STOCK =====================

    public StockMouvementMsg stockEntree(long medicamentId, int quantite, String lot,
                                         String datePeremption, String source, String user, String motif) {
        try {
            return stub().stockEntree(StockEntreeRequest.newBuilder()
                    .setMedicamentId(medicamentId)
                    .setQuantite(quantite)
                    .setNumeroLot(lot != null ? lot : "")
                    .setDatePeremption(datePeremption != null ? datePeremption : "")
                    .setSource(source != null ? source : "")
                    .setUserLogin(user != null ? user : "")
                    .setMotif(motif != null ? motif : "")
                    .build()).getMouvement();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public StockMouvementMsg stockSortie(long medicamentId, int quantite, String motif,
                                         long patientId, String service, String user,
                                         boolean doubleCtrl, String controleur) {
        try {
            return stub().stockSortie(StockSortieRequest.newBuilder()
                    .setMedicamentId(medicamentId)
                    .setQuantite(quantite)
                    .setMotif(motif != null ? motif : "")
                    .setPatientId(patientId)
                    .setServiceSoins(service != null ? service : "")
                    .setUserLogin(user != null ? user : "")
                    .setDoubleControle(doubleCtrl)
                    .setControleur(controleur != null ? controleur : "")
                    .build()).getMouvement();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public StockMouvementMsg stockRetour(long medicamentId, int quantite, String service,
                                         String user, String motif) {
        try {
            return stub().stockRetour(StockRetourRequest.newBuilder()
                    .setMedicamentId(medicamentId)
                    .setQuantite(quantite)
                    .setServiceSoins(service != null ? service : "")
                    .setUserLogin(user != null ? user : "")
                    .setMotif(motif != null ? motif : "")
                    .build()).getMouvement();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public StockMouvementMsg stockInventaire(long medicamentId, int quantiteComptee,
                                             String user, String motif) {
        try {
            return stub().stockInventaire(StockInventaireRequest.newBuilder()
                    .setMedicamentId(medicamentId)
                    .setQuantiteComptee(quantiteComptee)
                    .setUserLogin(user != null ? user : "")
                    .setMotif(motif != null ? motif : "")
                    .build()).getMouvement();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Medicament bloquerLot(String numeroLot, String motif) {
        try {
            return stub().bloquerLot(BloquerLotRequest.newBuilder()
                    .setNumeroLot(numeroLot)
                    .setMotif(motif != null ? motif : "")
                    .build()).getMedicament();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public AlertesStockResponse getAlertesStock(int joursPeremption) {
        try {
            return stub().getAlertesStock(GetAlertesStockRequest.newBuilder()
                    .setJoursPeremption(joursPeremption)
                    .build());
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<StockMouvementMsg> getHistoriqueStock(long medicamentId) {
        try {
            return stub().getHistoriqueStock(GetHistoriqueStockRequest.newBuilder()
                    .setMedicamentId(medicamentId)
                    .build()).getMouvementsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    // ===================== RAPPORTS =====================

    public RapportValorisationResponse getRapportValorisation(String depuis) {
        try {
            return stub().getRapportValorisation(GetRapportRequest.newBuilder()
                    .setDepuis(depuis != null ? depuis : "")
                    .build());
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public RapportTopConsoResponse getRapportTopConso(int limit, String service, String depuis) {
        try {
            return stub().getRapportTopConso(GetRapportTopConsoRequest.newBuilder()
                    .setLimit(limit)
                    .setService(service != null ? service : "")
                    .setDepuis(depuis != null ? depuis : "")
                    .build());
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public RapportDepensesResponse getRapportDepensesService(String depuis) {
        try {
            return stub().getRapportDepensesService(GetRapportRequest.newBuilder()
                    .setDepuis(depuis != null ? depuis : "")
                    .build());
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Medicament> getRapportPeremption(int jours) {
        try {
            return stub().getRapportPeremption(GetRapportPeremptionRequest.newBuilder()
                    .setJours(jours)
                    .build()).getMedicamentsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Medicament> getRapportRuptures() {
        try {
            return stub().getRapportRuptures(GetRapportRequest.newBuilder().build())
                    .getMedicamentsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<StockMouvementMsg> getRapportLot(String lot) {
        try {
            return stub().getRapportLot(GetRapportLotRequest.newBuilder()
                    .setNumeroLot(lot)
                    .build()).getMouvementsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<StockMouvementMsg> getRapportStupefiants(String depuis) {
        try {
            return stub().getRapportStupefiants(GetRapportRequest.newBuilder()
                    .setDepuis(depuis != null ? depuis : "")
                    .build()).getMouvementsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public RapportOrdonnancesResponse getRapportOrdonnances() {
        try {
            return stub().getRapportOrdonnances(GetRapportRequest.newBuilder().build());
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<StockMouvementMsg> getRapportAudit(String user) {
        try {
            return stub().getRapportAudit(GetRapportAuditRequest.newBuilder()
                    .setUserLogin(user != null ? user : "")
                    .build()).getMouvementsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public RapportKpiRotationResponse getRapportKpiRotation(int jours) {
        try {
            return stub().getRapportKpiRotation(GetRapportKpiRequest.newBuilder()
                    .setJours(jours)
                    .build());
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }
}