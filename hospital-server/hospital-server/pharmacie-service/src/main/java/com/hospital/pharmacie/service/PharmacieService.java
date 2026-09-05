package com.hospital.pharmacie.service;

import com.hospital.common.exception.FailedPreconditionException;
import com.hospital.common.exception.InvalidArgumentException;
import com.hospital.common.exception.NotFoundException;
import com.hospital.pharmacie.entity.Medicament;
import com.hospital.pharmacie.entity.Ordonnance;
import com.hospital.pharmacie.entity.OrdonnanceLigne;
import com.hospital.pharmacie.repository.MedicamentRepository;
import com.hospital.pharmacie.repository.OrdonnanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PharmacieService {

    private final MedicamentRepository medicamentRepository;
    private final OrdonnanceRepository ordonnanceRepository;

    public PharmacieService(MedicamentRepository medicamentRepository,
                            OrdonnanceRepository ordonnanceRepository) {
        this.medicamentRepository = medicamentRepository;
        this.ordonnanceRepository = ordonnanceRepository;
    }

    // ===================== MÉDICAMENTS =====================

    @Transactional
    public Medicament create(String code, String nom, String description, int stock, double prix, String unite) {
        if (code == null || code.isBlank()) throw new InvalidArgumentException("Code obligatoire");
        if (nom == null || nom.isBlank()) throw new InvalidArgumentException("Nom obligatoire");
        Medicament m = new Medicament();
        m.setCode(code.trim());
        m.setNom(nom.trim());
        m.setDescription(description);
        m.setStock(Math.max(0, stock));
        m.setPrixUnitaire(Math.max(0, prix));
        m.setUnite(unite != null ? unite : "unité");
        m.setStatut(stock > 0 ? "DISPONIBLE" : "RUPTURE");
        return medicamentRepository.save(m);
    }

    public Medicament get(long id) {
        return medicamentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Médicament introuvable: " + id));
    }

    public List<Medicament> getAll(boolean onlyAvailable) {
        if (onlyAvailable) return medicamentRepository.findByStatut("DISPONIBLE");
        return medicamentRepository.findAll();
    }

    @Transactional
    public Medicament updateStock(long medicamentId, int quantite, String motif) {
        Medicament m = get(medicamentId);
        int nouveau = m.getStock() + quantite;
        if (nouveau < 0) {
            throw new FailedPreconditionException(
                    "Stock insuffisant pour " + m.getNom() + " (disponible: " + m.getStock() + ")");
        }
        m.setStock(nouveau);
        m.setStatut(nouveau == 0 ? "RUPTURE" : "DISPONIBLE");
        return medicamentRepository.save(m);
    }

    @Transactional
    public Medicament dispense(long medicamentId, int quantite) {
        if (quantite <= 0) throw new InvalidArgumentException("Quantité doit être > 0");
        return updateStock(medicamentId, -quantite, "Délivrance");
    }

    // ===================== ORDONNANCES =====================

    @Transactional
    public Ordonnance createOrdonnance(long patientId, long medecinId, String typePatient,
                                       String serviceSoins, String observations,
                                       List<LigneInput> lignes) {
        if (patientId <= 0) throw new InvalidArgumentException("Patient ID invalide");
        String type = (typePatient == null || typePatient.isBlank()) ? "AMBULATOIRE" : typePatient.trim().toUpperCase();
        if (!type.equals("AMBULATOIRE") && !type.equals("HOSPITALISE")) {
            throw new InvalidArgumentException("Type doit être AMBULATOIRE ou HOSPITALISE");
        }

        Ordonnance o = new Ordonnance();
        o.setPatientId(patientId);
        o.setMedecinId(medecinId > 0 ? medecinId : null);
        o.setDateOrdonnance(LocalDateTime.now());
        o.setStatut("CREEE");
        o.setTypePatient(type);
        o.setServiceSoins(serviceSoins);
        o.setObservations(observations);

        if (lignes != null) {
            for (LigneInput li : lignes) {
                if (li.medicamentId <= 0 || li.quantite <= 0) {
                    throw new InvalidArgumentException("Ligne invalide (médicament / quantité)");
                }
                Medicament med = get(li.medicamentId);
                OrdonnanceLigne ligne = new OrdonnanceLigne();
                ligne.setMedicamentId(med.getId());
                ligne.setNomMedicament(med.getNom());
                ligne.setQuantite(li.quantite);
                ligne.setQuantiteDelivree(0);
                ligne.setPosologie(li.posologie);
                o.addLigne(ligne);
            }
        }

        return ordonnanceRepository.save(o);
    }

    public Ordonnance getOrdonnance(long id) {
        return ordonnanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ordonnance introuvable: " + id));
    }

    public List<Ordonnance> getOrdonnancesByPatient(long patientId) {
        return ordonnanceRepository.findByPatientIdOrderByDateOrdonnanceDesc(patientId);
    }

    public List<Ordonnance> getOrdonnancesEnAttente() {
        return ordonnanceRepository.findByStatutIn(List.of("CREEE", "PARTIELLE"));
    }

    /**
     * Délivre une quantité d'un médicament sur une ordonnance et décrémente le stock.
     */
    @Transactional
    public DispenseResult dispenseLigne(long ordonnanceId, long medicamentId, int quantite) {
        if (quantite <= 0) throw new InvalidArgumentException("Quantité doit être > 0");
        Ordonnance o = getOrdonnance(ordonnanceId);
        if ("ANNULEE".equals(o.getStatut()) || "DELIVREE".equals(o.getStatut())) {
            throw new FailedPreconditionException("Ordonnance non délivrable (statut: " + o.getStatut() + ")");
        }

        OrdonnanceLigne ligne = o.getLignes().stream()
                .filter(l -> l.getMedicamentId().equals(medicamentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Médicament " + medicamentId + " absent de l'ordonnance " + ordonnanceId));

        int reste = ligne.resteADelivrer();
        if (quantite > reste) {
            throw new FailedPreconditionException(
                    "Quantité demandée (" + quantite + ") > reste à délivrer (" + reste + ")");
        }

        Medicament m = updateStock(medicamentId, -quantite, "Délivrance ord. #" + ordonnanceId);
        ligne.setQuantiteDelivree(ligne.getQuantiteDelivree() + quantite);

        boolean allDone = o.getLignes().stream().allMatch(l -> l.resteADelivrer() == 0);
        boolean anyDone = o.getLignes().stream().anyMatch(l -> l.getQuantiteDelivree() > 0);
        o.setStatut(allDone ? "DELIVREE" : (anyDone ? "PARTIELLE" : "CREEE"));
        ordonnanceRepository.save(o);

        return new DispenseResult(true, "Délivrance OK", m.getStock(), o);
    }

    // ---------- DTOs internes ----------
    public static class LigneInput {
        public long medicamentId;
        public int quantite;
        public String posologie;
        public LigneInput(long medicamentId, int quantite, String posologie) {
            this.medicamentId = medicamentId;
            this.quantite = quantite;
            this.posologie = posologie;
        }
    }

    public static class DispenseResult {
        public final boolean success;
        public final String message;
        public final int stockRestant;
        public final Ordonnance ordonnance;
        public DispenseResult(boolean success, String message, int stockRestant, Ordonnance ordonnance) {
            this.success = success;
            this.message = message;
            this.stockRestant = stockRestant;
            this.ordonnance = ordonnance;
        }
    }
}
