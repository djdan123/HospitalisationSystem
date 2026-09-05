package com.hospital.consultation.service;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.util.ValidationUtils;
import com.hospital.consultation.entity.Consultation;
import com.hospital.consultation.repository.ConsultationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service @Transactional
public class ConsultationService {
    private static final Logger log = LoggerFactory.getLogger(ConsultationService.class);
    private final ConsultationRepository repo;
    public ConsultationService(ConsultationRepository repo) { this.repo = repo; }

    public Consultation create(Long patientId, Long medecinId, String date, String motif, String observations) {
        ValidationUtils.requirePositive(patientId, "patientId");
        Consultation c = new Consultation();
        c.setPatientId(patientId);
        c.setMedecinId(medecinId > 0 ? medecinId : null);
        c.setMotif(motif);
        c.setObservations(observations);
        c.setStatut("PLANIFIEE");
        if (date != null && !date.isBlank()) c.setDateConsultation(LocalDateTime.parse(date));
        Consultation saved = repo.save(c);
        log.info("Consultation créée id={} patient={}", saved.getId(), patientId);
        return saved;
    }
    public Consultation get(Long id) { return repo.findById(id).orElseThrow(() -> new NotFoundException("Consultation introuvable: " + id)); }
    public List<Consultation> byPatient(Long patientId) { return repo.findByPatientId(patientId); }
    public List<Consultation> byDoctor(Long medecinId) { return repo.findByMedecinId(medecinId); }
    public List<Consultation> all() { return repo.findAll(); }
    public Consultation update(Long id, String diagnostic, String observations, String prescription, String statut) {
        Consultation c = get(id);
        if (diagnostic != null) c.setDiagnostic(diagnostic);
        if (observations != null) c.setObservations(observations);
        if (prescription != null) c.setPrescription(prescription);
        if (statut != null) c.setStatut(statut);
        return repo.save(c);
    }
    public Consultation cancel(Long id, String motif) {
        Consultation c = get(id);
        c.setStatut("ANNULEE");
        if (motif != null) c.setObservations((c.getObservations() != null ? c.getObservations() + " | " : "") + "Annulation: " + motif);
        return repo.save(c);
    }
}
