package com.hospital.maternite.service;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.util.ValidationUtils;
import com.hospital.maternite.entity.DossierMaternite;
import com.hospital.maternite.repository.DossierMaterniteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service @Transactional
public class MaterniteService {
    private static final Logger log = LoggerFactory.getLogger(MaterniteService.class);
    private final DossierMaterniteRepository repo;
    public MaterniteService(DossierMaterniteRepository repo) { this.repo = repo; }

    public DossierMaternite create(Long patientId, String ddr, int nbGrossesses, String groupe, String obs) {
        ValidationUtils.requirePositive(patientId, "patientId");
        DossierMaternite d = new DossierMaternite();
        d.setPatientId(patientId);
        if (ddr != null && !ddr.isBlank()) {
            LocalDate dateDdr = LocalDate.parse(ddr);
            d.setDateDernieresRegles(dateDdr);
            d.setDatePrevueAccouchement(dateDdr.plusDays(280));
        }
        d.setNombreGrossesses(nbGrossesses > 0 ? nbGrossesses : 1);
        d.setGroupeSanguin(groupe);
        d.setObservations(obs);
        d.setStatut("OUVERT");
        DossierMaternite saved = repo.save(d);
        log.info("Dossier maternité créé id={} patient={}", saved.getId(), patientId);
        return saved;
    }
    public DossierMaternite get(Long id) { return repo.findById(id).orElseThrow(() -> new NotFoundException("Dossier maternité introuvable: " + id)); }
    public List<DossierMaternite> byPatient(Long patientId) { return repo.findByPatientId(patientId); }
}
