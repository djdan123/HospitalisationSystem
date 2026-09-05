package com.hospital.pharmacie.grpc;

import com.hospital.grpc.pharmacie.*;
import com.hospital.pharmacie.entity.Medicament;
import com.hospital.pharmacie.entity.Ordonnance;
import com.hospital.pharmacie.entity.OrdonnanceLigne;
import com.hospital.pharmacie.entity.StockMouvement;
import com.hospital.pharmacie.service.PharmacieService;
import com.hospital.pharmacie.service.StockService;
import com.hospital.pharmacie.service.RapportService;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PharmacieGrpcService extends PharmacieServiceGrpc.PharmacieServiceImplBase {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final PharmacieService service;
    private final StockService stockService;
    private final RapportService rapportService;

    public PharmacieGrpcService(PharmacieService service, StockService stockService, RapportService rapportService) {
        this.service = service;
        this.stockService = stockService;
        this.rapportService = rapportService;
    }

    // ===================== MÉDICAMENTS =====================

    @Override
    public void createMedicament(CreateMedicamentRequest r, StreamObserver<MedicamentResponse> o) {
        Medicament m = service.create(r.getCode(), r.getNom(), r.getDescription(),
                r.getStockInitial(), r.getPrixUnitaire(), r.getUnite());
        o.onNext(toResp(m));
        o.onCompleted();
    }

    @Override
    public void getMedicament(GetMedicamentRequest r, StreamObserver<MedicamentResponse> o) {
        o.onNext(toResp(service.get(r.getId())));
        o.onCompleted();
    }

    @Override
    public void getMedicaments(GetMedicamentsRequest r, StreamObserver<MedicamentsResponse> o) {
        MedicamentsResponse.Builder b = MedicamentsResponse.newBuilder();
        service.getAll(r.getOnlyAvailable()).forEach(m -> b.addMedicaments(toProto(m)));
        o.onNext(b.build());
        o.onCompleted();
    }

    @Override
    public void updateMedicament(UpdateMedicamentRequest r, StreamObserver<MedicamentResponse> o) {
        Medicament m = service.get(r.getId());
        if (!r.getNom().isBlank()) m.setNom(r.getNom());
        if (!r.getDescription().isBlank()) m.setDescription(r.getDescription());
        if (r.getPrixUnitaire() > 0) m.setPrixUnitaire(r.getPrixUnitaire());
        if (!r.getStatut().isBlank()) m.setStatut(r.getStatut());
        // Mise à jour via updateStock(0) pour simplifier (ou on pourrait faire repo.save)
        o.onNext(toResp(service.updateStock(m.getId(), 0, "update meta")));
        o.onCompleted();
    }

    @Override
    public void updateStock(UpdateStockRequest r, StreamObserver<MedicamentResponse> o) {
        o.onNext(toResp(service.updateStock(r.getMedicamentId(), r.getQuantite(), r.getMotif())));
        o.onCompleted();
    }

    // ===================== ORDONNANCES =====================

    @Override
    public void createOrdonnance(CreateOrdonnanceRequest r, StreamObserver<OrdonnanceResponse> o) {
        List<PharmacieService.LigneInput> lignes = new ArrayList<>();
        for (com.hospital.grpc.pharmacie.OrdonnanceLigne l : r.getLignesList()) {
            lignes.add(new PharmacieService.LigneInput(l.getMedicamentId(), l.getQuantite(), l.getPosologie()));
        }
        Ordonnance ord = service.createOrdonnance(
                r.getPatientId(),
                r.getMedecinId(),
                r.getTypePatient(),
                r.getServiceSoins(),
                r.getObservations(),
                lignes
        );
        o.onNext(OrdonnanceResponse.newBuilder().setOrdonnance(toProto(ord)).build());
        o.onCompleted();
    }

    @Override
    public void getOrdonnance(GetOrdonnanceRequest r, StreamObserver<OrdonnanceResponse> o) {
        o.onNext(OrdonnanceResponse.newBuilder().setOrdonnance(toProto(service.getOrdonnance(r.getId()))).build());
        o.onCompleted();
    }

    @Override
    public void getOrdonnancesByPatient(GetOrdonnancesByPatientRequest r, StreamObserver<OrdonnancesResponse> o) {
        OrdonnancesResponse.Builder b = OrdonnancesResponse.newBuilder();
        service.getOrdonnancesByPatient(r.getPatientId()).forEach(ord -> b.addOrdonnances(toProto(ord)));
        o.onNext(b.build());
        o.onCompleted();
    }

    @Override
    public void getOrdonnancesEnAttente(GetOrdonnancesEnAttenteRequest r, StreamObserver<OrdonnancesResponse> o) {
        OrdonnancesResponse.Builder b = OrdonnancesResponse.newBuilder();
        service.getOrdonnancesEnAttente().forEach(ord -> b.addOrdonnances(toProto(ord)));
        o.onNext(b.build());
        o.onCompleted();
    }

    @Override
    public void dispenseMedicament(DispenseMedicamentRequest r, StreamObserver<DispenseResponse> o) {
        PharmacieService.DispenseResult result = service.dispenseLigne(
                r.getOrdonnanceId(), r.getMedicamentId(), r.getQuantite());
        o.onNext(DispenseResponse.newBuilder()
                .setSuccess(result.success)
                .setMessage(result.message)
                .setStockRestant(result.stockRestant)
                .setOrdonnance(toProto(result.ordonnance))
                .build());
        o.onCompleted();
    }

    // ===================== GESTION DE STOCK =====================

    @Override
    public void stockEntree(StockEntreeRequest r, StreamObserver<StockMouvementResponse> o) {
        LocalDate peremp = r.getDatePeremption().isBlank() ? null : LocalDate.parse(r.getDatePeremption());
        StockMouvement mv = stockService.entree(
                r.getMedicamentId(),
                r.getQuantite(),
                r.getNumeroLot(),
                peremp,
                r.getSource(),
                r.getUserLogin(),
                r.getMotif()
        );
        o.onNext(StockMouvementResponse.newBuilder().setMouvement(toMvtProto(mv)).build());
        o.onCompleted();
    }

    @Override
    public void stockSortie(StockSortieRequest r, StreamObserver<StockMouvementResponse> o) {
        Long pid = r.getPatientId() > 0 ? r.getPatientId() : null;
        StockMouvement mv = stockService.sortie(
                r.getMedicamentId(),
                r.getQuantite(),
                r.getMotif(),
                pid,
                r.getServiceSoins(),
                r.getUserLogin(),
                r.getDoubleControle(),
                r.getControleur()
        );
        o.onNext(StockMouvementResponse.newBuilder().setMouvement(toMvtProto(mv)).build());
        o.onCompleted();
    }

    @Override
    public void stockRetour(StockRetourRequest r, StreamObserver<StockMouvementResponse> o) {
        StockMouvement mv = stockService.retour(
                r.getMedicamentId(),
                r.getQuantite(),
                r.getServiceSoins(),
                r.getUserLogin(),
                r.getMotif()
        );
        o.onNext(StockMouvementResponse.newBuilder().setMouvement(toMvtProto(mv)).build());
        o.onCompleted();
    }

    @Override
    public void stockInventaire(StockInventaireRequest r, StreamObserver<StockMouvementResponse> o) {
        StockMouvement mv = stockService.inventaire(
                r.getMedicamentId(),
                r.getQuantiteComptee(),
                r.getUserLogin(),
                r.getMotif()
        );
        o.onNext(StockMouvementResponse.newBuilder().setMouvement(toMvtProto(mv)).build());
        o.onCompleted();
    }

    @Override
    public void bloquerLot(BloquerLotRequest r, StreamObserver<MedicamentResponse> o) {
        Medicament m = stockService.bloquerLot(r.getNumeroLot(), r.getMotif());
        o.onNext(toResp(m));
        o.onCompleted();
    }

    @Override
    public void getAlertesStock(GetAlertesStockRequest r, StreamObserver<AlertesStockResponse> o) {
        int jours = r.getJoursPeremption() > 0 ? r.getJoursPeremption() : 30;
        AlertesStockResponse.Builder b = AlertesStockResponse.newBuilder();
        stockService.alertesStockFaible().forEach(m -> b.addStockFaible(toProto(m)));
        stockService.alertesPeremption(jours).forEach(m -> b.addPeremption(toProto(m)));
        stockService.alertesSurstock().forEach(m -> b.addSurstock(toProto(m)));
        o.onNext(b.build());
        o.onCompleted();
    }

    @Override
    public void getHistoriqueStock(GetHistoriqueStockRequest r, StreamObserver<StockMouvementsResponse> o) {
        Long mid = r.getMedicamentId() > 0 ? r.getMedicamentId() : null;
        StockMouvementsResponse.Builder b = StockMouvementsResponse.newBuilder();
        stockService.historique(mid).forEach(mv -> b.addMouvements(toMvtProto(mv)));
        o.onNext(b.build());
        o.onCompleted();
    }

    // ===================== RAPPORTS =====================

    @Override
    public void getRapportValorisation(GetRapportRequest r, StreamObserver<RapportValorisationResponse> o) {
        RapportValorisationResponse.Builder b = RapportValorisationResponse.newBuilder()
                .setTotal(rapportService.valorisationStock());
        rapportService.detailValorisation().forEach(l -> b.addLignes(LigneValorisationMsg.newBuilder()
                .setCode(n(l.code())).setNom(n(l.nom())).setStock(l.stock())
                .setPrix(l.prix()).setValeur(l.valeur()).setUnite(n(l.unite())).build()));
        o.onNext(b.build());
        o.onCompleted();
    }

    @Override
    public void getRapportTopConso(GetRapportTopConsoRequest r, StreamObserver<RapportTopConsoResponse> o) {
        LocalDate d = parseDate(r.getDepuis());
        RapportTopConsoResponse.Builder b = RapportTopConsoResponse.newBuilder();
        rapportService.topConsommations(r.getLimit() > 0 ? r.getLimit() : 20, r.getService(), d)
                .forEach(t -> b.addLignes(TopConsoMsg.newBuilder()
                        .setCode(n(t.code())).setNom(n(t.nom()))
                        .setQuantite(t.quantite()).setService(n(t.service())).build()));
        o.onNext(b.build());
        o.onCompleted();
    }

    @Override
    public void getRapportDepensesService(GetRapportRequest r, StreamObserver<RapportDepensesResponse> o) {
        RapportDepensesResponse.Builder b = RapportDepensesResponse.newBuilder();
        rapportService.depensesParService(parseDate(r.getDepuis()))
                .forEach(d -> b.addLignes(DepenseServiceMsg.newBuilder()
                        .setService(n(d.service())).setQuantite(d.quantite()).setValeur(d.valeur()).build()));
        o.onNext(b.build());
        o.onCompleted();
    }

    @Override
    public void getRapportPeremption(GetRapportPeremptionRequest r, StreamObserver<MedicamentsResponse> o) {
        MedicamentsResponse.Builder b = MedicamentsResponse.newBuilder();
        rapportService.peremptionDans(r.getJours() > 0 ? r.getJours() : 30)
                .forEach(m -> b.addMedicaments(toProto(m)));
        o.onNext(b.build());
        o.onCompleted();
    }

    @Override
    public void getRapportRuptures(GetRapportRequest r, StreamObserver<MedicamentsResponse> o) {
        MedicamentsResponse.Builder b = MedicamentsResponse.newBuilder();
        rapportService.rupturesActuelles().forEach(m -> b.addMedicaments(toProto(m)));
        o.onNext(b.build());
        o.onCompleted();
    }

    @Override
    public void getRapportLot(GetRapportLotRequest r, StreamObserver<StockMouvementsResponse> o) {
        StockMouvementsResponse.Builder b = StockMouvementsResponse.newBuilder();
        rapportService.distributionsLot(r.getNumeroLot()).forEach(mv -> b.addMouvements(toMvtProto(mv)));
        o.onNext(b.build());
        o.onCompleted();
    }

    @Override
    public void getRapportStupefiants(GetRapportRequest r, StreamObserver<StockMouvementsResponse> o) {
        StockMouvementsResponse.Builder b = StockMouvementsResponse.newBuilder();
        rapportService.registreStupefiants(parseDate(r.getDepuis()))
                .forEach(mv -> b.addMouvements(toMvtProto(mv)));
        o.onNext(b.build());
        o.onCompleted();
    }

    @Override
    public void getRapportOrdonnances(GetRapportRequest r, StreamObserver<RapportOrdonnancesResponse> o) {
        var s = rapportService.statsOrdonnances();
        o.onNext(RapportOrdonnancesResponse.newBuilder()
                .setTotal(s.total()).setDelivrees(s.delivrees())
                .setPartielles(s.partielles()).setCreees(s.creees()).build());
        o.onCompleted();
    }

    @Override
    public void getRapportAudit(GetRapportAuditRequest r, StreamObserver<StockMouvementsResponse> o) {
        StockMouvementsResponse.Builder b = StockMouvementsResponse.newBuilder();
        rapportService.auditUtilisateurs(r.getUserLogin()).forEach(mv -> b.addMouvements(toMvtProto(mv)));
        o.onNext(b.build());
        o.onCompleted();
    }

    @Override
    public void getRapportKpiRotation(GetRapportKpiRequest r, StreamObserver<RapportKpiRotationResponse> o) {
        RapportKpiRotationResponse.Builder b = RapportKpiRotationResponse.newBuilder();
        rapportService.rotationStock(r.getJours() > 0 ? r.getJours() : 30)
                .forEach(k -> b.addLignes(KpiRotationMsg.newBuilder()
                        .setCode(n(k.code())).setNom(n(k.nom()))
                        .setSorties(k.sorties()).setStockActuel(k.stockActuel())
                        .setRotation(k.rotation()).build()));
        o.onNext(b.build());
        o.onCompleted();
    }

    // ===================== MAPPING =====================

    private MedicamentResponse toResp(Medicament m) {
        return MedicamentResponse.newBuilder().setMedicament(toProto(m)).build();
    }

    private com.hospital.grpc.pharmacie.Medicament toProto(Medicament m) {
        com.hospital.grpc.pharmacie.Medicament.Builder b = com.hospital.grpc.pharmacie.Medicament.newBuilder()
                .setId(m.getId() != null ? m.getId() : 0)
                .setCode(m.getCode() != null ? m.getCode() : "")
                .setNom(m.getNom() != null ? m.getNom() : "")
                .setDescription(m.getDescription() != null ? m.getDescription() : "")
                .setStock(m.getStock() != null ? m.getStock() : 0)
                .setPrixUnitaire(m.getPrixUnitaire() != null ? m.getPrixUnitaire() : 0)
                .setUnite(m.getUnite() != null ? m.getUnite() : "")
                .setStatut(m.getStatut() != null ? m.getStatut() : "")
                .setSeuilMin(m.getSeuilMin() != null ? m.getSeuilMin() : 10)
                .setSeuilMax(m.getSeuilMax() != null ? m.getSeuilMax() : 500)
                .setDatePeremption(m.getDatePeremption() != null ? m.getDatePeremption().toString() : "")
                .setNumeroLot(m.getNumeroLot() != null ? m.getNumeroLot() : "")
                .setIsStupefiant(m.getIsStupefiant() != null && m.getIsStupefiant())
                .setEmplacement(m.getEmplacement() != null ? m.getEmplacement() : "");
        return b.build();
    }

    private com.hospital.grpc.pharmacie.Ordonnance toProto(Ordonnance o) {
        com.hospital.grpc.pharmacie.Ordonnance.Builder b = com.hospital.grpc.pharmacie.Ordonnance.newBuilder()
                .setId(o.getId() != null ? o.getId() : 0)
                .setPatientId(o.getPatientId() != null ? o.getPatientId() : 0)
                .setMedecinId(o.getMedecinId() != null ? o.getMedecinId() : 0)
                .setDateOrdonnance(o.getDateOrdonnance() != null ? o.getDateOrdonnance().format(FMT) : "")
                .setStatut(o.getStatut() != null ? o.getStatut() : "")
                .setTypePatient(o.getTypePatient() != null ? o.getTypePatient() : "AMBULATOIRE")
                .setServiceSoins(o.getServiceSoins() != null ? o.getServiceSoins() : "")
                .setObservations(o.getObservations() != null ? o.getObservations() : "");
        if (o.getLignes() != null) {
            for (OrdonnanceLigne l : o.getLignes()) {
                b.addLignes(com.hospital.grpc.pharmacie.OrdonnanceLigne.newBuilder()
                        .setMedicamentId(l.getMedicamentId() != null ? l.getMedicamentId() : 0)
                        .setNomMedicament(l.getNomMedicament() != null ? l.getNomMedicament() : "")
                        .setQuantite(l.getQuantite() != null ? l.getQuantite() : 0)
                        .setPosologie(l.getPosologie() != null ? l.getPosologie() : "")
                        .setQuantiteDelivree(l.getQuantiteDelivree() != null ? l.getQuantiteDelivree() : 0)
                        .build());
            }
        }
        return b.build();
    }

    private StockMouvementMsg toMvtProto(StockMouvement mv) {
        return StockMouvementMsg.newBuilder()
                .setId(mv.getId() != null ? mv.getId() : 0)
                .setMedicamentId(mv.getMedicamentId())
                .setTypeMouvement(mv.getTypeMouvement())
                .setQuantite(mv.getQuantite())
                .setStockAvant(mv.getStockAvant() != null ? mv.getStockAvant() : 0)
                .setStockApres(mv.getStockApres() != null ? mv.getStockApres() : 0)
                .setNumeroLot(mv.getNumeroLot() != null ? mv.getNumeroLot() : "")
                .setDatePeremption(mv.getDatePeremption() != null ? mv.getDatePeremption().toString() : "")
                .setMotif(mv.getMotif() != null ? mv.getMotif() : "")
                .setSourceDest(mv.getSourceDest() != null ? mv.getSourceDest() : "")
                .setPatientId(mv.getPatientId() != null ? mv.getPatientId() : 0)
                .setUserLogin(mv.getUserLogin() != null ? mv.getUserLogin() : "")
                .setServiceSoins(mv.getServiceSoins() != null ? mv.getServiceSoins() : "")
                .setDoubleControle(mv.getDoubleControle())
                .setControleur(mv.getControleur() != null ? mv.getControleur() : "")
                .setDateMouvement(mv.getDateMouvement() != null ? mv.getDateMouvement().toString() : "")
                .build();
    }

    // ===================== UTILITAIRES =====================

    private static String n(String s) {
        return s != null ? s : "";
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}