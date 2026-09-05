package com.hospital.hospitalisation.repository;

import com.hospital.hospitalisation.entity.Hospitalisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalisationRepository extends JpaRepository<Hospitalisation, Long> {
    List<Hospitalisation> findByPatientId(Long patientId);
    List<Hospitalisation> findByPatientIdAndStatut(Long patientId, String statut);
    List<Hospitalisation> findByStatut(String statut);
}
