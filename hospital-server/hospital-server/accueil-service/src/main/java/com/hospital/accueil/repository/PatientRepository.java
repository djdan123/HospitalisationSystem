package com.hospital.accueil.repository;

import com.hospital.accueil.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByNumeroDossier(String numeroDossier);

    boolean existsByNumeroDossier(String numeroDossier);

    @Query("SELECT p FROM Patient p WHERE LOWER(p.nom) LIKE LOWER(CONCAT('%', :nom, '%')) " +
           "AND (:prenom IS NULL OR LOWER(p.prenom) LIKE LOWER(CONCAT('%', :prenom, '%')))")
    List<Patient> searchByNomAndPrenom(@Param("nom") String nom, @Param("prenom") String prenom);

    List<Patient> findByStatut(String statut);

    @Query("SELECT p FROM Patient p WHERE p.statut = 'ACTIF' OR :includeInactive = true")
    List<Patient> findAllWithOptionalInactive(@Param("includeInactive") boolean includeInactive);
}
