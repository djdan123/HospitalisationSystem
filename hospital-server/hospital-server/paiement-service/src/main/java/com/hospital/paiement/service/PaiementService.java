package com.hospital.paiement.service;

import com.hospital.common.exception.NotFoundException;
import com.hospital.common.util.ValidationUtils;
import com.hospital.paiement.entity.Facture;
import com.hospital.paiement.repository.FactureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaiementService {
    private static final Logger log = LoggerFactory.getLogger(PaiementService.class);
    private final FactureRepository repository;

    public PaiementService(FactureRepository repository) { this.repository = repository; }

    public Facture createFacture(Long patientId, double montant, String description) {
        ValidationUtils.requirePositive(patientId, "patientId");
        ValidationUtils.requirePositive(montant, "montant");
        Facture f = new Facture();
        f.setPatientId(patientId);
        f.setMontantTotal(montant);
        f.setMontantPaye(0.0);
        f.setStatut("IMPAYEE");
        f.setNumeroFacture("FAC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        f.setDescription(description);
        Facture saved = repository.save(f);
        log.info("Facture créée: {}", saved.getNumeroFacture());
        return saved;
    }

    public Facture getFacture(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Facture introuvable: " + id));
    }

    public List<Facture> getByPatient(Long patientId) {
        return repository.findByPatientId(patientId);
    }

    public Facture makePayment(Long factureId, double montant, String mode) {
        Facture f = getFacture(factureId);
        ValidationUtils.requirePositive(montant, "montant");
        f.setMontantPaye(f.getMontantPaye() + montant);
        if (f.getMontantPaye() >= f.getMontantTotal()) {
            f.setStatut("PAYEE");
        } else {
            f.setStatut("PARTIELLEMENT_PAYEE");
        }
        log.info("Paiement {} sur facture {} mode={}", montant, factureId, mode);
        return repository.save(f);
    }
}
