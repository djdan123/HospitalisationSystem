package com.hospital.paiement.repository;
import com.hospital.paiement.entity.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {
    List<Facture> findByPatientId(Long patientId);
}
