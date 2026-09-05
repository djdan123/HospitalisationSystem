package com.hospital.hospitalisation.service;

import com.hospital.common.exception.FailedPreconditionException;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.util.ValidationUtils;
import com.hospital.hospitalisation.entity.Hospitalisation;
import com.hospital.hospitalisation.repository.HospitalisationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class HospitalisationService {

    private static final Logger log = LoggerFactory.getLogger(HospitalisationService.class);
    private final HospitalisationRepository repository;

    public HospitalisationService(HospitalisationRepository repository) {
        this.repository = repository;
    }

    public Hospitalisation admit(Long patientId, String motif, String observations, Long chambreId, Long litId) {
        ValidationUtils.requirePositive(patientId, "patientId");
        ValidationUtils.requireNonBlank(motif, "motif");

        Hospitalisation h = new Hospitalisation();
        h.setPatientId(patientId);
        h.setMotif(motif);
        h.setObservations(observations);
        h.setChambreId(chambreId);
        h.setLitId(litId);
        h.setStatut("EN_COURS");
        h.setDateAdmission(LocalDateTime.now());

        Hospitalisation saved = repository.save(h);
        log.info("Admission patient {} -> hospitalisation id={}", patientId, saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Hospitalisation getById(Long id) {
        ValidationUtils.requirePositive(id, "id");
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hospitalisation introuvable: " + id));
    }

    @Transactional(readOnly = true)
    public List<Hospitalisation> getAll(Long patientId, String statut) {
        if (patientId != null && patientId > 0) {
            if (statut != null && !statut.isBlank()) {
                return repository.findByPatientIdAndStatut(patientId, statut);
            }
            return repository.findByPatientId(patientId);
        }
        if (statut != null && !statut.isBlank()) {
            return repository.findByStatut(statut);
        }
        return repository.findAll();
    }

    public Hospitalisation assignRoom(Long hospitalisationId, Long chambreId, Long litId) {
        Hospitalisation h = getById(hospitalisationId);
        if (!"EN_COURS".equals(h.getStatut())) {
            throw new FailedPreconditionException("Impossible d'affecter une chambre: hospitalisation non en cours");
        }
        h.setChambreId(chambreId);
        h.setLitId(litId);
        return repository.save(h);
    }

    public Hospitalisation transfer(Long hospitalisationId, Long nouvelleChambreId, Long nouveauLitId, String motif) {
        Hospitalisation h = getById(hospitalisationId);
        if (!"EN_COURS".equals(h.getStatut())) {
            throw new FailedPreconditionException("Transfert impossible: hospitalisation non en cours");
        }
        h.setChambreId(nouvelleChambreId);
        h.setLitId(nouveauLitId);
        if (motif != null) {
            h.setObservations((h.getObservations() != null ? h.getObservations() + " | " : "") + "Transfert: " + motif);
        }
        log.info("Transfert hospitalisation {} vers chambre {}", hospitalisationId, nouvelleChambreId);
        return repository.save(h);
    }

    public Hospitalisation discharge(Long hospitalisationId, String observationsSortie) {
        Hospitalisation h = getById(hospitalisationId);
        if (!"EN_COURS".equals(h.getStatut())) {
            throw new FailedPreconditionException("Sortie impossible: hospitalisation non en cours");
        }
        h.setStatut("SORTI");
        h.setDateSortie(LocalDateTime.now());
        if (observationsSortie != null) {
            h.setObservations((h.getObservations() != null ? h.getObservations() + " | " : "") + observationsSortie);
        }
        log.info("Sortie patient hospitalisation id={}", hospitalisationId);
        return repository.save(h);
    }

    @Transactional(readOnly = true)
    public List<Hospitalisation> getHistory(Long patientId) {
        ValidationUtils.requirePositive(patientId, "patientId");
        return repository.findByPatientId(patientId);
    }
}
