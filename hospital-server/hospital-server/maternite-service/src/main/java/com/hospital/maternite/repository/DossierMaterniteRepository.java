package com.hospital.maternite.repository;
import com.hospital.maternite.entity.DossierMaternite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface DossierMaterniteRepository extends JpaRepository<DossierMaternite, Long> {
    List<DossierMaternite> findByPatientId(Long patientId);
}
