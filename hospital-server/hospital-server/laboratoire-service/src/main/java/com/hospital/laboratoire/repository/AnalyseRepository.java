package com.hospital.laboratoire.repository;
import com.hospital.laboratoire.entity.Analyse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface AnalyseRepository extends JpaRepository<Analyse, Long> {
    List<Analyse> findByPatientId(Long patientId);
}
