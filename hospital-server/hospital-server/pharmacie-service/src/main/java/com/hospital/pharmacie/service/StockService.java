package com.hospital.pharmacie.service;

import com.hospital.common.exception.FailedPreconditionException;
import com.hospital.common.exception.InvalidArgumentException;
import com.hospital.common.exception.NotFoundException;
import com.hospital.pharmacie.entity.Medicament;
import com.hospital.pharmacie.entity.StockMouvement;
import com.hospital.pharmacie.repository.MedicamentRepository;
import com.hospital.pharmacie.repository.StockMouvementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestion stocks : entrées, sorties, retours, FEFO, lots, alertes, stupéfiants.
 */
@Service
public class StockService {

    private final MedicamentRepository medicamentRepository;
    private final StockMouvementRepository mouvementRepository;

    public StockService(MedicamentRepository medicamentRepository,
                        StockMouvementRepository mouvementRepository) {
        this.medicamentRepository = medicamentRepository;
        this.mouvementRepository = mouvementRepository;
    }

    // ---------- Entrée (livraison labo / grossiste) ----------
    @Transactional
    public StockMouvement entree(long medicamentId, int quantite, String numeroLot,
                                 LocalDate datePeremption, String source,
                                 String userLogin, String motif) {
        if (quantite <= 0) throw new InvalidArgumentException("Quantité entrée doit être > 0");
        Medicament m = getMed(medicamentId);
        int avant = m.getStock();
        m.setStock(avant + quantite);
        if (numeroLot != null && !numeroLot.isBlank()) m.setNumeroLot(numeroLot.trim());
        if (datePeremption != null) m.setDatePeremption(datePeremption);
        m.setStatut(m.getStock() > 0 ? "DISPONIBLE" : "RUPTURE");
        medicamentRepository.save(m);

        return saveMvt(m, "ENTREE", quantite, avant, m.getStock(),
                numeroLot, datePeremption, motif, source, null, userLogin, null, false, null);
    }

    // ---------- Sortie (délivrance patient ou envoi service) — FEFO ----------
    @Transactional
    public StockMouvement sortie(long medicamentId, int quantite, String motif,
                                 Long patientId, String serviceSoins, String userLogin,
                                 boolean stupefiantDoubleControle, String controleur) {
        if (quantite <= 0) throw new InvalidArgumentException("Quantité sortie doit être > 0");
        Medicament m = getMed(medicamentId);

        if (m.getIsStupefiant()) {
            if (!stupefiantDoubleControle || controleur == null || controleur.isBlank()) {
                throw new FailedPreconditionException(
                        "Stupéfiant : double contrôle obligatoire (contrôleur requis).");
            }
            if (controleur.equalsIgnoreCase(userLogin)) {
                throw new FailedPreconditionException(
                        "Stupéfiant : le contrôleur doit être différent de l'opérateur.");
            }
        }
        if (m.isPerime()) {
            throw new FailedPreconditionException("Médicament périmé — sortie interdite (lot "
                    + m.getNumeroLot() + ").");
        }
        int avant = m.getStock();
        if (avant < quantite) {
            throw new FailedPreconditionException("Stock insuffisant (" + avant + " disponible).");
        }
        m.setStock(avant - quantite);
        m.setStatut(m.getStock() == 0 ? "RUPTURE" : "DISPONIBLE");
        medicamentRepository.save(m);

        String dest = patientId != null ? "Patient#" + patientId
                : (serviceSoins != null ? serviceSoins : "sortie");
        return saveMvt(m, "SORTIE", quantite, avant, m.getStock(),
                m.getNumeroLot(), m.getDatePeremption(), motif, dest,
                patientId, userLogin, serviceSoins, stupefiantDoubleControle, controleur);
    }

    // ---------- Retour (médicaments non utilisés par un service) ----------
    @Transactional
    public StockMouvement retour(long medicamentId, int quantite, String serviceSoins,
                                 String userLogin, String motif) {
        if (quantite <= 0) throw new InvalidArgumentException("Quantité retour doit être > 0");
        Medicament m = getMed(medicamentId);
        int avant = m.getStock();
        m.setStock(avant + quantite);
        m.setStatut("DISPONIBLE");
        medicamentRepository.save(m);
        return saveMvt(m, "RETOUR", quantite, avant, m.getStock(),
                m.getNumeroLot(), m.getDatePeremption(), motif,
                serviceSoins, null, userLogin, serviceSoins, false, null);
    }

    // ---------- Inventaire (ajustement après comptage physique) ----------
    @Transactional
    public StockMouvement inventaire(long medicamentId, int quantiteComptee, String userLogin, String motif) {
        Medicament m = getMed(medicamentId);
        int avant = m.getStock();
        int delta = quantiteComptee - avant;
        m.setStock(quantiteComptee);
        m.setStatut(quantiteComptee == 0 ? "RUPTURE" : "DISPONIBLE");
        medicamentRepository.save(m);
        return saveMvt(m, "INVENTAIRE", delta, avant, quantiteComptee,
                m.getNumeroLot(), m.getDatePeremption(),
                motif != null ? motif : "Inventaire physique",
                "inventaire", null, userLogin, null, false, null);
    }

    // ---------- Blocage lot (rappel sanitaire) ----------
    @Transactional
    public Medicament bloquerLot(String numeroLot, String motif) {
        if (numeroLot == null || numeroLot.isBlank()) {
            throw new InvalidArgumentException("Numéro de lot obligatoire");
        }
        List<Medicament> list = medicamentRepository.findAll().stream()
                .filter(m -> numeroLot.equalsIgnoreCase(m.getNumeroLot()))
                .collect(Collectors.toList());
        if (list.isEmpty()) throw new NotFoundException("Aucun médicament avec le lot " + numeroLot);
        for (Medicament m : list) {
            m.setStatut("BLOQUE");
            medicamentRepository.save(m);
            saveMvt(m, "BLOCAGE", 0, m.getStock(), m.getStock(),
                    numeroLot, m.getDatePeremption(), motif, "rappel", null, "system", null, false, null);
        }
        return list.get(0);
    }

    // ---------- Alertes ----------
    public List<Medicament> alertesStockFaible() {
        return medicamentRepository.findAll().stream()
                .filter(m -> !"BLOQUE".equals(m.getStatut()) && !"DESACTIVE".equals(m.getStatut()))
                .filter(Medicament::isStockFaible)
                .sorted(Comparator.comparing(Medicament::getStock))
                .collect(Collectors.toList());
    }

    public List<Medicament> alertesPeremption(int jours) {
        return medicamentRepository.findAll().stream()
                .filter(m -> m.getDatePeremption() != null)
                .filter(m -> m.isPerime() || m.isProchePeremption(jours))
                .sorted(Comparator.comparing(Medicament::getDatePeremption)) // FEFO
                .collect(Collectors.toList());
    }

    public List<Medicament> alertesSurstock() {
        return medicamentRepository.findAll().stream()
                .filter(Medicament::isSurstock)
                .sorted(Comparator.comparing(Medicament::getStock).reversed())
                .collect(Collectors.toList());
    }

    /** FEFO : médicaments triés par date de péremption croissante (les plus proches d'abord). */
    public List<Medicament> catalogueFefo(boolean onlyAvailable) {
        return medicamentRepository.findAll().stream()
                .filter(m -> !onlyAvailable || "DISPONIBLE".equals(m.getStatut()))
                .sorted(Comparator.comparing(Medicament::getDatePeremption,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public List<StockMouvement> historique(Long medicamentId) {
        if (medicamentId != null) {
            return mouvementRepository.findByMedicamentIdOrderByDateMouvementDesc(medicamentId);
        }
        return mouvementRepository.findTop50ByOrderByDateMouvementDesc();
    }

    // ---------- helpers ----------
    private Medicament getMed(long id) {
        return medicamentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Médicament introuvable: " + id));
    }

    private StockMouvement saveMvt(Medicament m, String type, int qte, int avant, int apres,
                                   String lot, LocalDate peremp, String motif, String sourceDest,
                                   Long patientId, String user, String service,
                                   boolean doubleCtrl, String controleur) {
        StockMouvement mv = new StockMouvement();
        mv.setMedicamentId(m.getId());
        mv.setTypeMouvement(type);
        mv.setQuantite(qte);
        mv.setStockAvant(avant);
        mv.setStockApres(apres);
        mv.setNumeroLot(lot);
        mv.setDatePeremption(peremp);
        mv.setMotif(motif);
        mv.setSourceDest(sourceDest);
        mv.setPatientId(patientId);
        mv.setUserLogin(user);
        mv.setServiceSoins(service);
        mv.setDoubleControle(doubleCtrl);
        mv.setControleur(controleur);
        mv.setDateMouvement(LocalDateTime.now());
        return mouvementRepository.save(mv);
    }
}
