package com.hospital.accueil.service;

import com.hospital.accueil.entity.Patient;
import com.hospital.accueil.repository.PatientRepository;
import com.hospital.common.exception.AlreadyExistsException;
import com.hospital.common.exception.InvalidArgumentException;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient create(String numeroDossier, String nom, String prenom, String dateNaissance,
                          String sexe, String telephone, String email, String adresse) {

        ValidationUtils.requireNonBlank(numeroDossier, "numeroDossier");
        ValidationUtils.requireNonBlank(nom, "nom");
        ValidationUtils.requireNonBlank(prenom, "prenom");
        LocalDate dn = ValidationUtils.parseDate(dateNaissance, "dateNaissance");
        ValidationUtils.validateSexe(sexe);
        ValidationUtils.validatePhone(telephone);
        ValidationUtils.validateEmail(email);

        if (patientRepository.existsByNumeroDossier(numeroDossier)) {
            throw new AlreadyExistsException("Un patient avec le numéro de dossier " + numeroDossier + " existe déjà");
        }

        Patient patient = new Patient();
        patient.setNumeroDossier(numeroDossier.trim());
        patient.setNom(nom.trim().toUpperCase());
        patient.setPrenom(prenom.trim());
        patient.setDateNaissance(dn);
        patient.setSexe(sexe.toUpperCase());
        patient.setTelephone(telephone != null ? telephone.trim() : null);
        patient.setEmail(email != null ? email.trim().toLowerCase() : null);
        patient.setAdresse(adresse != null ? adresse.trim() : null);
        patient.setStatut("ACTIF");

        Patient saved = patientRepository.save(patient);
        log.info("Patient créé: id={}, dossier={}", saved.getId(), saved.getNumeroDossier());
        return saved;
    }

    @Transactional(readOnly = true)
    public Patient getById(Long id) {
        ValidationUtils.requirePositive(id, "id");
        return patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient introuvable avec l'id: " + id));
    }

    @Transactional(readOnly = true)
    public Patient getByDossier(String numeroDossier) {
        ValidationUtils.requireNonBlank(numeroDossier, "numeroDossier");
        return patientRepository.findByNumeroDossier(numeroDossier.trim())
                .orElseThrow(() -> new NotFoundException("Patient introuvable avec le dossier: " + numeroDossier));
    }

    @Transactional(readOnly = true)
    public List<Patient> search(String nom, String prenom) {
        ValidationUtils.requireNonBlank(nom, "nom");
        return patientRepository.searchByNomAndPrenom(nom.trim(), prenom != null ? prenom.trim() : null);
    }

    @Transactional(readOnly = true)
    public List<Patient> getAll(boolean includeInactive) {
        return patientRepository.findAllWithOptionalInactive(includeInactive);
    }

    public Patient update(Long id, String nom, String prenom, String dateNaissance,
                          String sexe, String telephone, String email, String adresse, String statut) {
        Patient patient = getById(id);

        if (nom != null && !nom.isBlank()) {
            patient.setNom(nom.trim().toUpperCase());
        }
        if (prenom != null && !prenom.isBlank()) {
            patient.setPrenom(prenom.trim());
        }
        if (dateNaissance != null && !dateNaissance.isBlank()) {
            patient.setDateNaissance(ValidationUtils.parseDate(dateNaissance, "dateNaissance"));
        }
        if (sexe != null && !sexe.isBlank()) {
            ValidationUtils.validateSexe(sexe);
            patient.setSexe(sexe.toUpperCase());
        }
        if (telephone != null) {
            ValidationUtils.validatePhone(telephone);
            patient.setTelephone(telephone.isBlank() ? null : telephone.trim());
        }
        if (email != null) {
            ValidationUtils.validateEmail(email);
            patient.setEmail(email.isBlank() ? null : email.trim().toLowerCase());
        }
        if (adresse != null) {
            patient.setAdresse(adresse.isBlank() ? null : adresse.trim());
        }
        if (statut != null && !statut.isBlank()) {
            if (!statut.equalsIgnoreCase("ACTIF") && !statut.equalsIgnoreCase("INACTIF")) {
                throw new InvalidArgumentException("Statut doit être ACTIF ou INACTIF");
            }
            patient.setStatut(statut.toUpperCase());
        }

        Patient updated = patientRepository.save(patient);
        log.info("Patient mis à jour: id={}", updated.getId());
        return updated;
    }

    public void delete(Long id) {
        Patient patient = getById(id);
        patient.setStatut("INACTIF");
        patientRepository.save(patient);
        log.info("Patient désactivé: id={}", id);
    }

    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        if (id == null || id <= 0) {
            return false;
        }
        return patientRepository.existsById(id);
    }
}
