package com.hospital.laboratoire.service;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.util.ValidationUtils;
import com.hospital.laboratoire.entity.Analyse;
import com.hospital.laboratoire.repository.AnalyseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service @Transactional
public class LaboratoireService {
    private static final Logger log = LoggerFactory.getLogger(LaboratoireService.class);
    private final AnalyseRepository repo;
    public LaboratoireService(AnalyseRepository repo) { this.repo = repo; }
    public Analyse create(Long patientId, String type, String obs) {
        ValidationUtils.requirePositive(patientId, "patientId");
        ValidationUtils.requireNonBlank(type, "typeAnalyse");
        Analyse a = new Analyse();
        a.setPatientId(patientId); a.setTypeAnalyse(type); a.setObservations(obs); a.setStatut("DEMANDEE");
        Analyse saved = repo.save(a);
        log.info("Analyse créée id={} type={}", saved.getId(), type);
        return saved;
    }
    public Analyse get(Long id) { return repo.findById(id).orElseThrow(() -> new NotFoundException("Analyse introuvable: " + id)); }
    public List<Analyse> byPatient(Long patientId) { return repo.findByPatientId(patientId); }
    public Analyse update(Long id, String statut, String datePrelevement, String obs) {
        Analyse a = get(id);
        if (statut != null) a.setStatut(statut);
        if (datePrelevement != null && !datePrelevement.isBlank()) a.setDatePrelevement(LocalDateTime.parse(datePrelevement));
        if (obs != null) a.setObservations(obs);
        return repo.save(a);
    }
}
